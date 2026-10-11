package gt.marcos.joyeria.ui.credit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `CustomerProfileScreen` (CLAUDE.md sección 5). */
@Composable
fun CustomerProfileRoute(
    onSellClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CustomerProfileScreen(
        state = state,
        onSellClick = { onSellClick(state.customerId) },
        onEditClick = viewModel::onEditClick,
        onFormNameChange = viewModel::onFormNameChange,
        onFormPhoneChange = viewModel::onFormPhoneChange,
        onFormNotesChange = viewModel::onFormNotesChange,
        onFormDismiss = viewModel::onFormDismiss,
        onFormConfirm = viewModel::onFormConfirm,
        onRegisterPaymentClick = viewModel::onRegisterPaymentClick,
        onPaymentDialogDismiss = viewModel::onPaymentDialogDismiss,
        onAmountChange = viewModel::onAmountChanged,
        onMethodChange = viewModel::onMethodChanged,
        onPaymentConfirm = viewModel::onPaymentConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
