package gt.marcos.joyeria.ui.barcode

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.BarcodeRepository
import gt.marcos.joyeria.domain.barcode.LabelCaption
import gt.marcos.joyeria.domain.barcode.LabelSheetSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla de etiquetas. Arma el PDF fuera del hilo
 * principal y deja el resultado en el estado; la pantalla lo imprime o lo comparte y
 * llama a [onOutputHandled].
 */
@HiltViewModel
class LabelsViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelsUiState())
    val uiState: StateFlow<LabelsUiState> = _uiState.asStateFlow()

    private val placeholderName = context.getString(R.string.add_product_default_name)

    init {
        viewModelScope.launch {
            repository.observeLabelPieces().collectLatest { pieces ->
                val items = pieces.map { LabelListItem(it, LabelCaption.of(it, placeholderName)) }
                _uiState.update { it.withItems(items) }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.withQuery(value) }
    fun onPieceToggled(uid: String) = _uiState.update { it.toggled(uid) }
    fun onSelectAllVisibleClick() = _uiState.update { it.withAllVisibleSelected() }
    fun onDeselectVisibleClick() = _uiState.update { it.withVisibleDeselected() }
    fun onDeselectAllClick() = _uiState.update { it.withNothingSelected() }

    fun onPrintClick() = build(LabelAction.PRINT)
    fun onShareClick() = build(LabelAction.SHARE)
    fun onTestSheetClick() = build(LabelAction.TEST_SHEET)

    fun onOutputHandled() = _uiState.update { it.copy(output = null) }

    private fun build(action: LabelAction) {
        val state = _uiState.value
        if (state.working) return
        if (action != LabelAction.TEST_SHEET && !state.canPrint) return
        val labels = state.selectedPieces.map { it.label }
        _uiState.update { it.copy(working = true, failed = false) }
        viewModelScope.launch {
            try {
                val output = withContext(Dispatchers.Default) { writePdf(action, labels) }
                _uiState.update { it.copy(working = false, output = output) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(working = false, failed = true) }
            }
        }
    }

    private fun writePdf(action: LabelAction, labels: List<gt.marcos.joyeria.domain.barcode.BarcodeLabel>): LabelOutputReady {
        val dir = LabelOutput.outputDir(context)
        dir.listFiles()?.forEach { it.delete() }
        // Nombre nuevo cada vez, como el catálogo: nada reusa un archivo viejo.
        val file = File(dir, "etiquetas-${System.currentTimeMillis()}.pdf")
        val spec = LabelSheetSpec()
        FileOutputStream(file).use { out ->
            if (action == LabelAction.TEST_SHEET) {
                LabelSheetPdf.writeTestSheet(
                    out,
                    LabelTestSheetTexts(
                        title = context.getString(R.string.labels_test_title),
                        ruler = context.getString(R.string.labels_test_ruler),
                        instructions = context.getString(R.string.labels_test_instructions),
                        moduleFormat = context.getString(R.string.labels_test_module),
                        moduleRealFormat = context.getString(R.string.labels_test_module_real),
                    ),
                    spec,
                )
            } else {
                LabelSheetPdf.write(labels, out, spec)
            }
        }
        val pages = if (action == LabelAction.TEST_SHEET) 1 else LabelSheetPdf.pageCount(labels.size, spec)
        return LabelOutputReady(file.absolutePath, pages, action)
    }
}
