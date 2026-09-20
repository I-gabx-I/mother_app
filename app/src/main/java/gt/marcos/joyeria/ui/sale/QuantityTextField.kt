package gt.marcos.joyeria.ui.sale

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

// Solo dígitos, o vacío -- ver ESTADO.md "Fase 06 -- Plan", punto 2. Una
// letra no entra nunca (regla de formato fija, se filtra en silencio,
// mismo criterio que MoneyTextField/D-030). Una cantidad mayor al stock
// SÍ se deja escribir completa -- ese no es un problema de formato, es
// de negocio, y se muestra como aviso (supportingText), nunca como un
// tope invisible acá: bloquear un dígito por una razón que ella no
// puede adivinar mientras escribe sería indistinguible de un campo roto.
private val QUANTITY_INPUT_REGEX = Regex("^[0-9]*$")

fun isValidQuantityInputText(text: String): Boolean = QUANTITY_INPUT_REGEX.matches(text)

/**
 * Campo de cantidad de una línea de venta. Mismo mecanismo que
 * `MoneyTextField` (`TextFieldValue`, nunca reformatea lo que se está
 * escribiendo) para no repetir el riesgo de desincronizar cursor y
 * estado que causó D-030. Vive en `ui/sale/` porque hoy es el único
 * consumidor -- si aparece un segundo caso de uso, se generaliza a
 * `ui/format/` en ese momento (CLAUDE.md sección 5).
 */
@Composable
fun QuantityTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { proposed ->
            if (isValidQuantityInputText(proposed.text)) {
                fieldValue = proposed
                onValueChange(proposed.text)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = supportingText != null,
        supportingText = supportingText?.let { text -> { Text(text) } },
        modifier = modifier,
    )
}
