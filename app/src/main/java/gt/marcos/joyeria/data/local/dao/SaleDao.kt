package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

/**
 * `getLastSaleDate` nació acotado a un solo método en Fase 05 (D-032):
 * en ese momento `sale`/`sale_item` no tenían DAO todavía. Fase 06
 * extiende esta misma interfaz con el resto (insertar, anular,
 * consultar), tal como D-032 ya anticipaba -- no un `SaleDao`
 * competidor, mismo criterio que D-021 usó para `CategoryRepository`
 * en Fase 03.
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
}
