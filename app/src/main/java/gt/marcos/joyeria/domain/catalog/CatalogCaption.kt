package gt.marcos.joyeria.domain.catalog

/**
 * El texto de nombre que sale debajo de la foto de una pieza en el catálogo.
 *
 * El alta rápida no pide nombre, y un nombre en blanco **se guarda como el
 * texto de relleno** (`add_product_default_name`, "Pieza sin nombre"): queda
 * esa frase literal en la base. Un catálogo que la imprime debajo de cada
 * foto no sirve, así que en ese caso se usa el nombre de la categoría
 * ("Anillos", "Aretes"); y si tampoco hay categoría, no hay texto de nombre
 * (queda el código y el precio).
 *
 * Riesgo conocido: la comparación es contra el texto de relleno **actual**. Si
 * algún día esa frase cambia, las piezas ya guardadas con la anterior se
 * mostrarían con ella. `CatalogCaptionTest` fija la regla.
 */
object CatalogCaption {

    fun of(item: CatalogItem, placeholderName: String): String {
        val name = item.name.trim()
        val isPlaceholder = name.isEmpty() || name == placeholderName.trim()
        return if (isPlaceholder) item.categoryName?.trim().orEmpty() else name
    }
}
