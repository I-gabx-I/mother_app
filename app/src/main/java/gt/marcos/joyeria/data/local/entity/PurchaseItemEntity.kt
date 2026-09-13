package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 04.
@Entity(
    tableName = "purchase_item",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchase_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["purchase_id"]),
        Index(value = ["product_id"]),
    ],
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "purchase_id")
    val purchaseId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val qty: Int,
    @ColumnInfo(name = "unit_cost_cents")
    val unitCostCents: Long,
    @ColumnInfo(name = "allocated_extra_cents")
    val allocatedExtraCents: Long,
)
