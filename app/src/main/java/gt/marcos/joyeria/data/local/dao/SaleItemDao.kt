package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Una pieza agregada sobre un rango de fechas (Fase 11, "Top 5"): cuánto
 * se vendió y cuánta ganancia dejó, sumado de todas sus líneas del mes.
 * `discountedLineCount` cuenta cuántas de esas líneas vinieron de una
 * venta con `discount_cents > 0` (legado, D-047) -- esa ganancia no resta
 * el descuento (es de toda la venta, no de una línea, `ESQUEMA.md`), así
 * que puede verse un poco más alta que la real. `> 0` es la señal para
 * marcar la fila en pantalla, no un valor a mostrar tal cual.
 */
data class ProductSalesAggregateRow(
    val productUid: String,
    val productName: String,
    val totalQty: Int,
    val totalProfitCents: Long,
    val discountedLineCount: Int,
)

@Dao
interface SaleItemDao {
    @Insert
    suspend fun insertAll(items: List<SaleItemEntity>)

    // Hace falta al anular una venta: para devolver stock hay que saber
    // qué producto y cuánta cantidad tenía cada línea (D-006: los
    // productos se archivan, nunca se borran físico, así que product_id
    // casi siempre resuelve -- pero el esquema permite null vía
    // ON DELETE SET NULL, y esa fila simplemente no puede devolver stock
    // a ningún lado).
    @Query("SELECT * FROM sale_item WHERE sale_id = :saleId")
    suspend fun getForSale(saleId: Long): List<SaleItemEntity>

    // "Top 5" (Fase 11): una sola consulta: "por cantidad" y "por
    // ganancia" son la misma fila ordenada distinto, en Kotlin. Usa el
    // snapshot (product_uid_snapshot/product_name_snapshot), nunca JOIN a
    // product -- ver ESTADO.md "Fase 11 -- Plan" punto 1 (criterio 3 de
    // FASES.md). Excluye CANCELLED (D-050: condición propia de esta
    // consulta, no heredada de otra).
    @Query(
        """
        SELECT sale_item.product_uid_snapshot AS productUid,
               sale_item.product_name_snapshot AS productName,
               SUM(sale_item.qty) AS totalQty,
               SUM((sale_item.unit_price_cents - sale_item.unit_cost_cents) * sale_item.qty) AS totalProfitCents,
               SUM(CASE WHEN sale.discount_cents > 0 THEN 1 ELSE 0 END) AS discountedLineCount
        FROM sale_item
        INNER JOIN sale ON sale.id = sale_item.sale_id
        WHERE sale.sold_at >= :startMillis AND sale.sold_at < :endMillis AND sale.status != 'CANCELLED'
        GROUP BY sale_item.product_uid_snapshot, sale_item.product_name_snapshot
        """,
    )
    fun observeProductSalesAggregate(startMillis: Long, endMillis: Long): Flow<List<ProductSalesAggregateRow>>
}
