package gt.marcos.joyeria.ui.barcode

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.barcode.LabelPiece
import gt.marcos.joyeria.domain.barcode.ProductUid
import org.junit.Test

class LabelsUiStateTest {

    private fun item(raw: String, caption: String) =
        LabelListItem(LabelPiece(checkNotNull(ProductUid.parse(raw)), caption, null, null), caption)

    private val anillo = item("XP-000003", "Anillo azul")
    private val arete = item("XP-000002", "Aretes de perla")
    private val cadena = item("XP-000001", "Cadena fina")

    private fun state() = LabelsUiState(items = listOf(anillo, arete, cadena), isLoaded = true)

    @Test
    fun toggled_selectsAndUnselectsAPiece() {
        assertThat(state().toggled("XP-000002").selectedUids).containsExactly("XP-000002")
        assertThat(state().toggled("XP-000002").toggled("XP-000002").selectedUids).isEmpty()
    }

    @Test
    fun canPrint_isFalseWithNothingSelected_andFalseWhileWorking() {
        assertThat(state().canPrint).isFalse()
        assertThat(state().toggled("XP-000001").canPrint).isTrue()
        assertThat(state().toggled("XP-000001").copy(working = true).canPrint).isFalse()
    }

    @Test
    fun visibleItems_filtersByTheCaptionShownOnScreen() {
        assertThat(state().withQuery("perla").visibleItems).containsExactly(arete)
    }

    @Test
    fun visibleItems_filtersByPieceCode() {
        assertThat(state().withQuery("XP-000001").visibleItems).containsExactly(cadena)
    }

    @Test
    fun withAllVisibleSelected_withASearch_keepsWhatWasPickedBeforeAndHidden() {
        val result = state().toggled("XP-000001").withQuery("Anillo").withAllVisibleSelected()
        assertThat(result.selectedUids).containsExactly("XP-000001", "XP-000003")
    }

    @Test
    fun withVisibleDeselected_withASearch_removesTheVisibleOnesAndKeepsTheHiddenOnes() {
        val result = state().withAllVisibleSelected().withQuery("Anillo").withVisibleDeselected()
        assertThat(result.selectedUids).containsExactly("XP-000002", "XP-000001")
    }

    @Test
    fun canClearAll_isTrueOnlyWhenQuitarLasQueSeVenWouldLeaveSomethingSelected() {
        assertThat(state().canClearAll).isFalse()
        assertThat(state().withAllVisibleSelected().canClearAll).isFalse()
        assertThat(state().withAllVisibleSelected().withQuery("Anillo").canClearAll).isTrue()
        assertThat(state().toggled("XP-000001").canClearAll).isTrue()
    }

    @Test
    fun selectedPieces_followsTheListOrder() {
        val result = state().toggled("XP-000001").toggled("XP-000003")
        assertThat(result.selectedPieces.map { it.uid.value }).containsExactly("XP-000003", "XP-000001").inOrder()
    }

    @Test
    fun withItems_dropsASelectedPieceThatNoLongerExists() {
        val result = state().toggled("XP-000003").toggled("XP-000002").withItems(listOf(arete, cadena))
        assertThat(result.selectedUids).containsExactly("XP-000002")
    }
}
