package gt.marcos.joyeria.ui.credit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.CustomerRepository
import gt.marcos.joyeria.data.repository.EditCustomerInput
import gt.marcos.joyeria.data.repository.RegisterPaymentInput
import gt.marcos.joyeria.data.repository.SaleRepository
import gt.marcos.joyeria.domain.usecase.RegisterPaymentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` del perfil de clienta (CLAUDE.md sección 5).
 * `customerId` llega por `SavedStateHandle` (argumento de navegación),
 * mismo patrón que `ProductEditViewModel` (Fase 04).
 */
@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val registerPaymentUseCase: RegisterPaymentUseCase,
) : ViewModel() {

    private val customerId: Long = checkNotNull(savedStateHandle["customerId"]) {
        "CustomerProfileViewModel necesita un argumento de navegación customerId"
    }

    private val _uiState = MutableStateFlow(CustomerProfileUiState(customerId = customerId))
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    init {
        refreshCustomer()
        viewModelScope.launch {
            saleRepository.observeAccountStatements(customerId).collectLatest { statements ->
                _uiState.update { it.copy(statements = statements) }
            }
        }
        viewModelScope.launch {
            saleRepository.observeCustomerHistory(customerId).collectLatest { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    private fun refreshCustomer() {
        viewModelScope.launch {
            val customer = customerRepository.getById(customerId)
            _uiState.update { it.copy(customer = customer) }
        }
    }

    // --- B-09: editar sin salir del perfil, con el diálogo compartido ---

    fun onEditClick() {
        val customer = _uiState.value.customer ?: return
        _uiState.update {
            it.copy(
                showEditDialog = true,
                formName = customer.name,
                formPhone = customer.phone.orEmpty(),
                formNotes = customer.notes.orEmpty(),
            )
        }
    }

    fun onFormNameChange(value: String) = _uiState.update { it.copy(formName = value) }

    fun onFormPhoneChange(value: String) = _uiState.update { it.copy(formPhone = value) }

    fun onFormNotesChange(value: String) = _uiState.update { it.copy(formNotes = value) }

    fun onFormDismiss() = _uiState.update { it.copy(showEditDialog = false) }

    fun onFormConfirm() {
        val state = _uiState.value
        if (!state.canSaveForm) return
        viewModelScope.launch {
            customerRepository.update(
                EditCustomerInput(
                    id = customerId,
                    name = state.formName,
                    phone = state.formPhone.ifBlank { null },
                    notes = state.formNotes.ifBlank { null },
                ),
            )
            _uiState.update { it.copy(showEditDialog = false) }
            // `customer` no es un Flow (CustomerRepository.getById es
            // suspend), así que tras editar hay que releerla -- si no, el
            // encabezado seguiría mostrando el nombre viejo.
            refreshCustomer()
        }
    }

    // --- Estado de cuenta: igual que en Fase 07, sin cambios ---

    fun onRegisterPaymentClick(saleId: Long) {
        _uiState.update { it.copy(paymentDialogSaleId = saleId, amountText = "", method = "CASH") }
    }

    fun onPaymentDialogDismiss() {
        _uiState.update { it.copy(paymentDialogSaleId = null, amountText = "") }
    }

    fun onAmountChanged(raw: String) {
        _uiState.update { it.copy(amountText = raw) }
    }

    fun onMethodChanged(method: String) {
        _uiState.update { it.copy(method = method) }
    }

    /**
     * Registra el abono (criterios 3/4 de FASES.md Fase 07: rechazo claro si
     * supera el saldo, `PAID` automático si lo cubre exacto) -- ambos
     * resueltos en `SaleRepository.registerPayment`, acá solo se arma el
     * input y se limpia el diálogo.
     */
    fun onPaymentConfirm() {
        val state = _uiState.value
        val saleId = state.paymentDialogSaleId ?: return
        val amount = state.amount ?: return
        if (!state.canConfirmPayment) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            registerPaymentUseCase(
                RegisterPaymentInput(
                    saleId = saleId,
                    amount = amount,
                    paidAt = System.currentTimeMillis(),
                    method = state.method,
                    notes = null,
                ),
            )
            _uiState.update { it.copy(isSaving = false, paymentDialogSaleId = null, amountText = "") }
        }
    }
}
