package gt.marcos.joyeria.ui.reports

import gt.marcos.joyeria.data.repository.InventoryReport
import gt.marcos.joyeria.data.repository.MonthlyReport
import gt.marcos.joyeria.domain.reports.ReportMonth

/**
 * Estado único de la pantalla de reportes (CLAUDE.md sección 5). Dos
 * bloques, como en el plan (`ESTADO.md`, "Fase 11 -- Plan", punto 4): lo
 * que depende del mes seleccionado (`monthly`) y lo que es siempre "ahora
 * mismo" (`inventory`, no se mueve con el selector de mes).
 */
data class ReportsUiState(
    val currentMonth: ReportMonth? = null,
    val monthly: MonthlyReport? = null,
    val inventory: InventoryReport? = null,
) {
    /** "Mes siguiente" se deshabilita al llegar al mes real de hoy -- no tiene sentido mostrar un mes futuro vacío. */
    val canGoToNextMonth: Boolean
        get() {
            val current = currentMonth ?: return false
            val shown = monthly?.month ?: return false
            return shown < current
        }
}
