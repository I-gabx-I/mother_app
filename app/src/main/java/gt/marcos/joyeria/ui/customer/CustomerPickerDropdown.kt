package gt.marcos.joyeria.ui.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.Customer

/**
 * Selector de clienta para una venta a crédito (D-042), entre las clientas
 * activas. Suma "+ Nueva clienta" (D-040) al final de la lista -- abre
 * `CustomerQuickAddDialog` sin salir de la pantalla de venta ni perder el
 * carrito. El estado del diálogo (texto tecleado) es local a este
 * Composable -- efímero, mismo criterio que `expanded` en
 * `ProductPickerDropdown`/`CategoryDropdown` -- el alta real la resuelve
 * quien recibe `onNewCustomerConfirmed`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerDropdown(
    customers: List<Customer>,
    selectedCustomerId: Long?,
    onCustomerSelected: (Long) -> Unit,
    onNewCustomerConfirmed: (name: String, phone: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    val selected = customers.find { it.id == selectedCustomerId }
    val label = selected?.name ?: stringResource(R.string.sale_credit_customer_placeholder)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.sale_credit_customer_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            customers.forEach { customer ->
                DropdownMenuItem(
                    text = { Text(customer.name) },
                    onClick = {
                        onCustomerSelected(customer.id)
                        expanded = false
                    },
                )
            }
            if (customers.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.customer_quick_add_menu_item)) },
                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                onClick = {
                    expanded = false
                    showAddDialog = true
                },
            )
        }
    }

    if (showAddDialog) {
        CustomerQuickAddDialog(
            name = newName,
            phone = newPhone,
            onNameChange = { newName = it },
            onPhoneChange = { newPhone = it },
            onDismiss = {
                showAddDialog = false
                newName = ""
                newPhone = ""
            },
            onConfirm = {
                onNewCustomerConfirmed(newName.trim(), newPhone.trim().ifBlank { null })
                showAddDialog = false
                newName = ""
                newPhone = ""
            },
        )
    }
}
