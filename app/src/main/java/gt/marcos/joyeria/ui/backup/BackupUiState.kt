package gt.marcos.joyeria.ui.backup

import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupReminder

/** Qué está haciendo la pantalla ahora mismo; mientras haya algo, los botones se bloquean. */
enum class BackupAction { EXPORT, CSV_INVENTORY, CSV_SALES, READING, IMPORT, UNDO }

/** Lo que ella ve antes de confirmar una recuperación: qué trae el archivo y qué hay hoy en el teléfono. */
data class BackupSummary(
    val exportedAt: Long,
    val products: Int,
    val sales: Int,
    val customers: Int,
    val photos: Int,
    val currentProducts: Int,
    val currentSales: Int,
    val currentCustomers: Int,
    /** Hay algo en el teléfono más nuevo que el archivo: recuperar lo borraría. */
    val backupIsOlder: Boolean,
) {
    val extraProducts: Int get() = (currentProducts - products).coerceAtLeast(0)
    val extraSales: Int get() = (currentSales - sales).coerceAtLeast(0)
    val extraCustomers: Int get() = (currentCustomers - customers).coerceAtLeast(0)
}

sealed interface BackupDialog {
    /** Primera confirmación: «esto reemplaza todo lo del teléfono». */
    data class ConfirmImport(val summary: BackupSummary) : BackupDialog

    /** Segunda confirmación, distinta, solo cuando el archivo es más viejo que lo que hay en el teléfono. */
    data class ConfirmOlder(val summary: BackupSummary) : BackupDialog

    data class ConfirmUndo(val createdAt: Long) : BackupDialog
}

/** Un archivo listo para pasarlo al selector de Android (WhatsApp, Drive, correo...). */
data class BackupShare(val path: String, val mime: String)

/** El resultado que se le deja a la vista hasta que lo cierre. */
sealed interface BackupMessage {
    data class Exported(val products: Int, val sales: Int, val photos: Int) : BackupMessage
    data class SpreadsheetReady(val sales: Boolean) : BackupMessage
    data class Imported(val products: Int, val sales: Int, val customers: Int) : BackupMessage
    data object Undone : BackupMessage
    data class Failed(val failure: BackupFailure?) : BackupMessage
}

data class BackupUiState(
    val lastBackupAt: Long? = null,
    val reminder: BackupReminder.Due? = null,
    val working: BackupAction? = null,
    val dialog: BackupDialog? = null,
    val share: BackupShare? = null,
    val message: BackupMessage? = null,
    /** Cuándo se hizo la copia que «Deshacer» devolvería, o `null` si no hay nada que deshacer. */
    val undoAvailableAt: Long? = null,
) {
    val busy: Boolean get() = working != null
}
