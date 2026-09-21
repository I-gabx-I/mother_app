package gt.marcos.joyeria.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.AddCustomerInput
import gt.marcos.joyeria.data.repository.CreditSaleDetails
import gt.marcos.joyeria.data.repository.CustomerRepository
import gt.marcos.joyeria.data.repository.ProductRepository
import gt.marcos.joyeria.data.repository.RegisterSaleInput
import gt.marcos.joyeria.data.repository.SaleLineInput
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.usecase.RegisterSaleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). */
@HiltViewModel
class RegisterSaleViewModel @Inject constructor(
    private val registerSaleUseCase: RegisterSaleUseCase,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterSaleUiState())
    val uiState: StateFlow<RegisterSaleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeFiltered(query = "", categoryId = null).collectLatest { products ->
                _uiState.update { state ->
                    // Si una pieza que ya está en el carrito cambió de
                    // precio/costo/stock mientras tanto, la vista previa se
                    // actualiza -- el snapshot real igual se toma de nuevo
                    // al confirmar (punto 1 del plan), esto es solo para
                    // que lo que ella ve en pantalla no quede desactualizado.
                    val refreshedLines = state.lines.map { line ->
                        val fresh = products.find { it.id == line.productId } ?: return@map line
                        line.copy(
                            productName = fresh.name,
                            productUid = fresh.uid,
                            unitPrice = fresh.salePrice,
                            unitCost = fresh.cost,
                            availableStock = fresh.stockQty,
                        )
                    }
                    state.copy(products = products, lines = refreshedLines)
                }
            }
        }
        viewModelScope.launch {
            customerRepository.observeActive().collectLatest { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun onSaleTypeChanged(type: SaleTypeChoice) {
        _uiState.update { it.copy(saleType = type) }
    }

    fun onCustomerSelected(customerId: Long) {
        _uiState.update { it.copy(selectedCustomerId = customerId) }
    }

    fun onInitialPaymentChanged(raw: String) {
        _uiState.update { it.copy(initialPaymentText = raw) }
    }

    /** D-040: alta rápida de clienta sin salir de "Vender", desde `CustomerPickerDropdown`. */
    fun onNewCustomerConfirmed(name: String, phone: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = customerRepository.add(AddCustomerInput(name = name, phone = phone, notes = null))
            _uiState.update { it.copy(selectedCustomerId = id) }
        }
    }

    fun onQueryChanged(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun onProductSelected(productId: Long) {
        _uiState.update { state ->
            val existingIndex = state.lines.indexOfFirst { it.productId == productId }
            if (existingIndex >= 0) {
                val newLines = state.lines.toMutableList()
                val line = newLines[existingIndex]
                val nextQty = (line.qty ?: 0) + 1
                newLines[existingIndex] = line.copy(qtyText = nextQty.toString())
                state.copy(lines = newLines)
            } else {
                val product = state.products.find { it.id == productId } ?: return@update state
                state.copy(
                    lines = state.lines + SaleLineUiState(
                        productId = product.id,
                        productName = product.name,
                        productUid = product.uid,
                        unitPrice = product.salePrice,
                        unitCost = product.cost,
                        availableStock = product.stockQty,
                    ),
                )
            }
        }
    }

    fun onLineQtyChanged(index: Int, raw: String) {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            newLines[index] = newLines[index].copy(qtyText = raw)
            state.copy(lines = newLines)
        }
    }

    fun onRemoveLineClick(index: Int) {
        _uiState.update { state ->
            state.copy(lines = state.lines.toMutableList().apply { removeAt(index) })
        }
    }

    /** `raw` ya viene filtrado por `MoneyTextField` (D-030): nunca hace falta sanitizarlo acá. */
    fun onDiscountChanged(raw: String) {
        _uiState.update { it.copy(discountText = raw) }
    }

    /** D-035: si la venta da ganancia cero o negativa, pide confirmación explícita antes de guardar. */
    fun onConfirmClick() {
        val state = _uiState.value
        val profit = state.profit
        if (!state.canSave || profit == null) return
        if (profit <= Money.ZERO) {
            _uiState.update { it.copy(pendingLossConfirmation = true) }
        } else {
            save()
        }
    }

    fun onLossConfirmed() {
        _uiState.update { it.copy(pendingLossConfirmation = false) }
        save()
    }

    fun onLossDialogDismissed() {
        _uiState.update { it.copy(pendingLossConfirmation = false) }
    }

    private fun save() {
        val state = _uiState.value
        val discount = state.discount ?: return

        // Reconstruye las líneas desde cero en vez de confiar en canSave a
        // ciegas -- si alguna quedara incompleta igual, se aborta sin
        // guardar nada, sin `!!` (CLAUDE.md sección 5).
        val lines = state.lines.mapNotNull { line ->
            val qty = line.qty ?: return@mapNotNull null
            SaleLineInput(productId = line.productId, qty = qty)
        }
        if (lines.size != state.lines.size) return

        // D-042: `credit == null` es el camino de Fase 06 sin ningún cambio.
        // Si es crédito, reconstruye igual desde cero (mismo criterio que
        // las líneas): sin clienta o sin un abono inicial completo, se
        // aborta sin guardar -- `canSave` ya no debería dejar llegar acá,
        // pero el ViewModel no confía ciegamente en su propio estado previo.
        val credit = if (state.isCredit) {
            val customerId = state.selectedCustomerId ?: return
            val initialPayment = state.initialPayment ?: return
            if (state.initialPaymentExceedsNet) return
            CreditSaleDetails(customerId = customerId, initialPayment = initialPayment)
        } else {
            null
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = registerSaleUseCase(
                RegisterSaleInput(
                    soldAt = System.currentTimeMillis(),
                    discount = discount,
                    notes = null,
                    lines = lines,
                    credit = credit,
                ),
            )
            _uiState.value = RegisterSaleUiState(products = state.products, customers = state.customers, result = result)
        }
    }

    fun onResultDismissed() {
        _uiState.update { it.copy(result = null) }
    }
}
