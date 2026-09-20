package gt.marcos.joyeria.ui.product.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `ProductEditScreen` (CLAUDE.md sección 5). */
@Composable
fun ProductEditRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Ya se archivó: no queda nada que editar, vuelve sola al listado.
    LaunchedEffect(state.archived) {
        if (state.archived) onBackClick()
    }

    ProductEditScreen(
        state = state,
        onNameChange = viewModel::onNameChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onCostTextChange = viewModel::onCostTextChanged,
        onSalePriceTextChange = viewModel::onSalePriceTextChanged,
        onQuantityChange = viewModel::onQuantityChanged,
        onSupplierChange = viewModel::onSupplierChanged,
        onNotesChange = viewModel::onNotesChanged,
        onSaveClick = viewModel::onSaveClick,
        onSavedConfirmationDismissed = viewModel::onSavedConfirmationDismissed,
        onArchiveClick = viewModel::onArchiveClick,
        onArchiveDismiss = viewModel::onArchiveDismiss,
        onArchiveConfirm = viewModel::onArchiveConfirm,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
