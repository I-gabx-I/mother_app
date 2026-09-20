package gt.marcos.joyeria.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla declarada en Fase 01 (D-011); su DAO y su UI llegan en Fase 04.
// `purchaseId`/`cycleStart` se agregan en la versión 2 (Fase 05, D-033) --
// ver MIGRATION_1_2 en AppDatabase.kt.
@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchase_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["purchase_id"]),
    ],
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "cost_cents")
    val costCents: Long,
    @ColumnInfo(name = "sale_price_cents")
    val salePriceCents: Long,
    @ColumnInfo(name = "changed_at")
    val changedAt: Long,
    // `null` cuando la fila viene de una edición manual (Fase 04), no de
    // una compra.
    @ColumnInfo(name = "purchase_id")
    val purchaseId: Long? = null,
    // `true` únicamente cuando `stock_qty` era 0 justo antes de esta
    // compra (D-029/D-033): inicio de un ciclo de compra nuevo.
    @ColumnInfo(name = "cycle_start")
    val cycleStart: Boolean = false,
)
