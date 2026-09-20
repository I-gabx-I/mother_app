package gt.marcos.joyeria.ui.product.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.format.MoneyTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.product.CategoryDropdown

/**
 * Pantalla de detalle/edición. Stateless y `@Preview`-able (CLAUDE.md
 * sección 5): recibe estado y lambdas, nunca el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    state: ProductEditUiState,
    onNameChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onCostTextChange: (String) -> Unit,
    onSalePriceTextChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSupplierChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSavedConfirmationDismissed: () -> Unit,
    onArchiveClick: () -> Unit,
    onArchiveDismiss: () -> Unit,
    onArchiveConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.uid.isNotEmpty()) "${stringResource(R.string.product_edit_title)} · ${state.uid}" else stringResource(R.string.product_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    // Mismo fix que AddProductScreen: imePadding() antes de
                    // verticalScroll(), para que el teclado encoja el
                    // contenedor que scrollea en vez de taparlo (mismo bug,
                    // misma clase de pantalla).
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProductPhoto(photoPath = state.photoPath)

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.product_edit_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryDropdown(
                        categories = state.categories,
                        selectedCategoryId = state.categoryId,
                        onCategorySelected = onCategorySelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MoneyTextField(
                        label = stringResource(R.string.add_product_cost_label),
                        value = state.costText,
                        onValueChange = onCostTextChange,
                    )
                    MoneyTextField(
                        label = stringResource(R.string.add_product_sale_price_label),
                        value = state.salePriceText,
                        onValueChange = onSalePriceTextChange,
                    )
                    state.profit?.let { profit ->
                        Text(
                            text = stringResource(R.string.add_product_profit, profit.format()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    OutlinedTextField(
                        value = state.quantityText,
                        onValueChange = onQuantityChange,
                        label = { Text(stringResource(R.string.product_edit_quantity_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.supplier,
                        onValueChange = onSupplierChange,
                        label = { Text(stringResource(R.string.product_edit_supplier_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = onNotesChange,
                        label = { Text(stringResource(R.string.product_edit_notes_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = onSaveClick,
                        enabled = state.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(stringResource(R.string.product_edit_save_button))
                    }

                    OutlinedButton(
                        onClick = onArchiveClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(stringResource(R.string.product_edit_archive_button))
                    }
                }
            }

            if (state.savedConfirmationVisible) {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text(stringResource(R.string.product_edit_saved_confirmation))
                }
                LaunchedEffectDismiss(onSavedConfirmationDismissed)
            }
        }

        if (state.showArchiveConfirmation) {
            AlertDialog(
                onDismissRequest = onArchiveDismiss,
                title = { Text(stringResource(R.string.product_edit_archive_confirm_title)) },
                text = { Text(stringResource(R.string.product_edit_archive_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = onArchiveConfirm) {
                        Text(stringResource(R.string.product_edit_archive_confirm_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onArchiveDismiss) {
                        Text(stringResource(R.string.product_edit_archive_confirm_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun ProductPhoto(photoPath: String?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = photoPath,
                contentDescription = stringResource(R.string.add_product_photo_description),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Descarta la confirmación de guardado sola, después de un momento --
 * mismo espíritu que el `Snackbar` clickeable de alta rápida, pero acá
 * se cierra sin que ella tenga que tocarlo (no bloquea el flujo de edición).
 */
@Composable
private fun LaunchedEffectDismiss(onDismiss: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2_000)
        onDismiss()
    }
}
