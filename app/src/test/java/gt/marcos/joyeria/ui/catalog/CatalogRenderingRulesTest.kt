package gt.marcos.joyeria.ui.catalog

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.catalog.render.CatalogStyle
import gt.marcos.joyeria.ui.catalog.render.catalogPriceText
import org.junit.Test
import java.util.Locale

class CatalogRenderingRulesTest {

    // El texto de la imagen viaja al teléfono de otra persona: tiene que verse
    // igual sin importar el idioma de quien la genera (ver D-058).
    private fun priceTextWithDeviceLocale(locale: Locale, cents: Long): String {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            return catalogPriceText(Money(cents))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun priceText_whenDeviceLocaleIsSpanishSpain_stillUsesTheGuatemalanConvention() {
        assertThat(priceTextWithDeviceLocale(Locale.forLanguageTag("es-ES"), 125000)).isEqualTo("Q1,250.00")
    }

    @Test
    fun priceText_whenDeviceLocaleIsArabicEgypt_stillUsesAsciiDigits() {
        assertThat(priceTextWithDeviceLocale(Locale.forLanguageTag("ar-EG"), 125000)).isEqualTo("Q1,250.00")
    }

    @Test
    fun priceText_keepsTheCentavosAndTheQSymbol() {
        assertThat(priceTextWithDeviceLocale(Locale.ROOT, 7590)).isEqualTo("Q75.90")
    }

    @Test
    fun pageHeight_growsOneRowForEachTwoPieces() {
        val oneRow = CatalogStyle.pageHeight(1)
        assertThat(CatalogStyle.pageHeight(2)).isEqualTo(oneRow)
        assertThat(CatalogStyle.pageHeight(3)).isEqualTo(oneRow + CatalogStyle.TILE_HEIGHT + CatalogStyle.GUTTER)
        assertThat(CatalogStyle.pageHeight(4)).isEqualTo(CatalogStyle.pageHeight(3))
    }

    @Test
    fun tiles_fitInsideThePageWidthWithoutOverlapping() {
        val firstRight = CatalogStyle.tileLeft(0) + CatalogStyle.TILE_WIDTH
        assertThat(CatalogStyle.tileLeft(1)).isEqualTo(firstRight + CatalogStyle.GUTTER)
        assertThat(CatalogStyle.tileLeft(1) + CatalogStyle.TILE_WIDTH + CatalogStyle.MARGIN)
            .isEqualTo(CatalogStyle.PAGE_WIDTH)
    }

    @Test
    fun sampleSizeFor_keepsTheLongestSideAtOrAboveTheTarget() {
        assertThat(CatalogStyle.sampleSizeFor(longestSide = 1600, targetSide = 486)).isEqualTo(2)
        assertThat(CatalogStyle.sampleSizeFor(longestSide = 4000, targetSide = 486)).isEqualTo(8)
    }

    @Test
    fun sampleSizeFor_neverUpscalesASmallPhoto() {
        assertThat(CatalogStyle.sampleSizeFor(longestSide = 400, targetSide = 486)).isEqualTo(1)
        assertThat(CatalogStyle.sampleSizeFor(longestSide = 486, targetSide = 486)).isEqualTo(1)
    }
}
