package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Excepción acotada a un solo método (D-032): Fase 05 necesita saber si
 * existe una venta posterior a la fecha de una compra retroactiva, pero
 * `sale`/`sale_item` no tienen DAO todavía -- ese trabajo completo
 * (insertar venta, anular, etc.) es de Fase 06 (`FASES.md`). Fase 06
 * extiende esta misma interfaz con lo que le falte, no crea un
 * `SaleDao` competidor -- mismo criterio que D-021 usó para
 * `CategoryRepository` en Fase 03.
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
}
