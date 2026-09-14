package gt.marcos.joyeria.ui.product.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Borde delgado entre navegación/Hilt y `ProductListScreen` (CLAUDE.md sección 5). */
@Composable
fun ProductListRoute(
    onProductClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProductListScreen(
        state = state,
        onQueryChange = viewModel::onQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onProductClick = onProductClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
