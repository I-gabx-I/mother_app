package gt.marcos.joyeria.ui.product.list

import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.data.repository.ProductSummary

/** Estado único del listado de inventario (CLAUDE.md sección 5). */
data class ProductListUiState(
    val products: List<ProductSummary> = emptyList(),
    val query: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
)
