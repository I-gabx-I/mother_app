package gt.marcos.joyeria.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.AppSettingRepository
import gt.marcos.joyeria.data.repository.ReportRepository
import gt.marcos.joyeria.domain.reports.ReportDates
import gt.marcos.joyeria.domain.reports.ReportMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). El mes
 * mostrado es estado propio de este `ViewModel` (no de `ReportRepository`,
 * que es de solo lectura y no sabe qué mes está mirando ella); cambiar de
 * mes vuelve a consultar sola (`flatMapLatest`), mismo patrón que
 * `ProductListViewModel` con la búsqueda/categoría.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val appSettingRepository: AppSettingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val selectedMonth = MutableStateFlow<ReportMonth?>(null)

    init {
        val now = System.currentTimeMillis()
        val currentMonth = ReportDates.monthOf(now)
        _uiState.update { it.copy(currentMonth = currentMonth) }
        selectedMonth.value = currentMonth

        viewModelScope.launch {
            selectedMonth.filterNotNull()
                .flatMapLatest { month -> reportRepository.observeMonthlyReport(month) }
                .collectLatest { report -> _uiState.update { it.copy(monthly = report) } }
        }
        viewModelScope.launch {
            val staleStockDays = appSettingRepository.getStaleStockDays()
            val lowStockThreshold = appSettingRepository.getLowStockThreshold()
            reportRepository.observeInventoryReport(now, staleStockDays, lowStockThreshold)
                .collectLatest { report -> _uiState.update { it.copy(inventory = report) } }
        }
    }

    fun onPreviousMonthClick() {
        val month = selectedMonth.value ?: return
        selectedMonth.value = ReportDates.previous(month)
    }

    fun onNextMonthClick() {
        if (!_uiState.value.canGoToNextMonth) return
        val month = selectedMonth.value ?: return
        selectedMonth.value = ReportDates.next(month)
    }
}
