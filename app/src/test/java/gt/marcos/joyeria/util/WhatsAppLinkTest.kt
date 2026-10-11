package gt.marcos.joyeria.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// JUnit4 puro, sin Robolectric: normalizePhone/buildReminderUrl no tocan
// Android (D-012, mismo criterio que domain/).
class WhatsAppLinkTest {

    @Test
    fun normalizePhone_eightDigits_prependsCountryCode() {
        assertThat(WhatsAppLink.normalizePhone("55551234")).isEqualTo("50255551234")
    }

    @Test
    fun normalizePhone_elevenDigitsStartingWith502_isUsedAsIs() {
        assertThat(WhatsAppLink.normalizePhone("50255551234")).isEqualTo("50255551234")
    }

    @Test
    fun normalizePhone_thirteenDigitsWithDoubleZeroPrefix_stripsTheDoubleZero() {
        assertThat(WhatsAppLink.normalizePhone("0050255551234")).isEqualTo("50255551234")
    }

    // Criterio 2 de FASES.md Fase 10: los cuatro ejemplos de la fase dan el mismo resultado.
    @Test
    fun normalizePhone_theFourFasesExamples_allNormalizeToTheSameResult() {
        val expected = "50255551234"
        assertThat(WhatsAppLink.normalizePhone("5555-1234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("55551234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("+502 5555 1234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("50255551234")).isEqualTo(expected)
    }

    // Formatos adicionales reales de Guatemala, agregados en el plan de Fase 10.
    @Test
    fun normalizePhone_additionalRealGuatemalanFormats_allNormalizeToTheSameResult() {
        val expected = "50255551234"
        assertThat(WhatsAppLink.normalizePhone("5555 1234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("+50255551234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("(502) 5555-1234")).isEqualTo(expected)
        assertThat(WhatsAppLink.normalizePhone("00502 5555 1234")).isEqualTo(expected)
    }

    @Test
    fun normalizePhone_tooFewOrTooManyDigits_returnsNull() {
        assertThat(WhatsAppLink.normalizePhone("12345")).isNull()
        assertThat(WhatsAppLink.normalizePhone("555512345")).isNull()
    }

    @Test
    fun normalizePhone_nonGuatemalanCountryCode_returnsNull() {
        // Once digits, pero empieza con "1" (EE. UU.), no con "502".
        assertThat(WhatsAppLink.normalizePhone("15555551234")).isNull()
    }

    @Test
    fun buildReminderUrl_encodesAccentsAndNewlines() {
        val url = WhatsAppLink.buildReminderUrl("50255551234", "Hola, ¿cómo estás?\nTe debés Q150.00 de las piezas.")

        assertThat(url).startsWith("https://wa.me/50255551234?text=")
        assertThat(url).contains("%C3%B3") // "ó" en UTF-8
        assertThat(url).contains("%C3%A1") // "á"
        assertThat(url).contains("%0A") // salto de línea
        assertThat(url).doesNotContain(" ")
        assertThat(url).doesNotContain("+")
    }

    @Test
    fun buildReminderUrl_encodesEne() {
        val url = WhatsAppLink.buildReminderUrl("50255551234", "mañana")

        assertThat(url).contains("ma%C3%B1ana")
    }
}
