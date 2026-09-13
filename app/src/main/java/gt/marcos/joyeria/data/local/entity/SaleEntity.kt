package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 05/06.
@Entity(
    tableName = "sale",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["sold_at"]),
        Index(value = ["customer_id"]),
        Index(value = ["status"]),
    ],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sold_at")
    val soldAt: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long?,
    val type: String,
    val status: String,
    @ColumnInfo(name = "total_cents")
    val totalCents: Long,
    @ColumnInfo(name = "total_cost_cents")
    val totalCostCents: Long,
    @ColumnInfo(name = "discount_cents")
    val discountCents: Long = 0,
    val notes: String?,
    @ColumnInfo(name = "cancelled_at")
    val cancelledAt: Long?,
    @ColumnInfo(name = "cancel_reason")
    val cancelReason: String?,
)
