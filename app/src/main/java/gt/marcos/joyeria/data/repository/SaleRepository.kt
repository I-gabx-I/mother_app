package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.dao.SaleItemDao
import gt.marcos.joyeria.data.local.entity.SaleEntity
import gt.marcos.joyeria.data.local.entity.SaleItemEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Registra una venta al contado y la anula (Fase 06). Los snapshots de
 * `sale_item` (`product_uid_snapshot`/`product_name_snapshot`/
 * `unit_price_cents`/`unit_cost_cents`) se toman leyendo cada producto
 * **fresco desde `ProductDao`, dentro de la misma transacción** que
 * inserta la venta -- nunca desde un valor que ya venía cacheado en el
 * `ViewModel` -- para que nada escrito después pueda cambiar lo que
 * quedó guardado (D-002). Mismo patrón que `PurchaseRepository`
 * (Fase 05) ya usa para leer el costo/stock real antes de recalcular.
 */
class SaleRepository @Inject constructor(
    private val db: AppDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao,
) {
    suspend fun register(input: RegisterSaleInput): RegisterSaleResult = db.withTransaction {
        // D-036: dos líneas del mismo producto leerían y escribirían la
        // misma fila de `product` por separado dentro de esta transacción
        // -- el segundo `productDao.update` pisaría al primero en vez de
        // sumarse, vendiendo de más sin que el chequeo de stock lo note
        // (los dos checks leen el mismo stockQty original). Se rechaza en
        // vez de fusionar en silencio: el carrito ya agrupa por producto,
        // así que un duplicado acá es un error del llamador, no un caso de
        // uso real a resolver.
        val duplicateProductIds = input.lines
            .groupingBy { it.productId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateProductIds.isEmpty()) {
            "La venta trae el mismo producto repetido en más de una línea " +
                "($duplicateProductIds) -- agrupá la cantidad en una sola línea " +
                "por producto antes de vender."
        }

        val items = input.lines.map { line ->
            val product = checkNotNull(productDao.getById(line.productId)) {
                "Producto ${line.productId} no existe -- no se puede vender algo que no está"
            }
            check(line.qty <= product.stockQty) {
                "No hay suficiente stock de ${product.uid}: pidió ${line.qty}, hay ${product.stockQty}"
            }
            product to line.qty
        }

        val totalCents = items.sumOf { (product, qty) -> product.salePriceCents * qty }
        val totalCostCents = items.sumOf { (product, qty) -> product.costCents * qty }

        val saleId = saleDao.insert(
            SaleEntity(
                soldAt = input.soldAt,
                customerId = null,
                type = "CASH",
                status = "PAID",
                totalCents = totalCents,
                totalCostCents = totalCostCents,
                discountCents = input.discount.cents,
                notes = input.notes,
                cancelledAt = null,
                cancelReason = null,
            ),
        )

        saleItemDao.insertAll(
            items.map { (product, qty) ->
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productUidSnapshot = product.uid,
                    productNameSnapshot = product.name,
                    qty = qty,
                    unitPriceCents = product.salePriceCents,
                    unitCostCents = product.costCents,
                )
            },
        )

        items.forEach { (product, qty) ->
            productDao.update(product.copy(stockQty = product.stockQty - qty))
        }

        RegisterSaleResult(saleId)
    }

    /**
     * Anula la venta y devuelve exactamente el stock que había descontado
     * (D-006: nunca se borra, siempre con motivo y fecha). `cancelledAt` se
     * recibe como parámetro -- igual que `soldAt` en `register()` -- para
     * que el repositorio sea determinístico y testeable sin depender del
     * reloj real; quien llama (el `ViewModel`) es quien decide "ahora".
     */
    suspend fun cancel(saleId: Long, cancelledAt: Long, cancelReason: String?): Unit = db.withTransaction {
        val sale = checkNotNull(saleDao.getById(saleId)) { "Venta $saleId no existe" }
        check(sale.status != "CANCELLED") { "La venta $saleId ya estaba anulada" }

        saleItemDao.getForSale(saleId).forEach { item ->
            val productId = item.productId ?: return@forEach
            productDao.getById(productId)?.let { product ->
                productDao.update(product.copy(stockQty = product.stockQty + item.qty))
            }
        }

        saleDao.updateStatus(
            id = saleId,
            status = "CANCELLED",
            cancelledAt = cancelledAt,
            cancelReason = cancelReason,
        )
    }

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SaleSummary>> =
        saleDao.observeBetween(startMillis, endMillis).map { sales -> sales.map { it.toSummary() } }

    suspend fun getDetail(saleId: Long): SaleDetail? {
        val sale = saleDao.getById(saleId) ?: return null
        val items = saleItemDao.getForSale(saleId)
        return SaleDetail(
            id = sale.id,
            soldAt = sale.soldAt,
            total = Money(sale.totalCents),
            discount = Money(sale.discountCents),
            profit = sale.toProfit(),
            items = items.map { item ->
                SaleItemDetail(
                    productUidSnapshot = item.productUidSnapshot,
                    productNameSnapshot = item.productNameSnapshot,
                    qty = item.qty,
                    unitPrice = Money(item.unitPriceCents),
                    unitCost = Money(item.unitCostCents),
                )
            },
        )
    }

    private fun SaleEntity.toProfit(): Money =
        PricingCalculator.saleProfit(Money(totalCents), Money(discountCents), Money(totalCostCents))

    private fun SaleEntity.toSummary() = SaleSummary(
        id = id,
        soldAt = soldAt,
        total = Money(totalCents),
        discount = Money(discountCents),
        profit = toProfit(),
    )
}
