package gt.marcos.joyeria.domain.barcode

/**
 * La geometría de una hoja de etiquetas adhesivas (Fase 13), en milímetros. Pura.
 *
 * Por defecto: hoja **Carta** (la que se usa en Guatemala) con etiquetas de
 * **38 × 16 mm** en 4 columnas y 14 filas (56 por hoja), la grilla centrada. El
 * módulo del código (la barra más angosta) es **0.254 mm** (1/100 de pulgada):
 * son 3 puntos a 300 dpi y 6 a 600 dpi, así que sale parejo en impresoras
 * comunes. `XP-000042` ocupa 112 módulos = 28.4 mm de barras. Las medidas
 * reales de la hoja de etiquetas de ella se confirman con lo que use (D-060).
 */
data class LabelSheetSpec(
    val pageWidthMm: Double = 215.9,
    val pageHeightMm: Double = 279.4,
    val labelWidthMm: Double = 38.0,
    val labelHeightMm: Double = 16.0,
    val gapXMm: Double = 2.5,
    val gapYMm: Double = 2.0,
    val marginXMm: Double = 8.0,
    val marginYMm: Double = 10.0,
    val moduleMm: Double = 0.254,
    val barHeightMm: Double = 8.0,
    val topPaddingMm: Double = 2.0,
) {
    val columns: Int get() = ((pageWidthMm - 2 * marginXMm + gapXMm) / (labelWidthMm + gapXMm)).toInt().coerceAtLeast(1)
    val rows: Int get() = ((pageHeightMm - 2 * marginYMm + gapYMm) / (labelHeightMm + gapYMm)).toInt().coerceAtLeast(1)
    val labelsPerPage: Int get() = columns * rows

    private val gridWidthMm: Double get() = columns * labelWidthMm + (columns - 1) * gapXMm
    private val gridHeightMm: Double get() = rows * labelHeightMm + (rows - 1) * gapYMm

    /** Esquina superior izquierda de la etiqueta número [index] de la hoja, en mm desde la esquina de la hoja. */
    fun labelLeftMm(index: Int): Double =
        (pageWidthMm - gridWidthMm) / 2 + (index % columns) * (labelWidthMm + gapXMm)

    fun labelTopMm(index: Int): Double =
        (pageHeightMm - gridHeightMm) / 2 + (index / columns) * (labelHeightMm + gapYMm)

    /**
     * El módulo que se usa para un código de [modules] módulos: el de la hoja, salvo
     * que el código con sus zonas de silencio (10 módulos a cada lado) no entre en
     * la etiqueta; en ese caso se achica lo justo. Nunca se desborda la etiqueta.
     */
    fun moduleMmFor(modules: Int): Double = minOf(moduleMm, labelWidthMm / (modules + 2 * QUIET_ZONE_MODULES))

    companion object {
        const val QUIET_ZONE_MODULES = 10
    }
}

/** Reparte las etiquetas en hojas. Función pura: conserva el orden, no pierde ni repite ninguna. */
object LabelSheetPaging {
    fun <T> paginate(items: List<T>, spec: LabelSheetSpec = LabelSheetSpec()): List<List<T>> =
        items.chunked(spec.labelsPerPage)
}
