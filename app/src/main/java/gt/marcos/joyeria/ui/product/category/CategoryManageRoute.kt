package gt.marcos.joyeria.ui.product.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `CategoryManageScreen` (CLAUDE.md sección 5). */
@Composable
fun CategoryManageRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryManageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CategoryManageScreen(
        state = state,
        onNewCategoryNameChange = viewModel::onNewCategoryNameChanged,
        onAddClick = viewModel::onAddClick,
        onRenameClick = viewModel::onRenameClick,
        onRenamingNameChange = viewModel::onRenamingNameChanged,
        onRenameDismiss = viewModel::onRenameDismiss,
        onRenameConfirm = viewModel::onRenameConfirm,
        onArchiveClick = viewModel::onArchiveClick,
        onArchiveDismiss = viewModel::onArchiveDismiss,
        onArchiveConfirm = viewModel::onArchiveConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
