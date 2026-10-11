package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.dao.CategoryDao
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Categoría tal como la ve `ui`: nunca `CategoryEntity` (Room) directo. */
data class Category(val id: Long, val name: String, val archived: Boolean = false)

/** Se intentó crear o renombrar una categoría al nombre de otra ya activa (Fase 09). */
class CategoryNameAlreadyExistsException(val name: String) :
    IllegalArgumentException("Ya existe una categoría activa llamada \"$name\".")

/**
 * Nunca se usa para un alta/rename nuevo (ver abajo): `-1` no es un `id`
 * real de `category` (autogenerado desde `1`), así que nunca excluye una
 * fila existente por accidente.
 */
private const val NO_CATEGORY_TO_EXCLUDE = -1L

/**
 * Envuelve `CategoryDao` para que `ui` nunca importe `data/local` (Room)
 * directo -- mismo motivo que `AppSettingRepository` (D-021/Fase 03):
 * `ui` habla con repositorios de `data/repository`, nunca con DAOs.
 *
 * `category.name` **no tiene restricción de unicidad en la base** (D-057):
 * la garantiza únicamente `add()`/`rename()`, acá. Cualquier escritura que
 * no pase por este repositorio (una migración, una importación futura de
 * Fase 14) puede crear dos categorías activas con el mismo nombre sin que
 * nada lo impida -- ver la nota completa en `ESQUEMA.md`, tabla `category`.
 */
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { it.toCategory() } }

    /** Para selectores (dropdown de edición, filtro de Inventario): activas + archivadas con piezas (Opción A, Fase 09). */
    fun observeSelectable(): Flow<List<Category>> =
        categoryDao.observeSelectable().map { list -> list.map { it.toCategory() } }

    /** Para la pantalla de administración: todas, incluida una archivada sin ninguna pieza. */
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toCategory() } }

    /** @throws CategoryNameAlreadyExistsException si ya hay una categoría activa con ese nombre. */
    suspend fun add(name: String): Long {
        val trimmed = name.trim()
        if (categoryDao.countActiveWithName(trimmed, excludeId = NO_CATEGORY_TO_EXCLUDE) > 0) {
            throw CategoryNameAlreadyExistsException(trimmed)
        }
        return categoryDao.insert(CategoryEntity(name = trimmed))
    }

    /** @throws CategoryNameAlreadyExistsException si otra categoría activa ya tiene ese nombre. */
    suspend fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        if (categoryDao.countActiveWithName(trimmed, excludeId = id) > 0) {
            throw CategoryNameAlreadyExistsException(trimmed)
        }
        categoryDao.rename(id, trimmed)
    }

    suspend fun archive(id: Long) = categoryDao.archive(id)

    private fun CategoryEntity.toCategory() = Category(id = id, name = name, archived = archived)
}
