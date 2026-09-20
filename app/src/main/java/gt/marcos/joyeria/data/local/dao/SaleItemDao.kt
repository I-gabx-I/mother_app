package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.SaleItemEntity

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
}
