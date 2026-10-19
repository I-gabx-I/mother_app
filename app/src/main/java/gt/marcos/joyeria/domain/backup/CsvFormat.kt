package gt.marcos.joyeria.domain.backup

/**
 * El formato de los CSV para Excel (Fase 14): coma como separador (Guatemala usa punto decimal), UTF-8 **con BOM**
 * para que Excel respete las tildes y la ñ, fin de línea CRLF. Puro.
 */
object CsvFormat {
    /** Los tres bytes `EF BB BF` del BOM de UTF-8. */
    val BOM: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    private const val NEWLINE = "\r\n"

    /**
     * Un texto como celda. Se escapan comas, comillas y saltos de línea, y **una celda que empieza con `=`, `+`, `-`
     * o `@` se neutraliza con un apóstrofe**: un nombre de pieza o de clienta no puede convertirse en una fórmula al
     * abrirse en Excel. Solo para texto; los montos (que pueden empezar con `-`) van por [money].
     */
    fun text(value: String?): String {
        if (value == null) return ""
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** Un monto en centavos como número de quetzales con dos decimales (`75.90`), con aritmética entera: nunca un `Double` (D-001). */
    fun money(cents: Long): String {
        val negative = cents < 0
        val abs = if (cents == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(cents)
        val text = "${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
        return if (negative) "-$text" else text
    }

    fun line(cells: List<String>): String = cells.joinToString(",") + NEWLINE
}
