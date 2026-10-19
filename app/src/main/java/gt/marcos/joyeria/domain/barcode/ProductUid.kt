package gt.marcos.joyeria.domain.barcode

/**
 * El código de una pieza (`XP-000042`), el único dato que lleva una etiqueta de
 * código de barras (D-003: el código codifica el `uid`, nunca el precio).
 *
 * **Es la garantía estructural de la Fase 13:** el constructor es privado y la
 * única puerta es [parse], que solo acepta `XP-` seguido de 6 o más dígitos.
 * Un monto (`Q90.00`, `9000`) **no se puede construir** como [ProductUid], y
 * todo lo que genera o lee etiquetas trabaja con este tipo y no con un `String`
 * cualquiera. Mismo patrón que `SaleLineInput` (D-045) y `CatalogItem` (D-058).
 *
 * El formato sale de `ProductUidGenerator.format` (`XP-%06d`, con `Locale.ROOT`):
 * 6 dígitos mínimo, y crece si el contador pasa de 999999.
 */
@JvmInline
value class ProductUid private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        private val FORMAT = Regex("XP-[0-9]{6,}")

        /** `null` si [raw] no es exactamente un código de pieza: sin espacios, sin recortar, sin adivinar. */
        fun parse(raw: String?): ProductUid? =
            if (raw != null && FORMAT.matches(raw)) ProductUid(raw) else null
    }
}
