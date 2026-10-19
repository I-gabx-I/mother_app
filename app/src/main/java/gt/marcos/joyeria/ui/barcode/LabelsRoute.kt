package gt.marcos.joyeria.ui.barcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gt.marcos.joyeria.R
import java.io.File

/** Borde delgado entre navegación/Hilt y `LabelsScreen` (CLAUDE.md sección 5). Imprime o comparte el PDF cuando está listo. */
@Composable
fun LabelsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LabelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val printJob = stringResource(R.string.labels_print_job)
    val shareTitle = stringResource(R.string.labels_share_chooser)

    LaunchedEffect(state.output) {
        val output = state.output ?: return@LaunchedEffect
        val file = File(output.path)
        when (output.action) {
            LabelAction.PRINT, LabelAction.TEST_SHEET -> LabelOutput.print(context, printJob, file, output.pageCount)
            LabelAction.SHARE -> LabelOutput.share(context, file, shareTitle)
        }
        viewModel.onOutputHandled()
    }

    LabelsScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onPieceToggled = viewModel::onPieceToggled,
        onSelectAllVisibleClick = viewModel::onSelectAllVisibleClick,
        onDeselectVisibleClick = viewModel::onDeselectVisibleClick,
        onDeselectAllClick = viewModel::onDeselectAllClick,
        onPrintClick = viewModel::onPrintClick,
        onShareClick = viewModel::onShareClick,
        onTestSheetClick = viewModel::onTestSheetClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
