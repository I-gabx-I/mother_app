package gt.marcos.joyeria.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `ReportsScreen` (CLAUDE.md sección 5). */
@Composable
fun ReportsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReportsScreen(
        state = state,
        onPreviousMonthClick = viewModel::onPreviousMonthClick,
        onNextMonthClick = viewModel::onNextMonthClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
