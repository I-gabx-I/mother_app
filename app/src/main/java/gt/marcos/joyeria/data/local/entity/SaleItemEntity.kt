package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 05.
@Entity(
    tableName = "sale_item",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["sale_id"]),
        Index(value = ["product_id"]),
    ],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sale_id")
    val saleId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long?,
    @ColumnInfo(name = "product_uid_snapshot")
    val productUidSnapshot: String,
    @ColumnInfo(name = "product_name_snapshot")
    val productNameSnapshot: String,
    val qty: Int,
    @ColumnInfo(name = "unit_price_cents")
    val unitPriceCents: Long,
    @ColumnInfo(name = "unit_cost_cents")
    val unitCostCents: Long,
)
