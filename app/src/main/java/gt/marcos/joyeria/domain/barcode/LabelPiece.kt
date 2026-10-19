package gt.marcos.joyeria.domain.barcode

/**
 * Una pieza en la pantalla de etiquetas. **Sin monto**: solo el código (un
 * [ProductUid], no un `String`) y lo que hace falta para que ella reconozca la
 * pieza en la lista. Lo que viaja a la etiqueta es el [ProductUid] (D-060).
 */
data class LabelPiece(
    val uid: ProductUid,
    val name: String,
    val categoryName: String?,
    val photoPath: String?,
) {
    val label: BarcodeLabel get() = BarcodeLabel(uid)
}

/**
 * Mismo criterio que `CatalogCaption`: un nombre en blanco se guarda como el
 * texto de relleno («Pieza sin nombre»); ahí se muestra la categoría.
 */
object LabelCaption {
    fun of(piece: LabelPiece, placeholderName: String): String {
        val name = piece.name.trim()
        val isPlaceholder = name.isEmpty() || name == placeholderName.trim()
        return if (isPlaceholder) piece.categoryName?.trim().orEmpty() else name
    }
}
