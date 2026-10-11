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

// Robolectric: Room in-memory necesita un Context de Android (D-012).
@RunWith(AndroidJUnit4::class)
class CustomerRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CustomerRepository
    private lateinit var saleRepository: SaleRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        repository = CustomerRepository(db = db, customerDao = db.customerDao(), saleDao = db.saleDao())
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

    @Test
    fun add_and_observeActive_returnsTheNewCustomer() = runTest {
        repository.add(AddCustomerInput(name = "Doña María", phone = "5555-1234", notes = "turno de la tarde", createdAt = 1_000L))

        val customers = repository.observeActive().first()

        assertThat(customers).hasSize(1)
        assertThat(customers.single().name).isEqualTo("Doña María")
        assertThat(customers.single().phone).isEqualTo("5555-1234")
    }

    @Test
    fun update_changesTheStoredFields() = runTest {
        val id = repository.add(AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 1_000L))

        repository.update(EditCustomerInput(id = id, name = "Doña María Corregido", phone = "5555-9999", notes = "nota"))

        val customer = checkNotNull(repository.getById(id))
        assertThat(customer.name).isEqualTo("Doña María Corregido")
        assertThat(customer.phone).isEqualTo("5555-9999")
        assertThat(customer.notes).isEqualTo("nota")
    }

    // --- D-006: nunca se borra, se archiva -- y desaparece de observeActive ---

    @Test
    fun archive_withoutPendingBalance_succeeds() = runTest {
        val id = repository.add(AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 1_000L))

        repository.archive(id)

        assertThat(repository.observeActive().first()).isEmpty()
        assertThat(checkNotNull(repository.getById(id)).archived).isTrue()
    }

    // --- D-039: archivar con saldo pendiente se rechaza con un mensaje claro ---

    @Test
    fun archive_withPendingBalance_throwsAndDoesNotArchive() = runTest {
        val customerId = repository.add(AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 1_000L))
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        saleRepository.register(
            RegisterSaleInput(
                soldAt = 1_000L,
                discount = Money.ZERO,
                notes = null,
                lines = listOf(SaleLineInput(productId, qty = 1, unitPrice = Money(8000))),
                credit = CreditSaleDetails(customerId = customerId, initialPayment = null),
            ),
        )

        var exception: CustomerHasPendingBalanceException? = null
        try {
            repository.archive(customerId)
        } catch (e: CustomerHasPendingBalanceException) {
            exception = e
        }

        assertThat(exception).isNotNull()
        assertThat(exception!!.pendingCents).isEqualTo(8000L)
        assertThat(exception.pendingSaleCount).isEqualTo(1)
        // No quedó archivada -- sigue en la lista de activas.
        assertThat(repository.observeActive().first()).hasSize(1)
    }

    @Test
    fun archive_missingCustomer_isRejected() = runTest {
        // Antes, `customerDao.archive(id)` sobre un id inexistente afectaba
        // cero filas y la operación "funcionaba": un no-op silencioso, que
        // es justo lo que `update()` ya rechazaba con mensaje.
        var threw = false
        try {
            repository.archive(999_999L)
        } catch (e: IllegalStateException) {
            threw = true
        }

        assertThat(threw).isTrue()
    }

    @Test
    fun add_usesTheCreatedAtGivenByTheCaller_notTheRealClock() = runTest {
        // D-055: el repositorio no lee el reloj. Si lo leyera, este test no
        // podría afirmar nada sobre `created_at` sin depender de la hora a
        // la que corre.
        val id = repository.add(
            AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 12_345L),
        )

        assertThat(checkNotNull(db.customerDao().getById(id)).createdAt).isEqualTo(12_345L)
    }

    @Test
    fun archive_withOnlyCashSales_succeeds() = runTest {
        // Fase 08: una venta de contado a su nombre NO es una deuda. Si la
        // consulta de pendientes dejara colar el contado (D-050), no se
        // podría archivar a ninguna clienta a la que se le haya vendido de
        // contado, con un mensaje de deuda que no existe.
        val customerId = repository.add(AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 1_000L))
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        saleRepository.register(
            RegisterSaleInput(
                soldAt = 1_000L,
                discount = Money.ZERO,
                notes = null,
                lines = listOf(SaleLineInput(productId, qty = 1, unitPrice = Money(8000))),
                customerId = customerId,
            ),
        )

        repository.archive(customerId)

        assertThat(repository.observeActive().first()).isEmpty()
    }

    @Test
    fun archive_afterBalancePaidOff_succeeds() = runTest {
        val customerId = repository.add(AddCustomerInput(name = "Doña María", phone = null, notes = null, createdAt = 1_000L))
        val productId = insertProduct(stockQty = 5, costCents = 4000, salePriceCents = 8000)
        // Abono inicial que cubre el total exacto -- la venta queda PAID,
        // así que ya no bloquea el archivado.
        saleRepository.register(
            RegisterSaleInput(
                soldAt = 1_000L,
                discount = Money.ZERO,
                notes = null,
                lines = listOf(SaleLineInput(productId, qty = 1, unitPrice = Money(8000))),
                credit = CreditSaleDetails(customerId = customerId, initialPayment = Money(8000)),
            ),
        )

        repository.archive(customerId)

        assertThat(checkNotNull(repository.getById(customerId)).archived).isTrue()
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
}
