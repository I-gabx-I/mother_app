package gt.marcos.joyeria.domain.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

// JUnit4 puro, sin Robolectric: ReportDates no toca Android (D-012, mismo
// criterio que domain/pricing).
class ReportDatesTest {

    private fun guatemalaMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        val calendar = Calendar.getInstance(ReportDates.GUATEMALA_ZONE).apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
        }
        return calendar.timeInMillis
    }

    @Test
    fun bounds_returnsFirstAndLastInstantOfTheMonthInGuatemalaTime() {
        val (start, end) = ReportDates.bounds(ReportMonth(2026, 3))

        assertThat(start).isEqualTo(guatemalaMillis(2026, 3, 1, 0, 0, 0))
        assertThat(end).isEqualTo(guatemalaMillis(2026, 4, 1, 0, 0, 0))
    }

    // Criterio 5 de FASES.md Fase 11: una venta a las 23:59 del día 31
    // cuenta en ese mes, no en el siguiente -- dos tests, no uno (sección 8
    // de CLAUDE.md: dos límites distintos, cada uno con su caso).
    @Test
    fun monthOf_saleAt2359Guatemala31st_belongsToThatMonth() {
        val saleAt = guatemalaMillis(2026, 1, 31, 23, 59, 59)

        assertThat(ReportDates.monthOf(saleAt)).isEqualTo(ReportMonth(2026, 1))
    }

    @Test
    fun monthOf_saleAt0000GuatemalaFirst_belongsToTheNextMonth() {
        val saleAt = guatemalaMillis(2026, 2, 1, 0, 0, 0)

        assertThat(ReportDates.monthOf(saleAt)).isEqualTo(ReportMonth(2026, 2))
    }

    @Test
    fun previous_and_next_areInversesAcrossAYearBoundary() {
        val december = ReportMonth(2026, 12)
        val january = ReportDates.next(december)

        assertThat(january).isEqualTo(ReportMonth(2027, 1))
        assertThat(ReportDates.previous(january)).isEqualTo(december)
    }

    @Test
    fun previous_and_next_areInversesWithinTheSameYear() {
        val june = ReportMonth(2026, 6)

        assertThat(ReportDates.next(june)).isEqualTo(ReportMonth(2026, 7))
        assertThat(ReportDates.previous(june)).isEqualTo(ReportMonth(2026, 5))
    }

    @Test
    fun reportMonth_comparesByYearThenByMonth() {
        assertThat(ReportMonth(2026, 3) < ReportMonth(2026, 4)).isTrue()
        assertThat(ReportMonth(2025, 12) < ReportMonth(2026, 1)).isTrue()
        assertThat(ReportMonth(2026, 5) == ReportMonth(2026, 5)).isTrue()
    }

    @Test
    fun reportMonth_monthOutOfRange_throws() {
        try {
            ReportMonth(2026, 13)
            throw AssertionError("Debió rechazar month=13")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }
}
