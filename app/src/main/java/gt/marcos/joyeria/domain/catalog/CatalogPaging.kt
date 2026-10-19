package gt.marcos.joyeria.domain.catalog

/**
 * Reparte las piezas elegidas en páginas de [PIECES_PER_PAGE]. Cada página es
 * una imagen: cuatro piezas por imagen dejan la foto grande (es lo que decide
 * la compra) y la página con la proporción que cabe en un chat de WhatsApp
 * sin recortarse. Para 20 piezas son 5 imágenes. Función pura: conserva el
 * orden, no pierde ni repite ninguna pieza.
 */
object CatalogPaging {
    const val PIECES_PER_PAGE = 4

    fun <T> paginate(items: List<T>): List<List<T>> = items.chunked(PIECES_PER_PAGE)
}
