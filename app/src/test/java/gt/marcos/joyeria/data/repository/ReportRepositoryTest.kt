package gt.marcos.joyeria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.reports.ReportDates
import gt.marcos.joyeria.domain.reports.ReportMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (D-012).
// Se prueba contra SaleRepository de verdad (no inserts a mano de `sale`)
// para que cada venta tenga sus snapshots armados como en producción --
// mismo criterio que SaleRepositoryTest.
@RunWith(AndroidJUnit4::class)
class ReportRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var reportRepository: ReportRepository
    private lateinit var saleRepository: SaleRepository

    private val october = ReportMonth(2026, 10)
    private val octoberStart = ReportDates.bounds(october).first
    private val novemberStart = ReportDates.bounds(ReportDates.next(october)).first

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        reportRepository = ReportRepository(saleDao = db.saleDao(), saleItemDao = db.saleItemDao(), productDao = db.productDao())
        saleRepository = SaleRepository(
            db = db,
            saleDao = db.saleDao(),
            saleItemDao = db.saleItemDao(),
            productDao = db.productDao(),
            customerDao = db.customerDao(),
            paymentDao = db.paymentDao(),
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    // --- Criterio 2: agregación mensual contra un dataset calculado a mano ---

    @Test
    fun observeMonthlyReport_withAFixedDataset_matchesTheHandCalculatedProfit() = runTest {
        // Dos piezas, dos ventas en octubre: (100-40)*2 + (200-150)*1 = 120 + 50 = Q1.70 -> 170 centavos... en centavos reales:
        // Pieza A: costo 4000, precio 10000, qty 2 -> ganancia 12000
        // Pieza B: costo 15000, precio 20000, qty 1 -> ganancia 5000
        // Total a mano: 17000 centavos (Q170.00)
        val productA = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        val productB = insertProduct(uid = "XP-B", stockQty = 5, costCents = 15000, salePriceCents = 20000)
        saleRepository.register(sale(productA, qty = 2, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))
        saleRepository.register(sale(productB, qty = 1, soldAt = octoberStart + 2 * ONE_DAY, unitPrice = Money(20000)))

        val report = reportRepository.observeMonthlyReport(october).first()

        assertThat(report.profit).isEqualTo(Money(17000))
    }

    // --- Criterio 3: solo snapshots de sale_item, nunca product en vivo ---

    @Test
    fun observeMonthlyReport_profitIgnoresProductChangesAfterTheSale() = runTest {
        val productId = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        saleRepository.register(sale(productId, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))
        val profitBefore = reportRepository.observeMonthlyReport(october).first().profit

        val product = checkNotNull(db.productDao().getById(productId))
        db.productDao().update(product.copy(costCents = 99999, salePriceCents = 199999))

        val profitAfter = reportRepository.observeMonthlyReport(october).first().profit
        assertThat(profitAfter).isEqualTo(profitBefore)
        assertThat(profitAfter).isEqualTo(Money(6000))
    }

    // --- Criterio 4: CANCELLED afuera de todos los reportes (dos tests, dos consultas distintas) ---

    @Test
    fun observeMonthlyReport_excludesCancelledSales() = runTest {
        val productId = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        val keep = saleRepository.register(sale(productId, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))
        val cancel = saleRepository.register(sale(productId, qty = 1, soldAt = octoberStart + 2 * ONE_DAY, unitPrice = Money(10000)))
        saleRepository.cancel(cancel.saleId, cancelledAt = octoberStart + 3 * ONE_DAY, cancelReason = null)

        val report = reportRepository.observeMonthlyReport(october).first()

        assertThat(report.profit).isEqualTo(Money(6000)) // solo la venta "keep"
        assertThat(keep.saleId).isNotEqualTo(cancel.saleId) // sanity: dos ventas distintas
    }

    @Test
    fun observeInventoryReport_recoveredCostExcludesCancelledSales() = runTest {
        val productId = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        saleRepository.register(sale(productId, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))
        val cancel = saleRepository.register(sale(productId, qty = 1, soldAt = octoberStart + 2 * ONE_DAY, unitPrice = Money(10000)))
        saleRepository.cancel(cancel.saleId, cancelledAt = octoberStart + 3 * ONE_DAY, cancelReason = null)

        val report = reportRepository.observeInventoryReport(now = novemberStart, staleStockDays = 90, lowStockThreshold = 2).first()

        // Solo el costo de la venta viva (4000), no el de la anulada.
        assertThat(report.capitalRecuperado).isEqualTo(Money(4000))
    }

    // --- Criterio 5: frontera de mes, hora de Guatemala -- a nivel de repositorio completo ---

    @Test
    fun observeMonthlyReport_saleAtLastInstantOfOctober_isIncludedInOctober() = runTest {
        val productId = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        saleRepository.register(sale(productId, qty = 1, soldAt = novemberStart - 1L, unitPrice = Money(10000)))

        val octoberReport = reportRepository.observeMonthlyReport(october).first()
        val novemberReport = reportRepository.observeMonthlyReport(ReportDates.next(october)).first()

        assertThat(octoberReport.profit).isEqualTo(Money(6000))
        assertThat(novemberReport.profit).isEqualTo(Money.ZERO)
    }

    @Test
    fun observeMonthlyReport_saleAtFirstInstantOfNovember_isIncludedInNovemberNotOctober() = runTest {
        val productId = insertProduct(uid = "XP-A", stockQty = 5, costCents = 4000, salePriceCents = 10000)
        saleRepository.register(sale(productId, qty = 1, soldAt = novemberStart, unitPrice = Money(10000)))

        val octoberReport = reportRepository.observeMonthlyReport(october).first()
        val novemberReport = reportRepository.observeMonthlyReport(ReportDates.next(october)).first()

        assertThat(octoberReport.profit).isEqualTo(Money.ZERO)
        assertThat(novemberReport.profit).isEqualTo(Money(6000))
    }

    // --- Top 5: una pieza vendida más veces que otra, ordenado por cantidad y por ganancia ---

    @Test
    fun observeProductSalesAggregate_topFiveByQtyAndByProfit_withAFixedDataset() = runTest {
        // A: 3 unidades, ganancia 3*(100-40)=180 -> 18000 centavos
        // B: 1 unidad, ganancia (500-100)=400 -> 40000 centavos
        // Por cantidad: A primero (3 > 1). Por ganancia: B primero (40000 > 18000).
        val productA = insertProduct(uid = "XP-A", stockQty = 10, costCents = 4000, salePriceCents = 10000)
        val productB = insertProduct(uid = "XP-B", stockQty = 10, costCents = 10000, salePriceCents = 50000)
        saleRepository.register(sale(productA, qty = 3, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))
        saleRepository.register(sale(productB, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(50000)))

        val report = reportRepository.observeMonthlyReport(october).first()

        assertThat(report.topByQty.first().productUid).isEqualTo("XP-A")
        assertThat(report.topByProfit.first().productUid).isEqualTo("XP-B")
    }

    // --- Pedido explícito del humano: marcar en pantalla la fila de top-5-por-ganancia
    //     que viene de una venta con descuento legado (D-047), no solo dejarlo en una nota ---

    @Test
    fun observeProductSalesAggregate_marksOnlyRowsFromADiscountedSale() = runTest {
        val productA = insertProduct(uid = "XP-A", stockQty = 10, costCents = 4000, salePriceCents = 10000)
        val productB = insertProduct(uid = "XP-B", stockQty = 10, costCents = 4000, salePriceCents = 10000)
        saleRepository.register(
            sale(productA, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000))
                .copy(discount = Money(500)),
        )
        saleRepository.register(sale(productB, qty = 1, soldAt = octoberStart + ONE_DAY, unitPrice = Money(10000)))

        val report = reportRepository.observeMonthlyReport(october).first()

        val rowA = report.topByProfit.single { it.productUid == "XP-A" }
        val rowB = report.topByProfit.single { it.productUid == "XP-B" }
        assertThat(rowA.possiblyInflatedByDiscount).isTrue()
        assertThat(rowB.possiblyInflatedByDiscount).isFalse()
    }

    // --- Capital estancado y bajo stock ---

    @Test
    fun observeInventoryReport_staleCapital_excludesProductsWithZeroStock() = runTest {
        val longUnsoldButEmpty = insertProduct(
            uid = "XP-A", stockQty = 0, costCents = 4000, salePriceCents = 10000,
            createdAt = octoberStart - 200 * ONE_DAY,
        )
        val longUnsoldWithStock = insertProduct(
            uid = "XP-B", stockQty = 3, costCents = 4000, salePriceCents = 10000,
            createdAt = octoberStart - 200 * ONE_DAY,
        )

        val report = reportRepository.observeInventoryReport(now = octoberStart, staleStockDays = 90, lowStockThreshold = 2).first()

        val staleUids = report.staleProducts.map { it.productUid }
        assertThat(staleUids).contains("XP-B")
        assertThat(staleUids).doesNotContain("XP-A")
        assertThat(longUnsoldButEmpty).isNotEqualTo(longUnsoldWithStock) // sanity
    }

    @Test
    fun observeInventoryReport_lowStock_includesProductsWithExactlyZeroStock() = runTest {
        insertProduct(uid = "XP-A", stockQty = 0, costCents = 4000, salePriceCents = 10000)

        val report = reportRepository.observeInventoryReport(now = octoberStart, staleStockDays = 90, lowStockThreshold = 2).first()

        assertThat(report.lowStockProducts.map { it.productUid }).contains("XP-A")
    }

    private suspend fun insertProduct(
        uid: String,
        stockQty: Int,
        costCents: Long,
        salePriceCents: Long,
        createdAt: Long = 0L,
    ): Long =
        db.productDao().insert(
            ProductEntity(
                uid = uid,
                name = "Pieza $uid",
                categoryId = null,
                costCents = costCents,
                salePriceCents = salePriceCents,
                stockQty = stockQty,
                photoPath = null,
                supplier = null,
                notes = null,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )

    private fun sale(productId: Long, qty: Int, soldAt: Long, unitPrice: Money) = RegisterSaleInput(
        soldAt = soldAt,
        discount = Money.ZERO,
        notes = null,
        lines = listOf(SaleLineInput(productId, qty, unitPrice)),
    )

    private companion object {
        const val ONE_DAY = 86_400_000L
    }
}
