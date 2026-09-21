package gt.marcos.joyeria.ui.credit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.CustomerRepository
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
 * Único `StateFlow<UiState>` del estado de cuenta (CLAUDE.md sección 5).
 * `customerId` llega por `SavedStateHandle` (argumento de navegación),
 * mismo patrón que `ProductEditViewModel` (Fase 04).
 */
@HiltViewModel
class AccountStatementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val registerPaymentUseCase: RegisterPaymentUseCase,
) : ViewModel() {

    private val customerId: Long = checkNotNull(savedStateHandle["customerId"]) {
        "AccountStatementViewModel necesita un argumento de navegación customerId"
    }

    private val _uiState = MutableStateFlow(AccountStatementUiState(customerId = customerId))
    val uiState: StateFlow<AccountStatementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val customer = customerRepository.getById(customerId)
            _uiState.update { it.copy(customerName = customer?.name.orEmpty()) }
        }
        viewModelScope.launch {
            saleRepository.observeAccountStatements(customerId).collectLatest { statements ->
                _uiState.update { it.copy(statements = statements) }
            }
        }
    }

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
