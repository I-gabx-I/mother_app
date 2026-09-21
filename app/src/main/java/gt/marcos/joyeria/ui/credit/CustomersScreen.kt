package gt.marcos.joyeria.ui.credit

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.CustomerDebtSummary
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.util.concurrent.TimeUnit

// D-041: umbrales fijos en código, no una clave de app_setting -- nadie pidió
// que sean ajustables, un color de aviso no es una regla de negocio (ver
// DECISIONES.md D-041 para el motivo completo, corregido por el humano).
private const val WARNING_DAYS = 15
private const val ALERT_DAYS = 30

/**
 * Pantalla "Clientes" (D-038): "¿Quién me debe?" (pestaña por defecto, la
 * que más se abre) y el CRUD completo en una segunda pestaña, en vez de dos
 * pantallas separadas. Stateless y `@Preview`-able (CLAUDE.md sección 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    state: CustomersUiState,
    onTabSelected: (CustomersTab) -> Unit,
    onDebtClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Customer) -> Unit,
    onFormNameChange: (String) -> Unit,
    onFormPhoneChange: (String) -> Unit,
    onFormNotesChange: (String) -> Unit,
    onFormDismiss: () -> Unit,
    onFormConfirm: () -> Unit,
    onArchiveClick: (Customer) -> Unit,
    onArchiveDismiss: () -> Unit,
    onArchiveConfirm: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customers_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.tab == CustomersTab.ALL_CUSTOMERS) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.customers_add_button))
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(
                    selected = state.tab == CustomersTab.WHO_OWES_ME,
                    onClick = { onTabSelected(CustomersTab.WHO_OWES_ME) },
                    text = { Text(stringResource(R.string.customers_tab_who_owes_me)) },
                )
                Tab(
                    selected = state.tab == CustomersTab.ALL_CUSTOMERS,
                    onClick = { onTabSelected(CustomersTab.ALL_CUSTOMERS) },
                    text = { Text(stringResource(R.string.customers_tab_all)) },
                )
            }

            when (state.tab) {
                CustomersTab.WHO_OWES_ME -> WhoOwesMeTab(state = state, onDebtClick = onDebtClick)
                CustomersTab.ALL_CUSTOMERS -> AllCustomersTab(
                    customers = state.allCustomers,
                    onEditClick = onEditClick,
                    onArchiveClick = onArchiveClick,
                )
            }
        }
    }

    if (state.showFormDialog) {
        CustomerFormDialog(
            isEditing = state.editingCustomerId != null,
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

    state.archiveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onArchiveDismiss,
            title = { Text(stringResource(R.string.customers_archive_confirm_title)) },
            text = {
                val blocked = state.archiveBlocked
                Text(
                    if (blocked != null) {
                        stringResource(
                            R.string.customers_archive_blocked_message,
                            Money(blocked.pendingCents).format(),
                            blocked.pendingSaleCount,
                        )
                    } else {
                        stringResource(R.string.customers_archive_confirm_message, target.name)
                    },
                )
            },
            confirmButton = {
                if (state.archiveBlocked == null) {
                    TextButton(onClick = onArchiveConfirm) {
                        Text(stringResource(R.string.customers_archive_confirm_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onArchiveDismiss) {
                    Text(stringResource(R.string.customer_form_cancel))
                }
            },
        )
    }
}

@Composable
private fun WhoOwesMeTab(state: CustomersUiState, onDebtClick: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.customers_total_debt_label, state.totalDebt.format()),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        if (state.debts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.customers_who_owes_me_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.debts, key = { it.customerId }) { debt ->
                    DebtRow(debt = debt, onClick = { onDebtClick(debt.customerId) })
                }
            }
        }
    }
}

@Composable
private fun DebtRow(debt: CustomerDebtSummary, onClick: () -> Unit) {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - debt.oldestPendingSaleAt).toInt().coerceAtLeast(0)
    val ageColor = when {
        days >= ALERT_DAYS -> MaterialTheme.colorScheme.error
        days >= WARNING_DAYS -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(debt.customerName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = daysAgoText(days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ageColor,
                )
            }
            Text(debt.totalBalance.format(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun daysAgoText(days: Int): String =
    if (days == 0) {
        stringResource(R.string.customers_debt_age_today)
    } else {
        pluralStringResource(R.plurals.customers_debt_age_days, days, days)
    }

@Composable
private fun AllCustomersTab(
    customers: List<Customer>,
    onEditClick: (Customer) -> Unit,
    onArchiveClick: (Customer) -> Unit,
) {
    if (customers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.customers_all_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(customers, key = { it.id }) { customer ->
                CustomerRow(customer = customer, onEditClick = { onEditClick(customer) }, onArchiveClick = { onArchiveClick(customer) })
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: Customer, onEditClick: () -> Unit, onArchiveClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEditClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.bodyLarge)
                if (!customer.phone.isNullOrBlank()) {
                    Text(
                        customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.customers_edit_button))
            }
            IconButton(onClick = onArchiveClick) {
                Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.customers_archive_button))
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    isEditing: Boolean,
    name: String,
    phone: String,
    notes: String,
    canSave: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEditing) R.string.customers_edit_title else R.string.customers_add_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.customer_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.customer_phone_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.customer_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canSave) {
                Text(stringResource(R.string.customers_form_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.customer_form_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CustomersScreenPreview() {
    JoyeriaTheme {
        CustomersScreen(
            state = CustomersUiState(
                debts = listOf(
                    CustomerDebtSummary(
                        customerId = 1,
                        customerName = "Doña María",
                        customerPhone = "5555-1234",
                        totalBalance = Money(15000),
                        oldestPendingSaleAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(40),
                    ),
                ),
            ),
            onTabSelected = {},
            onDebtClick = {},
            onAddClick = {},
            onEditClick = {},
            onFormNameChange = {},
            onFormPhoneChange = {},
            onFormNotesChange = {},
            onFormDismiss = {},
            onFormConfirm = {},
            onArchiveClick = {},
            onArchiveDismiss = {},
            onArchiveConfirm = {},
            onBackClick = {},
        )
    }
}
