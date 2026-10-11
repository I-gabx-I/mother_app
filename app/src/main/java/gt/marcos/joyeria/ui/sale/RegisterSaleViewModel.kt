package gt.marcos.joyeria.ui.sale

import androidx.lifecycle.SavedStateHandle
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
import gt.marcos.joyeria.ui.format.toEditableText
import gt.marcos.joyeria.ui.navigation.JoyeriaDestination
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
    savedStateHandle: SavedStateHandle,
    private val registerSaleUseCase: RegisterSaleUseCase,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    /**
     * Clienta preelegida cuando se entra desde su perfil (Fase 08). Llega
     * como `String?` porque `NavType.LongType` no admite nulos
     * (`JoyeriaDestination.RegisterSale`); `toLongOrNull()` hace que un
     * valor corrupto se comporte como "sin clienta" en vez de tirar la app.
     *
     * `null` -- entrar desde el inicio -- es el camino de siempre, sin
     * ningún cambio de comportamiento.
     */
    private val preselectedCustomerId: Long? =
        savedStateHandle.get<String?>(JoyeriaDestination.RegisterSale.ARG_CUSTOMER_ID)?.toLongOrNull()

    private val _uiState = MutableStateFlow(
        RegisterSaleUiState(
            selectedCustomerId = preselectedCustomerId,
            // D-042 intacto: entrar desde el perfil NO fuerza crédito. Ella
            // dijo "de contado o a crédito", así que el selector sigue en
            // "Al contado" y la decisión la toma ella, no la navegación.
            cameFromCustomerProfile = preselectedCustomerId != null,
        ),
    )
    val uiState: StateFlow<RegisterSaleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeFiltered(query = "", categoryId = null).collectLatest { products ->
                _uiState.update { state ->
                    // Si una pieza que ya está en el carrito cambió de
                    // costo/stock mientras tanto, la vista previa se
                    // actualiza -- el snapshot real igual se toma de nuevo
                    // al confirmar (punto 1 del plan), esto es solo para
                    // que lo que ella ve en pantalla no quede desactualizado.
                    //
                    // `unitPriceText` NO se refresca a propósito (D-045): es
                    // el precio que ella tecleó para esta venta. Pisarlo con
                    // el precio de lista le borraría el número que acaba de
                    // escribir, y este flujo se dispara con cualquier cambio
                    // del catálogo.
                    val refreshedLines = state.lines.map { line ->
                        val fresh = products.find { it.id == line.productId } ?: return@map line
                        line.copy(
                            productName = fresh.name,
                            productUid = fresh.uid,
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

    /** Quita la clienta de una venta de contado (el "Cambiar" del encabezado). */
    fun onClearSelectedCustomer() {
        _uiState.update { it.copy(selectedCustomerId = null) }
    }

    fun onInitialPaymentChanged(raw: String) {
        _uiState.update { it.copy(initialPaymentText = raw) }
    }

    /** D-040: alta rápida de clienta sin salir de "Vender", desde `CustomerPickerDropdown`. */
    fun onNewCustomerConfirmed(name: String, phone: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = customerRepository.add(
                AddCustomerInput(name = name, phone = phone, notes = null, createdAt = System.currentTimeMillis()),
            )
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
                        // El precio de lista, precargado y editable (D-045).
                        // `toEditableText()` usa Locale.ROOT porque este texto
                        // vuelve a entrar a `parseMoneyToCents` apenas ella
                        // siga escribiendo (CLAUDE.md sección 5).
                        unitPriceText = product.salePrice.toEditableText(),
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

    /** `raw` ya viene filtrado por `MoneyTextField` (D-030): nunca hace falta sanitizarlo acá. */
    fun onLinePriceChanged(index: Int, raw: String) {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            newLines[index] = newLines[index].copy(unitPriceText = raw)
            state.copy(lines = newLines)
        }
    }

    fun onRemoveLineClick(index: Int) {
        _uiState.update { state ->
            state.copy(lines = state.lines.toMutableList().apply { removeAt(index) })
        }
    }

    /**
     * D-046: si **alguna línea** no deja ganancia, pide confirmación
     * explícita antes de guardar, nombrando esa pieza y su monto. Evaluar
     * sobre el total (D-035) dejaba pasar en silencio un collar rematado
     * bajo costo cuando el resto de la venta lo tapaba.
     */
    fun onConfirmClick() {
        val state = _uiState.value
        if (!state.canSave) return
        if (state.lossLines.isNotEmpty()) {
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

        // Reconstruye las líneas desde cero en vez de confiar en canSave a
        // ciegas -- si alguna quedara incompleta igual, se aborta sin
        // guardar nada, sin `!!` (CLAUDE.md sección 5).
        val lines = state.lines.mapNotNull { line ->
            val qty = line.qty ?: return@mapNotNull null
            val unitPrice = line.unitPrice ?: return@mapNotNull null
            SaleLineInput(productId = line.productId, qty = qty, unitPrice = unitPrice)
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
                    // Fase 08: en contado la clienta se asocia a la venta
                    // sin convertirla en deuda. En crédito este campo se
                    // ignora -- la clienta sale de `credit`, abajo.
                    customerId = if (state.isCredit) null else state.selectedCustomerId,
                    // D-047: el descuento general ya no existe en la app, pero
                    // la columna sigue viva y sus fórmulas la siguen usando
                    // para las ventas viejas. Las nuevas escriben cero.
                    discount = Money.ZERO,
                    notes = null,
                    lines = lines,
                    credit = credit,
                ),
            )
            // Se conserva la clienta preelegida al limpiar: si entró desde
            // el perfil de Doña María para venderle, lo más probable es que
            // la siguiente venta también sea para ella.
            _uiState.value = RegisterSaleUiState(
                products = state.products,
                customers = state.customers,
                result = result,
                selectedCustomerId = preselectedCustomerId,
                cameFromCustomerProfile = preselectedCustomerId != null,
            )
        }
    }

    fun onResultDismissed() {
        _uiState.update { it.copy(result = null) }
    }
}
