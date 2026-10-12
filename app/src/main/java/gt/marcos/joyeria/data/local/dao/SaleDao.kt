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
    //
    // `type = 'CREDIT'` es redundante HOY (una venta de contado nace PAID,
    // así que `status = 'PENDING'` ya la deja afuera) y va igual: D-050.
    // Ese "nace PAID" se cumple en SaleRepository, otro archivo -- la
    // consulta estaba acertando por una coincidencia entre dos archivos, no
    // por lo que ella misma pide. Desde Fase 08 una venta de contado SÍ
    // puede tener `customer_id`, así que el margen de error desapareció.
    @Query(
        """
        SELECT sale.id AS saleId, sale.customer_id AS customerId, customer.name AS customerName,
               customer.phone AS customerPhone, sale.sold_at AS soldAt, sale.total_cents AS totalCents,
               sale.discount_cents AS discountCents,
               COALESCE((SELECT SUM(amount_cents) FROM payment WHERE payment.sale_id = sale.id), 0) AS paidCents
        FROM sale
        INNER JOIN customer ON customer.id = sale.customer_id
        WHERE sale.status = 'PENDING' AND sale.type = 'CREDIT'
        ORDER BY sale.sold_at ASC
        """,
    )
    fun observePendingCreditSales(): Flow<List<PendingSaleRow>>

    // Mismo dato que arriba, para una sola clienta -- lo usa
    // CustomerRepository.archive() (D-039) para decidir si bloquea el
    // archivado, y el perfil de clienta para listar sus ventas pendientes.
    //
    // `type = 'CREDIT'` por la misma razón que la consulta de arriba
    // (D-050), y acá el costo de no tenerlo sería peor: con una venta de
    // contado colada, no se podría archivar a ninguna clienta a la que se
    // le haya vendido de contado, con un mensaje de deuda que no existe.
    @Query(
        """
        SELECT sale.id AS saleId, sale.customer_id AS customerId, customer.name AS customerName,
               customer.phone AS customerPhone, sale.sold_at AS soldAt, sale.total_cents AS totalCents,
               sale.discount_cents AS discountCents,
               COALESCE((SELECT SUM(amount_cents) FROM payment WHERE payment.sale_id = sale.id), 0) AS paidCents
        FROM sale
        INNER JOIN customer ON customer.id = sale.customer_id
        WHERE sale.status = 'PENDING' AND sale.type = 'CREDIT' AND sale.customer_id = :customerId
        ORDER BY sale.sold_at ASC
        """,
    )
    fun observePendingCreditSalesForCustomer(customerId: Long): Flow<List<PendingSaleRow>>

    /**
     * Misma consulta que `observePendingCreditSalesForCustomer`, de una
     * sola lectura. Existe para `CustomerRepository.archive()`, que corre
     * dentro de una transacción: recolectar el `Flow` reactivo ahí adentro
     * (con `.first()`) es pedirle a la transacción que espere a una
     * consulta que necesita salir de ella. Ver D-056.
     */
    @Query(
        """
        SELECT sale.id AS saleId, sale.customer_id AS customerId, customer.name AS customerName,
               customer.phone AS customerPhone, sale.sold_at AS soldAt, sale.total_cents AS totalCents,
               sale.discount_cents AS discountCents,
               COALESCE((SELECT SUM(amount_cents) FROM payment WHERE payment.sale_id = sale.id), 0) AS paidCents
        FROM sale
        INNER JOIN customer ON customer.id = sale.customer_id
        WHERE sale.status = 'PENDING' AND sale.type = 'CREDIT' AND sale.customer_id = :customerId
        ORDER BY sale.sold_at ASC
        """,
    )
    suspend fun getPendingCreditSalesForCustomer(customerId: Long): List<PendingSaleRow>

    /**
     * Historial completo de una clienta (Fase 08): contado y crédito,
     * cobradas y pendientes, **y también las anuladas** (B-06). No se
     * excluye `CANCELLED` acá, a diferencia de `observeBetween`: en
     * "Ventas de hoy" una anulada distorsionaría el total del día, pero en
     * el historial de una clienta esconder una compra que ella recuerda
     * haber hecho es peor -- D-006 existe justamente para que nada
     * desaparezca. La pantalla las marca y **no las suma a ningún total**.
     */
    @Query("SELECT * FROM sale WHERE customer_id = :customerId ORDER BY sold_at DESC")
    fun observeSalesForCustomer(customerId: Long): Flow<List<SaleEntity>>

    /**
     * Fecha de la **primera venta de contado registrada con clienta** en
     * toda la base, o `null` si todavía no hay ninguna.
     *
     * Antes de Fase 08 ninguna venta de contado guardaba `customer_id`:
     * nadie anotaba a quién se le vendía. Esta fecha es, en la práctica, el
     * momento en que la app empezó a registrarlo, **deducido de los datos
     * que ya existen** -- sin ninguna clave nueva, sin migración y sin
     * nada escrito a mano (B-10). Sirve para decidir, clienta por clienta,
     * si su historial puede tener huecos.
     */
    @Query("SELECT MIN(sold_at) FROM sale WHERE type = 'CASH' AND customer_id IS NOT NULL")
    fun observeFirstNamedCashSaleDate(): Flow<Long?>

    // "Capital recuperado" (Fase 11, ESTADO.md "Fase 11 -- Plan" punto 5):
    // costo de TODO lo que ya se vendió, de siempre, no acotado al mes --
    // responde "cuánto de lo que compré ya volvió" frente al capital
    // todavía guardado en inventario (ProductDao.observeInventorySnapshot).
    // Excluye CANCELLED, igual que observeBetween (D-050: condición propia,
    // no heredada de otro archivo).
    @Query("SELECT COALESCE(SUM(total_cost_cents), 0) FROM sale WHERE status != 'CANCELLED'")
    fun observeTotalRecoveredCost(): Flow<Long>
}
