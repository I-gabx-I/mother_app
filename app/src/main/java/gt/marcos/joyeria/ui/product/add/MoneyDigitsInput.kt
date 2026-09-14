package gt.marcos.joyeria.ui.product.add

// Lógica pura del campo de costo/precio, separada del Composable a
// propósito para que sea testeable sin Robolectric (FASES.md Fase 03,
// pedido explícito del humano): es el punto exacto por donde el dinero
// entra al sistema desde el dedo de la usuaria, y un error de un factor
// de 10 acá arruina todos los precios de la app sin que nada lo note.

// Tope de dígitos: Q9,999,999.99, de sobra para joyería. Evita un Long
// absurdo si alguien mantiene el dedo apretado sobre una tecla.
private const val MAX_MONEY_DIGITS = 9

/**
 * Se queda solo con los dígitos de `raw` y aplica el tope de longitud
 * (los dígitos más nuevos ganan, se descartan los más viejos por la
 * izquierda). Nunca lanza, nunca devuelve algo que no sean dígitos.
 */
fun sanitizeMoneyDigits(raw: String): String =
    raw.filter { it.isDigit() }.takeLast(MAX_MONEY_DIGITS)

/**
 * Interpreta un buffer de dígitos como centavos: escribir "7", "5", "9",
 * "0" dan el buffer "7590", que son **7590 centavos** (Q75.90) — los dos
 * últimos dígitos son siempre los centavos, como una caja registradora.
 * Sin punto decimal que tipear ni ambigüedad de separador por locale.
 * Un buffer vacío o sin dígitos son 0 centavos, nunca un error.
 */
fun digitsToCents(raw: String): Long =
    sanitizeMoneyDigits(raw).toLongOrNull() ?: 0L
