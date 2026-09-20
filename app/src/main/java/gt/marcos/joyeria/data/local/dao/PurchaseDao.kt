package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import gt.marcos.joyeria.data.local.entity.PurchaseEntity

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long
}
