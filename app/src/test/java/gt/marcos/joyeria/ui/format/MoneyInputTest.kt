package gt.marcos.joyeria.ui.format

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
import org.junit.Test

// JUnit4 puro, sin Robolectric: son funciones puras (mismo criterio que
// D-012 aplicó al buffer de dígitos que estas reemplazan, ver
// ESTADO.md/D-030). Es el punto exacto por donde el dinero entra al
// sistema desde el dedo de la usuaria -- un error acá arruina precios sin
// que nada lo note.
class MoneyInputTest {

    // --- isValidMoneyInputText: qué puede entrar mientras ella escribe ---

    @Test
    fun isValidMoneyInputText_emptyText_isAllowed() {
        assertThat(isValidMoneyInputText("")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_digitsOnly_isAllowed() {
        assertThat(isValidMoneyInputText("20")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_trailingSeparatorWithNoDecimalsYet_isAllowed() {
        // Recién tecleó el punto -- todavía no escribió los decimales.
        // Tiene que poder seguir escribiendo, aunque todavía no sea un
        // monto completo (eso lo decide parseMoneyToCents, no esto).
        assertThat(isValidMoneyInputText("20.")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_commaAsTrailingSeparator_isAllowed() {
        assertThat(isValidMoneyInputText("20,")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_oneDecimalDigit_isAllowed() {
        assertThat(isValidMoneyInputText("20.5")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_twoDecimalDigits_isAllowed() {
        assertThat(isValidMoneyInputText("20.50")).isTrue()
    }

    @Test
    fun isValidMoneyInputText_thirdDecimalDigit_isRejected() {
        // El caso que decidiste: el tercer decimal no entra al campo.
        assertThat(isValidMoneyInputText("20.999")).isFalse()
        assertThat(isValidMoneyInputText("20.505")).isFalse()
    }

    @Test
    fun isValidMoneyInputText_secondSeparator_isRejected() {
        assertThat(isValidMoneyInputText("20.5.")).isFalse()
        assertThat(isValidMoneyInputText("20.5,3")).isFalse()
    }

    @Test
    fun isValidMoneyInputText_letters_areRejected() {
        assertThat(isValidMoneyInputText("abc")).isFalse()
        assertThat(isValidMoneyInputText("20a")).isFalse()
    }

    @Test
    fun isValidMoneyInputText_minusSign_isRejected() {
        assertThat(isValidMoneyInputText("-")).isFalse()
        assertThat(isValidMoneyInputText("-5")).isFalse()
        assertThat(isValidMoneyInputText("20-5")).isFalse()
    }

    @Test
    fun isValidMoneyInputText_doubleDash_isRejected() {
        assertThat(isValidMoneyInputText("--")).isFalse()
    }

    @Test
    fun isValidMoneyInputText_whitespace_isRejected() {
        assertThat(isValidMoneyInputText(" ")).isFalse()
        assertThat(isValidMoneyInputText("20 5")).isFalse()
    }

    // --- parseMoneyToCents: cuándo ya hay un monto completo para guardar ---

    @Test
    fun parseMoneyToCents_wholeNumber_interpretedAsQuetzales() {
        assertThat(parseMoneyToCents("20")).isEqualTo(2000L)
    }

    @Test
    fun parseMoneyToCents_oneDecimalDigit_padsWithZero() {
        assertThat(parseMoneyToCents("20.5")).isEqualTo(2050L)
    }

    @Test
    fun parseMoneyToCents_twoDecimalDigits() {
        assertThat(parseMoneyToCents("20.50")).isEqualTo(2050L)
    }

    @Test
    fun parseMoneyToCents_amountBelowOneQuetzal() {
        assertThat(parseMoneyToCents("0.05")).isEqualTo(5L)
    }

    @Test
    fun parseMoneyToCents_emptyText_isNull() {
        assertThat(parseMoneyToCents("")).isNull()
    }

    @Test
    fun parseMoneyToCents_commaAsDecimalSeparator() {
        assertThat(parseMoneyToCents("20,50")).isEqualTo(2050L)
    }

    @Test
    fun parseMoneyToCents_trailingSeparatorWithNoDecimals_isIncomplete() {
        // Válido para seguir escribiendo (isValidMoneyInputText), pero
        // todavía no es un monto completo para guardar.
        assertThat(parseMoneyToCents("20.")).isNull()
    }

    @Test
    fun parseMoneyToCents_moreThanTwoDecimals_isInvalid() {
        // Defensa en profundidad: MoneyTextField ya no deja escribir esto,
        // pero la función sigue sin inventar un número si de algún modo
        // llega un texto así (no se redondea "20.999" a "21.00" ni se
        // trunca a "20.99").
        assertThat(parseMoneyToCents("20.999")).isNull()
    }

    @Test
    fun parseMoneyToCents_letters_isInvalid() {
        assertThat(parseMoneyToCents("abc")).isNull()
    }

    @Test
    fun parseMoneyToCents_twoSeparators_isInvalid() {
        assertThat(parseMoneyToCents("20.5.3")).isNull()
    }

    @Test
    fun parseMoneyToCents_doubleDash_isInvalid() {
        assertThat(parseMoneyToCents("--")).isNull()
    }

    @Test
    fun parseMoneyToCents_negativeSign_isInvalid() {
        assertThat(parseMoneyToCents("-5")).isNull()
    }

    // --- Money.toEditableText(): la ida y vuelta con el parser ---

    @Test
    fun toEditableText_wholeQuetzales() {
        assertThat(Money(2000).toEditableText()).isEqualTo("20.00")
    }

    @Test
    fun toEditableText_belowOneQuetzal() {
        assertThat(Money(5).toEditableText()).isEqualTo("0.05")
    }

    @Test
    fun toEditableText_and_parseMoneyToCents_areInverses() {
        val cents = 2050L
        assertThat(parseMoneyToCents(Money(cents).toEditableText())).isEqualTo(cents)
    }
}
