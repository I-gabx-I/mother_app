package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import gt.marcos.joyeria.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Una pieza activa, con lo que hace falta para los tres reportes de
 * "ahora mismo" de Fase 11 (capital invertido, capital estancado, bajo
 * stock) -- una sola consulta, tres lecturas distintas en Kotlin, para no
 * repetir casi la misma consulta tres veces. `lastSaleAt` sale de una
 * subconsulta que excluye `CANCELLED` (mismo criterio que
 * `SaleDao.getLastSaleDate`); `null` si la pieza nunca se vendió.
 */
data class ProductInventorySnapshotRow(
    val id: Long,
    val uid: String,
    val name: String,
    val costCents: Long,
    val stockQty: Int,
    val createdAt: Long,
    val lastSaleAt: Long?,
)

@Dao
interface ProductDao {
    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE product SET archived = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun archive(id: Long, updatedAt: Long)

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM product WHERE uid = :uid")
    suspend fun getByUid(uid: String): ProductEntity?

    @Query("SELECT * FROM product WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<ProductEntity>>

    // Búsqueda por nombre o uid, y filtro opcional por categoría (FASES.md
    // Fase 04). `:query` vacío o `:categoryId` null desactivan ese filtro
    // en particular -- no hace falta una consulta separada para "sin filtro".
    @Query(
        """
        SELECT * FROM product
        WHERE archived = 0
        AND (:categoryId IS NULL OR category_id = :categoryId)
        AND (:query = '' OR name LIKE '%' || :query || '%' OR uid LIKE '%' || :query || '%')
        ORDER BY name
        """,
    )
    fun observeFiltered(query: String, categoryId: Long?): Flow<List<ProductEntity>>

    // Fase 11: base de "capital invertido", "capital estancado" y "bajo
    // stock". `stock_qty = 0` entra igual (aporta 0 al capital invertido
    // sin filtro aparte); capital estancado y bajo stock deciden qué
    // hacer con eso en Kotlin, no acá.
    @Query(
        """
        SELECT product.id, product.uid, product.name, product.cost_cents AS costCents,
               product.stock_qty AS stockQty, product.created_at AS createdAt,
               (SELECT MAX(sale.sold_at) FROM sale INNER JOIN sale_item ON sale_item.sale_id = sale.id
                WHERE sale_item.product_id = product.id AND sale.status != 'CANCELLED') AS lastSaleAt
        FROM product
        WHERE archived = 0
        """,
    )
    fun observeInventorySnapshot(): Flow<List<ProductInventorySnapshotRow>>
}
