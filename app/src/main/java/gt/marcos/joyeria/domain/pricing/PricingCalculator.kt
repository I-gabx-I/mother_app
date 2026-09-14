package gt.marcos.joyeria.domain.pricing

import gt.marcos.joyeria.domain.model.Money

/**
 * Aritmética del negocio: ganancia, margen, recargo y precio sugerido.
 * Pura, sin Android, sin Room, sin UI (CLAUDE.md sección 5 / FASES.md Fase 02).
 *
 * Convenciones:
 * - Los porcentajes (marginOnSale, markupOnCost) son Int en puntos básicos,
 *   nunca Double (D-013): 1 punto básico = 0.01%, `valor / 100` = el
 *   porcentaje con dos decimales. 6000 = 60.00%, 15000 = 150.00%.
 * - markupOnCost y suggestedPrice usan la misma fórmula de recargo sobre
 *   costo (D-014): son operaciones inversas entre sí.
 * - marginOnSale y markupOnCost devuelven Int? : null cuando la división no
 *   está definida (salePrice o cost en cero), NUNCA 0 como centinela — un
 *   margen o recargo real de 0% (costo == precio) es un resultado legítimo
 *   y tiene que poder distinguirse de "no se pudo calcular" (D-015).
 */
object PricingCalculator {

    /** Ganancia unitaria = precioVenta - costo. Nunca divide, nunca es null. */
    fun profit(cost: Money, salePrice: Money): Money =
        salePrice - cost

    /**
     * Margen sobre venta, en puntos básicos: gananciaUnitaria * 10000 / salePriceCents.
     * `null` cuando salePrice.cents == 0: no hay precio de venta con el cual
     * expresar un porcentaje (D-015). No es lo mismo que un margen real de 0%.
     */
    fun marginOnSale(cost: Money, salePrice: Money): Int? {
        if (salePrice.cents == 0L) return null
        return (profit(cost, salePrice).cents * 10_000L / salePrice.cents).toInt()
    }

    /**
     * Recargo sobre costo, en puntos básicos: gananciaUnitaria * 10000 / costCents.
     * `null` cuando cost.cents == 0: no hay costo con el cual expresar un
     * recargo (D-015). No es lo mismo que un recargo real de 0%.
     */
    fun markupOnCost(cost: Money, salePrice: Money): Int? {
        if (cost.cents == 0L) return null
        return (profit(cost, salePrice).cents * 10_000L / cost.cents).toInt()
    }

    /**
     * Precio sugerido = costo + costo * markupBp / 10000 (D-014), la misma
     * fórmula que markupOnCost, luego redondeado hacia arriba al múltiplo de
     * roundingStep más cercano (CLAUDE.md 3.4).
     *
     * `markupBp` es la misma unidad y el mismo significado que devuelve
     * markupOnCost, y la que usa `default_markup_bp` de app_setting.
     */
    fun suggestedPrice(cost: Money, markupBp: Int, roundingStep: Money): Money {
        val raw = cost.cents + (cost.cents * markupBp / 10_000L)
        return Money(roundUpToMultiple(raw, roundingStep.cents))
    }

    /**
     * Redondea `value` hacia arriba al múltiplo de `step` más cercano
     * (el más chico que sea >= value), para cualquier signo de `value`.
     *
     * Implementación: el múltiplo de `step` más grande que no supera a
     * `value` es `value.floorDiv(step) * step` (división que redondea hacia
     * menos infinito, no hacia cero — por eso funciona igual de bien para
     * negativos que para positivos). Si ese múltiplo ya es exactamente
     * `value`, no hay nada que redondear; si no, el siguiente múltiplo hacia
     * arriba es ese resultado más un `step`. Ejemplo con valor negativo:
     * -4100 con paso 500 -> floorDiv da -9 (no -8, porque floorDiv redondea
     * hacia abajo), múltiplo base -4500, como -4500 != -4100 se suma el
     * paso: -4000. Es el múltiplo superior correcto, no -3500.
     *
     * Si `step` es cero o negativo, devuelve `value` sin tocar:
     * `roundingStep` en producción sale de `price_rounding_step_cents` en
     * `app_setting`, un String genérico que se parsea a entero (D-010); si
     * ese valor llegara corrupto o mal configurado, preferimos un precio sin
     * redondear antes que dividir entre cero o crashear en medio del alta
     * rápida de pieza (D-015, CLAUDE.md sección 6: máximo 3 taps y 20
     * segundos).
     */
    private fun roundUpToMultiple(value: Long, step: Long): Long {
        if (step <= 0L) return value
        val floorMultiple = value.floorDiv(step) * step
        return if (floorMultiple == value) value else floorMultiple + step
    }
}
