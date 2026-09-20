package gt.marcos.joyeria.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.SaleRepository
import gt.marcos.joyeria.domain.usecase.CancelSaleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). */
@HiltViewModel
class TodaySalesViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val cancelSaleUseCase: CancelSaleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodaySalesUiState())
    val uiState: StateFlow<TodaySalesUiState> = _uiState.asStateFlow()

    init {
        val (startMillis, endMillis) = todayBounds()
        viewModelScope.launch {
            saleRepository.observeBetween(startMillis, endMillis).collectLatest { sales ->
                _uiState.update { it.copy(sales = sales) }
            }
        }
    }

    fun onSaleClick(saleId: Long) {
        viewModelScope.launch {
            val detail = saleRepository.getDetail(saleId)
            _uiState.update { it.copy(selectedSale = detail) }
        }
    }

    fun onDetailDismissed() {
        _uiState.update { it.copy(selectedSale = null, showCancelConfirmation = false, cancelReasonText = "") }
    }

    fun onCancelClick() {
        _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    fun onCancelReasonChange(value: String) {
        _uiState.update { it.copy(cancelReasonText = value) }
    }

    fun onCancelDismiss() {
        _uiState.update { it.copy(showCancelConfirmation = false, cancelReasonText = "") }
    }

    /**
     * No hay `DELETE`: se anula (D-006), con motivo y fecha. El motivo es
     * opcional (D-036) -- texto en blanco se guarda como `null`, no como
     * cadena vacía. El stock vuelve a sumarse (`SaleRepository.cancel`).
     */
    fun onCancelConfirm() {
        val saleId = _uiState.value.selectedSale?.id ?: return
        val reason = _uiState.value.cancelReasonText.trim().ifBlank { null }
        viewModelScope.launch {
            cancelSaleUseCase(saleId, cancelledAt = System.currentTimeMillis(), cancelReason = reason)
            _uiState.update { it.copy(selectedSale = null, showCancelConfirmation = false, cancelReasonText = "") }
        }
    }

    // Límites del día con la zona horaria del dispositivo -- el ajuste
    // fino para reportes (Fase 09) no se resuelve acá (ESTADO.md, "Fase
    // 06 -- Plan").
    private fun todayBounds(): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return start.timeInMillis to end.timeInMillis
    }
}
