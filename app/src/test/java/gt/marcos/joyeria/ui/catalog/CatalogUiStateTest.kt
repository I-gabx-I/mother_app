package gt.marcos.joyeria.ui.catalog

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.catalog.CatalogItem
import gt.marcos.joyeria.domain.model.Money
import org.junit.Test
import java.util.Locale

class CatalogUiStateTest {

    private fun piece(id: Long, caption: String) = CatalogPiece(
        item = CatalogItem(id, String.format(Locale.ROOT, "XP-%06d", id), caption, null, null, Money(1000)),
        caption = caption,
    )

    private val anillo = piece(1, "Anillo azul")
    private val arete = piece(2, "Aretes de perla")
    private val cadena = piece(3, "Cadena fina")

    private fun state() = CatalogUiState(pieces = listOf(anillo, arete, cadena), isLoaded = true)

    @Test
    fun toggled_selectsAnUnselectedPiece() {
        assertThat(state().toggled(2).selectedIds).containsExactly(2L)
    }

    @Test
    fun toggled_unselectsASelectedPiece() {
        assertThat(state().toggled(2).toggled(2).selectedIds).isEmpty()
    }

    @Test
    fun canCreate_isFalseWithNothingSelectedAndTrueWithOnePiece() {
        assertThat(state().canCreate).isFalse()
        assertThat(state().toggled(1).canCreate).isTrue()
    }

    @Test
    fun visiblePieces_filtersByTheCaptionShownOnScreen() {
        assertThat(state().withQuery("perla").visiblePieces).containsExactly(arete)
    }

    @Test
    fun visiblePieces_filtersByPieceCode() {
        assertThat(state().withQuery("XP-000003").visiblePieces).containsExactly(cadena)
    }

    @Test
    fun visiblePieces_ignoresLetterCaseOfTheQuery() {
        assertThat(state().withQuery("CADENA").visiblePieces).containsExactly(cadena)
    }

    @Test
    fun withAllVisibleSelected_withASearch_selectsOnlyWhatIsVisible() {
        val result = state().withQuery("Anillo").withAllVisibleSelected()
        assertThat(result.selectedIds).containsExactly(1L)
    }

    @Test
    fun withAllVisibleSelected_keepsPiecesPickedBeforeThatTheSearchNowHides() {
        val result = state().toggled(3).withQuery("Anillo").withAllVisibleSelected()
        assertThat(result.selectedIds).containsExactly(1L, 3L)
    }

    @Test
    fun allVisibleSelected_isFalseWhenTheSearchShowsNothing() {
        assertThat(state().toggled(1).withQuery("zzz").allVisibleSelected).isFalse()
    }

    @Test
    fun withNothingSelected_clearsEvenThePiecesTheSearchHides() {
        val result = state().toggled(1).toggled(3).withQuery("Anillo").withNothingSelected()
        assertThat(result.selectedIds).isEmpty()
    }

    @Test
    fun withVisibleDeselected_withASearch_removesTheVisibleOnesAndKeepsTheHiddenOnes() {
        // Elige las tres, filtra por «Anillo» (se ve una) y quita las que se ven:
        // la selección que el buscador esconde NO se pierde.
        val result = state().withAllVisibleSelected().withQuery("Anillo").withVisibleDeselected()
        assertThat(result.selectedIds).containsExactly(2L, 3L)
    }

    @Test
    fun withVisibleDeselected_withoutASearch_clearsTheWholeSelection() {
        val result = state().withAllVisibleSelected().withVisibleDeselected()
        assertThat(result.selectedIds).isEmpty()
    }

    @Test
    fun hiddenSelectedCount_countsOnlyTheSelectedPiecesTheSearchHides() {
        val result = state().withAllVisibleSelected().withQuery("Anillo")
        assertThat(result.hiddenSelectedCount).isEqualTo(2)
    }

    @Test
    fun hiddenSelectedCount_isZeroWithoutASearch() {
        assertThat(state().withAllVisibleSelected().hiddenSelectedCount).isEqualTo(0)
    }

    @Test
    fun canClearAll_isFalseWithNothingSelected() {
        assertThat(state().canClearAll).isFalse()
    }

    @Test
    fun canClearAll_isFalseWhenQuitarLasQueSeVenAlreadyClearsEverything() {
        assertThat(state().withAllVisibleSelected().canClearAll).isFalse()
    }

    @Test
    fun canClearAll_isTrueWhenTheSearchHidesSelectedPieces() {
        assertThat(state().withAllVisibleSelected().withQuery("Anillo").canClearAll).isTrue()
    }

    @Test
    fun canClearAll_isTrueWhenTheSearchShowsNothingButPiecesAreSelected() {
        assertThat(state().toggled(1).withQuery("zzz").canClearAll).isTrue()
    }

    @Test
    fun canClearAll_isTrueWithAPartialSelectionAndNoSearch() {
        assertThat(state().toggled(1).canClearAll).isTrue()
    }

    @Test
    fun selectedItems_followsTheListOrderNotTheOrderTheyWereTapped() {
        val result = state().toggled(3).toggled(1)
        assertThat(result.selectedItems.map { it.productId }).containsExactly(1L, 3L).inOrder()
    }

    @Test
    fun withPieces_dropsASelectedPieceThatIsNoLongerAvailable() {
        val result = state().toggled(1).toggled(2).withPieces(listOf(arete, cadena))
        assertThat(result.selectedIds).containsExactly(2L)
    }
}
