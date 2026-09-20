package gt.marcos.joyeria.ui.purchase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `RegisterPurchaseScreen` (CLAUDE.md sección 5). */
@Composable
fun RegisterPurchaseRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterPurchaseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RegisterPurchaseScreen(
        state = state,
        onSupplierChange = viewModel::onSupplierChanged,
        onDateChange = viewModel::onPurchasedAtSelected,
        onLineProductSelected = viewModel::onLineProductSelected,
        onLineQtyChange = viewModel::onLineQtyChanged,
        onLineUnitCostChange = viewModel::onLineUnitCostChanged,
        onRemoveLineClick = viewModel::onRemoveLineClick,
        onAddLineClick = viewModel::onAddLineClick,
        onExtraCostChange = viewModel::onExtraCostChanged,
        onSaveClick = viewModel::onSaveClick,
        onResultDismissed = viewModel::onResultDismissed,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
