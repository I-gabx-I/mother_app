package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.dao.CategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Categoría tal como la ve `ui`: nunca `CategoryEntity` (Room) directo. */
data class Category(val id: Long, val name: String)

/**
 * Envuelve `CategoryDao` para que `ui` nunca importe `data/local` (Room)
 * directo -- mismo motivo que `AppSettingRepository` (D-021/Fase 03):
 * `ui` habla con repositorios de `data/repository`, nunca con DAOs.
 */
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { Category(it.id, it.name) } }
}
