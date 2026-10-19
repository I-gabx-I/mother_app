package gt.marcos.joyeria.domain.catalog

import com.google.common.truth.Truth.assertWithMessage
import gt.marcos.joyeria.data.local.dao.CatalogRow
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * El cerrojo del criterio 2 de la Fase 12: el catálogo NUNCA muestra costo ni
 * ganancia. La garantía de verdad es estructural (los tipos no tienen el
 * campo); estos tests fijan el conjunto exacto de campos para que agregar uno
 * no pase en silencio. Ver D-058.
 */
class CatalogItemTest {

    private val deniedWords = listOf("cost", "costo", "profit", "ganancia", "margin", "margen", "markup", "recargo")

    private fun fieldNames(type: Class<*>): Set<String> =
        type.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

    @Test
    fun catalogItem_fieldsAreExactlyTheSixAllowed() {
        assertWithMessage(
            "CatalogItem es el tipo que viaja a la pantalla y a la imagen del catálogo, y NUNCA puede " +
                "cargar costo ni ganancia (FASES.md Fase 12, D-058). Si agregaste un campo, pensá si " +
                "una clienta puede verlo: si la respuesta es no, no va acá.",
        )
            .that(fieldNames(CatalogItem::class.java))
            .containsExactly("productId", "uid", "name", "categoryName", "photoPath", "price")
    }

    @Test
    fun catalogRow_columnsAreExactlyTheSixAllowed() {
        assertWithMessage(
            "CatalogRow es lo que la consulta del catálogo lee de la base: ninguna columna de costo " +
                "(FASES.md Fase 12, D-058).",
        )
            .that(fieldNames(CatalogRow::class.java))
            .containsExactly("id", "uid", "name", "categoryName", "photoPath", "salePriceCents")
    }

    @Test
    fun catalogItem_noFieldNameSuggestsCostOrProfit() {
        val offending = fieldNames(CatalogItem::class.java)
            .filter { name -> deniedWords.any { name.contains(it, ignoreCase = true) } }
        assertWithMessage("CatalogItem tiene un campo que suena a costo o ganancia: $offending")
            .that(offending).isEmpty()
    }

    @Test
    fun catalogRow_noColumnNameSuggestsCostOrProfit() {
        val offending = fieldNames(CatalogRow::class.java)
            .filter { name -> deniedWords.any { name.contains(it, ignoreCase = true) } }
        assertWithMessage("CatalogRow tiene una columna que suena a costo o ganancia: $offending")
            .that(offending).isEmpty()
    }
}
