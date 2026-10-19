package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.barcode.UidLookup
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

/**
 * Una pieza tal como la necesita el catálogo compartible (Fase 12): **solo
 * estas seis columnas**. No hay `cost_cents` acá, y la consulta que la llena
 * (`observeCatalogRows`) lista cada columna a mano en vez de `SELECT *`: el
 * costo no sale de SQLite hacia el catálogo. No es un dato que se filtra
 * después, es un dato que nunca se lee. Ver D-058 y `CatalogItem`.
 */
data class CatalogRow(
    val id: Long,
    val uid: String,
    val name: String,
    val categoryName: String?,
    val photoPath: String?,
    val salePriceCents: Long,
)

/**
 * Una pieza para la hoja de etiquetas de la Fase 13: **sin ningún monto** (ni precio
 * ni costo). Las columnas se listan a mano en `observeLabelRows`, igual que
 * `CatalogRow`: la etiqueta solo lleva el código de la pieza (D-003, D-060).
 */
data class LabelRow(
    val id: Long,
    val uid: String,
    val name: String,
    val categoryName: String?,
    val photoPath: String?,
)

@Dao
interface ProductDao {
    // Fase 14: desde cuándo hay datos que respaldar (el recordatorio cuenta desde la primera pieza si nunca hizo uno).
    @Query("SELECT MIN(created_at) FROM product")
    fun observeFirstCreatedAt(): Flow<Long?>

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

    // Fase 12: las piezas que se pueden poner en el catálogo -- las que ella
    // tiene a la mano para vender. La condición se expresa ENTERA aunque parte
    // sea hoy redundante (CLAUDE.md sección 4): `archived = 0` y
    // `stock_qty > 0`. Una pieza archivada que conserva stock existe (archivar
    // no pide stock en cero), y una activa sin stock también. Las columnas se
    // listan una por una a propósito: ninguna es de costo (ver `CatalogRow`).
    // `LEFT JOIN`: una pieza sin categoría igual entra.
    @Query(
        """
        SELECT product.id AS id, product.uid AS uid, product.name AS name,
               category.name AS categoryName, product.photo_path AS photoPath,
               product.sale_price_cents AS salePriceCents
        FROM product
        LEFT JOIN category ON category.id = product.category_id
        WHERE product.archived = 0 AND product.stock_qty > 0
        ORDER BY product.name, product.uid
        """,
    )
    fun observeCatalogRows(): Flow<List<CatalogRow>>

    // Fase 13: qué hay detrás de un código escaneado. Devuelve el estado (archivada,
    // existencias) y no decide nada: `ScanResolver` decide. Una sola columna de
    // identidad, `uid` (índice único).
    @Query("SELECT id AS productId, archived AS archived, stock_qty AS stockQty FROM product WHERE uid = :uid")
    suspend fun findByUid(uid: String): UidLookup?

    // Fase 13: las piezas a las que se les puede imprimir etiqueta -- las activas, con
    // las más nuevas primero (la pieza que acaba de cargar es la que más se etiqueta).
    // Columnas a mano: ninguna es un monto (ver `LabelRow`).
    @Query(
        """
        SELECT product.id AS id, product.uid AS uid, product.name AS name,
               category.name AS categoryName, product.photo_path AS photoPath
        FROM product
        LEFT JOIN category ON category.id = product.category_id
        WHERE product.archived = 0
        ORDER BY product.id DESC
        """,
    )
    fun observeLabelRows(): Flow<List<LabelRow>>
}
