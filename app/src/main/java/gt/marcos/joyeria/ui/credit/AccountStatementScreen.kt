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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import gt.marcos.joyeria.data.repository.PaymentSummary
import gt.marcos.joyeria.data.repository.SaleAccountStatement
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.MoneyTextField
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.text.DateFormat
import java.util.Date

/**
 * Estado de cuenta de una clienta (FASES.md, Fase 07): todas sus ventas
 * a crédito pendientes, cada una con total/abonos/saldo, y el botón para
 * registrar un abono nuevo. Stateless y `@Preview`-able (CLAUDE.md sección 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementScreen(
    state: AccountStatementUiState,
    onRegisterPaymentClick: (Long) -> Unit,
    onPaymentDialogDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onPaymentConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.customerName.ifBlank { stringResource(R.string.account_statement_title) }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                text = stringResource(R.string.account_statement_total_balance, state.totalBalance.format()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
            if (state.statements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.account_statement_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.statements, key = { it.saleId }) { statement ->
                        SaleStatementCard(
                            statement = statement,
                            onRegisterPaymentClick = { onRegisterPaymentClick(statement.saleId) },
                        )
                    }
                }
            }
        }
    }

    state.paymentDialogSale?.let { sale ->
        AlertDialog(
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
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
private fun AccountStatementScreenPreview() {
    JoyeriaTheme {
        AccountStatementScreen(
            state = AccountStatementUiState(
                customerId = 1,
                customerName = "Doña María",
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
            onRegisterPaymentClick = {},
            onPaymentDialogDismiss = {},
            onAmountChange = {},
            onMethodChange = {},
            onPaymentConfirm = {},
            onBackClick = {},
        )
    }
}
