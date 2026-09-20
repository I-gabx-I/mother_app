package gt.marcos.joyeria.ui.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.PurchaseWarningLevel
import gt.marcos.joyeria.data.repository.RegisterPurchaseResult
import gt.marcos.joyeria.ui.format.MoneyTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.text.DateFormat
import java.util.Date

/**
 * Pantalla de registro de compra a mayorista (Fase 05). Stateless y
 * `@Preview`-able (CLAUDE.md sección 5): recibe estado y lambdas, nunca
 * el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPurchaseScreen(
    state: RegisterPurchaseUiState,
    onSupplierChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onLineProductSelected: (index: Int, productId: Long) -> Unit,
    onLineQtyChange: (index: Int, raw: String) -> Unit,
    onLineUnitCostChange: (index: Int, raw: String) -> Unit,
    onRemoveLineClick: (index: Int) -> Unit,
    onAddLineClick: () -> Unit,
    onExtraCostChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onResultDismissed: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.purchase_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.products.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.purchase_no_products),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.supplier,
                    onValueChange = onSupplierChange,
                    label = { Text(stringResource(R.string.purchase_supplier_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.purchase_date_label), style = MaterialTheme.typography.bodyMedium)
                        Text(formatDate(state.purchasedAtMillis), style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.purchase_date_change))
                    }
                }

                state.lines.forEachIndexed { index, line ->
                    PurchaseLineCard(
                        index = index,
                        line = line,
                        products = state.products,
                        showRemove = state.lines.size > 1,
                        onProductSelected = { productId -> onLineProductSelected(index, productId) },
                        onQtyChange = { raw -> onLineQtyChange(index, raw) },
                        onUnitCostChange = { raw -> onLineUnitCostChange(index, raw) },
                        onRemoveClick = { onRemoveLineClick(index) },
                    )
                }

                OutlinedButton(onClick = onAddLineClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.purchase_add_line))
                }

                MoneyTextField(
                    label = stringResource(R.string.purchase_extra_cost_label),
                    value = state.extraCostText,
                    onValueChange = onExtraCostChange,
                )

                Button(
                    onClick = onSaveClick,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.purchase_save_button))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.purchasedAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onDateChange)
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.purchase_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.purchase_date_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    state.result?.let { result ->
        PurchaseResultDialog(result = result, onDismiss = onResultDismissed)
    }
}

@Composable
private fun PurchaseLineCard(
    index: Int,
    line: PurchaseLineUiState,
    products: List<gt.marcos.joyeria.data.repository.ProductSummary>,
    showRemove: Boolean,
    onProductSelected: (Long) -> Unit,
    onQtyChange: (String) -> Unit,
    onUnitCostChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.purchase_line_title, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (showRemove) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.purchase_line_remove))
                    }
                }
            }
            ProductPickerDropdown(
                products = products,
                selectedProductId = line.productId,
                onProductSelected = onProductSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = line.qtyText,
                onValueChange = onQtyChange,
                label = { Text(stringResource(R.string.purchase_line_qty_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            MoneyTextField(
                label = stringResource(R.string.purchase_line_unit_cost_label),
                value = line.unitCostText,
                onValueChange = onUnitCostChange,
            )
        }
    }
}

@Composable
private fun PurchaseResultDialog(result: RegisterPurchaseResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (result.lineWarnings.isEmpty() && !result.retroactiveWarning) {
                    stringResource(R.string.purchase_saved_confirmation)
                } else {
                    stringResource(R.string.purchase_warnings_title)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.lineWarnings.forEach { warning ->
                    val text = when (warning.level) {
                        PurchaseWarningLevel.PROFIT_ALERT -> stringResource(
                            R.string.purchase_warning_profit,
                            warning.productName,
                            warning.newCost.format(),
                        )
                        PurchaseWarningLevel.MARGIN_WARNING -> stringResource(
                            R.string.purchase_warning_margin,
                            warning.productName,
                            warning.newCost.format(),
                        )
                        PurchaseWarningLevel.NONE -> null
                    }
                    if (text != null) {
                        val color = if (warning.level == PurchaseWarningLevel.PROFIT_ALERT) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = text,
                            color = color,
                            style = if (warning.level == PurchaseWarningLevel.PROFIT_ALERT) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                        )
                    }
                }
                if (result.retroactiveWarning) {
                    Text(
                        text = stringResource(R.string.purchase_warning_retroactive),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.purchase_warnings_dismiss))
            }
        },
    )
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.LONG).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun RegisterPurchaseScreenPreview() {
    JoyeriaTheme {
        RegisterPurchaseScreen(
            state = RegisterPurchaseUiState(
                products = listOf(
                    gt.marcos.joyeria.data.repository.ProductSummary(
                        id = 1,
                        uid = "XP-000001",
                        name = "Anillo corazón dorado",
                        photoPath = null,
                        stockQty = 3,
                        cost = gt.marcos.joyeria.domain.model.Money(4000),
                        salePrice = gt.marcos.joyeria.domain.model.Money(8000),
                    ),
                ),
            ),
            onSupplierChange = {},
            onDateChange = {},
            onLineProductSelected = { _, _ -> },
            onLineQtyChange = { _, _ -> },
            onLineUnitCostChange = { _, _ -> },
            onRemoveLineClick = {},
            onAddLineClick = {},
            onExtraCostChange = {},
            onSaveClick = {},
            onResultDismissed = {},
            onBackClick = {},
        )
    }
}
