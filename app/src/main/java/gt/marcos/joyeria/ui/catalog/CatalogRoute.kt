package gt.marcos.joyeria.ui.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gt.marcos.joyeria.R

/**
 * Borde delgado entre navegación/Hilt y las pantallas del catálogo (CLAUDE.md
 * sección 5). Un solo `ViewModel` y tres pantallas según el paso: elegir,
 * armando/error, y vista previa con «Compartir». La tecla de volver, fuera del
 * paso de elegir, no sale de la pantalla: vuelve a elegir con lo elegido intacto.
 */
@Composable
fun CatalogRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.catalog_share_chooser_title)

    BackHandler(enabled = state.step !is CatalogStep.Picking) { viewModel.onBackToPicking() }

    when (val step = state.step) {
        CatalogStep.Picking -> CatalogPickerScreen(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onPieceToggled = viewModel::onPieceToggled,
            onSelectAllVisibleClick = viewModel::onSelectAllVisibleClick,
            onDeselectVisibleClick = viewModel::onDeselectVisibleClick,
            onDeselectAllClick = viewModel::onDeselectAllClick,
            onCreateClick = viewModel::onCreateClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
        is CatalogStep.Generating, CatalogStep.Failed -> CatalogStatusScreen(
            step = step,
            onBackToPickingClick = viewModel::onBackToPicking,
            onRetryClick = viewModel::onCreateClick,
            modifier = modifier,
        )
        is CatalogStep.Ready -> CatalogResultScreen(
            pages = step.pages,
            onShareClick = { shareCatalog(context, step.pages, chooserTitle) },
            onBackToPickingClick = viewModel::onBackToPicking,
            modifier = modifier,
        )
    }
}
