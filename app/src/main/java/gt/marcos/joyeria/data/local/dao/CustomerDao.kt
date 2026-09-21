package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import gt.marcos.joyeria.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    // D-006: nunca se borra. D-039: el repositorio valida saldo pendiente
    // antes de llamar a esto -- este método no sabe nada de ventas.
    @Query("UPDATE customer SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("SELECT * FROM customer WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customer WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<CustomerEntity>>
}
