package gt.marcos.joyeria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.CustomerEntity
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
            customerDao = db.customerDao(),
            paymentDao = db.paymentDao(),
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
                        SaleLineInput(productId, qty = 1, unitPrice = Money(8000)),
                        SaleLineInput(productId, qty = 1, unitPrice = Money(8000)),
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
                        SaleLineInput(productId, qty = 1, unitPrice = Money(8000)),
                        SaleLineInput(missingProductId, qty = 1, unitPrice = Money(8000)),
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

    // --- Fase 07: venta a crédito nace PENDING, con o sin abono inicial ---

    @Test
    fun register_creditSale_startsAsPendingWithCustomer() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")

        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.type).isEqualTo("CREDIT")
        assertThat(sale.status).isEqualTo("PENDING")
        assertThat(sale.customerId).isEqualTo(customerId)
    }

    @Test
    fun register_creditSale_withoutInitialPayment_hasNoPaymentRows() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")

        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        assertThat(db.paymentDao().sumForSale(result.saleId)).isEqualTo(0L)
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PENDING")
    }

    @Test
    fun register_creditSale_initialPaymentCoversTotal_marksPaidImmediately() = runTest {
        // El abono inicial pasa por el mismo camino que cualquier otro
        // abono (registerPaymentInternal) -- si cubre el total exacto, la
        // venta queda PAID sin ningún `if` especial para "es el primero".
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")

        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = Money(8000)))

        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PAID")
        assertThat(db.paymentDao().sumForSale(result.saleId)).isEqualTo(8000L)
    }

    @Test
    fun register_creditSale_partialInitialPayment_staysPending() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")

        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = Money(3000)))

        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PENDING")
        assertThat(db.paymentDao().sumForSale(result.saleId)).isEqualTo(3000L)
    }

    @Test
    fun register_creditSale_unknownCustomer_throwsInsteadOfOrphaningTheSale() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        var threw = false
        try {
            repository.register(creditSale(productId, customerId = 999_999L, qty = 1, initialPayment = null))
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()

        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(5)
        assertThat(db.saleDao().observeBetween(0L, Long.MAX_VALUE).first()).isEmpty()
    }

    @Test
    fun register_creditSale_archivedCustomer_throws() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        db.customerDao().archive(customerId)

        var threw = false
        try {
            repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    // --- Fase 07 criterios 3/4: registerPayment ---

    @Test
    fun registerPayment_exceedsBalance_throwsWithClearMessage() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        var message: String? = null
        try {
            repository.registerPayment(
                RegisterPaymentInput(saleId = result.saleId, amount = Money(8001), paidAt = 1_000L, method = "CASH", notes = null),
            )
        } catch (e: IllegalArgumentException) {
            message = e.message
        }
        assertThat(message).isNotNull()

        // El abono rechazado no se guardó: el saldo sigue intacto.
        assertThat(db.paymentDao().sumForSale(result.saleId)).isEqualTo(0L)
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PENDING")
    }

    @Test
    fun registerPayment_zeroAmount_throws() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        var threw = false
        try {
            repository.registerPayment(
                RegisterPaymentInput(saleId = result.saleId, amount = Money.ZERO, paidAt = 1_000L, method = "CASH", notes = null),
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun registerPayment_exactBalance_marksPaid() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        val paymentResult = repository.registerPayment(
            RegisterPaymentInput(saleId = result.saleId, amount = Money(8000), paidAt = 1_000L, method = "CASH", notes = null),
        )

        assertThat(paymentResult.saleNowPaid).isTrue()
        assertThat(paymentResult.newBalance).isEqualTo(Money.ZERO)
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PAID")
    }

    @Test
    fun registerPayment_oneCentShort_staysPending() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        val paymentResult = repository.registerPayment(
            RegisterPaymentInput(saleId = result.saleId, amount = Money(7999), paidAt = 1_000L, method = "CASH", notes = null),
        )

        assertThat(paymentResult.saleNowPaid).isFalse()
        assertThat(paymentResult.newBalance).isEqualTo(Money(1))
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PENDING")
    }

    @Test
    fun registerPayment_onAlreadyPaidCashSale_throws() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val result = repository.register(sale(productId, qty = 1))

        var threw = false
        try {
            repository.registerPayment(
                RegisterPaymentInput(saleId = result.saleId, amount = Money(1000), paidAt = 1_000L, method = "CASH", notes = null),
            )
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun registerPayment_twoPartialPayments_accumulateTowardsBalance() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))

        repository.registerPayment(
            RegisterPaymentInput(saleId = result.saleId, amount = Money(3000), paidAt = 1_000L, method = "CASH", notes = null),
        )
        val second = repository.registerPayment(
            RegisterPaymentInput(saleId = result.saleId, amount = Money(5000), paidAt = 2_000L, method = "TRANSFER", notes = null),
        )

        assertThat(second.saleNowPaid).isTrue()
        assertThat(db.paymentDao().sumForSale(result.saleId)).isEqualTo(8000L)
    }

    // --- Fase 07 D-043: no se anula una venta con abonos ya registrados ---

    @Test
    fun cancel_saleWithPayments_throws() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        val result = repository.register(creditSale(productId, customerId, qty = 1, initialPayment = Money(3000)))

        var threw = false
        try {
            repository.cancel(result.saleId, cancelledAt = 2_000L, cancelReason = null)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertThat(threw).isTrue()

        // No se tocó nada: ni el stock, ni el status.
        val product = checkNotNull(db.productDao().getById(productId))
        assertThat(product.stockQty).isEqualTo(4)
        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.status).isEqualTo("PENDING")
    }

    // --- Fase 07: "¿Quién me debe?" (D-038) ---

    @Test
    fun observeCustomerDebts_everyRowHasPositiveBalance() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")
        repository.register(creditSale(productId, customerId, qty = 1, initialPayment = null))
        // Esta segunda venta queda PAID de inmediato -- no debería aparecer.
        repository.register(creditSale(productId, customerId, qty = 1, initialPayment = Money(8000)))

        val debts = repository.observeCustomerDebts().first()

        assertThat(debts).hasSize(1)
        assertThat(debts.all { it.totalBalance > Money.ZERO }).isTrue()
    }

    @Test
    fun observeCustomerDebts_sortedByBalanceDescending() = runTest {
        val productId = insertProduct(stockQty = 10, costCents = 4000, salePriceCents = 8000)
        val smallDebtor = insertCustomer("Debe poco")
        val bigDebtor = insertCustomer("Debe mucho")
        repository.register(creditSale(productId, smallDebtor, qty = 1, initialPayment = Money(7000)))
        repository.register(creditSale(productId, bigDebtor, qty = 1, initialPayment = null))

        val debts = repository.observeCustomerDebts().first()

        assertThat(debts.map { it.customerName }).containsExactly("Debe mucho", "Debe poco").inOrder()
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

    private fun sale(
        productId: Long,
        qty: Int,
        soldAt: Long = 1_000L,
        unitPrice: Money = Money(8000),
    ) = RegisterSaleInput(
        soldAt = soldAt,
        discount = Money.ZERO,
        notes = null,
        lines = listOf(SaleLineInput(productId, qty, unitPrice)),
    )

    private suspend fun insertCustomer(name: String): Long =
        db.customerDao().insert(CustomerEntity(name = name, phone = null, notes = null, createdAt = 0L))

    private fun creditSale(
        productId: Long,
        customerId: Long,
        qty: Int,
        initialPayment: Money?,
        soldAt: Long = 1_000L,
        unitPrice: Money = Money(8000),
    ) =
        RegisterSaleInput(
            soldAt = soldAt,
            discount = Money.ZERO,
            notes = null,
            lines = listOf(SaleLineInput(productId, qty, unitPrice)),
            credit = CreditSaleDetails(customerId = customerId, initialPayment = initialPayment),
        )

    // --- D-045: el precio de la línea es dato de entrada, el costo no ---

    @Test
    fun register_storesTypedPrice_notTheProductListPrice() = runTest {
        // El caso que motivó todo el cambio: vende MÁS CARO que la lista.
        // Un descuento no podía representar esto, solo restar.
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 1, unitPrice = Money(10000)))

        val items = db.saleItemDao().getForSale(result.saleId)
        assertThat(items).hasSize(1)
        assertThat(items.first().unitPriceCents).isEqualTo(10000L)
        // El precio de lista del inventario NO se toca.
        assertThat(checkNotNull(db.productDao().getById(productId)).salePriceCents).isEqualTo(8000L)
    }

    @Test
    fun register_totalIsSumOfTypedPrices_notListPrices() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 3, unitPrice = Money(9000)))

        val sale = checkNotNull(db.saleDao().getById(result.saleId))
        assertThat(sale.totalCents).isEqualTo(27000L)
        // El costo total sigue saliendo del producto fresco, no de la pantalla.
        assertThat(sale.totalCostCents).isEqualTo(12000L)
    }

    @Test
    fun register_costSnapshotComesFromProduct_evenWhenPriceComesFromScreen() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4500, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 1, unitPrice = Money(3000)))

        val item = db.saleItemDao().getForSale(result.saleId).first()
        // Precio: el tecleado. Costo: el del inventario, intacto.
        assertThat(item.unitPriceCents).isEqualTo(3000L)
        assertThat(item.unitCostCents).isEqualTo(4500L)
    }

    @Test
    fun register_priceBelowCost_isAllowed_andProfitIsNegative() = runTest {
        // Rematar bajo costo es legítimo: la pantalla ya le avisó y ella
        // confirmó. El repositorio no lo bloquea (D-015: una ganancia
        // negativa es un resultado correcto, no un error).
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 1, unitPrice = Money(3000)))

        val detail = checkNotNull(repository.getDetail(result.saleId))
        assertThat(detail.profit).isEqualTo(Money(-1000))
    }

    @Test
    fun register_zeroPrice_isAllowed() = runTest {
        // Regalar una pieza: precio cero es un caso real, no un error.
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        val result = repository.register(sale(productId, qty = 1, unitPrice = Money.ZERO))

        assertThat(checkNotNull(db.saleDao().getById(result.saleId)).totalCents).isEqualTo(0L)
    }

    @Test
    fun register_negativePrice_isRejected() = runTest {
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)

        var threw = false
        try {
            repository.register(sale(productId, qty = 1, unitPrice = Money(-100)))
        } catch (e: IllegalArgumentException) {
            threw = true
        }

        assertThat(threw).isTrue()
        // Nada se aplicó: el stock quedó intacto.
        assertThat(checkNotNull(db.productDao().getById(productId)).stockQty).isEqualTo(5)
    }

    // --- D-047: la columna de descuento es legado y las ventas nuevas
    // escriben cero, pero las viejas conservan su valor y sus cuentas lo
    // siguen usando. ---

    @Test
    fun register_withLegacyDiscount_stillSubtractsItFromProfitAndBalance() = runTest {
        // Simula una venta vieja (antes de D-047) con descuento real: la
        // fórmula tiene que seguir restándolo. Si alguien saca el término
        // de `saleProfit`/`saleBalance`, este test falla -- es la red que
        // protege la ganancia histórica y el saldo de las clientas.
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        val customerId = insertCustomer("Doña María")

        val result = repository.register(
            RegisterSaleInput(
                soldAt = 1_000L,
                discount = Money(1000),
                notes = null,
                lines = listOf(SaleLineInput(productId, qty = 1, unitPrice = Money(8000))),
                credit = CreditSaleDetails(customerId = customerId, initialPayment = null),
            ),
        )

        val detail = checkNotNull(repository.getDetail(result.saleId))
        // (8000 - 1000) - 4000
        assertThat(detail.profit).isEqualTo(Money(3000))

        val statement = repository.observeAccountStatements(customerId).first().first()
        // (8000 - 1000) - 0 abonado
        assertThat(statement.balance).isEqualTo(Money(7000))
    }
}
