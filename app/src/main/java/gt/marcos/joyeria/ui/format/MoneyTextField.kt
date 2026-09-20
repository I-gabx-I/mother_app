package gt.marcos.joyeria.ui.format

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Campo de costo/precio: texto decimal normal y corriente, sin ningún
 * reformateo de lo que se muestra (reemplaza al buffer de dígitos de
 * Fase 03 -- ver ESTADO.md, fix money-field-plain-decimal, D-030). La
 * causa del bug anterior era mostrar un `value` distinto al que ella
 * tecleó; acá el único filtro posible es que una tecla que no corresponde
 * (letra, segundo separador decimal, un tercer dígito después del
 * separador) simplemente **no entra** -- el texto y el cursor quedan
 * exactamente donde estaban antes de esa tecla, nunca se le presenta un
 * texto "corregido".
 *
 * Se usa `TextFieldValue` (texto + selección), no `String`, a propósito:
 * es lo que permite rechazar una tecla sin perturbar dónde estaba el
 * cursor. Con un `value: String` simple, ignorar una tecla inválida no
 * alcanza para preservar la posición del cursor de forma confiable.
 */
@Composable
fun MoneyTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    if (fieldValue.text != value) {
        // El texto cambió desde afuera (precio sugerido autocompletado, o
        // carga inicial al entrar a editar), no por una tecla de ella --
        // ahí no hay una posición de cursor propia que preservar.
        fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { proposed ->
            if (isValidMoneyInputText(proposed.text)) {
                fieldValue = proposed
                onValueChange(proposed.text)
            }
            // Si no es válido, no se actualiza nada: la tecla no entra.
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        textStyle = TextStyle(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
        modifier = modifier.fillMaxWidth(),
    )
}
