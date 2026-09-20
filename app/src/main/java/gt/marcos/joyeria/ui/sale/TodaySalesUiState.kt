package gt.marcos.joyeria.ui.sale

import gt.marcos.joyeria.data.repository.SaleDetail
import gt.marcos.joyeria.data.repository.SaleSummary
import gt.marcos.joyeria.domain.model.Money

/** Estado único de la pantalla "Ventas de hoy" (CLAUDE.md sección 5). */
data class TodaySalesUiState(
    val sales: List<SaleSummary> = emptyList(),
    val selectedSale: SaleDetail? = null,
    val showCancelConfirmation: Boolean = false,
    val cancelReasonText: String = "",
) {
    /** Lo que ella cobró de verdad hoy (suma de `total - descuento` de cada venta). */
    val totalNet: Money get() = sales.fold(Money.ZERO) { acc, sale -> acc + sale.net }
    val totalProfit: Money get() = sales.fold(Money.ZERO) { acc, sale -> acc + sale.profit }
}
