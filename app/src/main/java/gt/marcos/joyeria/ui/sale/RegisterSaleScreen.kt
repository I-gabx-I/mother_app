package gt.marcos.joyeria.ui.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
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
import gt.marcos.joyeria.ui.format.QuantityTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import gt.marcos.joyeria.ui.theme.JoyeriaAlertDialog

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
    onLinePriceChange: (index: Int, raw: String) -> Unit,
    onRemoveLineClick: (index: Int) -> Unit,
    onConfirmClick: () -> Unit,
    onLossConfirmed: () -> Unit,
    onLossDialogDismissed: () -> Unit,
    onResultDismissed: () -> Unit,
    onBackClick: () -> Unit,
    onSaleTypeChange: (SaleTypeChoice) -> Unit,
    onCustomerSelected: (Long) -> Unit,
    onClearCustomerClick: () -> Unit,
    onNewCustomerConfirmed: (name: String, phone: String?) -> Unit,
    onInitialPaymentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchTrailingIcon: (@Composable () -> Unit)? = null,
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
            SaleBottomBar(state = state, onConfirmClick = onConfirmClick)
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

            // B-07, Fase 08: entrando desde el perfil de una clienta, la
            // venta de contado se le asocia. Sin este encabezado ella no
            // tendría forma de ver ni de corregir a quién se la están
            // cargando. Es una línea de texto y un "Cambiar", no el
            // desplegable entero: la pantalla más usada no gana un control
            // más (CLAUDE.md sección 6).
            if (state.showsCashCustomerHeader) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = state.selectedCustomer?.name?.let { name ->
                                stringResource(R.string.sale_customer_header, name)
                            } ?: stringResource(R.string.sale_customer_none),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.selectedCustomerId != null) {
                            TextButton(onClick = onClearCustomerClick) {
                                Text(stringResource(R.string.sale_customer_change))
                            }
                        }
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
                    trailingIcon = searchTrailingIcon,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.lines.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.sale_cart_title), style = MaterialTheme.typography.titleMedium)
                }
                // `key` por producto: cada fila del carrito tiene campos con
                // estado de entrada propio, y sin `key` ese estado queda atado a
                // la posición en la lista (ver CLAUDE.md sección 5 y D-048).
                //
                // El prefijo "cart-" no es decorativo: el carrito y la lista de
                // piezas disponibles son dos `items` del MISMO `LazyColumn`, así
                // que **comparten el espacio de claves**. Con `line.productId`
                // pelado, la misma pieza en el carrito y en la lista de abajo
                // producían la clave repetida y Compose tiraba
                // `IllegalArgumentException: Key "1" was already used` al
                // agregar la primera pieza. Encontrado corriendo la app, no por
                // los tests ni por el compilador (D-048).
                itemsIndexed(state.lines, key = { _, line -> "cart-${line.productId}" }) { index, line ->
                    CartLineRow(
                        line = line,
                        onQtyChange = { raw -> onLineQtyChange(index, raw) },
                        onPriceChange = { raw -> onLinePriceChange(index, raw) },
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
                items(state.availableProducts, key = { "product-${it.id}" }) { product ->
                    ProductPickRow(product = product, onClick = { onProductSelected(product.id) })
                }
            }
        }
    }

    if (state.pendingLossConfirmation) {
        JoyeriaAlertDialog(
            onDismissRequest = onLossDialogDismissed,
            title = { Text(stringResource(R.string.sale_confirm_dialog_title)) },
            text = {
                // D-046: una línea por pieza en pérdida, con su monto. Antes
                // era un solo texto sobre el total de la venta, que no decía
                // en qué pieza estaba el problema -- y con precio por pieza,
                // saber cuál es lo único que le permite arreglarlo.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sale_confirm_loss_lines_intro))
                    state.lossLines.forEach { line ->
                        val lineProfit = line.profit ?: Money.ZERO
                        Text(
                            text = if (lineProfit == Money.ZERO) {
                                stringResource(R.string.sale_confirm_zero_profit_line, line.productName)
                            } else {
                                stringResource(
                                    R.string.sale_confirm_loss_line,
                                    line.productName,
                                    Money(-lineProfit.cents).format(),
                                )
                            },
                        )
                    }
                    Text(stringResource(R.string.sale_confirm_loss_lines_question))
                }
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
        JoyeriaAlertDialog(
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
    onConfirmClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // El `Scaffold` de Material 3 aplica los window insets SOLO al
            // `innerPadding` del contenido; el slot `bottomBar` no recibe
            // ninguno. Con `enableEdgeToEdge()` activo (MainActivity) eso
            // dejaba el botón "Confirmar venta" debajo de la barra de
            // navegación -- la usuaria le pegaba al botón de inicio del
            // teléfono en vez de confirmar la venta.
            //
            // `safeDrawing` es la unión de barras del sistema, recorte de
            // pantalla y teclado, así que da el MÁXIMO de los dos casos con
            // un solo modificador. Sumar `.navigationBarsPadding()` y
            // `.imePadding()` sería incorrecto: con el teclado abierto se
            // duplicaría el padding, porque el teclado ya cubre la barra de
            // navegación.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // El campo de descuento se eliminó (D-047): lo reemplazó el precio
        // editable de cada línea, que puede subir y bajar. Un descuento solo
        // podía restar.
        //
        // Sin un subtotal completo (alguna línea a medio escribir) no hay un
        // Total real que mostrar: afirmar un número parcial sería un cálculo
        // que no corresponde a nada que ella haya terminado de elegir (mismo
        // espíritu que D-015/D-030).
        state.subtotal?.let { subtotal ->
            Text(
                text = stringResource(R.string.sale_total_label, subtotal.format()),
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
private fun CartLineRow(
    line: SaleLineUiState,
    onQtyChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.productName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    line.productUid,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                line.subtotal?.let { subtotal ->
                    Text(
                        subtotal.format(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        // D-045: el precio de esta venta, precargado con el de lista y
        // editable hacia arriba o hacia abajo. `MoneyTextField` sin
        // excepciones (CLAUDE.md sección 5).
        MoneyTextField(
            label = stringResource(R.string.sale_line_price_label),
            value = line.unitPriceText,
            onValueChange = onPriceChange,
        )
        // D-046: el aviso de pérdida, en la pieza misma y mientras teclea --
        // no solo en el diálogo al final. Es el momento en que todavía puede
        // corregir el precio.
        if (line.isLoss) {
            val lineProfit = line.profit ?: Money.ZERO
            Text(
                text = if (lineProfit == Money.ZERO) {
                    stringResource(R.string.sale_line_zero_profit_warning)
                } else {
                    stringResource(R.string.sale_line_loss_warning, Money(-lineProfit.cents).format())
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
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
            // D-044: ella busca la pieza por su código, no por el nombre --
            // el código está impreso en la etiqueta que tiene en la mano, no
            // lo tiene que recordar. Misma disposición que la fila de
            // Inventario (`ProductListScreen.ProductRow`), que es el
            // componente que ya conoce.
            Text(
                product.uid,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            onLinePriceChange = { _, _ -> },
            onRemoveLineClick = {},
            onConfirmClick = {},
            onLossConfirmed = {},
            onLossDialogDismissed = {},
            onResultDismissed = {},
            onBackClick = {},
            onSaleTypeChange = {},
            onCustomerSelected = {},
            onClearCustomerClick = {},
            onNewCustomerConfirmed = { _, _ -> },
            onInitialPaymentChange = {},
        )
    }
}
