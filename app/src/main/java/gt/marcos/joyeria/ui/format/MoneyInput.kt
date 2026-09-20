package gt.marcos.joyeria.ui.format

import gt.marcos.joyeria.domain.model.Money
import java.util.Locale

// Reemplaza al patrón de "buffer de dígitos" de Fase 03 (`MoneyDigitsInput.kt`,
// eliminado) -- ver ESTADO.md, fix money-field-plain-decimal, y D-030 en
// DECISIONES.md. El campo de costo/precio ahora es un texto decimal normal:
// lo que filtra qué puede entrar mientras ella teclea es `isValidMoneyInputText`
// (usada en `MoneyTextField`, letra por letra); lo que decide si ya hay un
// monto completo para guardar es `parseMoneyToCents` (usada al leer el
// estado, nunca al mostrar el campo).

// Texto parcial válido mientras se escribe: vacío, dígitos, y como mucho un
// separador decimal (punto o coma) seguido de 0, 1 o 2 dígitos. Un tercer
// dígito después del separador, un segundo separador, una letra o un signo
// no matchean -- por eso simplemente no entran al campo (MoneyTextField
// descarta el cambio en vez de aceptarlo y "corregirlo").
private val MONEY_PARTIAL_TEXT_REGEX = Regex("^[0-9]*([.,][0-9]{0,2})?$")

// Texto completo, listo para convertir a centavos: igual que el de arriba
// pero exige al menos un dígito antes del separador (o antes del final, si
// no hay separador) -- "20.", ".", "" o vacío no alcanzan para guardar.
private val MONEY_COMPLETE_TEXT_REGEX = Regex("^[0-9]+([.,][0-9]{1,2})?$")

fun isValidMoneyInputText(text: String): Boolean = MONEY_PARTIAL_TEXT_REGEX.matches(text)

/**
 * Interpreta texto decimal como centavos. `null` significa "todavía no hay
 * un monto completo que se pueda guardar" -- campo vacío, o a mitad de
 * escribir (ej. "20." recién tecleado el separador). Con `MoneyTextField`
 * filtrando la entrada, cualquier texto que llegue acá ya cumple la
 * gramática parcial de arriba; esta función solo decide si además está
 * *completo*.
 */
fun parseMoneyToCents(text: String): Long? {
    if (!MONEY_COMPLETE_TEXT_REGEX.matches(text)) return null

    val parts = text.replace(',', '.').split('.')
    val quetzales = parts[0].toLongOrNull() ?: return null
    val centavos = if (parts.size == 2) parts[1].padEnd(2, '0').toLong() else 0L
    return quetzales * 100 + centavos
}

/**
 * Texto editable equivalente a este monto (ej. `2050` -> `"20.50"`), para
 * precargar un campo de costo/precio (precio sugerido en alta rápida,
 * valores actuales al entrar a editar). Usa `Locale.ROOT`, no el locale de
 * la usuaria: este texto se le vuelve a dar de comer a `parseMoneyToCents`
 * (y antes que nada, a `isValidMoneyInputText`, letra por letra) apenas ella
 * siga escribiendo, y esas dos funciones solo reconocen dígitos ASCII --
 * exactamente el mismo caso que ya cubre CLAUDE.md sección 5 para cualquier
 * valor que se vuelve a leer como dato, no solo el que se persiste en Room.
 * `Money.format()` (`MoneyFormat.kt`) es distinta a propósito: ese texto
 * nunca vuelve a entrar a un parser, solo se lee, así que sí usa el locale
 * de la usuaria.
 */
fun Money.toEditableText(): String =
    String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100)
