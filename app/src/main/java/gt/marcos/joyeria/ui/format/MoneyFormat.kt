package gt.marcos.joyeria.ui.format

import gt.marcos.joyeria.domain.model.Money
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// Única función de formateo de moneda de toda la app (CLAUDE.md 3.3):
// domain y data nunca formatean, solo operan Money en centavos. Esta sí
// usa el locale del dispositivo (a diferencia del uid o cualquier valor
// persistido/impreso, que va con Locale.ROOT — CLAUDE.md sección 5): un
// monto en pantalla es para que ella lo lea, no una clave técnica.
//
// Todo el cálculo es con Long (CLAUDE.md 3.1: nada de Double/Float para
// dinero, tampoco acá en ui): se separan quetzales y centavos con
// división y resto entera, nunca se construye un número de punto
// flotante para representar el monto.
fun Money.format(locale: Locale = Locale.getDefault()): String {
    val negative = cents < 0
    val absCents = abs(cents)
    val quetzales = absCents / 100
    val centavos = absCents % 100

    val quetzalesFormatted = NumberFormat.getIntegerInstance(locale).format(quetzales)
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val centavosFormatted = centavos.toString().padStart(2, '0')
    val sign = if (negative) "-" else ""

    return "${sign}Q$quetzalesFormatted$decimalSeparator$centavosFormatted"
}

/**
 * El monto para un texto o una imagen que **sale a otro teléfono**: `Q1,250.00`,
 * con `Locale.ROOT`, igual en cualquier teléfono.
 *
 * No contradice [format]: ese usa el idioma del teléfono para lo que ELLA lee en
 * pantalla. Esto viaja al teléfono de otra persona (el catálogo, el recordatorio
 * de WhatsApp, el estado de cuenta compartido) y no se vuelve a formatear al
 * llegar, así que tiene que verse igual sin importar el idioma de quien lo
 * genera: con un teléfono en `es-ES` la clienta vería `Q1.250,00`, que en
 * Guatemala se lee mal. Es una razón distinta a la de la regla de `Locale.ROOT`
 * para lo persistido o impreso (claves técnicas como el uid). Ver D-058.
 */
fun Money.formatForOtherPhone(): String = format(Locale.ROOT)
