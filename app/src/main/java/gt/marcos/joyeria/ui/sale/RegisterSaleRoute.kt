package gt.marcos.joyeria.ui.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `RegisterSaleScreen` (CLAUDE.md sección 5). */
@Composable
fun RegisterSaleRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterSaleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RegisterSaleScreen(
        state = state,
        onQueryChange = viewModel::onQueryChanged,
        onProductSelected = viewModel::onProductSelected,
        onLineQtyChange = viewModel::onLineQtyChanged,
        onRemoveLineClick = viewModel::onRemoveLineClick,
        onDiscountChange = viewModel::onDiscountChanged,
        onConfirmClick = viewModel::onConfirmClick,
        onLossConfirmed = viewModel::onLossConfirmed,
        onLossDialogDismissed = viewModel::onLossDialogDismissed,
        onResultDismissed = viewModel::onResultDismissed,
        onBackClick = onBackClick,
        onSaleTypeChange = viewModel::onSaleTypeChanged,
        onCustomerSelected = viewModel::onCustomerSelected,
        onNewCustomerConfirmed = viewModel::onNewCustomerConfirmed,
        onInitialPaymentChange = viewModel::onInitialPaymentChanged,
        modifier = modifier,
    )
}
