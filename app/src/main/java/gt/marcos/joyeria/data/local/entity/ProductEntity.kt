package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["category_id"]),
        Index(value = ["archived"]),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val name: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    @ColumnInfo(name = "cost_cents")
    val costCents: Long,
    @ColumnInfo(name = "sale_price_cents")
    val salePriceCents: Long,
    @ColumnInfo(name = "stock_qty")
    val stockQty: Int,
    @ColumnInfo(name = "photo_path")
    val photoPath: String?,
    val supplier: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    val archived: Boolean = false,
)
