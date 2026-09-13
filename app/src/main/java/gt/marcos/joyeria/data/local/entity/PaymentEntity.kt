package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 06.
@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sale_id"])],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sale_id")
    val saleId: Long,
    @ColumnInfo(name = "paid_at")
    val paidAt: Long,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    val method: String,
    val notes: String?,
)
