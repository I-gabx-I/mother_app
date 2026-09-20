package gt.marcos.joyeria.ui.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `TodaySalesScreen` (CLAUDE.md sección 5). */
@Composable
fun TodaySalesRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodaySalesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TodaySalesScreen(
        state = state,
        onSaleClick = viewModel::onSaleClick,
        onDetailDismissed = viewModel::onDetailDismissed,
        onCancelClick = viewModel::onCancelClick,
        onCancelReasonChange = viewModel::onCancelReasonChange,
        onCancelDismiss = viewModel::onCancelDismiss,
        onCancelConfirm = viewModel::onCancelConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
