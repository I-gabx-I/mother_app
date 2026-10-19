package gt.marcos.joyeria.domain.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UidSequenceTest {

    @Test
    fun nextAfterImport_whenTheFileCounterIsAheadOfTheCodes_usesTheFileCounter() {
        assertThat(UidSequence.nextAfterImport(fileSeq = 50, uids = listOf("XP-000001", "XP-000010"))).isEqualTo(50)
    }

    @Test
    fun nextAfterImport_whenACodeIsAheadOfTheFileCounter_neverRepeatsIt() {
        assertThat(UidSequence.nextAfterImport(fileSeq = 5, uids = listOf("XP-000001", "XP-000042"))).isEqualTo(43)
    }

    @Test
    fun nextAfterImport_withoutCounterAndWithoutPieces_startsAtOne() {
        assertThat(UidSequence.nextAfterImport(null, emptyList())).isEqualTo(1)
    }

    @Test
    fun nextAfterImport_ignoresTextThatIsNotACode() {
        assertThat(UidSequence.nextAfterImport(2, listOf("XP-000003", "Q90.00", "otra-cosa"))).isEqualTo(4)
    }

    @Test
    fun nextAfterImport_handlesCodesWithSevenDigits() {
        assertThat(UidSequence.nextAfterImport(2, listOf("XP-1234567"))).isEqualTo(1234568)
    }
}
