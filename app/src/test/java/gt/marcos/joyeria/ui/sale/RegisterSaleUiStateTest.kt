package gt.marcos.joyeria.ui.sale

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.domain.model.Money
import org.junit.Test

// JUnit4 puro, sin Robolectric: validación de estado, nada de Android
// (mismo criterio que MoneyInputTest en ui/format). Ver ESTADO.md "Fase
// 06 -- Plan", punto 2: cantidad > stock y descuento > subtotal no se
// filtran al tipear, quedan "incompletos" acá con un mensaje -- estos
// tests son los que prueban esa distinción, no solo la documentan.
class RegisterSaleUiStateTest {

    private fun line(
        qtyText: String = "1",
        availableStock: Int = 5,
        unitPriceText: String = "80.00",
        productId: Long = 1,
        productName: String = "Anillo de prueba",
        unitCost: Money = Money(4000),
    ) = SaleLineUiState(
        productId = productId,
        productName = productName,
        productUid = "XP-00000$productId",
        unitPriceText = unitPriceText,
        unitCost = unitCost,
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

    // --- D-045: el precio de la línea es editable, y es dinero (D-030) ---

    @Test
    fun linePrice_blankText_isNull_notZero() {
        // A diferencia del descuento y del abono inicial, un precio vacío NO
        // es cero: una venta sin precio no es una venta regalada, es una
        // venta a medio escribir.
        val state = line(unitPriceText = "")

        assertThat(state.unitPrice).isNull()
        assertThat(state.isValid).isFalse()
    }

    @Test
    fun linePrice_incompleteText_isNull() {
        val state = line(unitPriceText = "20.")

        assertThat(state.unitPrice).isNull()
        assertThat(state.isValid).isFalse()
    }

    @Test
    fun linePrice_commaSeparator_parsesTheSameAsPeriod() {
        assertThat(line(unitPriceText = "20,50").unitPrice).isEqualTo(Money(2050))
        assertThat(line(unitPriceText = "20.50").unitPrice).isEqualTo(Money(2050))
    }

    @Test
    fun linePrice_aboveListPrice_isPerfectlyValid() {
        // El caso que motivó el cambio entero: vende más caro que la lista.
        val state = line(qtyText = "1", unitPriceText = "120.00")

        assertThat(state.isValid).isTrue()
        assertThat(state.subtotal).isEqualTo(Money(12000))
        assertThat(state.profit).isEqualTo(Money(8000))
        assertThat(state.isLoss).isFalse()
    }

    @Test
    fun linePrice_belowCost_isValidButIsLoss() {
        val state = line(qtyText = "1", unitPriceText = "30.00") // costo 4000

        assertThat(state.isValid).isTrue()
        assertThat(state.profit).isEqualTo(Money(-1000))
        assertThat(state.isLoss).isTrue()
    }

    @Test
    fun linePrice_exactlyAtCost_isLoss_withZeroProfit() {
        val state = line(qtyText = "1", unitPriceText = "40.00") // costo 4000

        assertThat(state.profit).isEqualTo(Money.ZERO)
        assertThat(state.isLoss).isTrue()
    }

    @Test
    fun lineProfit_multipliesByQty() {
        val state = line(qtyText = "3", unitPriceText = "80.00") // (8000-4000)*3

        assertThat(state.profit).isEqualTo(Money(12000))
    }

    @Test
    fun lineIsLoss_isFalseWhileQtyIsIncomplete() {
        // Con cantidad vacía la ganancia da cero por multiplicar por cero, y
        // eso NO es una pérdida: es una línea a medio escribir. Avisarle de
        // una pérdida inexistente mientras teclea sería ruido.
        val state = line(qtyText = "", unitPriceText = "30.00")

        assertThat(state.isLoss).isFalse()
    }

    // --- D-046: el aviso de pérdida se evalúa POR LÍNEA, no sobre el total ---

    @Test
    fun lossLines_namesTheLosingPiece_evenWhenTotalProfitIsPositive() {
        // El caso exacto que D-035 dejaba pasar: tres anillos con buena
        // ganancia tapan un collar rematado bajo costo. El total da
        // positivo y el aviso por total nunca se disparaba.
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, productName = "Anillo", qtyText = "3", unitPriceText = "80.00"),
                line(productId = 2, productName = "Collar", qtyText = "1", unitPriceText = "30.00"),
            ),
        )

        assertThat(state.profit).isEqualTo(Money(11000)) // 12000 + (-1000)
        assertThat(state.profit?.cents).isGreaterThan(0L)
        assertThat(state.lossLines.map { it.productName }).containsExactly("Collar")
        assertThat(state.lossLines.first().profit).isEqualTo(Money(-1000))
    }

    @Test
    fun lossLines_isEmptyWhenEveryLineLeavesProfit() {
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "1", unitPriceText = "80.00"),
                line(productId = 2, qtyText = "2", unitPriceText = "90.00"),
            ),
        )

        assertThat(state.lossLines).isEmpty()
    }

    @Test
    fun lossLines_listsEveryLosingLine_notJustTheFirst() {
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, productName = "Collar", qtyText = "1", unitPriceText = "30.00"),
                line(productId = 2, productName = "Aretes", qtyText = "1", unitPriceText = "40.00"),
                line(productId = 3, productName = "Anillo", qtyText = "1", unitPriceText = "80.00"),
            ),
        )

        assertThat(state.lossLines.map { it.productName }).containsExactly("Collar", "Aretes")
    }

    @Test
    fun perLineWarningCoversMoreThanTotalWarning_allLinesProfitableImpliesTotalProfitable() {
        // La razón por la que se puede retirar el aviso por total sin dejar
        // un hueco: sin descuento de venta, la ganancia total es la suma de
        // las ganancias por línea, así que si todas son positivas el total
        // no puede ser negativo. Este test fija esa relación.
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "1", unitPriceText = "40.01"),
                line(productId = 2, qtyText = "5", unitPriceText = "40.01"),
            ),
        )

        assertThat(state.lossLines).isEmpty()
        assertThat(state.profit?.cents).isGreaterThan(0L)
    }

    // --- subtotal/profit: null mientras alguna línea esté incompleta ---

    @Test
    fun subtotal_isNull_whileAnyLinePriceIsIncomplete() {
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "1", unitPriceText = "80.00"),
                line(productId = 2, qtyText = "1", unitPriceText = "5."),
            ),
        )

        assertThat(state.subtotal).isNull()
        assertThat(state.profit).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun subtotal_isNull_whenCartIsEmpty() {
        assertThat(RegisterSaleUiState().subtotal).isNull()
    }

    @Test
    fun subtotal_sumsEveryLine() {
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "2", unitPriceText = "80.00"),
                line(productId = 2, qtyText = "1", unitPriceText = "45.50"),
            ),
        )

        assertThat(state.subtotal).isEqualTo(Money(16000 + 4550))
    }

    @Test
    fun profit_canBeNegative_andStillComputed_notBlocked() {
        // Venta entera con pérdida: se calcula y NO bloquea el guardado --
        // el aviso de D-046 le pregunta antes de confirmar, no acá.
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1", unitPriceText = "30.00")), // costo 4000
        )

        assertThat(state.profit).isEqualTo(Money(-1000))
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
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "1"),
                line(productId = 2, qtyText = ""),
            ),
        )

        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_falseWhenAnyLineHasNoCompletePrice() {
        val state = RegisterSaleUiState(
            lines = listOf(
                line(productId = 1, qtyText = "1", unitPriceText = "80.00"),
                line(productId = 2, qtyText = "1", unitPriceText = ""),
            ),
        )

        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_trueWhenEverythingIsValid() {
        val state = RegisterSaleUiState(lines = listOf(line(qtyText = "2")))

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
    fun availableProducts_filtersByName_caseInsensitive() {
        val state = RegisterSaleUiState(
            products = listOf(
                product(1, "Anillo corazón", "XP-000001", stockQty = 3),
                product(2, "Aretes luna", "XP-000002", stockQty = 3),
            ),
            query = "anillo",
        )

        assertThat(state.availableProducts.map { it.id }).containsExactly(1L)
    }

    // D-044: ella busca por código como camino principal. Hasta esta ronda
    // había un solo test con nombre `filtersByNameOrUid` que ejercitaba
    // únicamente la rama del nombre -- el nombre del test prometía una
    // cobertura que no tenía, y la búsqueda por código (la que ella de
    // verdad usa) quedaba sin red. Los tres casos de abajo son las tres
    // formas en que puede teclear el código de la etiqueta.

    @Test
    fun availableProducts_filtersByFullUid() {
        val state = RegisterSaleUiState(
            products = listOf(
                product(1, "Anillo corazón", "XP-000001", stockQty = 3),
                product(2, "Aretes luna", "XP-000002", stockQty = 3),
            ),
            query = "XP-000002",
        )

        assertThat(state.availableProducts.map { it.id }).containsExactly(2L)
    }

    @Test
    fun availableProducts_filtersByUidDigitsOnly() {
        // Lo más probable: teclea solo los números, sin el prefijo.
        val state = RegisterSaleUiState(
            products = listOf(
                product(1, "Anillo corazón", "XP-000001", stockQty = 3),
                product(2, "Aretes luna", "XP-000002", stockQty = 3),
            ),
            query = "000002",
        )

        assertThat(state.availableProducts.map { it.id }).containsExactly(2L)
    }

    @Test
    fun availableProducts_filtersByUid_regardlessOfQueryCase() {
        // El teclado del teléfono entrega minúsculas por defecto; el uid se
        // genera siempre en mayúsculas (`XP-%06d`). Sin ignorar el caso,
        // teclear el código tal como sale del teclado no encontraría nada.
        //
        // El nombre dice "sea cual sea el caso de la CONSULTA", no
        // "caseInsensitive" a secas (auditoría de nombres de test,
        // 2026-10-10): lo único que puede variar de caso acá es lo que ella
        // teclea. Un uid en minúsculas no existe, así que prometer
        // insensibilidad en los dos lados sería prometer algo que el dato no
        // permite probar. Se ejercitan las tres formas que sí puede teclear.
        val products = listOf(
            product(1, "Anillo corazón", "XP-000001", stockQty = 3),
            product(2, "Aretes luna", "XP-000002", stockQty = 3),
        )

        listOf("xp-000001", "XP-000001", "Xp-000001").forEach { query ->
            val state = RegisterSaleUiState(products = products, query = query)

            assertThat(state.availableProducts.map { it.id }).containsExactly(1L)
        }
    }

    @Test
    fun availableProducts_uidSearchStillExcludesOutOfStock() {
        // Buscar el código exacto de una pieza agotada no la trae de vuelta:
        // el filtro de stock gana sobre la búsqueda, para no ofrecerle algo
        // que no puede vender.
        val state = RegisterSaleUiState(
            products = listOf(product(1, "Anillo corazón", "XP-000001", stockQty = 0)),
            query = "XP-000001",
        )

        assertThat(state.availableProducts).isEmpty()
    }

    // --- Fase 07: contado/crédito (D-042). Contado sigue exactamente igual
    // que Fase 06 -- estos tests prueban que agregar el crédito no le
    // cambió nada al camino CASH. ---

    @Test
    fun canSave_cashSale_ignoresCustomerAndInitialPayment() {
        // Auditoría de nombres de test (2026-10-10): este test dejaba los
        // dos campos en su valor por defecto, o sea que probaba "funciona
        // cuando no están", no "los ignora" (CLAUDE.md sección 8).
        //
        // Ahora los pone PRESENTES y en el peor estado posible: sin clienta
        // elegida y con un abono inicial de Q900.00 sobre un carrito de
        // Q80.00, que en una venta a crédito bloquearía el guardado. En
        // contado tiene que dar igual, porque `canSave` corta en
        // `!isCredit ||` y nunca llega a evaluar esa parte. Se afirma
        // explícitamente que la condición que bloquearía ESTÁ activa, si no
        // el test volvería a probar la ausencia en vez de la indiferencia.
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")), // subtotal 8000
            selectedCustomerId = null,
            initialPaymentText = "900.00", // 90000 > 8000
        )

        assertThat(state.isCredit).isFalse()
        assertThat(state.initialPaymentExceedsNet).isTrue()
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun canSave_creditSale_withTheSameExcessivePayment_isFalse() {
        // El contraste que le da sentido al test de arriba: los mismos
        // datos, cambiando solo el tipo de venta, sí bloquean. Sin esto,
        // "lo ignora" podría ser cierto por accidente (porque la condición
        // nunca bloquea en ningún caso).
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = 1L,
            initialPaymentText = "900.00",
        )

        assertThat(state.initialPaymentExceedsNet).isTrue()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_creditSale_withoutCustomer_isFalse() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = null,
        )

        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_creditSale_withCustomerAndNoInitialPayment_isTrue() {
        // Punto 5 del pedido de Fase 07: nunca exige un abono inicial.
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = 1L,
            initialPaymentText = "",
        )

        assertThat(state.initialPayment).isEqualTo(Money.ZERO)
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun canSave_creditSale_initialPaymentExceedsNet_isFalse() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")), // net = 8000
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = 1L,
            initialPaymentText = "90.00", // 9000 > 8000
        )

        assertThat(state.initialPaymentExceedsNet).isTrue()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_creditSale_initialPaymentIncomplete_isFalse() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")),
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = 1L,
            initialPaymentText = "20.",
        )

        assertThat(state.initialPayment).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun canSave_creditSale_validPartialInitialPayment_isTrue() {
        val state = RegisterSaleUiState(
            lines = listOf(line(qtyText = "1")), // net = 8000
            saleType = SaleTypeChoice.CREDIT,
            selectedCustomerId = 1L,
            initialPaymentText = "30.00",
        )

        assertThat(state.canSave).isTrue()
    }

    @Test
    fun customers_availableForPicker_comeFromState() {
        val customer = Customer(id = 1, name = "Doña María", phone = null, notes = null, archived = false)
        val state = RegisterSaleUiState(customers = listOf(customer))

        assertThat(state.customers).containsExactly(customer)
    }
}
