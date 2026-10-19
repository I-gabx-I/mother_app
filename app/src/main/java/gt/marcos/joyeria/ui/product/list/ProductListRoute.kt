package gt.marcos.joyeria.ui.product.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gt.marcos.joyeria.ui.barcode.BarcodeScanButton

/** Borde delgado entre navegación/Hilt y `ProductListScreen` (CLAUDE.md sección 5). */
@Composable
fun ProductListRoute(
    onProductClick: (Long) -> Unit,
    onManageCategoriesClick: () -> Unit,
    onLabelsClick: () -> Unit,
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
        onManageCategoriesClick = onManageCategoriesClick,
        onLabelsClick = onLabelsClick,
        onBackClick = onBackClick,
        modifier = modifier,
        // Escanear una etiqueta abre la pieza, igual que tocarla en la lista.
        searchTrailingIcon = {
            BarcodeScanButton(onFound = { found ->
                onProductClick(found.productId)
                null
            })
        },
    )
}
