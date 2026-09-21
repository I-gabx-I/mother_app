package gt.marcos.joyeria.ui.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
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
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.SaleSummary
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.text.DateFormat
import java.util.Date

/**
 * Pantalla "Ventas de hoy" (Fase 06). Stateless y `@Preview`-able
 * (CLAUDE.md sección 5): recibe estado y lambdas, nunca el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaySalesScreen(
    state: TodaySalesUiState,
    onSaleClick: (Long) -> Unit,
    onDetailDismissed: () -> Unit,
    onCancelClick: () -> Unit,
    onCancelReasonChange: (String) -> Unit,
    onCancelDismiss: () -> Unit,
    onCancelConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_sales_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.today_sales_total_label, state.totalNet.format()),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.today_sales_profit_label, state.totalProfit.format()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.sales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.today_sales_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.sales, key = { it.id }) { sale ->
                        SaleRow(sale = sale, onClick = { onSaleClick(sale.id) })
                    }
                }
            }
        }
    }

    state.selectedSale?.let { detail ->
        AlertDialog(
            onDismissRequest = onDetailDismissed,
            title = { Text(stringResource(R.string.today_sales_detail_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (detail.isPendingCredit) {
                        Text(
                            text = stringResource(R.string.sale_credit_pending_badge),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    detail.items.forEach { item ->
                        Text(
                            stringResource(
                                R.string.today_sales_item_line,
                                item.qty,
                                item.productNameSnapshot,
                                item.productUidSnapshot,
                            ),
                        )
                    }
                    Text(
                        stringResource(R.string.today_sales_total_label, (detail.total - detail.discount).format()),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.today_sales_profit_label, detail.profit.format()),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    // D-043: el botón de abajo queda visible pero
                    // deshabilitado en vez de desaparecer -- un botón que se
                    // esconde es indistinguible de un bug (mismo principio
                    // que los topes de stock/saldo de Fase 06/07, nunca un
                    // límite invisible). El motivo se ve siempre que el
                    // botón esté deshabilitado por esto.
                    if (detail.hasPayments) {
                        Text(
                            text = stringResource(R.string.today_sales_cannot_cancel_has_payments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = onCancelClick, enabled = detail.canCancel) {
                    Text(stringResource(R.string.today_sales_cancel_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDetailDismissed) {
                    Text(stringResource(R.string.nav_back))
                }
            },
        )
    }

    if (state.showCancelConfirmation) {
        // D-036: el motivo es opcional -- sugerencias de un tap para no
        // obligarla a escribir con el teclado en medio de una anulación,
        // pero el campo sigue editable para un motivo propio.
        val reasonReturn = stringResource(R.string.today_sales_cancel_reason_suggestion_return)
        val reasonMistake = stringResource(R.string.today_sales_cancel_reason_suggestion_mistake)
        AlertDialog(
            onDismissRequest = onCancelDismiss,
            title = { Text(stringResource(R.string.today_sales_cancel_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.today_sales_cancel_confirm_message))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        AssistChip(onClick = { onCancelReasonChange(reasonReturn) }, label = { Text(reasonReturn) })
                        AssistChip(onClick = { onCancelReasonChange(reasonMistake) }, label = { Text(reasonMistake) })
                        AssistChip(
                            onClick = { onCancelReasonChange("") },
                            label = { Text(stringResource(R.string.today_sales_cancel_reason_suggestion_other)) },
                        )
                    }
                    OutlinedTextField(
                        value = state.cancelReasonText,
                        onValueChange = onCancelReasonChange,
                        label = { Text(stringResource(R.string.today_sales_cancel_reason_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onCancelConfirm) {
                    Text(stringResource(R.string.today_sales_cancel_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDismiss) {
                    Text(stringResource(R.string.today_sales_cancel_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun SaleRow(sale: SaleSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(formatTime(sale.soldAt), style = MaterialTheme.typography.bodyLarge)
                // D-042: sin esto, una venta a crédito recién hecha se ve
                // igual que una de contado ya cobrada -- ella podría creer
                // que tiene un dinero que en realidad no cobró.
                if (sale.isPendingCredit) {
                    Text(
                        text = stringResource(R.string.sale_credit_pending_badge),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(sale.net.format(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.today_sales_profit_label, sale.profit.format()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun TodaySalesScreenPreview() {
    JoyeriaTheme {
        TodaySalesScreen(
            state = TodaySalesUiState(),
            onSaleClick = {},
            onDetailDismissed = {},
            onCancelClick = {},
            onCancelReasonChange = {},
            onCancelDismiss = {},
            onCancelConfirm = {},
            onBackClick = {},
        )
    }
}
