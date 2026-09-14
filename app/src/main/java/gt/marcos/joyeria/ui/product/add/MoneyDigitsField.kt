package gt.marcos.joyeria.ui.product.add

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.format

/**
 * Campo de costo/precio, patrón "buffer de dígitos" (ver
 * `MoneyDigitsInput.kt`): lo que se le pasa a `onDigitsChange` es el texto
 * crudo que Compose reporta (el valor formateado más la tecla nueva); el
 * sanitizado y la interpretación como centavos viven en funciones puras
 * fuera de este Composable, no acá.
 */
@Composable
fun MoneyDigitsField(
    label: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = Money(digitsToCents(digits))
    OutlinedTextField(
        value = money.format(),
        onValueChange = onDigitsChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
        modifier = modifier.fillMaxWidth(),
    )
}
