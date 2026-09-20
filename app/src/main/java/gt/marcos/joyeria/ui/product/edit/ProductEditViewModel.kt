package gt.marcos.joyeria.ui.product.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.CategoryRepository
import gt.marcos.joyeria.data.repository.EditProductInput
import gt.marcos.joyeria.data.repository.ProductRepository
import gt.marcos.joyeria.domain.usecase.ArchiveProductUseCase
import gt.marcos.joyeria.domain.usecase.EditProductUseCase
import gt.marcos.joyeria.ui.format.toEditableText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla de detalle/edición
 * (CLAUDE.md sección 5). `productId` llega por `SavedStateHandle`
 * (argumento de navegación, Fase 04 / D-023).
 */
@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val editProductUseCase: EditProductUseCase,
    private val archiveProductUseCase: ArchiveProductUseCase,
) : ViewModel() {

    private val productId: Long = checkNotNull(savedStateHandle["productId"]) {
        "ProductEditViewModel necesita un argumento de navegación productId"
    }

    private val _uiState = MutableStateFlow(ProductEditUiState(productId = productId))
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = productRepository.getDetail(productId)
            if (detail != null) {
                _uiState.update {
                    it.copy(
                        uid = detail.uid,
                        name = detail.name,
                        categoryId = detail.categoryId,
                        costText = detail.cost.toEditableText(),
                        salePriceText = detail.salePrice.toEditableText(),
                        quantityText = detail.stockQty.toString(),
                        supplier = detail.supplier.orEmpty(),
                        notes = detail.notes.orEmpty(),
                        photoPath = detail.photoPath,
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collectLatest { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    /** `raw` ya viene filtrado por `MoneyTextField` (D-030): nunca hace falta sanitizarlo acá. */
    fun onCostTextChanged(raw: String) {
        _uiState.update { it.copy(costText = raw) }
    }

    fun onSalePriceTextChanged(raw: String) {
        _uiState.update { it.copy(salePriceText = raw) }
    }

    fun onQuantityChanged(raw: String) {
        _uiState.update { it.copy(quantityText = raw.filter(Char::isDigit)) }
    }

    fun onSupplierChanged(value: String) {
        _uiState.update { it.copy(supplier = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val cost = state.cost
        val salePrice = state.salePrice
        if (!state.canSave || cost == null || salePrice == null) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            editProductUseCase(
                EditProductInput(
                    id = productId,
                    name = state.name.trim(),
                    categoryId = state.categoryId,
                    cost = cost,
                    salePrice = salePrice,
                    stockQty = state.stockQty,
                    supplier = state.supplier.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null },
                ),
            )
            _uiState.update { it.copy(isSaving = false, savedConfirmationVisible = true) }
        }
    }

    fun onSavedConfirmationDismissed() {
        _uiState.update { it.copy(savedConfirmationVisible = false) }
    }

    fun onArchiveClick() {
        _uiState.update { it.copy(showArchiveConfirmation = true) }
    }

    fun onArchiveDismiss() {
        _uiState.update { it.copy(showArchiveConfirmation = false) }
    }

    /** No hay `DELETE`: se archiva (D-006). La pantalla vuelve al listado cuando `archived` pasa a `true`. */
    fun onArchiveConfirm() {
        viewModelScope.launch {
            archiveProductUseCase(productId)
            _uiState.update { it.copy(showArchiveConfirmation = false, archived = true) }
        }
    }
}
