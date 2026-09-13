package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 04.
@Entity(tableName = "purchase")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "purchased_at")
    val purchasedAt: Long,
    val supplier: String?,
    @ColumnInfo(name = "extra_cost_cents")
    val extraCostCents: Long = 0,
    val notes: String?,
)
