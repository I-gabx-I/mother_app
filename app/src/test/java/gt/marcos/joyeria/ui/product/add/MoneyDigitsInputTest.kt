package gt.marcos.joyeria.ui.product.add

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// JUnit4 puro, sin Robolectric: es el punto exacto por donde el dinero
// entra al sistema desde el dedo de la usuaria (FASES.md Fase 03, pedido
// explícito del humano). Un error de un factor de 10 acá arruina todos
// los precios de la app sin que nada lo note.
class MoneyDigitsInputTest {

    @Test
    fun digitsToCents_interprets_the_last_two_digits_as_centavos() {
        // "7590" es Q75.90, o sea 7590 centavos -- NO Q7,590 (753000
        // centavos) ni 75900 centavos. Este es el caso que protege contra
        // un error de un factor de 10.
        val result = digitsToCents("7590")

        assertThat(result).isEqualTo(7590L)
    }

    @Test
    fun digitsToCents_emptyBuffer_isZero() {
        assertThat(digitsToCents("")).isEqualTo(0L)
    }

    @Test
    fun digitsToCents_singleDigit_isCentavosOnly() {
        // "5" son 5 centavos (Q0.05), no Q5.
        assertThat(digitsToCents("5")).isEqualTo(5L)
    }

    @Test
    fun digitsToCents_twoDigits_areExactlyOneQuetzal() {
        // "100" -> Q1.00 (100 centavos), el borde entre "un dígito de
        // quetzales" y "dos dígitos de quetzales".
        assertThat(digitsToCents("100")).isEqualTo(100L)
    }

    @Test
    fun sanitizeMoneyDigits_dropsNonDigitCharacters() {
        val result = sanitizeMoneyDigits("7q5.9o")

        assertThat(result).isEqualTo("759")
    }

    @Test
    fun sanitizeMoneyDigits_capsLengthKeepingTheNewestDigits() {
        // Tope de 9 dígitos (Q9,999,999.99); un décimo dígito nuevo empuja
        // afuera al más viejo por la izquierda, no lo rechaza.
        val result = sanitizeMoneyDigits("1234567890")

        assertThat(result).isEqualTo("234567890")
        assertThat(result).hasLength(9)
    }

    @Test
    fun digitsToCents_usesTheSanitizedBuffer() {
        // Un texto con caracteres no numéricos igual se interpreta bien,
        // porque digitsToCents sanitiza antes de convertir.
        val result = digitsToCents("Q75.90")

        assertThat(result).isEqualTo(7590L)
    }
}
