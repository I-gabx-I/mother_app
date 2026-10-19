package gt.marcos.joyeria.domain.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class CsvFormatTest {

    @Test
    fun bom_isTheThreeBytesOfUtf8() {
        assertThat(CsvFormat.BOM.map { it.toInt() and 0xFF }).containsExactly(0xEF, 0xBB, 0xBF).inOrder()
    }

    @Test
    fun text_withAccentsAndEnye_isKeptAsIs() {
        assertThat(CsvFormat.text("Niñas — Ángeles")).isEqualTo("Niñas — Ángeles")
    }

    @Test
    fun text_withAComma_isQuoted() {
        assertThat(CsvFormat.text("Anillo, dorado")).isEqualTo("\"Anillo, dorado\"")
    }

    @Test
    fun text_withQuotes_doublesThemAndQuotes() {
        assertThat(CsvFormat.text("Anillo \"sol\"")).isEqualTo("\"Anillo \"\"sol\"\"\"")
    }

    @Test
    fun text_withALineBreak_isQuoted() {
        assertThat(CsvFormat.text("línea 1\nlínea 2")).isEqualTo("\"línea 1\nlínea 2\"")
    }

    @Test
    fun text_startingWithEquals_isNeutralizedSoExcelDoesNotRunItAsAFormula() {
        assertThat(CsvFormat.text("=HYPERLINK(\"http://x\")")).startsWith("\"'=")
    }

    @Test
    fun text_startingWithPlusMinusOrAt_isNeutralized() {
        assertThat(CsvFormat.text("+5")).isEqualTo("'+5")
        assertThat(CsvFormat.text("-cmd")).isEqualTo("'-cmd")
        assertThat(CsvFormat.text("@usuaria")).isEqualTo("'@usuaria")
    }

    @Test
    fun text_null_isAnEmptyCell() {
        assertThat(CsvFormat.text(null)).isEmpty()
    }

    @Test
    fun money_writesQuetzalesWithTwoDecimalsAndNoCurrencySymbol() {
        assertThat(CsvFormat.money(7590)).isEqualTo("75.90")
        assertThat(CsvFormat.money(5)).isEqualTo("0.05")
        assertThat(CsvFormat.money(100000)).isEqualTo("1000.00")
        assertThat(CsvFormat.money(0)).isEqualTo("0.00")
    }

    @Test
    fun money_negative_keepsTheSign() {
        assertThat(CsvFormat.money(-7590)).isEqualTo("-75.90")
    }

    @Test
    fun money_whenTheDeviceLocaleUsesCommaDecimals_stillUsesAPoint() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        try {
            assertThat(CsvFormat.money(125000)).isEqualTo("1250.00")
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun line_joinsWithCommasAndEndsWithCrLf() {
        assertThat(CsvFormat.line(listOf("a", "b", "c"))).isEqualTo("a,b,c\r\n")
    }
}
