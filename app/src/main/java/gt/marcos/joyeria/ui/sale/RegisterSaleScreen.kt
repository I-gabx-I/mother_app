package gt.marcos.joyeria.ui.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.customer.CustomerPickerDropdown
import gt.marcos.joyeria.ui.format.MoneyTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Pantalla "Vender" (Fase 06). Stateless y `@Preview`-able (CLAUDE.md
 * sección 5): recibe estado y lambdas, nunca el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSaleScreen(
    state: RegisterSaleUiState,
    onQueryChange: (String) -> Unit,
    onProductSelected: (Long) -> Unit,
    onLineQtyChange: (index: Int, raw: String) -> Unit,
    onRemoveLineClick: (index: Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onLossConfirmed: () -> Unit,
    onLossDialogDismissed: () -> Unit,
    onResultDismissed: () -> Unit,
    onBackClick: () -> Unit,
    onSaleTypeChange: (SaleTypeChoice) -> Unit,
    onCustomerSelected: (Long) -> Unit,
    onNewCustomerConfirmed: (name: String, phone: String?) -> Unit,
    onInitialPaymentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sale_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        bottomBar = {
            SaleBottomBar(state = state, onDiscountChange = onDiscountChange, onConfirmClick = onConfirmClick)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // D-042: selector arriba de todo, "Al contado" preseleccionado
            // siempre -- el caso común no gana ningún toque de más, el
            // crédito queda a un solo toque, nunca escondido.
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.saleType == SaleTypeChoice.CASH,
                        onClick = { onSaleTypeChange(SaleTypeChoice.CASH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(stringResource(R.string.sale_type_cash))
                    }
                    SegmentedButton(
                        selected = state.saleType == SaleTypeChoice.CREDIT,
                        onClick = { onSaleTypeChange(SaleTypeChoice.CREDIT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(stringResource(R.string.sale_type_credit))
                    }
                }
            }

            if (state.isCredit) {
                item {
                    CustomerPickerDropdown(
                        customers = state.customers,
                        selectedCustomerId = state.selectedCustomerId,
                        onCustomerSelected = onCustomerSelected,
                        onNewCustomerConfirmed = onNewCustomerConfirmed,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MoneyTextField(
                            label = stringResource(R.string.sale_credit_initial_payment_label),
                            value = state.initialPaymentText,
                            onValueChange = onInitialPaymentChange,
                        )
                        if (state.initialPaymentExceedsNet) {
                            Text(
                                text = stringResource(R.string.sale_credit_initial_payment_exceeds),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.product_list_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.lines.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.sale_cart_title), style = MaterialTheme.typography.titleMedium)
                }
                itemsIndexed(state.lines) { index, line ->
                    CartLineRow(
                        line = line,
                        onQtyChange = { raw -> onLineQtyChange(index, raw) },
                        onRemoveClick = { onRemoveLineClick(index) },
                    )
                }
                item { HorizontalDivider() }
            }

            item {
                Text(stringResource(R.string.sale_available_title), style = MaterialTheme.typography.titleMedium)
            }

            if (state.availableProducts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sale_empty_products),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.availableProducts, key = { it.id }) { product ->
                    ProductPickRow(product = product, onClick = { onProductSelected(product.id) })
                }
            }
        }
    }

    if (state.pendingLossConfirmation) {
        val profit = state.profit
        AlertDialog(
            onDismissRequest = onLossDialogDismissed,
            title = { Text(stringResource(R.string.sale_confirm_dialog_title)) },
            text = {
                Text(
                    if (profit == Money.ZERO) {
                        stringResource(R.string.sale_confirm_zero_profit_warning)
                    } else {
                        stringResource(R.string.sale_confirm_loss_warning, Money(-(profit?.cents ?: 0L)).format())
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onLossConfirmed) {
                    Text(stringResource(R.string.sale_confirm_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onLossDialogDismissed) {
                    Text(stringResource(R.string.sale_confirm_dialog_cancel))
                }
            },
        )
    }

    state.result?.let {
        AlertDialog(
            onDismissRequest = onResultDismissed,
            title = { Text(stringResource(R.string.sale_saved_confirmation)) },
            confirmButton = {
                TextButton(onClick = onResultDismissed) {
                    Text(stringResource(R.string.sale_confirm_dialog_confirm))
                }
            },
        )
    }
}

@Composable
private fun SaleBottomBar(
    state: RegisterSaleUiState,
    onDiscountChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoneyTextField(
            label = stringResource(R.string.sale_discount_label),
            value = state.discountText,
            onValueChange = onDiscountChange,
        )
        if (state.discountExceedsSubtotal) {
            Text(
                text = stringResource(R.string.sale_discount_exceeds_subtotal),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        // Sin piezas en el carrito, o con un descuento que ya no tiene
        // sentido (mayor al subtotal), no hay un Total real que mostrar --
        // "Total: -Q500.00" sería un número inventado, la venta ni siquiera
        // se puede guardar en ese estado (mismo espíritu que D-015/D-030:
        // no afirmar un cálculo que no corresponde a nada que ella haya
        // elegido).
        if (state.lines.isNotEmpty() && !state.discountExceedsSubtotal) {
            Text(
                text = stringResource(R.string.sale_total_label, (state.subtotal - (state.discount ?: Money.ZERO)).format()),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.profit?.let { profit ->
                Text(
                    text = stringResource(R.string.sale_profit_label, profit.format()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Button(
            onClick = onConfirmClick,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.sale_confirm_button))
        }
    }
}

@Composable
private fun CartLineRow(line: SaleLineUiState, onQtyChange: (String) -> Unit, onRemoveClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(line.productName, style = MaterialTheme.typography.bodyLarge)
            Text(
                line.subtotal.format(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        QuantityTextField(
            label = stringResource(R.string.sale_line_qty_label),
            value = line.qtyText,
            onValueChange = onQtyChange,
            supportingText = if (line.exceedsStock) {
                stringResource(R.string.sale_line_stock_warning, line.availableStock)
            } else {
                null
            },
            modifier = Modifier.width(120.dp),
        )
        IconButton(onClick = onRemoveClick) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.sale_line_remove))
        }
    }
}

@Composable
private fun ProductPickRow(product: ProductSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (product.photoPath != null) {
                AsyncImage(model = product.photoPath, contentDescription = null, modifier = Modifier.size(56.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.product_list_stock, product.stockQty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(product.salePrice.format(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterSaleScreenPreview() {
    JoyeriaTheme {
        RegisterSaleScreen(
            state = RegisterSaleUiState(
                products = listOf(
                    ProductSummary(
                        id = 1,
                        uid = "XP-000001",
                        name = "Anillo corazón dorado",
                        photoPath = null,
                        stockQty = 3,
                        cost = Money(4000),
                        salePrice = Money(8000),
                    ),
                ),
            ),
            onQueryChange = {},
            onProductSelected = {},
            onLineQtyChange = { _, _ -> },
            onRemoveLineClick = {},
            onDiscountChange = {},
            onConfirmClick = {},
            onLossConfirmed = {},
            onLossDialogDismissed = {},
            onResultDismissed = {},
            onBackClick = {},
            onSaleTypeChange = {},
            onCustomerSelected = {},
            onNewCustomerConfirmed = { _, _ -> },
            onInitialPaymentChange = {},
        )
    }
}
