package gt.marcos.joyeria.domain.catalog

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
import org.junit.Test

class CatalogCaptionTest {

    private val placeholder = "Pieza sin nombre"

    private fun item(name: String, category: String?) = CatalogItem(
        productId = 1, uid = "XP-000001", name = name, categoryName = category,
        photoPath = null, price = Money(10000),
    )

    @Test
    fun caption_forPlaceholderName_isCategoryName() {
        assertThat(CatalogCaption.of(item(placeholder, "Anillos"), placeholder)).isEqualTo("Anillos")
    }

    @Test
    fun caption_forPlaceholderNameWithoutCategory_isEmpty() {
        assertThat(CatalogCaption.of(item(placeholder, null), placeholder)).isEmpty()
    }

    @Test
    fun caption_forRealName_isTheNameEvenWhenThereIsACategory() {
        assertThat(CatalogCaption.of(item("Anillo con piedra azul", "Anillos"), placeholder))
            .isEqualTo("Anillo con piedra azul")
    }

    @Test
    fun caption_forBlankName_isCategoryName() {
        assertThat(CatalogCaption.of(item("   ", "Aretes"), placeholder)).isEqualTo("Aretes")
    }

    @Test
    fun caption_forPlaceholderWithSurroundingSpaces_isStillCategoryName() {
        assertThat(CatalogCaption.of(item("  Pieza sin nombre ", "Cadenas"), placeholder)).isEqualTo("Cadenas")
    }
}
