package gt.marcos.joyeria.ui.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// JUnit4 puro, sin Robolectric: es una función pura, mismo criterio que
// MoneyInputTest (CLAUDE.md sección 8). Antes de esta ronda de corrección
// `isValidQuantityInputText` vivía en `ui/sale/` sin ningún test: el filtro
// de lo que puede entrar a un campo de cantidad estaba sin red, y es el
// mismo punto por donde entró D-030 en los campos de dinero.
class QuantityInputTest {

    @Test
    fun emptyText_isAllowed() {
        // Vacío tiene que poder existir: ella borra el "1" para escribir
        // otro número. Que la cantidad vacía no alcance para guardar lo
        // decide `SaleLineUiState.isValid`, no este filtro.
        assertThat(isValidQuantityInputText("")).isTrue()
    }

    @Test
    fun digitsOnly_isAllowed() {
        assertThat(isValidQuantityInputText("12")).isTrue()
    }

    @Test
    fun zero_isAllowed() {
        // "0" entra como texto (es un paso intermedio hacia "10"); que no
        // sea una cantidad válida para vender es decisión de negocio.
        assertThat(isValidQuantityInputText("0")).isTrue()
    }

    @Test
    fun manyDigits_areAllowed() {
        // Renombrado en la auditoría de nombres de test (2026-10-10): se
        // llamaba `quantityLargerThanAnyStock_isAllowed`, y eso le atribuía
        // a esta función un conocimiento del stock que no tiene -- es un
        // filtro de formato puro, no ve el inventario (CLAUDE.md sección 8).
        //
        // Lo que sí prueba, y es el porqué: este filtro no pone ningún tope
        // de magnitud. Que una cantidad mayor al stock se pueda escribir
        // completa es decisión de negocio y se avisa como mensaje en la
        // pantalla; bloquear un dígito acá, por una razón que ella no puede
        // adivinar mientras escribe, sería indistinguible de un campo roto.
        assertThat(isValidQuantityInputText("9999")).isTrue()
        assertThat(isValidQuantityInputText("1000000")).isTrue()
    }

    @Test
    fun letter_isRejected() {
        assertThat(isValidQuantityInputText("1a")).isFalse()
    }

    @Test
    fun decimalSeparator_isRejected() {
        // Una cantidad de piezas es entera: no se vende medio anillo.
        assertThat(isValidQuantityInputText("1.5")).isFalse()
        assertThat(isValidQuantityInputText("1,5")).isFalse()
    }

    @Test
    fun negativeSign_isRejected() {
        assertThat(isValidQuantityInputText("-1")).isFalse()
    }

    @Test
    fun spaces_areRejected() {
        assertThat(isValidQuantityInputText(" 1")).isFalse()
        assertThat(isValidQuantityInputText("1 ")).isFalse()
    }

    @Test
    fun nonAsciiDigits_areRejected() {
        // Dígitos árabe-índicos: un teclado con otro locale los puede
        // producir, y `toIntOrNull()` no los entiende. Si entraran al
        // campo, la cantidad quedaría en `null` sin que nada lo explique
        // (mismo razonamiento que `Locale.ROOT`, CLAUDE.md sección 5).
        assertThat(isValidQuantityInputText("١٢")).isFalse()
    }
}
