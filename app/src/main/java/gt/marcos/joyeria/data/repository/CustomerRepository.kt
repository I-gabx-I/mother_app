package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.dao.CustomerDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.entity.CustomerEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class Customer(
    val id: Long,
    val name: String,
    val phone: String?,
    val notes: String?,
    val archived: Boolean,
)

/**
 * `createdAt` lo decide quien llama, igual que `soldAt` en una venta o
 * `cancelledAt` en una anulación (D-055): el repositorio no lee el reloj,
 * así que es determinístico y testeable sin depender de la hora real.
 */
data class AddCustomerInput(
    val name: String,
    val phone: String?,
    val notes: String?,
    val createdAt: Long,
)

data class EditCustomerInput(
    val id: Long,
    val name: String,
    val phone: String?,
    val notes: String?,
)

/** Se intentó archivar a alguien que todavía tiene una deuda real (D-039). */
class CustomerHasPendingBalanceException(val pendingCents: Long, val pendingSaleCount: Int) :
    IllegalStateException(
        "No se puede archivar: tiene ${pendingCents} centavos pendientes en $pendingSaleCount venta(s).",
    )

/**
 * CRUD de clientas, sin `UseCase` intermedio -- mismo criterio que
 * `CategoryRepository` (Fase 04): no hay ninguna regla de negocio propia
 * más allá de "guardar/archivar", salvo el bloqueo de archivado con saldo
 * pendiente (D-039), que sí vive acá porque necesita consultar `SaleDao`.
 */
class CustomerRepository @Inject constructor(
    private val db: AppDatabase,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
) {
    suspend fun add(input: AddCustomerInput): Long = customerDao.insert(
        CustomerEntity(
            name = input.name,
            phone = input.phone,
            notes = input.notes,
            createdAt = input.createdAt,
        ),
    )

    suspend fun update(input: EditCustomerInput) {
        val existing = checkNotNull(customerDao.getById(input.id)) {
            "Cliente ${input.id} no existe -- no se puede editar alguien que no está"
        }
        customerDao.update(
            existing.copy(name = input.name, phone = input.phone, notes = input.notes),
        )
    }

    /**
     * Archiva a la clienta (D-006: nunca se borra), salvo que tenga saldo
     * pendiente en alguna venta a crédito -- archivarla la sacaría de
     * "¿Quién me debe?" (D-038) sin que la deuda real deje de existir.
     *
     * Las tres operaciones (existe / no debe / archivar) van en una sola
     * transacción, igual que el resto del proyecto (D-056). La lectura de
     * ventas pendientes usa la consulta **suspend**, no `.first()` sobre
     * el `Flow` reactivo: recolectar un `Flow` de Room dentro de su propia
     * transacción es pedirle a la transacción que espere a una consulta
     * que necesita salir de ella.
     */
    suspend fun archive(id: Long): Unit = db.withTransaction {
        // Antes esto no verificaba nada: `customerDao.archive(id)` sobre un
        // id inexistente afecta cero filas y la operación "funcionaba" en
        // silencio. Mismo no-op silencioso que `update()` ya rechazaba.
        checkNotNull(customerDao.getById(id)) {
            "Cliente $id no existe -- no se puede archivar a alguien que no está"
        }

        val pendingSales = saleDao.getPendingCreditSalesForCustomer(id)
        if (pendingSales.isNotEmpty()) {
            val pendingCents = pendingSales.fold(0L) { acc, row ->
                acc + PricingCalculator.saleBalance(Money(row.totalCents), Money(row.discountCents), Money(row.paidCents)).cents
            }
            throw CustomerHasPendingBalanceException(pendingCents = pendingCents, pendingSaleCount = pendingSales.size)
        }
        customerDao.archive(id)
    }

    suspend fun getById(id: Long): Customer? = customerDao.getById(id)?.toCustomer()

    fun observeActive(): Flow<List<Customer>> =
        customerDao.observeActive().map { customers -> customers.map { it.toCustomer() } }

    private fun CustomerEntity.toCustomer() = Customer(
        id = id,
        name = name,
        phone = phone,
        notes = notes,
        archived = archived,
    )
}
