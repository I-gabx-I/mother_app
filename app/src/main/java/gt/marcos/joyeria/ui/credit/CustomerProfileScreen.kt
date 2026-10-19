package gt.marcos.joyeria.ui.credit

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import kotlinx.coroutines.launch
import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.CustomerPurchaseHistory
import gt.marcos.joyeria.data.repository.PaymentSummary
import gt.marcos.joyeria.data.repository.SaleAccountStatement
import gt.marcos.joyeria.data.repository.SaleSummary
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.customer.CustomerFormDialog
import gt.marcos.joyeria.ui.format.MoneyTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.text.DateFormat
import java.util.Date
import gt.marcos.joyeria.ui.theme.JoyeriaAlertDialog

/**
 * Estado de cuenta de una clienta (FASES.md, Fase 07): todas sus ventas
 * a crédito pendientes, cada una con total/abonos/saldo, y el botón para
 * registrar un abono nuevo.
 *
 * Fase 08 (B-05): pasó de ser el estado de cuenta a ser el **perfil
 * completo** -- datos de la clienta, su historial de compras, su saldo si
 * lo tiene, y el botón para venderle. El estado de cuenta quedó como una
 * sección de acá en vez de una pantalla aparte: dos pantallas mostrando
 * saldos son dos lugares que se desincronizan.
 *
 * Stateless y `@Preview`-able (CLAUDE.md sección 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    state: CustomerProfileUiState,
    onSellClick: () -> Unit,
    onEditClick: () -> Unit,
    onFormNameChange: (String) -> Unit,
    onFormPhoneChange: (String) -> Unit,
    onFormNotesChange: (String) -> Unit,
    onFormDismiss: () -> Unit,
    onFormConfirm: () -> Unit,
    onRegisterPaymentClick: (Long) -> Unit,
    onPaymentDialogDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onPaymentConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fase 10: feedback efímero si el teléfono no se pudo preparar para
    // WhatsApp -- mismo criterio que CustomersScreen, estado local de UI.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.customerName.ifBlank { stringResource(R.string.customer_profile_title) }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    // B-09: editar sin salir del perfil. Mandarla a otra
                    // pantalla para corregir un teléfono, estando ya parada
                    // sobre esa clienta, es fricción sin motivo.
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.customers_edit_button))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CustomerDetails(state = state) }

            // La acción principal de esta pantalla, arriba y grande
            // (CLAUDE.md sección 6).
            item {
                Button(
                    onClick = onSellClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(stringResource(R.string.customer_profile_sell_button))
                }
            }

            // Sin deuda no se muestra un "Q0.00": se omite la sección
            // entera, igual que el Total de "Vender" cuando no hay carrito.
            if (state.hasDebt) {
                item {
                    Text(
                        text = stringResource(R.string.account_statement_total_balance, state.totalBalance.format()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                item {
                    val reminderMessage = stringResource(
                        R.string.whatsapp_reminder_message,
                        state.customerName,
                        state.totalBalance.format(),
                    )
                    val invalidPhoneMessage = stringResource(
                        R.string.whatsapp_reminder_invalid_phone,
                        state.customerName,
                    )
                    // Apilados, no lado a lado: "Compartir estado de
                    // cuenta" no entra en una sola línea a media pantalla
                    // (se probó así en el emulador y el texto quedaba
                    // cortado) -- mismo patrón que el resto de la app
                    // (Guardar/Archivar en ProductEditScreen).
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhatsAppReminderButton(
                            phone = state.customer?.phone,
                            message = reminderMessage,
                            onInvalidPhone = { scope.launch { snackbarHostState.showSnackbar(invalidPhoneMessage) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ShareAccountStatementButton(
                            customerName = state.customerName,
                            statements = state.statements,
                            totalBalance = state.totalBalance,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                items(state.statements, key = { "statement-${it.saleId}" }) { statement ->
                    SaleStatementCard(
                        statement = statement,
                        onRegisterPaymentClick = { onRegisterPaymentClick(statement.saleId) },
                    )
                }
            }

            item { HorizontalDivider() }
            item {
                Text(
                    text = stringResource(R.string.customer_profile_history_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (state.history.sales.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.customer_profile_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(
                            R.string.customer_profile_history_total,
                            state.history.totalPurchased.format(),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                // `key` con prefijo: esta lista y la de arriba viven en el
                // MISMO LazyColumn y comparten espacio de claves (D-048).
                items(state.history.sales, key = { "history-${it.id}" }) { sale ->
                    PurchaseRow(sale = sale)
                }
                // B-10: el aviso aparece SOLO si esta clienta concreta pudo
                // comprar de contado cuando la app no guardaba el nombre.
                // A una clienta cuyo historial entero es posterior no se le
                // dice nada: un cartel genérico le crearía la duda a quien
                // no la tenía.
                if (state.history.mayBeMissingCashSales) {
                    item {
                        Text(
                            text = stringResource(R.string.customer_profile_history_may_be_incomplete),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (state.showEditDialog) {
        CustomerFormDialog(
            isEditing = true,
            name = state.formName,
            phone = state.formPhone,
            notes = state.formNotes,
            canSave = state.canSaveForm,
            onNameChange = onFormNameChange,
            onPhoneChange = onFormPhoneChange,
            onNotesChange = onFormNotesChange,
            onDismiss = onFormDismiss,
            onConfirm = onFormConfirm,
        )
    }

    state.paymentDialogSale?.let { sale ->
        JoyeriaAlertDialog(
            onDismissRequest = onPaymentDialogDismiss,
            title = { Text(stringResource(R.string.account_statement_payment_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.account_statement_payment_dialog_balance, sale.balance.format()))
                    MoneyTextField(
                        label = stringResource(R.string.account_statement_payment_amount_label),
                        value = state.amountText,
                        onValueChange = onAmountChange,
                    )
                    if (state.amountExceedsBalance) {
                        Text(
                            text = stringResource(R.string.account_statement_payment_exceeds_balance),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        PaymentMethodChip(
                            label = stringResource(R.string.payment_method_cash),
                            selected = state.method == "CASH",
                            onClick = { onMethodChange("CASH") },
                        )
                        PaymentMethodChip(
                            label = stringResource(R.string.payment_method_transfer),
                            selected = state.method == "TRANSFER",
                            onClick = { onMethodChange("TRANSFER") },
                        )
                        PaymentMethodChip(
                            label = stringResource(R.string.payment_method_other),
                            selected = state.method == "OTHER",
                            onClick = { onMethodChange("OTHER") },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onPaymentConfirm, enabled = state.canConfirmPayment) {
                    Text(stringResource(R.string.account_statement_payment_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onPaymentDialogDismiss) {
                    Text(stringResource(R.string.customer_form_cancel))
                }
            },
        )
    }
}

@Composable
private fun CustomerDetails(state: CustomerProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(state.customerName, style = MaterialTheme.typography.headlineSmall)
        state.customer?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            Text(phone, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.customer?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Una compra del historial. Las anuladas se muestran marcadas (B-06): si
 * una clienta pregunta por una compra que se anuló y no aparece, parece
 * que la app perdió el dato -- y D-006 existe justamente para que nada
 * desaparezca. No suman a ningún total (`CustomerPurchaseHistory.activeSales`).
 */
@Composable
private fun PurchaseRow(sale: SaleSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(formatDate(sale.soldAt), style = MaterialTheme.typography.bodyLarge)
            when {
                sale.status == "CANCELLED" -> Text(
                    text = stringResource(R.string.customer_profile_history_cancelled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                sale.isPendingCredit -> Text(
                    text = stringResource(R.string.sale_credit_pending_badge),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(sale.net.format(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PaymentMethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            AssistChipDefaults.assistChipColors()
        },
    )
}

@Composable
private fun SaleStatementCard(statement: SaleAccountStatement, onRegisterPaymentClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatDate(statement.soldAt), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.account_statement_sale_total, (statement.total - statement.discount).format()),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (statement.payments.isEmpty()) {
                Text(
                    text = stringResource(R.string.account_statement_no_payments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                statement.payments.forEach { payment -> PaymentLine(payment) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.account_statement_sale_balance, statement.balance.format()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = onRegisterPaymentClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.account_statement_register_payment_button))
            }
        }
    }
}

@Composable
private fun PaymentLine(payment: PaymentSummary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatDate(payment.paidAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(payment.amount.format(), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun CustomerProfileScreenPreview() {
    JoyeriaTheme {
        CustomerProfileScreen(
            state = CustomerProfileUiState(
                customerId = 1,
                customer = Customer(id = 1, name = "Doña María", phone = "5555-1234", notes = null, archived = false),
                history = CustomerPurchaseHistory(
                    sales = listOf(
                        SaleSummary(
                            id = 1,
                            soldAt = System.currentTimeMillis(),
                            total = Money(15000),
                            discount = Money.ZERO,
                            profit = Money(7000),
                            type = "CREDIT",
                            status = "PENDING",
                        ),
                    ),
                    mayBeMissingCashSales = true,
                ),
                statements = listOf(
                    SaleAccountStatement(
                        saleId = 1,
                        soldAt = System.currentTimeMillis(),
                        total = Money(15000),
                        discount = Money.ZERO,
                        status = "PENDING",
                        payments = listOf(
                            PaymentSummary(id = 1, paidAt = System.currentTimeMillis(), amount = Money(5000), method = "CASH", notes = null),
                        ),
                    ),
                ),
            ),
            onSellClick = {},
            onEditClick = {},
            onFormNameChange = {},
            onFormPhoneChange = {},
            onFormNotesChange = {},
            onFormDismiss = {},
            onFormConfirm = {},
            onRegisterPaymentClick = {},
            onPaymentDialogDismiss = {},
            onAmountChange = {},
            onMethodChange = {},
            onPaymentConfirm = {},
            onBackClick = {},
        )
    }
}
