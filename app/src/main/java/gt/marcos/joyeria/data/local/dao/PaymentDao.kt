package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payment WHERE sale_id = :saleId ORDER BY paid_at ASC")
    fun observeForSale(saleId: Long): Flow<List<PaymentEntity>>

    // Suma de abonos ya registrados para una venta, usada para calcular el
    // saldo (PricingCalculator.saleBalance) antes de aceptar uno nuevo.
    // COALESCE porque SUM() de cero filas da NULL, no 0.
    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM payment WHERE sale_id = :saleId")
    suspend fun sumForSale(saleId: Long): Long
}
