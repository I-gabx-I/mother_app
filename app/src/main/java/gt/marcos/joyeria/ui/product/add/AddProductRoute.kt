package gt.marcos.joyeria.ui.product.add

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gt.marcos.joyeria.R

/**
 * Único punto donde esta fase "conoce" el ViewModel (CLAUDE.md sección 5:
 * los Composables reciben estado y lambdas, no ViewModels -- este es el
 * borde delgado entre la navegación/Hilt y `AddProductScreen`, que sí es
 * puro estado+lambdas). `hiltViewModel()` lo acota a esta entrada del
 * `NavHost` (Fase 04, D-023): al navegar afuera y volver, es una
 * instancia nueva, con el formulario en blanco, no la de la visita
 * anterior.
 */
@Composable
fun AddProductRoute(viewModel: AddProductViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isCameraOpen by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.add_product_default_name)

    AddProductScreen(
        state = state,
        onTakePhotoClick = { isCameraOpen = true },
        onCostTextChange = viewModel::onCostTextChanged,
        onSalePriceTextChange = viewModel::onSalePriceTextChanged,
        onNameChange = viewModel::onNameChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onQuantityChange = viewModel::onQuantityChanged,
        onNotesChange = viewModel::onNotesChanged,
        onSaveClick = { viewModel.onSaveClick(defaultName) },
        onSavedConfirmationDismissed = viewModel::onSavedConfirmationDismissed,
        modifier = modifier,
    )

    if (isCameraOpen) {
        CameraCaptureOverlay(
            onImageCaptured = { bitmap, rotationDegrees ->
                viewModel.onPhotoCaptured(bitmap, rotationDegrees)
                isCameraOpen = false
            },
            onDismiss = { isCameraOpen = false },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
