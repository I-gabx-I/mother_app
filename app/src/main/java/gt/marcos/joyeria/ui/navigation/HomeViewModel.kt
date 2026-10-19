package gt.marcos.joyeria.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.BackupStatusRepository
import gt.marcos.joyeria.domain.backup.BackupReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Lo único que el inicio necesita saber además de sus botones: si toca avisarle que haga un respaldo (Fase 14). */
data class HomeUiState(val backupReminder: BackupReminder.Due? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(
    status: BackupStatusRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = status.observeReminderInputs()
        .map { HomeUiState(BackupReminder.due(it.lastBackupAt, it.firstDataAt, System.currentTimeMillis())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
