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
