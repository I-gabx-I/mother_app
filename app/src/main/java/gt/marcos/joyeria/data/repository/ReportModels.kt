package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.reports.ReportMonth

/**
 * Una pieza dentro de un top 5 (Fase 11), por cantidad o por ganancia --
 * el mismo tipo sirve para las dos listas, `ReportsUiState` decide cuál
 * mostrar en cada bloque.
 *
 * `possiblyInflatedByDiscount`: `true` cuando alguna de las ventas que
 * componen `profit` tuvo `discount_cents > 0` (legado, D-047) -- ese
 * descuento era de toda la venta, no de esta línea (`ESQUEMA.md`), así
 * que `profit` puede verse un poco más alto que lo que esa venta dejó de
 * verdad. Solo afecta al orden/monto por ganancia, nunca a `qty`. Se
 * extingue solo: las ventas nuevas escriben descuento `0`.
 */
data class TopSellerSummary(
    val productUid: String,
    val productName: String,
    val qty: Int,
    val profit: Money,
    val possiblyInflatedByDiscount: Boolean,
)

/**
 * Reporte de un mes calendario, hora de Guatemala (`domain/reports
 * .ReportDates`). `profit` y `previousMonthProfit` salen de sumar
 * `PricingCalculator.saleProfit` sobre los snapshots de `sale` -- nunca
 * de `product` en vivo (criterio 3, FASES.md Fase 11). `CANCELLED` queda
 * afuera porque `SaleDao.observeBetween` ya lo excluye a nivel de SQL.
 */
data class MonthlyReport(
    val month: ReportMonth,
    val profit: Money,
    val previousMonthProfit: Money,
    val topByQty: List<TopSellerSummary>,
    val topByProfit: List<TopSellerSummary>,
) {
    /** Diferencia en quetzales contra el mes anterior -- nunca en porcentaje (ESTADO.md "Fase 11 -- Plan", punto 4). */
    val differenceFromPreviousMonth: Money get() = profit - previousMonthProfit
}

/** Una pieza sin venderse hace más de `stale_stock_days` (Fase 11). */
data class StaleProductSummary(
    val productUid: String,
    val productName: String,
    val daysSinceLastMovement: Int,
    val value: Money,
)

/** Una pieza con `stock_qty` en o por debajo de `low_stock_threshold` (Fase 11), incluido cero. */
data class LowStockProductSummary(
    val productUid: String,
    val productName: String,
    val stockQty: Int,
)

/**
 * Reporte de "ahora mismo" (Fase 11): no se mueve con el mes, siempre es
 * el estado actual del inventario.
 *
 * **`capitalInvertido` no es exacto al centavo (D-029, adenda en
 * `ESQUEMA.md`):** el redondeo hacia arriba del promedio ponderado tras
 * una compra sobreestima levemente el costo guardado, y ese sesgo se
 * arrastra a este total. Es sistemático hacia arriba (nunca lo
 * subestima) y en la práctica son centavos. **A propósito no se expone
 * en la UI** (ver `ESTADO.md`, "Fase 11 -- Plan", punto 2): ella no tiene
 * ninguna acción que tomar con ese aviso, y mostrárselo solo le resta
 * confianza a un número que casi siempre es exacto. Quien toque este
 * campo después sí tiene que saberlo -- por eso queda acá, no solo en
 * `ESQUEMA.md`.
 */
data class InventoryReport(
    val capitalInvertido: Money,
    val capitalRecuperado: Money,
    val staleProducts: List<StaleProductSummary>,
    val lowStockProducts: List<LowStockProductSummary>,
) {
    val staleTotal: Money get() = staleProducts.fold(Money.ZERO) { acc, product -> acc + product.value }
}
