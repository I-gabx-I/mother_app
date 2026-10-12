package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.ProductInventorySnapshotRow
import gt.marcos.joyeria.data.local.dao.ProductSalesAggregateRow
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.dao.SaleItemDao
import gt.marcos.joyeria.data.local.entity.SaleEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.domain.reports.ReportDates
import gt.marcos.joyeria.domain.reports.ReportMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Reportes (Fase 11): de solo lectura, nunca escribe nada. Inyecta DAOs de
 * varias tablas directo, sin pasar por otros repositorios -- mismo
 * criterio que `SaleRepository`, porque las consultas ya cruzan tablas por
 * su cuenta.
 *
 * Ninguna función de acá lee el reloj (D-055): `now`/`month` llegan de
 * quien llama (el `ViewModel`, que sí sabe qué hora es "ahora").
 */
class ReportRepository @Inject constructor(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao,
) {
    /**
     * `profit`/`previousMonthProfit` suman `PricingCalculator.saleProfit`
     * sobre los snapshots de `sale` de cada mes (criterio 3, FASES.md Fase
     * 11) -- `SaleDao.observeBetween` ya excluye `CANCELLED` (criterio 4).
     * `topByQty`/`topByProfit` salen de la misma consulta agregada,
     * ordenada distinto cada vez (ver `ESTADO.md`, "Fase 11 -- Plan",
     * sección 6).
     */
    fun observeMonthlyReport(month: ReportMonth): Flow<MonthlyReport> {
        val (start, end) = ReportDates.bounds(month)
        val (previousStart, previousEnd) = ReportDates.bounds(ReportDates.previous(month))
        return combine(
            saleDao.observeBetween(start, end),
            saleDao.observeBetween(previousStart, previousEnd),
            saleItemDao.observeProductSalesAggregate(start, end),
        ) { currentSales, previousSales, aggregateRows ->
            MonthlyReport(
                month = month,
                profit = currentSales.sumProfit(),
                previousMonthProfit = previousSales.sumProfit(),
                topByQty = aggregateRows.sortedByDescending { it.totalQty }.take(5).map { it.toTopSeller() },
                topByProfit = aggregateRows.sortedByDescending { it.totalProfitCents }.take(5).map { it.toTopSeller() },
            )
        }
    }

    /**
     * `staleStockDays`/`lowStockThreshold` llegan ya leídos por quien
     * llama (`AppSettingRepository`, Fase 03) -- no son reactivos acá
     * adentro: ninguna pantalla hoy los cambia, y leerlos cada vez que
     * esta función se invoca (una por apertura de pantalla) alcanza.
     */
    fun observeInventoryReport(now: Long, staleStockDays: Int, lowStockThreshold: Int): Flow<InventoryReport> =
        combine(
            productDao.observeInventorySnapshot(),
            saleDao.observeTotalRecoveredCost(),
        ) { snapshot, recoveredCostCents ->
            val staleProducts = snapshot
                .filter { it.stockQty > 0 } // sin stock no hay capital guardado que contar (ESTADO.md "Fase 11 -- Plan", sección 6)
                .mapNotNull { row -> row.toStaleProductOrNull(now, staleStockDays) }
                .sortedByDescending { it.daysSinceLastMovement }
            InventoryReport(
                capitalInvertido = snapshot.fold(Money.ZERO) { acc, row -> acc + Money(row.costCents * row.stockQty) },
                capitalRecuperado = Money(recoveredCostCents),
                staleProducts = staleProducts,
                // Bajo stock incluye el cero: quedarse sin piezas es la alerta
                // más urgente, no una que se filtra por llegar justo a cero.
                lowStockProducts = snapshot
                    .filter { it.stockQty <= lowStockThreshold }
                    .sortedBy { it.stockQty }
                    .map { LowStockProductSummary(it.uid, it.name, it.stockQty) },
            )
        }

    private fun List<SaleEntity>.sumProfit(): Money =
        fold(Money.ZERO) { acc, sale ->
            acc + PricingCalculator.saleProfit(Money(sale.totalCents), Money(sale.discountCents), Money(sale.totalCostCents))
        }

    private fun ProductSalesAggregateRow.toTopSeller() = TopSellerSummary(
        productUid = productUid,
        productName = productName,
        qty = totalQty,
        profit = Money(totalProfitCents),
        possiblyInflatedByDiscount = discountedLineCount > 0,
    )

    private fun ProductInventorySnapshotRow.toStaleProductOrNull(now: Long, staleStockDays: Int): StaleProductSummary? {
        val referenceDate = lastSaleAt ?: createdAt
        val daysSinceLastMovement = TimeUnit.MILLISECONDS.toDays(now - referenceDate).toInt()
        if (daysSinceLastMovement <= staleStockDays) return null
        return StaleProductSummary(
            productUid = uid,
            productName = name,
            daysSinceLastMovement = daysSinceLastMovement,
            value = Money(costCents * stockQty),
        )
    }
}
