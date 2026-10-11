package gt.marcos.joyeria.ui.product.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Administración de categorías (Fase 09), a la que se entra desde el menú
 * de Inventario -- no desde el inicio (CLAUDE.md sección 6: dos acciones
 * grandes, nada de tablero). Stateless y `@Preview`-able (sección 5):
 * recibe estado y lambdas, nunca el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    state: CategoryManageUiState,
    onNewCategoryNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onRenameClick: (Category) -> Unit,
    onRenamingNameChange: (String) -> Unit,
    onRenameDismiss: () -> Unit,
    onRenameConfirm: () -> Unit,
    onArchiveClick: (Category) -> Unit,
    onArchiveDismiss: () -> Unit,
    onArchiveConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Solo tiene sentido contra "Agregar" mientras no hay un
            // renombre en curso -- las dos acciones nunca compiten a la
            // vez, así que un solo mensaje de error alcanza.
            val addFieldError = state.duplicateNameAttempt?.takeIf { state.renamingCategoryId == null }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = state.newCategoryName,
                    onValueChange = onNewCategoryNameChange,
                    label = { Text(stringResource(R.string.category_manage_new_category_label)) },
                    singleLine = true,
                    isError = addFieldError != null,
                    supportingText = addFieldError?.let { name ->
                        { Text(stringResource(R.string.category_manage_duplicate_name_error, name)) }
                    },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onAddClick, enabled = state.canAddCategory, modifier = Modifier.height(56.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.category_manage_add_button))
                }
            }

            if (state.categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.category_manage_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.activeCategories, key = { "active-${it.id}" }) { category ->
                        if (state.renamingCategoryId == category.id) {
                            RenamingCategoryRow(
                                name = state.renamingName,
                                error = state.duplicateNameAttempt,
                                canConfirm = state.canConfirmRename,
                                onNameChange = onRenamingNameChange,
                                onConfirm = onRenameConfirm,
                                onCancel = onRenameDismiss,
                            )
                        } else {
                            ActiveCategoryRow(
                                category = category,
                                onRenameClick = { onRenameClick(category) },
                                onArchiveClick = { onArchiveClick(category) },
                            )
                        }
                    }
                    if (state.archivedCategories.isNotEmpty()) {
                        item(key = "archived-header") {
                            Text(
                                text = stringResource(R.string.category_manage_archived_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(state.archivedCategories, key = { "archived-${it.id}" }) { category ->
                            ArchivedCategoryRow(category = category)
                        }
                    }
                }
            }
        }
    }

    state.archiveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onArchiveDismiss,
            title = { Text(stringResource(R.string.category_manage_archive_confirm_title)) },
            text = { Text(stringResource(R.string.category_manage_archive_confirm_message, target.name)) },
            confirmButton = {
                TextButton(onClick = onArchiveConfirm) {
                    Text(stringResource(R.string.category_manage_archive_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onArchiveDismiss) {
                    Text(stringResource(R.string.category_manage_archive_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun ActiveCategoryRow(
    category: Category,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onRenameClick) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.category_manage_rename_button))
            }
            IconButton(onClick = onArchiveClick) {
                Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.category_manage_archive_button))
            }
        }
    }
}

@Composable
private fun RenamingCategoryRow(
    name: String,
    error: String?,
    canConfirm: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { duplicateName ->
                    { Text(stringResource(R.string.category_manage_duplicate_name_error, duplicateName)) }
                },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onConfirm, enabled = canConfirm) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.category_manage_rename_confirm))
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.category_manage_rename_cancel))
            }
        }
    }
}

// Sin sufijo "(archivada)" acá -- ya están bajo el encabezado "Archivadas",
// a diferencia de CategoryDropdown/CategoryFilterDropdown, donde el
// sufijo es lo único que distingue una archivada en medio de las activas.
@Composable
private fun ArchivedCategoryRow(category: Category) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManageScreenPreview() {
    JoyeriaTheme {
        CategoryManageScreen(
            state = CategoryManageUiState(
                categories = listOf(
                    Category(id = 1, name = "Anillos", archived = false),
                    Category(id = 2, name = "Pulseras", archived = true),
                ),
            ),
            onNewCategoryNameChange = {},
            onAddClick = {},
            onRenameClick = {},
            onRenamingNameChange = {},
            onRenameDismiss = {},
            onRenameConfirm = {},
            onArchiveClick = {},
            onArchiveDismiss = {},
            onArchiveConfirm = {},
            onBackClick = {},
        )
    }
}
