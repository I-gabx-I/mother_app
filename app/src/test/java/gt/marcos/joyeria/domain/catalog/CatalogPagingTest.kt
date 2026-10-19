package gt.marcos.joyeria.domain.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogPagingTest {

    private fun pagesFor(count: Int) = CatalogPaging.paginate((1..count).toList())

    @Test
    fun paginate_zeroPieces_givesNoPages() {
        assertThat(pagesFor(0)).isEmpty()
    }

    @Test
    fun paginate_onePiece_givesOnePage() {
        assertThat(pagesFor(1)).containsExactly(listOf(1))
    }

    @Test
    fun paginate_exactlyFourPieces_givesOneFullPage() {
        assertThat(pagesFor(4)).containsExactly(listOf(1, 2, 3, 4))
    }

    @Test
    fun paginate_fivePieces_givesTwoPagesWithTheLastHoldingOne() {
        assertThat(pagesFor(5)).containsExactly(listOf(1, 2, 3, 4), listOf(5)).inOrder()
    }

    @Test
    fun paginate_nineAndTwentyPieces_giveThreeAndFivePages() {
        assertThat(pagesFor(9)).hasSize(3)
        assertThat(pagesFor(20)).hasSize(5)
    }

    @Test
    fun paginate_twentyPieces_keepsEveryPieceOnceAndInOrder() {
        assertThat(pagesFor(20).flatten()).isEqualTo((1..20).toList())
    }
}
