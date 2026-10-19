package gt.marcos.joyeria.ui.barcode

import gt.marcos.joyeria.domain.barcode.LabelPiece

/** Una pieza elegible con el texto que ella reconoce ya resuelto. Sin ningún monto. */
data class LabelListItem(val piece: LabelPiece, val caption: String)

/** Qué hacer con el PDF recién armado. */
enum class LabelAction { PRINT, SHARE, TEST_SHEET }

/** Un PDF listo: la pantalla lo imprime o lo comparte y avisa que ya lo manejó. */
data class LabelOutputReady(val path: String, val pageCount: Int, val action: LabelAction)

/**
 * Único estado de la pantalla de etiquetas (CLAUDE.md sección 5). Nada acá puede
 * representar un monto: solo [LabelListItem] y los códigos elegidos.
 */
data class LabelsUiState(
    val items: List<LabelListItem> = emptyList(),
    val isLoaded: Boolean = false,
    val query: String = "",
    val selectedUids: Set<String> = emptySet(),
    val working: Boolean = false,
    val failed: Boolean = false,
    val output: LabelOutputReady? = null,
) {
    val visibleItems: List<LabelListItem>
        get() = if (query.isBlank()) items else items.filter {
            it.caption.contains(query, ignoreCase = true) || it.piece.uid.value.contains(query, ignoreCase = true)
        }

    val selectedCount: Int get() = selectedUids.size
    val canPrint: Boolean get() = selectedUids.isNotEmpty() && !working

    val hiddenSelectedCount: Int
        get() = selectedCount - visibleItems.count { it.piece.uid.value in selectedUids }

    /** Mismo criterio que el catálogo: vaciar todo solo se ofrece cuando «Quitar las que se ven» no alcanza. */
    val canClearAll: Boolean
        get() = selectedCount > 0 && !(allVisibleSelected && hiddenSelectedCount == 0)

    val allVisibleSelected: Boolean
        get() = visibleItems.isNotEmpty() && visibleItems.all { it.piece.uid.value in selectedUids }

    /** Las etiquetas elegidas, en el orden de la lista. */
    val selectedPieces: List<LabelPiece>
        get() = items.filter { it.piece.uid.value in selectedUids }.map { it.piece }

    fun withQuery(value: String) = copy(query = value)

    fun toggled(uid: String) = copy(selectedUids = if (uid in selectedUids) selectedUids - uid else selectedUids + uid)

    fun withAllVisibleSelected() = copy(selectedUids = selectedUids + visibleItems.map { it.piece.uid.value })

    fun withVisibleDeselected() = copy(selectedUids = selectedUids - visibleItems.map { it.piece.uid.value }.toSet())

    fun withNothingSelected() = copy(selectedUids = emptySet())

    fun withItems(newItems: List<LabelListItem>): LabelsUiState {
        val available = newItems.map { it.piece.uid.value }.toSet()
        return copy(items = newItems, isLoaded = true, selectedUids = selectedUids.filter { it in available }.toSet())
    }
}
