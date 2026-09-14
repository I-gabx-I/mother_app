package gt.marcos.joyeria.domain.pricing

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.model.Money
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
}
