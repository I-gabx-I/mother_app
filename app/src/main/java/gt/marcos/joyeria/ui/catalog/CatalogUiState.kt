package gt.marcos.joyeria.ui.catalog

import gt.marcos.joyeria.domain.catalog.CatalogItem

/**
 * Una pieza elegible, con el texto de nombre que va a salir en la imagen ya
 * resuelto (`caption`): la pantalla de elegir muestra exactamente lo que va a
 * ver la clienta. Solo carga un [CatalogItem], que no tiene costo (D-058).
 */
data class CatalogPiece(
    val item: CatalogItem,
    val caption: String,
)

/** En qué paso del flujo está la pantalla. */
sealed interface CatalogStep {
    /** Eligiendo piezas. */
    data object Picking : CatalogStep

    /** Armando las imágenes: [done] de [total] páginas listas. */
    data class Generating(val done: Int, val total: Int) : CatalogStep

    /** Imágenes listas para mirar y compartir: rutas absolutas, en orden de página. */
    data class Ready(val pages: List<String>) : CatalogStep

    /** No se pudo armar. */
    data object Failed : CatalogStep
}

/**
 * Único estado de la pantalla del catálogo (CLAUDE.md sección 5). Nada acá
 * puede representar un costo: solo [CatalogPiece] (y por dentro,
 * [CatalogItem]) y los ids elegidos.
 */
data class CatalogUiState(
    val pieces: List<CatalogPiece> = emptyList(),
    val isLoaded: Boolean = false,
    val query: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val step: CatalogStep = CatalogStep.Picking,
) {
    /** Las piezas que pasan el buscador (por el texto que se ve, o por el código). */
    val visiblePieces: List<CatalogPiece>
        get() = if (query.isBlank()) {
            pieces
        } else {
            pieces.filter {
                it.caption.contains(query, ignoreCase = true) ||
                    it.item.uid.contains(query, ignoreCase = true)
            }
        }

    val selectedCount: Int get() = selectedIds.size

    val canCreate: Boolean get() = selectedIds.isNotEmpty()

    /** Cuántas de las elegidas el buscador esconde ahora mismo. */
    val hiddenSelectedCount: Int
        get() = selectedCount - visiblePieces.count { it.item.productId in selectedIds }

    /**
     * Si se ofrece «Quitar las N elegidas» (vaciar todo, también lo que el
     * buscador esconde). Es la salida para cuando «Quitar las que se ven» no
     * alcanza para limpiar la selección: hay elegidas escondidas, o hay una
     * selección a medias y el primer botón es «Elegir todas las que se ven».
     * Cuando «Quitar las que se ven» ya vacía todo, sería un botón repetido.
     */
    val canClearAll: Boolean
        get() = selectedCount > 0 && !(allVisibleSelected && hiddenSelectedCount == 0)

    /** Verdadero solo si hay piezas visibles y todas están elegidas. */
    val allVisibleSelected: Boolean
        get() = visiblePieces.isNotEmpty() && visiblePieces.all { it.item.productId in selectedIds }

    /** Las piezas elegidas, en el orden de la lista (no en el orden en que se tocaron). */
    val selectedItems: List<CatalogItem>
        get() = pieces.filter { it.item.productId in selectedIds }.map { it.item }

    fun withQuery(value: String) = copy(query = value)

    fun toggled(productId: Long) = copy(
        selectedIds = if (productId in selectedIds) selectedIds - productId else selectedIds + productId,
    )

    /** Suma a lo ya elegido todas las que se ven; no toca las elegidas que el buscador esconde. */
    fun withAllVisibleSelected() = copy(selectedIds = selectedIds + visiblePieces.map { it.item.productId })

    /**
     * Simétrico de [withAllVisibleSelected]: quita solo las que se ven. Si ella
     * filtra y toca «Quitar las que se ven» esperando limpiar lo que tiene
     * adelante, no pierde sin enterarse la selección que el buscador esconde.
     */
    fun withVisibleDeselected() = copy(selectedIds = selectedIds - visiblePieces.map { it.item.productId }.toSet())

    /** Vacía todo, también lo que el buscador esconde: «Quitar las N elegidas», con el número a la vista. */
    fun withNothingSelected() = copy(selectedIds = emptySet())

    /**
     * Una pieza elegida puede dejar de estar disponible mientras ella elige (se
     * vendió la última, se archivó): se descarta de la selección en vez de
     * armar un catálogo con algo que ya no se puede vender.
     */
    fun withPieces(newPieces: List<CatalogPiece>): CatalogUiState {
        val available = newPieces.map { it.item.productId }.toSet()
        return copy(pieces = newPieces, isLoaded = true, selectedIds = selectedIds.filter { it in available }.toSet())
    }
}
