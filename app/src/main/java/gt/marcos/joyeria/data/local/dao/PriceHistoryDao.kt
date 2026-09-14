package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.PriceHistoryEntity

@Dao
interface PriceHistoryDao {
    @Insert
    suspend fun insert(entry: PriceHistoryEntity): Long

    // Ordenado del más reciente al más viejo: "¿por qué ganaba más antes
    // con este anillo?" (ESQUEMA.md) se responde leyendo de arriba hacia abajo.
    @Query("SELECT * FROM price_history WHERE product_id = :productId ORDER BY changed_at DESC")
    suspend fun getForProduct(productId: Long): List<PriceHistoryEntity>
}
