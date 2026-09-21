package gt.marcos.joyeria.ui.credit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `CustomersScreen` (CLAUDE.md sección 5). */
@Composable
fun CustomersRoute(
    onDebtClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CustomersScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onDebtClick = onDebtClick,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onFormNameChange = viewModel::onFormNameChange,
        onFormPhoneChange = viewModel::onFormPhoneChange,
        onFormNotesChange = viewModel::onFormNotesChange,
        onFormDismiss = viewModel::onFormDismiss,
        onFormConfirm = viewModel::onFormConfirm,
        onArchiveClick = viewModel::onArchiveClick,
        onArchiveDismiss = viewModel::onArchiveDismiss,
        onArchiveConfirm = viewModel::onArchiveConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
