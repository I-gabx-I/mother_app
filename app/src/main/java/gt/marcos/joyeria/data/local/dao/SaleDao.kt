package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Una venta `PENDING` (crédito, sin cobrar del todo) junto con datos de
 * su clienta y lo ya abonado -- fila cruda, sin el saldo calculado
 * (eso lo hace `PricingCalculator.saleBalance`, nunca SQL, para que la
 * resta que importa viva en un solo lugar tested -- D-041/Fase 07 plan).
 */
data class PendingSaleRow(
    val saleId: Long,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String?,
    val soldAt: Long,
    val totalCents: Long,
    val discountCents: Long,
    val paidCents: Long,
)

/**
 * `getLastSaleDate` nació acotado a un solo método en Fase 05 (D-032):
 * en ese momento `sale`/`sale_item` no tenían DAO todavía. Fase 06
 * extiende esta misma interfaz con el resto (insertar, anular,
 * consultar), tal como D-032 ya anticipaba -- no un `SaleDao`
 * competidor, mismo criterio que D-021 usó para `CategoryRepository`
 * en Fase 03. Fase 07 suma las consultas de ventas a crédito
 * pendientes, mismo criterio otra vez.
 */
@Dao
interface SaleDao {
    @Query(
        """
        SELECT MAX(sale.sold_at) FROM sale
        INNER JOIN sale_item ON sale_item.sale_id = sale.id
        WHERE sale_item.product_id = :productId AND sale.status != 'CANCELLED'
        """,
    )
    suspend fun getLastSaleDate(productId: Long): Long?

    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Query("SELECT * FROM sale WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Query(
        "UPDATE sale SET status = :status, cancelled_at = :cancelledAt, cancel_reason = :cancelReason WHERE id = :id",
    )
    suspend fun updateStatus(id: Long, status: String, cancelledAt: Long?, cancelReason: String?)

    // Excluye CANCELLED a nivel de consulta: "Ventas de hoy" nunca las
    // cuenta, ni siquiera por un instante entre leer y filtrar en Kotlin.
    @Query(
        """
        SELECT * FROM sale
        WHERE sold_at >= :startMillis AND sold_at < :endMillis AND status != 'CANCELLED'
        ORDER BY sold_at DESC
        """,
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SaleEntity>>

    // Todas las ventas a crédito sin cobrar del todo, con la clienta y lo ya
    // abonado -- "¿Quién me debe?" (D-038) agrupa esto por clienta en Kotlin,
    // nunca en SQL (la resta del saldo vive una sola vez, en PricingCalculator).
    @Query(
        """
        SELECT sale.id AS saleId, sale.customer_id AS customerId, customer.name AS customerName,
               customer.phone AS customerPhone, sale.sold_at AS soldAt, sale.total_cents AS totalCents,
               sale.discount_cents AS discountCents,
               COALESCE((SELECT SUM(amount_cents) FROM payment WHERE payment.sale_id = sale.id), 0) AS paidCents
        FROM sale
        INNER JOIN customer ON customer.id = sale.customer_id
        WHERE sale.status = 'PENDING'
        ORDER BY sale.sold_at ASC
        """,
    )
    fun observePendingCreditSales(): Flow<List<PendingSaleRow>>

    // Mismo dato que arriba, para una sola clienta -- lo usa
    // CustomerRepository.archive() (D-039) para decidir si bloquea el
    // archivado, y AccountStatement (estado de cuenta) para listar sus
    // ventas pendientes.
    @Query(
        """
        SELECT sale.id AS saleId, sale.customer_id AS customerId, customer.name AS customerName,
               customer.phone AS customerPhone, sale.sold_at AS soldAt, sale.total_cents AS totalCents,
               sale.discount_cents AS discountCents,
               COALESCE((SELECT SUM(amount_cents) FROM payment WHERE payment.sale_id = sale.id), 0) AS paidCents
        FROM sale
        INNER JOIN customer ON customer.id = sale.customer_id
        WHERE sale.status = 'PENDING' AND sale.customer_id = :customerId
        ORDER BY sale.sold_at ASC
        """,
    )
    fun observePendingCreditSalesForCustomer(customerId: Long): Flow<List<PendingSaleRow>>
}
