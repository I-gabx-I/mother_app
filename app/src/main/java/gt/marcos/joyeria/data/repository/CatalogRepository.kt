package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.dao.CatalogRow
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.domain.catalog.CatalogItem
import gt.marcos.joyeria.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Las piezas del catálogo compartible (Fase 12). De solo lectura y **a
 * propósito sin acceso a `ProductEntity`, `ProductSummary` ni `ProductDetail`**
 * (todos llevan el costo): este repositorio solo conoce `CatalogRow`, que no
 * tiene costo, y solo devuelve `CatalogItem`, que tampoco. Ver D-058.
 */
class CatalogRepository @Inject constructor(
    private val productDao: ProductDao,
) {
    fun observeItems(): Flow<List<CatalogItem>> =
        productDao.observeCatalogRows().map { rows -> rows.map { it.toItem() } }

    private fun CatalogRow.toItem() = CatalogItem(
        productId = id,
        uid = uid,
        name = name,
        categoryName = categoryName,
        photoPath = photoPath,
        price = Money(salePriceCents),
    )
}
