package gt.marcos.joyeria.ui.credit

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.formatForOtherPhone
import org.junit.Test
import java.util.Locale

/**
 * Los textos de la Fase 10 que viajan al teléfono de otra persona no se vuelven
 * a formatear al llegar: tienen que verse igual sin importar el idioma del
 * teléfono de ella (D-058). Las plantillas son las de `strings.xml`.
 */
class OutgoingTextTest {

    private val reminderTemplate = "Hola %1\$s, ¿cómo estás? Te escribo para recordarte que te quedó pendiente %2\$s de las piezas que llevaste."
    private val lineTemplate = "%1\$s — Total: %2\$s — Saldo: %3\$s"
    private val totalTemplate = "Saldo total: %1\$s"

    private val spanishSpain = Locale.forLanguageTag("es-ES")
    private val arabicEgypt = Locale.forLanguageTag("ar-EG")

    private fun <T> withDeviceLocale(locale: Locale, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun formatForOtherPhone_whenDeviceLocaleIsSpanishSpain_usesTheGuatemalanConvention() {
        assertThat(withDeviceLocale(spanishSpain) { Money(125000).formatForOtherPhone() }).isEqualTo("Q1,250.00")
    }

    @Test
    fun reminderMessage_whenDeviceLocaleIsSpanishSpain_writesTheAmountTheGuatemalanWay() {
        val message = withDeviceLocale(spanishSpain) {
            OutgoingText.reminderMessage(reminderTemplate, "María", Money(890000))
        }

        assertThat(message).contains("pendiente Q8,900.00 de las piezas")
        assertThat(message).doesNotContain("8.900,00")
    }

    @Test
    fun reminderMessage_whenDeviceLocaleIsArabicEgypt_stillUsesAsciiDigits() {
        val message = withDeviceLocale(arabicEgypt) {
            OutgoingText.reminderMessage(reminderTemplate, "María", Money(890000))
        }

        assertThat(message).contains("pendiente Q8,900.00 de las piezas")
    }

    @Test
    fun reminderMessage_keepsTheCustomerNameWithItsAccents() {
        val message = OutgoingText.reminderMessage(reminderTemplate, "Dña. Concepción Peña", Money(5000))

        assertThat(message).startsWith("Hola Dña. Concepción Peña, ¿cómo estás?")
    }

    @Test
    fun statementLine_whenDeviceLocaleIsSpanishSpain_writesBothAmountsTheGuatemalanWay() {
        val line = withDeviceLocale(spanishSpain) {
            OutgoingText.statementLine(lineTemplate, "19/10/26", total = Money(125000), balance = Money(75050))
        }

        assertThat(line).isEqualTo("19/10/26 — Total: Q1,250.00 — Saldo: Q750.50")
    }

    @Test
    fun statementTotal_whenDeviceLocaleIsSpanishSpain_writesTheAmountTheGuatemalanWay() {
        val total = withDeviceLocale(spanishSpain) { OutgoingText.statementTotal(totalTemplate, Money(1234567)) }

        assertThat(total).isEqualTo("Saldo total: Q12,345.67")
    }

    private val oct19of2026 = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear(); set(2026, java.util.Calendar.OCTOBER, 19, 12, 0)
    }.timeInMillis
    private val utc = java.util.TimeZone.getTimeZone("UTC")

    @Test
    fun date_whenDeviceLocaleIsSpanishSpain_isDayMonthYear() {
        assertThat(withDeviceLocale(spanishSpain) { OutgoingText.date(oct19of2026, utc) }).isEqualTo("19/10/2026")
    }

    @Test
    fun date_whenDeviceLocaleIsUnitedStates_isStillDayMonthYear() {
        // Con DateFormat.SHORT en en-US salía 10/19/26: la clienta lo leería como mes/día.
        assertThat(withDeviceLocale(Locale.US) { OutgoingText.date(oct19of2026, utc) }).isEqualTo("19/10/2026")
    }

    @Test
    fun date_whenDeviceLocaleIsArabicEgypt_stillUsesAsciiDigits() {
        assertThat(withDeviceLocale(arabicEgypt) { OutgoingText.date(oct19of2026, utc) }).isEqualTo("19/10/2026")
    }

    @Test
    fun date_padsSingleDigitDayAndMonthWithZero() {
        val march5 = java.util.GregorianCalendar(utc).apply { clear(); set(2026, java.util.Calendar.MARCH, 5, 12, 0) }.timeInMillis
        assertThat(OutgoingText.date(march5, utc)).isEqualTo("05/03/2026")
    }

    @Test
    fun statementTotal_whenDeviceLocaleIsArabicEgypt_stillUsesAsciiDigits() {
        val total = withDeviceLocale(arabicEgypt) { OutgoingText.statementTotal(totalTemplate, Money(1234567)) }

        assertThat(total).isEqualTo("Saldo total: Q12,345.67")
    }
}
