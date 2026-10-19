package gt.marcos.joyeria.domain.barcode

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.ProductUidGenerator
import org.junit.Test

class ProductUidTest {

    @Test
    fun parse_validSixDigitUid_returnsIt() {
        assertThat(ProductUid.parse("XP-000042")?.value).isEqualTo("XP-000042")
    }

    @Test
    fun parse_sevenDigitUid_returnsIt() {
        assertThat(ProductUid.parse("XP-1234567")?.value).isEqualTo("XP-1234567")
    }

    // La garantía de D-003: un monto no se puede construir como código de pieza.
    @Test
    fun parse_priceText_returnsNull() {
        assertThat(ProductUid.parse("Q90.00")).isNull()
        assertThat(ProductUid.parse("Q1,250.00")).isNull()
    }

    @Test
    fun parse_plainNumber_returnsNull() {
        assertThat(ProductUid.parse("9000")).isNull()
        assertThat(ProductUid.parse("7501234567890")).isNull()
    }

    @Test
    fun parse_uidWithSurroundingSpaces_returnsNull() {
        assertThat(ProductUid.parse(" XP-000042")).isNull()
        assertThat(ProductUid.parse("XP-000042 ")).isNull()
    }

    @Test
    fun parse_uidWithFewerThanSixDigits_returnsNull() {
        assertThat(ProductUid.parse("XP-42")).isNull()
    }

    @Test
    fun parse_uidFollowedByAnAmount_returnsNull() {
        assertThat(ProductUid.parse("XP-000042 Q90.00")).isNull()
        assertThat(ProductUid.parse("XP-000042Q9000")).isNull()
    }

    @Test
    fun parse_nullOrEmpty_returnsNull() {
        assertThat(ProductUid.parse(null)).isNull()
        assertThat(ProductUid.parse("")).isNull()
    }

    @Test
    fun generatorOutput_forSeveralSequences_alwaysParses() {
        for (seq in listOf(1L, 42L, 999_999L, 1_000_000L, 12_345_678L)) {
            assertThat(ProductUid.parse(ProductUidGenerator.format(seq))).isNotNull()
        }
    }
}
