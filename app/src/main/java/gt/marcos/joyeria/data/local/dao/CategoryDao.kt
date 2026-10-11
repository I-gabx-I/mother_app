package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM category WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<CategoryEntity>>

    /**
     * Todas las categorías, activas y archivadas (Fase 09) -- para la
     * pantalla de administración, no para un selector. Las archivadas van
     * al final.
     */
    @Query("SELECT * FROM category ORDER BY archived ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * Las activas, más cualquier archivada que todavía tenga al menos una
     * pieza no archivada apuntándole (Fase 09, Opción A: archivar no
     * esconde la clasificación que ella ya hizo). En cuanto esa última
     * pieza se reclasifica o se archiva, la categoría deja de aparecer
     * acá sola, sin ninguna marca extra que mantener.
     */
    @Query(
        """
        SELECT * FROM category
        WHERE archived = 0
           OR id IN (SELECT DISTINCT category_id FROM product WHERE category_id IS NOT NULL AND archived = 0)
        ORDER BY archived ASC, name ASC
        """,
    )
    fun observeSelectable(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("UPDATE category SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE category SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /**
     * Cuenta cuántas categorías **activas** ya tienen este nombre (sin
     * distinguir mayúsculas ni espacios al borde), para que
     * `CategoryRepository` rechace un duplicado antes de escribir --
     * CLAUDE.md 4 (D-050) y D-057: esto no es una restricción de la base
     * (no hay índice único en `category.name`), así que cualquier
     * escritura que no pase por acá puede crear uno.
     * `excludeId` (`-1` para un alta, el propio `id` para un rename) evita
     * que renombrar una categoría a su propio nombre actual se rechace
     * como si fuera un duplicado de otra.
     */
    @Query(
        "SELECT COUNT(*) FROM category WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND archived = 0 AND id != :excludeId",
    )
    suspend fun countActiveWithName(name: String, excludeId: Long): Int
}
