package gt.marcos.joyeria.domain.barcode

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LabelSheetSpecTest {

    private val spec = LabelSheetSpec()

    @Test
    fun letterSheetWith38x16Labels_fitsFourColumnsAndFourteenRows() {
        assertThat(spec.columns).isEqualTo(4)
        assertThat(spec.rows).isEqualTo(14)
        assertThat(spec.labelsPerPage).isEqualTo(56)
    }

    @Test
    fun everyLabelOfASheet_staysInsideThePage() {
        for (i in 0 until spec.labelsPerPage) {
            assertThat(spec.labelLeftMm(i)).isAtLeast(0.0)
            assertThat(spec.labelTopMm(i)).isAtLeast(0.0)
            assertThat(spec.labelLeftMm(i) + spec.labelWidthMm).isAtMost(spec.pageWidthMm)
            assertThat(spec.labelTopMm(i) + spec.labelHeightMm).isAtMost(spec.pageHeightMm)
        }
    }

    @Test
    fun neighbouringLabels_doNotOverlap() {
        assertThat(spec.labelLeftMm(1)).isAtLeast(spec.labelLeftMm(0) + spec.labelWidthMm)
        assertThat(spec.labelTopMm(spec.columns)).isAtLeast(spec.labelTopMm(0) + spec.labelHeightMm)
    }

    @Test
    fun moduleMmFor_aSixDigitUid_isTheSheetModuleAndFitsWithItsQuietZones() {
        val modules = 112
        assertThat(spec.moduleMmFor(modules)).isEqualTo(0.254)
        assertThat((modules + 2 * LabelSheetSpec.QUIET_ZONE_MODULES) * spec.moduleMmFor(modules))
            .isAtMost(spec.labelWidthMm)
    }

    @Test
    fun moduleMm_isAWholeNumberOfDotsAt300And600And1200Dpi() {
        // Con un módulo que no es entero en puntos de la impresora, las barras salen de anchos
        // irregulares y el código no se lee (se midió: a 203 dpi, 2.03 puntos, no leyó ninguna).
        for (dpi in listOf(300, 600, 1200)) {
            val dots = spec.moduleMm * dpi / 25.4
            assertThat(dots).isWithin(1e-9).of(Math.round(dots).toDouble())
        }
    }

    @Test
    fun moduleMmFor_aSevenDigitUid_isStillNotBelowTheQuarterMillimeterFloor() {
        assertThat(spec.moduleMmFor(123)).isAtLeast(0.25)
    }

    @Test
    fun moduleMmFor_anAbsurdlyLongCode_shrinksInsteadOfOverflowingTheLabel() {
        val modules = 300
        assertThat((modules + 2 * LabelSheetSpec.QUIET_ZONE_MODULES) * spec.moduleMmFor(modules))
            .isAtMost(spec.labelWidthMm + 1e-9)
    }

    @Test
    fun paginate_zeroOneFullAndOnePastFull_giveZeroOneOneAndTwoSheets() {
        fun sheets(n: Int) = LabelSheetPaging.paginate((1..n).toList(), spec).size
        assertThat(sheets(0)).isEqualTo(0)
        assertThat(sheets(1)).isEqualTo(1)
        assertThat(sheets(spec.labelsPerPage)).isEqualTo(1)
        assertThat(sheets(spec.labelsPerPage + 1)).isEqualTo(2)
    }

    @Test
    fun paginate_keepsEveryLabelOnceAndInOrder() {
        val items = (1..130).toList()
        assertThat(LabelSheetPaging.paginate(items, spec).flatten()).isEqualTo(items)
    }
}
