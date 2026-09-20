package gt.marcos.joyeria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.model.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (D-012). Se
// prueba a nivel de SaleRepository, no solo del DAO, porque ahí es donde
// vive la transacción completa que exige FASES.md Fase 06 (snapshot,
// descuento de stock, atomicidad) -- probar solo el DAO no ejercitaría
// nada de esa orquestación.
@RunWith(AndroidJUnit4::class)
class SaleRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: SaleRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        repository = SaleRepository(
            db = db,
            saleDao = db.saleDao(),
            saleItemDao = db.saleItemDao(),
            productDao = db.productDao(),
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    // --- Fase 06 criterio 2 / D-002: el snapshot no cambia si el producto cambia después ---

    @Test
    fun register_snapshotSurvivesProductPriceChange_saleProfitDoesNotChange() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 1))
        val itemBefore = db.saleItemDao().getForSale(result.saleId).single()
        assertThat(itemBefore.unitCostCents).isEqualTo(4000L)
        assertThat(itemBefore.unitPriceCents).isEqualTo(8000L)

        // Editar el producto DESPUÉS de vender -- ni el costo ni el precio
        // de esa venta pueden cambiar (D-002).
        val product = checkNotNull(db.productDao().getById(productId))
        db.productDao().update(product.copy(costCents = 9999, salePriceCents = 19999))

        val itemAfter = db.saleItemDao().getForSale(result.saleId).single()
        assertThat(itemAfter.unitCostCents).isEqualTo(4000L)
        assertThat(itemAfter.unitPriceCents).isEqualTo(8000L)

        // La ganancia de ESA venta se calcula solo con los snapshots de
        // sale_item, nunca con un JOIN al producto actual -- y da el
        // número viejo, no el nuevo.
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.totalCents).isEqualTo(8000L)
        assertThat(sale.totalCostCents).isEqualTo(4000L)
    }

    // --- Fase 06 criterio 3: no se puede vender más de lo que hay en stock ---

    @Test
    fun register_cannotSellMoreThanStock() = runTest {
        val productId = insertProduct(stockQty = 2, costCents = 4000, salePriceCents = 8000)

        var threw = false
        try {
            repository.register(sale(productId, qty = 3))
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()

        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(2)
        assertThat(db.saleDao().observeBetween(0L, Long.MAX_VALUE).first()).isEmpty()
    }

    // --- Fase 06 criterio 4: anular devuelve exactamente el stock descontado ---

    @Test
    fun cancel_returnsExactlyTheDiscountedStock() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val result = repository.register(sale(productId, qty = 2))

        val afterSale = checkNotNull(db.productDao().getById(productId))
        assertThat(afterSale.stockQty).isEqualTo(3)

        repository.cancel(result.saleId, cancelledAt = 2_000L, cancelReason = "Devolución")

        val afterCancel = checkNotNull(db.productDao().getById(productId))
        assertThat(afterCancel.stockQty).isEqualTo(5)
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("CANCELLED")
        // D-036: cancelledAt viene de afuera (mismo criterio que soldAt en
        // register()), no de System.currentTimeMillis() adentro del
        // repositorio -- por eso se puede comparar contra un valor exacto.
        assertThat(sale.cancelledAt).isEqualTo(2_000L)
        assertThat(sale.cancelReason).isEqualTo("Devolución")
    }

    @Test
    fun cancel_withoutReason_persistsNullNotEmptyString() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val result = repository.register(sale(productId, qty = 2))

        repository.cancel(result.saleId, cancelledAt = 2_000L, cancelReason = null)

        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.cancelReason).isNull()
    }

    @Test
    fun cancel_alreadyCancelled_throwsInsteadOfDoubleRestoringStock() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val result = repository.register(sale(productId, qty = 2))
        repository.cancel(result.saleId, cancelledAt = 2_000L, cancelReason = null)

        var threw = false
        try {
            repository.cancel(result.saleId, cancelledAt = 3_000L, cancelReason = null)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()

        // Un segundo "cancel" no puede haber sumado el stock dos veces.
        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(5)
    }

    // --- Bug de revisión (D-036): dos líneas del mismo producto no pueden
    // vender de más por leer/escribir la misma fila dos veces en silencio ---

    @Test
    fun register_duplicateProductInTwoLines_throwsInsteadOfSilentlyOversellingStock() = runTest {
        val productId = insertProduct(stockQty = 1, costCents = 4000, salePriceCents = 8000)

        var threw = false
        try {
            repository.register(
                RegisterSaleInput(
                    soldAt = 1_000L,
                    discount = Money.ZERO,
                    notes = null,
                    lines = listOf(
                        SaleLineInput(productId, qty = 1),
                        SaleLineInput(productId, qty = 1),
                    ),
                ),
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()

        // Con stock 1, las dos líneas de qty 1 cada una NO pueden haber
        // vendido 2 unidades ni dejar el stock en un valor intermedio raro
        // -- nada se aplicó.
        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(1)
        assertThat(db.saleDao().observeBetween(0L, Long.MAX_VALUE).first()).isEmpty()
    }

    // --- Fase 06 criterio 5: la venta y sus líneas son atómicas ---

    @Test
    fun register_isAtomic_ifOneLineFailsNothingIsSaved() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val missingProductId = 999_999L

        var threw = false
        try {
            repository.register(
                RegisterSaleInput(
                    soldAt = 1_000L,
                    discount = Money.ZERO,
                    notes = null,
                    lines = listOf(
                        SaleLineInput(productId, qty = 1),
                        SaleLineInput(missingProductId, qty = 1),
                    ),
                ),
            )
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()

        // La primera línea (producto real) no puede haber quedado
        // aplicada aunque la segunda haya fallado -- una sola transacción.
        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(5)
        assertThat(db.saleDao().observeBetween(0L, Long.MAX_VALUE).first()).isEmpty()
    }

    // --- observeBetween: excluye CANCELLED y fuera de rango ---

    @Test
    fun observeBetween_excludesCancelledSales() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val kept = repository.register(sale(productId, qty = 1, soldAt = 500L))
        val cancelled = repository.register(sale(productId, qty = 1, soldAt = 600L))
        repository.cancel(cancelled.saleId, cancelledAt = 700L, cancelReason = null)

        val summaries = repository.observeBetween(0L, 1_000L).first()

        assertThat(summaries.map { it.id }).containsExactly(kept.saleId)
    }

    @Test
    fun observeBetween_excludesSalesOutsideTheRange() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        repository.register(sale(productId, qty = 1, soldAt = 500L))

        val summaries = repository.observeBetween(1_000L, 2_000L).first()

        assertThat(summaries).isEmpty()
    }

    private suspend fun insertProduct(stockQty: Int, costCents: Long, salePriceCents: Long): Long =
        db.productDao().insert(
            ProductEntity(
                uid = "XP-000001",
                name = "Anillo de prueba",
                categoryId = null,
                costCents = costCents,
                salePriceCents = salePriceCents,
                stockQty = stockQty,
                photoPath = null,
                supplier = null,
                notes = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

    private fun sale(productId: Long, qty: Int, soldAt: Long = 1_000L) = RegisterSaleInput(
        soldAt = soldAt,
        discount = Money.ZERO,
        notes = null,
        lines = listOf(SaleLineInput(productId, qty)),
    )
}
