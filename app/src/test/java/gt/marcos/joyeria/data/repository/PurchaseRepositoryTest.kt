package gt.marcos.joyeria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.model.Money
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (D-012).
// Se prueba a nivel de PurchaseRepository, no solo del DAO, porque ahí es
// donde vive la transacción completa que exige FASES.md Fase 05
// (recalcular costo/stock, insertar price_history, los dos avisos de
// D-031, el aviso de D-032) -- probar solo el DAO no ejercitaría nada de
// esa orquestación.
@RunWith(AndroidJUnit4::class)
class PurchaseRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: PurchaseRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        repository = PurchaseRepository(
            db = db,
            purchaseDao = db.purchaseDao(),
            purchaseItemDao = db.purchaseItemDao(),
            productDao = db.productDao(),
            priceHistoryDao = db.priceHistoryDao(),
            saleDao = db.saleDao(),
            appSettingRepository = AppSettingRepository(db.appSettingDao()),
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    // --- D-029: promedio ponderado / reemplazo directo, vía la transacción real ---

    @Test
    fun register_weightedAverage_d029Example_10at40plus5at55_costs45() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 20000)

        repository.register(purchase(productId, qty = 5, unitCostCents = 5500))

        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.costCents).isEqualTo(4500L)
        assertThat(product.stockQty).isEqualTo(15)
    }

    // --- Fase 05 criterio 4, [TESTS OBLIGATORIOS]: stock = 0 reemplaza el costo, sin promediar con el valor viejo ---

    @Test
    fun register_stockZero_replacesCostDirectly_andMarksCycleStartInPriceHistory() = runTest {
        val productId = insertProduct(stockQty = 0, costCents = 9999, salePriceCents = 20000)

        repository.register(purchase(productId, qty = 5, unitCostCents = 4000))

        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.costCents).isEqualTo(4000L)

        val history = db.priceHistoryDao().getForProduct(productId)
        assertThat(history).hasSize(1)
        assertThat(history.single().cycleStart).isTrue()
    }

    // --- Fase 05 criterio 5: price_history con el costo nuevo y purchase_id ---

    @Test
    fun register_insertsPriceHistoryRow_withPurchaseIdNewCostAndCycleStartFalse() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 20000)

        val result = repository.register(purchase(productId, qty = 5, unitCostCents = 5500))

        val history = db.priceHistoryDao().getForProduct(productId)
        assertThat(history).hasSize(1)
        assertThat(history.single().purchaseId).isEqualTo(result.purchaseId)
        assertThat(history.single().costCents).isEqualTo(4500L)
        assertThat(history.single().cycleStart).isFalse()
    }

    // --- Fase 05 criterio 6: todo o nada ---

    @Test
    fun register_isAtomic_ifOneLineFailsNothingIsSaved() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 20000)
        val missingProductId = 999_999L

        var threw = false
        try {
            repository.register(
                RegisterPurchaseInput(
                    purchasedAt = 1_000L,
                    supplier = null,
                    extraCost = Money.ZERO,
                    notes = null,
                    lines = listOf(
                        PurchaseLineInput(productId, qty = 5, unitCost = Money(5500)),
                        PurchaseLineInput(missingProductId, qty = 1, unitCost = Money(1000)),
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
        assertThat(product.costCents).isEqualTo(4000L)
        assertThat(product.stockQty).isEqualTo(10)
        assertThat(db.priceHistoryDao().getForProduct(productId)).isEmpty()
    }

    // --- D-031: dos avisos distintos, nunca los dos a la vez para la misma línea ---

    @Test
    fun register_marginBelowFloor_triggersMarginWarning_notProfitAlert() = runTest {
        // min_margin_bp por defecto 2500 (25%). Costo nuevo 6500 sobre
        // venta 8000: ganancia 1500 (positiva), margen 18.75% -- por
        // debajo del piso, pero sin llegar a pérdida.
        val productId = insertProduct(stockQty = 0, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 6500))

        assertThat(result.lineWarnings).hasSize(1)
        assertThat(result.lineWarnings.single().level).isEqualTo(PurchaseWarningLevel.MARGIN_WARNING)
    }

    @Test
    fun register_zeroOrNegativeProfit_triggersProfitAlert() = runTest {
        val productId = insertProduct(stockQty = 0, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 9000))

        assertThat(result.lineWarnings).hasSize(1)
        assertThat(result.lineWarnings.single().level).isEqualTo(PurchaseWarningLevel.PROFIT_ALERT)
    }

    @Test
    fun register_healthyMargin_triggersNoWarning() = runTest {
        val productId = insertProduct(stockQty = 0, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 4000))

        assertThat(result.lineWarnings).isEmpty()
    }

    // --- D-032: aviso informativo si purchasedAt es anterior a la última venta no CANCELLED ---

    @Test
    fun register_purchaseDateBeforeLastSale_triggersRetroactiveWarning() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 8000)
        insertSale(productId, soldAt = 5_000L, status = "PAID")

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 4000, purchasedAt = 1_000L))

        assertThat(result.retroactiveWarning).isTrue()
    }

    @Test
    fun register_purchaseDateAfterLastSale_doesNotTriggerRetroactiveWarning() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 8000)
        insertSale(productId, soldAt = 1_000L, status = "PAID")

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 4000, purchasedAt = 5_000L))

        assertThat(result.retroactiveWarning).isFalse()
    }

    @Test
    fun register_cancelledSaleAfterPurchaseDate_doesNotTriggerRetroactiveWarning() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 8000)
        insertSale(productId, soldAt = 5_000L, status = "CANCELLED")

        val result = repository.register(purchase(productId, qty = 1, unitCostCents = 4000, purchasedAt = 1_000L))

        assertThat(result.retroactiveWarning).isFalse()
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

    private fun purchase(productId: Long, qty: Int, unitCostCents: Long, purchasedAt: Long = 1_000L) =
        RegisterPurchaseInput(
            purchasedAt = purchasedAt,
            supplier = null,
            extraCost = Money.ZERO,
            notes = null,
            lines = listOf(PurchaseLineInput(productId, qty, Money(unitCostCents))),
        )

    /**
     * `sale`/`sale_item` no tienen DAO de inserción todavía (D-032: Fase 05
     * solo agrega `SaleDao.getLastSaleDate`, el resto es de Fase 06) -- se
     * inserta directo por SQL, solo para este test, sin pasar por ninguna
     * pantalla ni caso de uso real.
     */
    private fun insertSale(productId: Long, soldAt: Long, status: String) {
        val writable = db.openHelper.writableDatabase
        writable.execSQL(
            """
            INSERT INTO sale (id, sold_at, customer_id, type, status, total_cents, total_cost_cents, discount_cents, notes, cancelled_at, cancel_reason)
            VALUES (1, $soldAt, NULL, 'CASH', '$status', 8000, 4000, 0, NULL, NULL, NULL)
            """.trimIndent(),
        )
        writable.execSQL(
            """
            INSERT INTO sale_item (id, sale_id, product_id, product_uid_snapshot, product_name_snapshot, qty, unit_price_cents, unit_cost_cents)
            VALUES (1, 1, $productId, 'XP-000001', 'Anillo de prueba', 1, 8000, 4000)
            """.trimIndent(),
        )
    }
}
