package gt.marcos.joyeria.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.AddCustomerInput
import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.CustomerHasPendingBalanceException
import gt.marcos.joyeria.data.repository.CustomerRepository
import gt.marcos.joyeria.data.repository.EditCustomerInput
import gt.marcos.joyeria.data.repository.SaleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). */
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            saleRepository.observeCustomerDebts().collectLatest { debts ->
                _uiState.update { it.copy(debts = debts) }
            }
        }
        viewModelScope.launch {
            customerRepository.observeActive().collectLatest { customers ->
                _uiState.update { it.copy(allCustomers = customers) }
            }
        }
    }

    fun onTabSelected(tab: CustomersTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun onAddClick() {
        _uiState.update {
            it.copy(showFormDialog = true, editingCustomerId = null, formName = "", formPhone = "", formNotes = "")
        }
    }

    fun onEditClick(customer: Customer) {
        _uiState.update {
            it.copy(
                showFormDialog = true,
                editingCustomerId = customer.id,
                formName = customer.name,
                formPhone = customer.phone.orEmpty(),
                formNotes = customer.notes.orEmpty(),
            )
        }
    }

    fun onFormDismiss() {
        _uiState.update { it.copy(showFormDialog = false, editingCustomerId = null) }
    }

    fun onFormNameChange(value: String) {
        _uiState.update { it.copy(formName = value) }
    }

    fun onFormPhoneChange(value: String) {
        _uiState.update { it.copy(formPhone = value) }
    }

    fun onFormNotesChange(value: String) {
        _uiState.update { it.copy(formNotes = value) }
    }

    fun onFormConfirm() {
        val state = _uiState.value
        if (!state.canSaveForm) return
        val name = state.formName.trim()
        val phone = state.formPhone.trim().ifBlank { null }
        val notes = state.formNotes.trim().ifBlank { null }
        val editingId = state.editingCustomerId

        viewModelScope.launch {
            if (editingId != null) {
                customerRepository.update(EditCustomerInput(id = editingId, name = name, phone = phone, notes = notes))
            } else {
                customerRepository.add(AddCustomerInput(name = name, phone = phone, notes = notes))
            }
            _uiState.update { it.copy(showFormDialog = false, editingCustomerId = null) }
        }
    }

    fun onArchiveClick(customer: Customer) {
        _uiState.update { it.copy(archiveTarget = customer, archiveBlocked = null) }
    }

    fun onArchiveDismiss() {
        _uiState.update { it.copy(archiveTarget = null, archiveBlocked = null) }
    }

    /** D-006: nunca se borra. D-039: rechaza con mensaje claro si hay saldo pendiente. */
    fun onArchiveConfirm() {
        val target = _uiState.value.archiveTarget ?: return
        viewModelScope.launch {
            try {
                customerRepository.archive(target.id)
                _uiState.update { it.copy(archiveTarget = null, archiveBlocked = null) }
            } catch (e: CustomerHasPendingBalanceException) {
                _uiState.update {
                    it.copy(archiveBlocked = ArchiveBlockedInfo(e.pendingCents, e.pendingSaleCount))
                }
            }
        }
    }
}
