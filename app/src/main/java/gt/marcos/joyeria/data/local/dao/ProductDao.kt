package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import gt.marcos.joyeria.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

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
}
