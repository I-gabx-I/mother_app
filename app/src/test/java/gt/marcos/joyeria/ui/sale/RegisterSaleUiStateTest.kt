package gt.marcos.joyeria.ui.sale

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.domain.model.Money
import org.junit.Test

// JUnit4 puro, sin Robolectric: validación de estado, nada de Android
// (mismo criterio que MoneyInputTest en ui/format). Ver ESTADO.md "Fase
// 06 -- Plan", punto 2: cantidad > stock y descuento > subtotal no se
// filtran al tipear, quedan "incompletos" acá con un mensaje -- estos
// tests son los que prueban esa distinción, no solo la documentan.
class RegisterSaleUiStateTest {

    private fun line(qtyText: String = "1", availableStock: Int = 5) = SaleLineUiState(
        productId = 1,
        productName = "Anillo de prueba",
        productUid = "XP-000001",
        unitPrice = Money(8000),
        unitCost = Money(4000),
        availableStock = availableStock,
        qtyText = qtyText,
    )

    // --- SaleLineUiState: cantidad vacía/0 es incompleta sin más; mayor a stock es incompleta CON aviso ---

    @Test
    fun line_emptyQty_isInvalid_withoutExceedingStock() {
        val state = line(qtyText = "")

        assertThat(state.isValid).isFalse()
        assertThat(state.exceedsStock).isFalse()
    }

    @Test
    fun line_zeroQty_isInvalid() {
        val state = line(qtyText = "0")

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun line_qtyWithinStock_isValid() {
        val state = line(qtyText = "3", availableStock = 5)

        assertThat(state.isValid).isTrue()
        assertThat(state.exceedsStock).isFalse()
    }

    @Test
    fun line_qtyEqualsStock_isValid() {
        val state = line(qtyText = "5", availableStock = 5)

        assertThat(state.isValid).isTrue()
    }

    @Test
    fun line_qtyGreaterThanStock_isInvalid_andExceedsStockIsTrue() {
        val state = line(qtyText = "6", availableStock = 5)

        assertThat(state.isValid).isFalse()
        assertThat(state.exceedsStock).isTrue()
    }

    @Test
    fun line_subtotalAndTotalCost_multiplyByQty() {
        val state = line(qtyText = "3", availableStock = 5)

        assertThat(state.subtotal).isEqualTo(Money(8000 * 3))
        assertThat(state.totalCost).isEqualTo(Money(4000 * 3))
    }

    // --- RegisterSaleUiState.discount: D-030, texto plano, sin transformar ---

    @Test
    fun discount_blankText_isZero() {
        val state = RegisterSaleUiState(discountText = "")

        assertThat(state.discount).isEqualTo(Money.ZERO)
    }

    @Test
    fun discount_validText_parsesToCents() {
        val state = RegisterSaleUiState(discountText = "20.50")

        assertThat(state.discount).isEqualTo(Money(2050))
    }

    @Test
    fun discount_commaSeparator_parsesTheSameAsPeriod() {
        val state = RegisterSaleUiState(discountText = "20,50")

        assertThat(state.discount).isEqualTo(Money(2050))
    }

    @Test
    fun discount_incompleteText_isNull() {
        val state = RegisterSaleUiState(discountText = "20.")

        assertThat(state.discount).isNull()
    }

    // --- discountExceedsSubtotal / profit: mismo criterio que qty > stock, no se filtra al tipear ---

    @Test
    fun discountExceedsSubtotal_whenDiscountIsBiggerThanCartSubtotal() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")), // subtotal = 8000
            discountText = "90.00", // 9000 > 8000
        )

        assertThat(state.discountExceedsSubtotal).isTrue()
        assertThat(state.profit).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun profit_isNull_whileDiscountTextIsIncomplete() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            discountText = "5.",
        )

        assertThat(state.profit).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun profit_computesSaleProfit_total_minus_discount_minus_cost() {
        // 1 pieza: precio 8000, costo 4000, descuento 1000 -> ganancia 3000
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            discountText = "10.00",
        )

        assertThat(state.profit).isEqualTo(Money(3000))
    }

    @Test
    fun profit_canBeNegative_andStillComputed_notBlocked() {
        // Descuento grande, pero no mayor al subtotal -- venta con pérdida real, D-035.
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")), // precio 8000, costo 4000
            discountText = "70.00", // deja ingreso neto 1000, costo 4000 -> ganancia -3000
        )

        assertThat(state.profit).isEqualTo(Money(-3000))
        // No bloquea el guardado -- D-035 avisa antes de confirmar, no acá.
        assertThat(state.canSave).isTrue()
    }

    // --- canSave ---

    @Test
    fun canSave_falseWhenNoLines() {
        val state = RegisterSaleUiState(lines = emptyList())

        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_falseWhenAnyLineIsInvalid() {
        val state = RegisterSaleUiState(lines = listOf(line(qtyText = "1"), line(qtyText = "")))

        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_trueWhenEverythingIsValid() {
        val state = RegisterSaleUiState(lines = listOf(line(qtyText = "2")), discountText = "")

        assertThat(state.canSave).isTrue()
    }

    // --- availableProducts: solo stock > 0, filtro por nombre o código ---

    private fun product(id: Long, name: String, uid: String, stockQty: Int) = ProductSummary(
        id = id,
        uid = uid,
        name = name,
        photoPath = null,
        stockQty = stockQty,
        cost = Money(4000),
        salePrice = Money(8000),
    )

    @Test
    fun availableProducts_excludesOutOfStock() {
        val state = RegisterSaleUiState(
            products = listOf(
                product(1, "Anillo", "XP-000001", stockQty = 0),
                product(2, "Aretes", "XP-000002", stockQty = 1),
            ),
        )

        assertThat(state.availableProducts.map { it.id }).containsExactly(2L)
    }

    @Test
    fun availableProducts_filtersByNameOrUid_caseInsensitive() {
        val state = RegisterSaleUiState(
            products = listOf(
                product(1, "Anillo corazón", "XP-000001", stockQty = 3),
                product(2, "Aretes luna", "XP-000002", stockQty = 3),
            ),
            query = "anillo",
        )

        assertThat(state.availableProducts.map { it.id }).containsExactly(1L)
    }
}
