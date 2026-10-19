package gt.marcos.joyeria.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

/** Borde delgado entre navegación/Hilt y `BackupScreen` (CLAUDE.md sección 5): abre el selector de archivos y el de compartir. */
@Composable
fun BackupRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.backup_share_chooser)

    // `*/*`: un respaldo que llega por WhatsApp o Drive puede traer cualquier tipo declarado.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> viewModel.onFilePicked(uri) }

    LaunchedEffect(state.share) {
        val share = state.share ?: return@LaunchedEffect
        BackupShareIntent.share(context, File(share.path), share.mime, shareTitle)
        viewModel.onShareHandled()
    }

    BackupScreen(
        state = state,
        onSaveClick = viewModel::onExportClick,
        onRestoreClick = { picker.launch(arrayOf("*/*")) },
        onUndoClick = viewModel::onUndoClick,
        onSpreadsheetClick = viewModel::onSpreadsheetClick,
        onMessageDismiss = viewModel::onMessageDismiss,
        onImportConfirm = viewModel::onImportConfirm,
        onOlderConfirm = viewModel::onOlderConfirm,
        onUndoConfirm = viewModel::onUndoConfirm,
        onDialogDismiss = viewModel::onDialogDismiss,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
