package gt.marcos.joyeria.ui.product.list

import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.CategoryRepository
import gt.marcos.joyeria.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` del listado (CLAUDE.md sección 5). Búsqueda
 * y filtro de categoría son reactivos: cambiar cualquiera de los dos
 * vuelve a consultar `ProductRepository` (Room ya notifica solo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    productRepository: ProductRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ProductListUiState> =
        combine(query, selectedCategoryId, categoryRepository.observeActive()) { q, categoryId, categories ->
            Triple(q, categoryId, categories)
        }
            .flatMapLatest { (q, categoryId, categories) ->
                productRepository.observeFiltered(q, categoryId).map { products ->
                    ProductListUiState(
                        products = products,
                        query = q,
                        categories = categories,
                        selectedCategoryId = categoryId,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun onCategorySelected(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }
}
