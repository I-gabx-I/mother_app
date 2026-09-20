package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.dao.PriceHistoryDao
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.PurchaseDao
import gt.marcos.joyeria.data.local.dao.PurchaseItemDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.entity.PriceHistoryEntity
import gt.marcos.joyeria.data.local.entity.PurchaseEntity
import gt.marcos.joyeria.data.local.entity.PurchaseItemEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import javax.inject.Inject

/**
 * Registra una compra completa en una sola transacción (FASES.md Fase 05,
 * criterio 6): inserta `purchase`/`purchase_item`, recalcula
 * `cost_cents`/`stock_qty` de cada producto (D-029), e inserta la fila de
 * `price_history` correspondiente (D-033). Nunca toca `sale_price_cents`.
 *
 * Igual que `ProductRepository.update()`, la regla de negocio (prorrateo,
 * promedio ponderado, los dos avisos de D-031, el aviso de D-032) vive acá
 * mismo, no en un caso de uso aparte -- `RegisterPurchaseUseCase` es un
 * paso directo a este método, mismo patrón que `AddProductUseCase`.
 */
class PurchaseRepository @Inject constructor(
    private val db: AppDatabase,
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val saleDao: SaleDao,
    private val appSettingRepository: AppSettingRepository,
) {
    suspend fun register(input: RegisterPurchaseInput): RegisterPurchaseResult {
        val minMarginBp = appSettingRepository.getMinMarginBp()
        val lineWarnings = mutableListOf<PurchaseLineWarning>()
        var retroactiveWarning = false

        val purchaseId = db.withTransaction {
            val purchaseId = purchaseDao.insert(
                PurchaseEntity(
                    purchasedAt = input.purchasedAt,
                    supplier = input.supplier,
                    extraCostCents = input.extraCost.cents,
                    notes = input.notes,
                ),
            )

            val allocatedExtra = PricingCalculator.allocateExtraCost(
                lines = input.lines.map { it.qty to it.unitCost },
                extraCents = input.extraCost.cents,
            )

            val now = System.currentTimeMillis()
            val purchaseItems = mutableListOf<PurchaseItemEntity>()

            input.lines.forEachIndexed { index, line ->
                val product = checkNotNull(productDao.getById(line.productId)) {
                    "Producto ${line.productId} no existe -- no se puede registrar una compra de algo que no está"
                }
                val allocated = allocatedExtra[index]
                val lineTotalCost = Money(line.qty.toLong() * line.unitCost.cents + allocated)
                val currentStock = product.stockQty
                val cycleStart = currentStock == 0
                val newCost = PricingCalculator.weightedAverageCost(
                    currentStockQty = currentStock,
                    currentCost = Money(product.costCents),
                    purchaseQty = line.qty,
                    lineTotalCost = lineTotalCost,
                )

                purchaseItems += PurchaseItemEntity(
                    purchaseId = purchaseId,
                    productId = line.productId,
                    qty = line.qty,
                    unitCostCents = line.unitCost.cents,
                    allocatedExtraCents = allocated,
                )

                productDao.update(
                    product.copy(
                        costCents = newCost.cents,
                        stockQty = currentStock + line.qty,
                        updatedAt = now,
                    ),
                )

                priceHistoryDao.insert(
                    PriceHistoryEntity(
                        productId = line.productId,
                        costCents = newCost.cents,
                        salePriceCents = product.salePriceCents,
                        changedAt = now,
                        purchaseId = purchaseId,
                        cycleStart = cycleStart,
                    ),
                )

                // D-031: dos avisos, no uno -- ALERTA (ganancia <= 0) es
                // más grave que ADVERTENCIA (margen bajo el piso), nunca
                // las dos a la vez para la misma línea.
                val salePrice = Money(product.salePriceCents)
                val profit = PricingCalculator.profit(newCost, salePrice)
                val margin = PricingCalculator.marginOnSale(newCost, salePrice)
                val level = when {
                    profit <= Money.ZERO -> PurchaseWarningLevel.PROFIT_ALERT
                    margin != null && margin < minMarginBp -> PurchaseWarningLevel.MARGIN_WARNING
                    else -> PurchaseWarningLevel.NONE
                }
                if (level != PurchaseWarningLevel.NONE) {
                    lineWarnings += PurchaseLineWarning(line.productId, product.name, newCost, level)
                }

                // D-032: informativo, nunca bloquea ni cambia el cálculo de
                // arriba -- purchasedAt ya se usó solo para guardar
                // `purchase.purchased_at`, no para decidir nada del costo.
                val lastSaleDate = saleDao.getLastSaleDate(line.productId)
                if (lastSaleDate != null && input.purchasedAt < lastSaleDate) {
                    retroactiveWarning = true
                }
            }

            purchaseItemDao.insertAll(purchaseItems)
            purchaseId
        }

        return RegisterPurchaseResult(purchaseId, lineWarnings, retroactiveWarning)
    }
}
