package gt.marcos.joyeria.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.ProductRepository
import gt.marcos.joyeria.data.repository.PurchaseLineInput
import gt.marcos.joyeria.data.repository.RegisterPurchaseInput
import gt.marcos.joyeria.domain.usecase.RegisterPurchaseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). */
@HiltViewModel
class RegisterPurchaseViewModel @Inject constructor(
    private val registerPurchaseUseCase: RegisterPurchaseUseCase,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterPurchaseUiState())
    val uiState: StateFlow<RegisterPurchaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeFiltered(query = "", categoryId = null).collectLatest { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun onAddLineClick() {
        _uiState.update { it.copy(lines = it.lines + PurchaseLineUiState()) }
    }

    fun onRemoveLineClick(index: Int) {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList().apply { removeAt(index) }
            state.copy(lines = newLines.ifEmpty { listOf(PurchaseLineUiState()) })
        }
    }

    fun onLineProductSelected(index: Int, productId: Long) {
        updateLine(index) { it.copy(productId = productId) }
    }

    fun onLineQtyChanged(index: Int, raw: String) {
        updateLine(index) { it.copy(qtyText = raw.filter(Char::isDigit)) }
    }

    /** `raw` ya viene filtrado por `MoneyTextField` (D-030): nunca hace falta sanitizarlo acá. */
    fun onLineUnitCostChanged(index: Int, raw: String) {
        updateLine(index) { it.copy(unitCostText = raw) }
    }

    fun onExtraCostChanged(raw: String) {
        _uiState.update { it.copy(extraCostText = raw) }
    }

    fun onSupplierChanged(value: String) {
        _uiState.update { it.copy(supplier = value) }
    }

    fun onPurchasedAtSelected(millis: Long) {
        _uiState.update { it.copy(purchasedAtMillis = millis) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val extraCost = state.extraCost
        if (!state.canSave || extraCost == null) return

        // Reconstruye cada línea desde cero en vez de confiar en canSave a
        // ciegas -- si alguna quedara incompleta igual, se aborta acá sin
        // guardar nada a medias, sin `!!` (CLAUDE.md sección 5).
        val lines = state.lines.mapNotNull { line ->
            val productId = line.productId ?: return@mapNotNull null
            val qty = line.qty ?: return@mapNotNull null
            val unitCost = line.unitCost ?: return@mapNotNull null
            PurchaseLineInput(productId = productId, qty = qty, unitCost = unitCost)
        }
        if (lines.size != state.lines.size) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = registerPurchaseUseCase(
                RegisterPurchaseInput(
                    purchasedAt = state.purchasedAtMillis,
                    supplier = state.supplier.trim().ifBlank { null },
                    extraCost = extraCost,
                    notes = null,
                    lines = lines,
                ),
            )
            // Compra guardada: formulario en blanco, con el resultado para
            // mostrar los avisos -- mismo espíritu que AddProductViewModel
            // tras guardar.
            _uiState.value = RegisterPurchaseUiState(products = state.products, result = result)
        }
    }

    fun onResultDismissed() {
        _uiState.update { it.copy(result = null) }
    }

    private fun updateLine(index: Int, transform: (PurchaseLineUiState) -> PurchaseLineUiState) {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            newLines[index] = transform(newLines[index])
            state.copy(lines = newLines)
        }
    }
}
