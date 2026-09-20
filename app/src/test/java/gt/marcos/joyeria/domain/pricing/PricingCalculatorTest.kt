package gt.marcos.joyeria.domain.pricing

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
import org.junit.Assert.assertThrows
import org.junit.Test

// JUnit4 puro, sin Robolectric: aritmética de domain, nada de Android
// (CLAUDE.md sección 8 / D-012).
class PricingCalculatorTest {

    // --- profit: nunca divide, nunca es null (CLAUDE.md 3.5) ---

    @Test
    fun profit_canonicalExample_cost40_price100_isProfit60() {
        val result = PricingCalculator.profit(cost = Money(4000), salePrice = Money(10000))

        assertThat(result).isEqualTo(Money(6000))
    }

    @Test
    fun profit_zeroCost() {
        val result = PricingCalculator.profit(cost = Money(0), salePrice = Money(5000))

        assertThat(result).isEqualTo(Money(5000))
    }

    @Test
    fun profit_zeroSalePrice_isNegativeCost() {
        val result = PricingCalculator.profit(cost = Money(4000), salePrice = Money(0))

        assertThat(result).isEqualTo(Money(-4000))
    }

    @Test
    fun profit_salePriceLessThanCost_isNegative() {
        val result = PricingCalculator.profit(cost = Money(10000), salePrice = Money(7000))

        assertThat(result).isEqualTo(Money(-3000))
    }

    @Test
    fun profit_bothZero_isZero() {
        val result = PricingCalculator.profit(cost = Money(0), salePrice = Money(0))

        assertThat(result).isEqualTo(Money.ZERO)
    }

    // --- marginOnSale: puntos básicos (D-013), null si salePrice es 0 (D-015) ---

    @Test
    fun marginOnSale_canonicalExample_is6000Bp() {
        val result = PricingCalculator.marginOnSale(cost = Money(4000), salePrice = Money(10000))

        assertThat(result).isEqualTo(6000)
    }

    @Test
    fun marginOnSale_zeroSalePrice_returnsNull() {
        val result = PricingCalculator.marginOnSale(cost = Money(4000), salePrice = Money(0))

        assertThat(result).isNull()
    }

    @Test
    fun marginOnSale_zeroCostPositivePrice_is10000Bp() {
        val result = PricingCalculator.marginOnSale(cost = Money(0), salePrice = Money(5000))

        assertThat(result).isEqualTo(10000)
    }

    @Test
    fun marginOnSale_priceLessThanCost_isNegativeBp() {
        val result = PricingCalculator.marginOnSale(cost = Money(10000), salePrice = Money(8000))

        assertThat(result).isEqualTo(-2500)
    }

    // --- markupOnCost: puntos básicos (D-013/D-014), null si cost es 0 (D-015) ---

    @Test
    fun markupOnCost_canonicalExample_is15000Bp() {
        val result = PricingCalculator.markupOnCost(cost = Money(4000), salePrice = Money(10000))

        assertThat(result).isEqualTo(15000)
    }

    @Test
    fun markupOnCost_zeroCost_returnsNull() {
        val result = PricingCalculator.markupOnCost(cost = Money(0), salePrice = Money(5000))

        assertThat(result).isNull()
    }

    @Test
    fun markupOnCost_zeroCostAndZeroPrice_returnsNull() {
        val result = PricingCalculator.markupOnCost(cost = Money(0), salePrice = Money(0))

        assertThat(result).isNull()
    }

    @Test
    fun markupOnCost_priceLessThanCost_isNegativeBp() {
        val result = PricingCalculator.markupOnCost(cost = Money(10000), salePrice = Money(8000))

        assertThat(result).isEqualTo(-2000)
    }

    // --- D-015: un margen/recargo real de 0% no es lo mismo que "no se puede
    // calcular". Estos dos tests existen para que nadie vuelva a colapsar
    // marginOnSale/markupOnCost a un centinela 0 en el futuro. ---

    @Test
    fun marginOnSale_equalCostAndPrice_isZeroBpNotNull() {
        val result = PricingCalculator.marginOnSale(cost = Money(5000), salePrice = Money(5000))

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun markupOnCost_equalCostAndPrice_isZeroBpNotNull() {
        val result = PricingCalculator.markupOnCost(cost = Money(5000), salePrice = Money(5000))

        assertThat(result).isEqualTo(0)
    }

    // --- suggestedPrice: misma fórmula de recargo que markupOnCost (D-014),
    // redondeo hacia arriba al múltiplo de roundingStep (CLAUDE.md 3.4) ---

    @Test
    fun suggestedPrice_defaultMarkupBp10000_doublesTheCost() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(4000),
            markupBp = 10000,
            roundingStep = Money(1),
        )

        assertThat(result).isEqualTo(Money(8000))
    }

    @Test
    fun suggestedPrice_roundingExactMultiple_q75StaysQ75() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(7500),
            markupBp = 0,
            roundingStep = Money(500),
        )

        assertThat(result).isEqualTo(Money(7500))
    }

    @Test
    fun suggestedPrice_roundingRoundsUp_q71ToQ75() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(7100),
            markupBp = 0,
            roundingStep = Money(500),
        )

        assertThat(result).isEqualTo(Money(7500))
    }

    @Test
    fun suggestedPrice_roundingRoundsUp_q76ToQ80() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(7600),
            markupBp = 0,
            roundingStep = Money(500),
        )

        assertThat(result).isEqualTo(Money(8000))
    }

    @Test
    fun suggestedPrice_zeroCost_isZero() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(0),
            markupBp = 10000,
            roundingStep = Money(500),
        )

        assertThat(result).isEqualTo(Money.ZERO)
    }

    @Test
    fun suggestedPrice_roundingStepOfOneCent_isEffectivelyNoRounding() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(4001),
            markupBp = 0,
            roundingStep = Money(1),
        )

        assertThat(result).isEqualTo(Money(4001))
    }

    @Test
    fun suggestedPrice_roundingStepZeroOrNegative_returnsRawPriceWithoutCrash() {
        val zeroStep = PricingCalculator.suggestedPrice(
            cost = Money(4000),
            markupBp = 10000,
            roundingStep = Money(0),
        )
        val negativeStep = PricingCalculator.suggestedPrice(
            cost = Money(4000),
            markupBp = 10000,
            roundingStep = Money(-500),
        )

        assertThat(zeroStep).isEqualTo(Money(8000))
        assertThat(negativeStep).isEqualTo(Money(8000))
    }

    // D-015 (adenda): roundUpToMultiple usaba `%`, que en Kotlin sigue el
    // signo del dividendo, y daba un múltiplo incorrecto para `value`
    // negativo. -4100 con paso 500 daba -3500; el múltiplo superior
    // correcto es -4000. Se corrigió con floorDiv. En la práctica esto solo
    // se alcanza con un markupBp < -10000 (recargo peor que -100%, fuera de
    // cualquier caso de negocio real), pero la función tiene que ser
    // correcta para cualquier entrada, no solo para las que hoy llegan.
    @Test
    fun suggestedPrice_roundingNegativeRawPrice_minus4100RoundsUpToMinus4000() {
        val result = PricingCalculator.suggestedPrice(
            cost = Money(10000),
            markupBp = -14100,
            roundingStep = Money(500),
        )

        assertThat(result).isEqualTo(Money(-4000))
    }

    @Test
    fun suggestedPrice_isInverseOfMarkupOnCost_forCanonicalPair() {
        val cost = Money(4000)
        val salePrice = Money(10000)

        val markupBp = checkNotNull(PricingCalculator.markupOnCost(cost, salePrice)) {
            "markupOnCost no puede dar null para el par canónico"
        }
        val reconstructed = PricingCalculator.suggestedPrice(
            cost = cost,
            markupBp = markupBp,
            roundingStep = Money(1),
        )

        assertThat(markupBp).isEqualTo(15000)
        assertThat(reconstructed).isEqualTo(salePrice)
    }

    // El criterio 5 de Fase 02 solo cubre el par con ganancia. Este test
    // prueba la misma inversa con un par en pérdida (salePrice < cost, D-015
    // adenda): markupOnCost negativo, y suggestedPrice tiene que reconstruir
    // el precio original igual que en el caso con ganancia.
    @Test
    fun suggestedPrice_isInverseOfMarkupOnCost_forLossPair() {
        val cost = Money(10000)
        val salePrice = Money(8000)

        val markupBp = checkNotNull(PricingCalculator.markupOnCost(cost, salePrice)) {
            "markupOnCost no puede dar null para este par"
        }
        val reconstructed = PricingCalculator.suggestedPrice(
            cost = cost,
            markupBp = markupBp,
            roundingStep = Money(1),
        )

        assertThat(markupBp).isEqualTo(-2000)
        assertThat(reconstructed).isEqualTo(salePrice)
    }

    // --- allocateExtraCost: prorratea proporcional al valor de cada línea,
    // el residuo de redondeo va a la línea de mayor valor (ESQUEMA.md,
    // Fase 05 criterio 2, [TESTS OBLIGATORIOS]) ---

    @Test
    fun allocateExtraCost_dividesExactly_proportionalToLineValue() {
        // línea 1: 2 u. a Q10 (valor 2000); línea 2: 2 u. a Q30 (valor 6000).
        // Extra Q8 (800), proporción 1:3 -> 200 y 600, exacto.
        val result = PricingCalculator.allocateExtraCost(
            lines = listOf(2 to Money(1000), 2 to Money(3000)),
            extraCents = 800,
        )

        assertThat(result).containsExactly(200L, 600L).inOrder()
        assertThat(result.sum()).isEqualTo(800L)
    }

    @Test
    fun allocateExtraCost_withRemainder_goesToTheHighestValueLine() {
        // Q10 (1000 centavos) entre 3 líneas del mismo valor: 1000/3 no es
        // exacto -- el residuo tiene que ir a la de mayor valor (acá,
        // empatadas, a la primera) para que la suma siga dando 1000 exacto.
        val result = PricingCalculator.allocateExtraCost(
            lines = listOf(1 to Money(1000), 1 to Money(1000), 1 to Money(1000)),
            extraCents = 1000,
        )

        assertThat(result.sum()).isEqualTo(1000L)
        assertThat(result[0]).isEqualTo(334L)
        assertThat(result[1]).isEqualTo(333L)
        assertThat(result[2]).isEqualTo(333L)
    }

    @Test
    fun allocateExtraCost_zeroExtra_isAllZeros() {
        val result = PricingCalculator.allocateExtraCost(
            lines = listOf(2 to Money(1000), 3 to Money(2000)),
            extraCents = 0,
        )

        assertThat(result).containsExactly(0L, 0L).inOrder()
    }

    @Test
    fun allocateExtraCost_singleLine_getsAllOfIt() {
        val result = PricingCalculator.allocateExtraCost(
            lines = listOf(4 to Money(1000)),
            extraCents = 777,
        )

        assertThat(result).containsExactly(777L)
    }

    // --- weightedAverageCost: regla híbrida de D-029, ceilDiv redondea
    // hacia arriba (Fase 05 criterio 3/4, [TESTS OBLIGATORIOS]) ---

    @Test
    fun weightedAverageCost_d029CanonicalExample_10at40plus5at55_is45Exact() {
        val result = PricingCalculator.weightedAverageCost(
            currentStockQty = 10,
            currentCost = Money(4000),
            purchaseQty = 5,
            lineTotalCost = Money(5 * 5500),
        )

        assertThat(result).isEqualTo(Money(4500))
    }

    @Test
    fun weightedAverageCost_notExactDivision_roundsUp() {
        // 10 u. a Q40 (40000) + 5 u. a Q30 (15000) = 55000 / 15 = 3666.67
        val result = PricingCalculator.weightedAverageCost(
            currentStockQty = 10,
            currentCost = Money(4000),
            purchaseQty = 5,
            lineTotalCost = Money(5 * 3000),
        )

        assertThat(result).isEqualTo(Money(3667))
    }

    @Test
    fun weightedAverageCost_stockZero_replacesCostDirectly_ignoringOldCost() {
        // D-029: con stock 0 la fórmula ya da el costo real de la compra,
        // sin importar qué "basura" tuviera el costo viejo.
        val result = PricingCalculator.weightedAverageCost(
            currentStockQty = 0,
            currentCost = Money(9999),
            purchaseQty = 5,
            lineTotalCost = Money(5 * 4000),
        )

        assertThat(result).isEqualTo(Money(4000))
    }

    @Test
    fun weightedAverageCost_stockZero_withProratedExtra_stillRoundsUp() {
        // Mismo caso de "Fase 05 -- Plan": 3 u. a Q40 + Q1 de transporte
        // prorrateado = 12100 / 3 = 4033.33.
        val result = PricingCalculator.weightedAverageCost(
            currentStockQty = 0,
            currentCost = Money(0),
            purchaseQty = 3,
            lineTotalCost = Money(3 * 4000 + 100),
        )

        assertThat(result).isEqualTo(Money(4034))
    }

    // --- weightedAverageCost: falla ruidosamente en vez de un Money.ZERO
    // silencioso (corrección post-cierre de Fase 05, mismo espíritu que
    // D-015: un costo cero se persiste en product.cost_cents como si fuera
    // un dato real, nadie lo cuestiona, y de ahí en más markupOnCost da
    // null y la ganancia se calcula sobre un costo inventado). ---

    @Test
    fun weightedAverageCost_totalQtyZeroOrLess_throwsInsteadOfSilentlyReturningZero() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.weightedAverageCost(
                currentStockQty = 0,
                currentCost = Money.ZERO,
                purchaseQty = 0,
                lineTotalCost = Money.ZERO,
            )
        }
    }

    // --- Limitación conocida del redondeo hacia arriba (documentada en
    // D-029): el sesgo se mide, no se descubre por sorpresa. El costo
    // redondeado hacia arriba se multiplica por TODO el stock, no solo
    // por lo comprado, y una compra que no divide exacto lo agranda sin
    // que ninguna compra futura lo compense -- hasta que stock_qty vuelve
    // a 0 (reemplazo directo, D-029). Números elegidos y verificados a
    // mano, no un rango genérico: el sesgo tiene que ser exactamente el
    // que predice la aritmética, ni un centavo más ni uno menos. ---

    @Test
    fun weightedAverageCost_repeatedPurchases_accumulateABoundedRoundingBias_neverUnderstatingCost() {
        // "Dinero real gastado" -- la suma de lo que costó cada unidad que
        // entró al inventario, sin ningún redondeo -- contra "valor de
        // inventario implícito" -- cost_cents (redondeado) x stock_qty,
        // que es el número que el sistema realmente persiste y que ella ve.
        var stockQty = 10
        var costCents = 4000L
        var totalMoneySpent = stockQty * costCents // las 10 unidades iniciales, costo real conocido

        // Compra 1: 10 u. a Q40 + 5 u. a Q55 -- el ejemplo canónico de
        // D-029, división exacta. No introduce sesgo todavía.
        costCents = PricingCalculator.weightedAverageCost(
            currentStockQty = stockQty,
            currentCost = Money(costCents),
            purchaseQty = 5,
            lineTotalCost = Money(5 * 5500),
        ).cents
        stockQty += 5
        totalMoneySpent += 5 * 5500
        assertThat(costCents).isEqualTo(4500L)
        assertThat(costCents * stockQty - totalMoneySpent).isEqualTo(0L)

        // Compra 2: 4 u. a Q33.33 -- (67500 + 13332) / 19 = 4255.26...,
        // redondea a 4255. El costo redondeado se aplica a las 19
        // unidades que quedan en stock, no solo a las 4 compradas: el
        // sesgo que introduce esta única compra es de 13 centavos
        // (4255 x 19 = 80845, contra 80832 realmente gastado), no de 1.
        costCents = PricingCalculator.weightedAverageCost(
            currentStockQty = stockQty,
            currentCost = Money(costCents),
            purchaseQty = 4,
            lineTotalCost = Money(4 * 3333),
        ).cents
        stockQty += 4
        totalMoneySpent += 4 * 3333
        assertThat(costCents).isEqualTo(4255L)
        assertThat(costCents * stockQty - totalMoneySpent).isEqualTo(13L)

        // Compra 3: 1 u. a Q42.55 -- división exacta (85100 / 20 = 4255),
        // no agrega sesgo nuevo -- pero tampoco lo compensa: los mismos
        // 13 centavos de la compra 2 siguen ahí, intactos.
        costCents = PricingCalculator.weightedAverageCost(
            currentStockQty = stockQty,
            currentCost = Money(costCents),
            purchaseQty = 1,
            lineTotalCost = Money(4255),
        ).cents
        stockQty += 1
        totalMoneySpent += 4255

        val impliedInventoryValue = costCents * stockQty
        val bias = impliedInventoryValue - totalMoneySpent

        // El sesgo nunca es negativo (redondear hacia arriba nunca
        // subestima el costo, CLAUDE.md 3.4/D-015) y quedó exactamente en
        // los mismos 13 centavos que dejó la compra 2 -- no se autocorrige,
        // pero tampoco crece sin que una nueva división no exacta lo cause.
        assertThat(bias).isAtLeast(0L)
        assertThat(bias).isEqualTo(13L)
    }
}
