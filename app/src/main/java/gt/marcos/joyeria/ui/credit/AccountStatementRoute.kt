package gt.marcos.joyeria.ui.credit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `AccountStatementScreen` (CLAUDE.md sección 5). */
@Composable
fun AccountStatementRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountStatementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AccountStatementScreen(
        state = state,
        onRegisterPaymentClick = viewModel::onRegisterPaymentClick,
        onPaymentDialogDismiss = viewModel::onPaymentDialogDismiss,
        onAmountChange = viewModel::onAmountChanged,
        onMethodChange = viewModel::onMethodChanged,
        onPaymentConfirm = viewModel::onPaymentConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
