package gt.marcos.joyeria.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.theme.JoyeriaAlertDialog

/**
 * Alta y edición de una clienta. Vive en `ui/customer/` y no dentro de una
 * pantalla porque lo usan dos (B-09, Fase 08): la lista "Clientes" y el
 * perfil de cada clienta, que la edita sin mandarla a otra pantalla --
 * mandarla a otro lado para corregir un teléfono, estando ya parada sobre
 * esa clienta, es fricción sin motivo.
 *
 * Es el MISMO diálogo, no uno parecido: dos formularios para el mismo dato
 * se desincronizan en cuanto uno gane un campo (mismo criterio por el que
 * el perfil absorbió al estado de cuenta en vez de duplicarlo).
 */
@Composable
fun CustomerFormDialog(
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
    JoyeriaAlertDialog(
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
