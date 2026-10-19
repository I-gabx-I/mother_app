package gt.marcos.joyeria.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.backup.BackupReminder
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Respaldo, recuperar y Excel (Fase 14). Stateless y `@Preview`-able. Guardar es lo primero y lo más grande;
 * recuperar —que reemplaza todo— está aparte, con su aviso, y pide dos confirmaciones si el archivo es más viejo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: BackupUiState,
    onSaveClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onUndoClick: () -> Unit,
    onSpreadsheetClick: (sales: Boolean) -> Unit,
    onMessageDismiss: () -> Unit,
    onImportConfirm: () -> Unit,
    onOlderConfirm: () -> Unit,
    onUndoConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(state)
            state.message?.let { MessageCard(it, onMessageDismiss) }
            state.working?.let { Working(it) }

            Section(title = R.string.backup_section_save, hint = R.string.backup_save_hint) {
                Button(
                    onClick = onSaveClick,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.backup_save_button), style = MaterialTheme.typography.titleMedium) }
            }

            Section(title = R.string.backup_section_restore, hint = R.string.backup_restore_hint) {
                OutlinedButton(
                    onClick = onRestoreClick,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.backup_restore_button)) }
                state.undoAvailableAt?.let { at ->
                    TextButton(
                        onClick = onUndoClick,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.backup_undo_button, backupDate(at))) }
                }
            }

            Section(title = R.string.backup_section_excel, hint = R.string.backup_excel_hint) {
                OutlinedButton(
                    onClick = { onSpreadsheetClick(false) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.backup_excel_inventory)) }
                OutlinedButton(
                    onClick = { onSpreadsheetClick(true) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.backup_excel_sales)) }
            }
        }
    }

    when (val dialog = state.dialog) {
        is BackupDialog.ConfirmImport -> ConfirmImportDialog(dialog.summary, onImportConfirm, onDialogDismiss)
        is BackupDialog.ConfirmOlder -> ConfirmOlderDialog(dialog.summary, onOlderConfirm, onDialogDismiss)
        is BackupDialog.ConfirmUndo -> ConfirmUndoDialog(dialog.createdAt, onUndoConfirm, onDialogDismiss)
        null -> Unit
    }
}

@Composable
private fun Section(title: Int, hint: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(hint), style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun StatusCard(state: BackupUiState) {
    val warning = state.reminder != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val last = state.lastBackupAt
            Text(
                text = if (last == null) stringResource(R.string.backup_status_never) else stringResource(R.string.backup_status_last, backupDate(last)),
                style = MaterialTheme.typography.titleMedium,
            )
            state.reminder?.let {
                Text(
                    text = if (it.neverBackedUp) stringResource(R.string.backup_reminder_never, it.days) else stringResource(R.string.backup_reminder_days, it.days),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun Working(action: BackupAction) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(
                when (action) {
                    BackupAction.EXPORT -> R.string.backup_working_export
                    BackupAction.CSV_INVENTORY, BackupAction.CSV_SALES -> R.string.backup_working_csv
                    BackupAction.READING -> R.string.backup_working_read
                    BackupAction.IMPORT -> R.string.backup_working_import
                    BackupAction.UNDO -> R.string.backup_working_undo
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MessageCard(message: BackupMessage, onDismiss: () -> Unit) {
    val failed = message is BackupMessage.Failed
    val text = when (message) {
        is BackupMessage.Exported -> stringResource(R.string.backup_msg_exported, message.products, message.sales, message.photos)
        is BackupMessage.SpreadsheetReady -> stringResource(R.string.backup_msg_sheet_ready)
        is BackupMessage.Imported -> stringResource(R.string.backup_msg_imported, message.products, message.sales, message.customers)
        BackupMessage.Undone -> stringResource(R.string.backup_msg_undone)
        is BackupMessage.Failed -> stringResource(message.failure.messageRes())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_msg_close)) }
        }
    }
}

@Composable
private fun ConfirmImportDialog(summary: BackupSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_confirm_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        R.string.backup_confirm_body,
                        backupDate(summary.exportedAt),
                        summary.products,
                        summary.sales,
                        summary.customers,
                        summary.photos,
                        summary.currentProducts,
                        summary.currentSales,
                        summary.currentCustomers,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_confirm_yes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_confirm_no)) }
        },
    )
}

@Composable
private fun ConfirmOlderDialog(summary: BackupSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_older_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(
                        R.string.backup_older_body,
                        backupDate(summary.exportedAt),
                        summary.extraProducts,
                        summary.extraSales,
                        summary.extraCustomers,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_older_yes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_confirm_no)) }
        },
    )
}

@Composable
private fun ConfirmUndoDialog(createdAt: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_undo_title)) },
        text = { Text(stringResource(R.string.backup_undo_body, backupDate(createdAt)), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_undo_yes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.backup_undo_no)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BackupScreenPreview() {
    JoyeriaTheme {
        BackupScreen(
            state = BackupUiState(
                lastBackupAt = 1_700_000_000_000,
                reminder = BackupReminder.Due(days = 21, neverBackedUp = false),
                undoAvailableAt = 1_700_000_000_000,
            ),
            onSaveClick = {}, onRestoreClick = {}, onUndoClick = {}, onSpreadsheetClick = {},
            onMessageDismiss = {}, onImportConfirm = {}, onOlderConfirm = {}, onUndoConfirm = {},
            onDialogDismiss = {}, onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupScreenImportDialogPreview() {
    JoyeriaTheme {
        BackupScreen(
            state = BackupUiState(
                dialog = BackupDialog.ConfirmImport(
                    BackupSummary(1_700_000_000_000, 120, 45, 12, 98, 130, 50, 12, backupIsOlder = true),
                ),
            ),
            onSaveClick = {}, onRestoreClick = {}, onUndoClick = {}, onSpreadsheetClick = {},
            onMessageDismiss = {}, onImportConfirm = {}, onOlderConfirm = {}, onUndoConfirm = {},
            onDialogDismiss = {}, onBackClick = {},
        )
    }
}
