package gt.marcos.joyeria.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.backup.BackupExporter
import gt.marcos.joyeria.data.backup.BackupImporter
import gt.marcos.joyeria.data.backup.BackupPaths
import gt.marcos.joyeria.data.backup.CsvExporter
import gt.marcos.joyeria.data.backup.CsvTexts
import gt.marcos.joyeria.data.backup.ImportPreview
import gt.marcos.joyeria.data.repository.BackupStatusRepository
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupReminder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla de respaldo (Fase 14). Todo lo pesado (armar el archivo, validar uno
 * ajeno, reemplazar la base) corre fuera del hilo principal; esta clase solo decide el orden y qué le muestra a ella.
 *
 * El respaldo se marca como hecho cuando se abre el selector de Android ([onShareHandled]): no hay forma de saber si
 * ella llegó a mandarlo, y esa es la única señal honesta que hay.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val csv: CsvExporter,
    private val paths: BackupPaths,
    private val status: BackupStatusRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** El archivo ya leído y validado que espera la confirmación; no se guarda en el estado (lleva un `File`). */
    private var pendingPreview: ImportPreview? = null
    private var pendingMarksBackup = false

    init {
        // Lo que quedó de una sesión anterior (un archivo elegido que nunca se confirmó) ya no sirve.
        paths.importTmpDir.listFiles()?.forEach { it.deleteRecursively() }
        viewModelScope.launch {
            importer.discardExpiredSafetyCopies(System.currentTimeMillis())
            refreshUndo()
        }
        viewModelScope.launch {
            status.observeReminderInputs().collect { inputs ->
                val now = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        lastBackupAt = inputs.lastBackupAt,
                        reminder = BackupReminder.due(inputs.lastBackupAt, inputs.firstDataAt, now),
                    )
                }
            }
        }
    }

    fun onExportClick() {
        if (_uiState.value.busy) return
        run(BackupAction.EXPORT) {
            val now = System.currentTimeMillis()
            clearOutputs()
            val result = exporter.export(File(paths.exportDir, "respaldo-joyeria-${day(now)}.zip"), now)
            pendingMarksBackup = true
            val rows = result.manifest.rowCounts
            _uiState.update {
                it.copy(
                    share = BackupShare(result.file.absolutePath, "application/zip"),
                    message = BackupMessage.Exported(rows["product"] ?: 0, rows["sale"] ?: 0, result.manifest.photos.size),
                )
            }
        }
    }

    fun onSpreadsheetClick(sales: Boolean) {
        if (_uiState.value.busy) return
        run(if (sales) BackupAction.CSV_SALES else BackupAction.CSV_INVENTORY) {
            val now = System.currentTimeMillis()
            clearOutputs()
            val texts = csvTexts()
            val file = if (sales) {
                csv.exportSales(texts, File(paths.exportDir, "ventas-${day(now)}.csv"))
            } else {
                csv.exportInventory(texts, File(paths.exportDir, "inventario-${day(now)}.csv"))
            }
            _uiState.update {
                it.copy(share = BackupShare(file.absolutePath, "text/csv"), message = BackupMessage.SpreadsheetReady(sales))
            }
        }
    }

    /** El selector de archivos devolvió algo (`null` si ella lo cerró sin elegir). */
    fun onFilePicked(uri: Uri?) {
        if (uri == null || _uiState.value.busy) return
        run(BackupAction.READING) {
            val now = System.currentTimeMillis()
            val file = importer.copyToTemp(uri, now)
            val preview = try {
                importer.prepare(file)
            } catch (error: Throwable) {
                importer.discardTemp(file)
                throw error
            }
            pendingPreview = preview
            _uiState.update { it.copy(dialog = BackupDialog.ConfirmImport(summaryOf(preview))) }
        }
    }

    fun onImportConfirm() {
        val dialog = _uiState.value.dialog as? BackupDialog.ConfirmImport ?: return
        if (dialog.summary.backupIsOlder) {
            // Más viejo que lo que hay: una segunda confirmación, distinta, con lo que se perdería.
            _uiState.update { it.copy(dialog = BackupDialog.ConfirmOlder(dialog.summary)) }
        } else {
            runImport()
        }
    }

    fun onOlderConfirm() {
        if (_uiState.value.dialog is BackupDialog.ConfirmOlder) runImport()
    }

    fun onUndoClick() {
        val at = _uiState.value.undoAvailableAt ?: return
        _uiState.update { it.copy(dialog = BackupDialog.ConfirmUndo(at)) }
    }

    fun onUndoConfirm() {
        if (_uiState.value.dialog !is BackupDialog.ConfirmUndo) return
        _uiState.update { it.copy(dialog = null) }
        run(BackupAction.UNDO) {
            importer.undo()
            refreshUndo()
            _uiState.update { it.copy(message = BackupMessage.Undone) }
        }
    }

    fun onDialogDismiss() {
        pendingPreview?.let { importer.discardTemp(it.backup.file) }
        pendingPreview = null
        _uiState.update { it.copy(dialog = null) }
    }

    fun onMessageDismiss() = _uiState.update { it.copy(message = null) }

    /** La pantalla ya abrió el selector de Android con el archivo. */
    fun onShareHandled() {
        val marks = pendingMarksBackup
        pendingMarksBackup = false
        _uiState.update { it.copy(share = null) }
        if (marks) {
            viewModelScope.launch {
                status.markBackupDone(System.currentTimeMillis())
                // Con un respaldo nuevo la copia previa a una importación ya no hace falta.
                importer.discardSafetyCopies()
                refreshUndo()
            }
        }
    }

    private fun runImport() {
        val preview = pendingPreview ?: return
        _uiState.update { it.copy(dialog = null) }
        run(BackupAction.IMPORT) {
            importer.import(preview, System.currentTimeMillis())
            pendingPreview = null
            refreshUndo()
            _uiState.update {
                it.copy(
                    message = BackupMessage.Imported(
                        preview.backupCounts["product"] ?: 0,
                        preview.backupCounts["sale"] ?: 0,
                        preview.backupCounts["customer"] ?: 0,
                    ),
                )
            }
        }
    }

    /** Corre [block] marcando que hay trabajo en curso y convierte cualquier fallo en un mensaje para ella. */
    private fun run(action: BackupAction, block: suspend () -> Unit) {
        _uiState.update { it.copy(working = action, message = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupException) {
                _uiState.update { it.copy(message = BackupMessage.Failed(error.failure)) }
            } catch (error: Exception) {
                _uiState.update { it.copy(message = BackupMessage.Failed(null)) }
            } finally {
                _uiState.update { it.copy(working = null) }
            }
        }
    }

    private suspend fun refreshUndo() {
        val at = importer.latestSafetyCopy()?.createdAt
        _uiState.update { it.copy(undoAvailableAt = at) }
    }

    /** Antes de armar un archivo nuevo se borran los de antes: no se acumulan respaldos viejos en la caché. */
    private fun clearOutputs() {
        paths.exportDir.listFiles()?.forEach { it.delete() }
    }

    private fun summaryOf(preview: ImportPreview) = BackupSummary(
        exportedAt = preview.backup.manifest.exportedAt,
        products = preview.backupCounts["product"] ?: 0,
        sales = preview.backupCounts["sale"] ?: 0,
        customers = preview.backupCounts["customer"] ?: 0,
        photos = preview.backup.manifest.photos.size,
        currentProducts = preview.currentCounts["product"] ?: 0,
        currentSales = preview.currentCounts["sale"] ?: 0,
        currentCustomers = preview.currentCounts["customer"] ?: 0,
        backupIsOlder = preview.backupIsOlder,
    )

    private fun csvTexts() = CsvTexts(
        inventoryHeaders = context.resources.getStringArray(R.array.csv_inventory_headers).toList(),
        salesHeaders = context.resources.getStringArray(R.array.csv_sales_headers).toList(),
        noCategory = context.getString(R.string.csv_no_category),
        yes = context.getString(R.string.csv_yes),
        no = context.getString(R.string.csv_no),
        typeCash = context.getString(R.string.csv_type_cash),
        typeCredit = context.getString(R.string.csv_type_credit),
        statusPaid = context.getString(R.string.csv_status_paid),
        statusPending = context.getString(R.string.csv_status_pending),
        statusCancelled = context.getString(R.string.csv_status_cancelled),
    )

    // El nombre del archivo lo va a leer ella en WhatsApp o en Drive: una fecha estable, no la del idioma del teléfono.
    private fun day(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(millis))
}
