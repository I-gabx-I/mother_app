package gt.marcos.joyeria.ui.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gt.marcos.joyeria.ui.barcode.BarcodeScanButton
import androidx.compose.ui.res.stringResource
import gt.marcos.joyeria.R

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
        onLinePriceChange = viewModel::onLinePriceChanged,
        onRemoveLineClick = viewModel::onRemoveLineClick,
        onConfirmClick = viewModel::onConfirmClick,
        onLossConfirmed = viewModel::onLossConfirmed,
        onLossDialogDismissed = viewModel::onLossDialogDismissed,
        onResultDismissed = viewModel::onResultDismissed,
        onBackClick = onBackClick,
        onSaleTypeChange = viewModel::onSaleTypeChanged,
        onCustomerSelected = viewModel::onCustomerSelected,
        onClearCustomerClick = viewModel::onClearSelectedCustomer,
        onNewCustomerConfirmed = viewModel::onNewCustomerConfirmed,
        onInitialPaymentChange = viewModel::onInitialPaymentChanged,
        modifier = modifier,
        // Escanear la etiqueta de una pieza la agrega al carrito: es donde ella busca
        // por código cuando está vendiendo. Sin existencias no se puede vender.
        searchTrailingIcon = {
            val noStock = stringResource(R.string.scanner_no_stock)
            BarcodeScanButton(onFound = { found ->
                if (found.stockQty > 0) {
                    viewModel.onProductSelected(found.productId)
                    null
                } else {
                    noStock
                }
            })
        },
    )
}
