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

    /**
     * Prorratea `extraCents` (ej. transporte de una compra, ESQUEMA.md)
     * entre `lines` (cantidad, costo unitario), proporcional al valor de
     * cada línea (`qty * unitCost`). El residuo de redondeo se asigna a la
     * línea de **mayor valor**, para que la suma del resultado sea
     * exactamente igual a `extraCents` -- nunca queda un centavo perdido
     * ni de más (Fase 05, criterio 2, `[TESTS OBLIGATORIOS]`).
     *
     * Si el valor total de las líneas es cero (ninguna compra real tiene
     * unitCost/qty en cero, pero esta función no lo asume), todo el
     * extra se asigna a la primera línea -- resultado explícito, sin
     * dividir entre cero.
     */
    fun allocateExtraCost(lines: List<Pair<Int, Money>>, extraCents: Long): List<Long> {
        if (lines.isEmpty()) return emptyList()
        val lineValues = lines.map { (qty, unitCost) -> qty.toLong() * unitCost.cents }
        val totalValue = lineValues.sum()
        if (extraCents == 0L) return List(lines.size) { 0L }
        if (totalValue == 0L) {
            return List(lines.size) { index -> if (index == 0) extraCents else 0L }
        }

        val allocated = lineValues.map { value -> extraCents * value / totalValue }.toMutableList()
        val remainder = extraCents - allocated.sum()
        val highestValueIndex = lineValues.indices.maxBy { lineValues[it] }
        allocated[highestValueIndex] = allocated[highestValueIndex] + remainder
        return allocated
    }

    /**
     * Costo nuevo tras una compra, regla híbrida de D-029: promedio
     * ponderado por cantidad entre el stock existente y la compra nueva.
     * `lineTotalCost` ya es el costo real total de la línea (`qty *
     * unitCost + extra prorrateado`), sin dividir nada todavía -- dividir
     * una sola vez al final evita acumular redondeo en dos pasos.
     *
     * Con `currentStockQty = 0` esta misma fórmula da exactamente
     * `lineTotalCost / purchaseQty` sin ninguna rama de código aparte
     * (D-029, "Nota de implementación") -- fase 05, criterio 4,
     * `[TESTS OBLIGATORIOS]`, lo prueba explícitamente igual.
     *
     * Redondea hacia **arriba** (`ceilDiv`) cuando la división no es
     * exacta: mismo criterio conservador que `suggestedPrice` (CLAUDE.md
     * 3.4, D-015) -- un centavo de más en el costo nunca hace que se
     * muestre más ganancia de la real, uno de menos sí podría.
     *
     * **Limitación conocida del redondeo hacia arriba (documentada en
     * D-029, no una sorpresa):** el centavo de más que puede sumar
     * `ceilDiv` es un costo **por unidad**, y ese costo por unidad se
     * multiplica por **todo** el stock que queda tras la compra, no solo
     * por las unidades recién compradas. Con 15 unidades en stock,
     * redondear el costo unitario un centavo hacia arriba infla el
     * "costo total" implícito (`cost_cents * stock_qty`) en **15**
     * centavos, no en uno. Además el sesgo no se autocorrige: como
     * siempre redondea en la misma dirección, cada compra que no divide
     * exacto lo puede agrandar, nunca lo compensa, y ese costo ya
     * "inflado" pasa a ser el `currentCost` de la siguiente compra --
     * el único evento que lo resetea a cero es que `stock_qty` vuelva a
     * `0` (reemplazo directo, D-029). Se mantiene la decisión porque el
     * sesgo va siempre hacia el lado conservador (nunca hace que se
     * muestre más ganancia de la real, sección 3 arriba) y en la
     * práctica son centavos por año -- pero **`valorInventario`**
     * (`ESQUEMA.md`, "Cálculos derivados") queda por la misma razón
     * levemente sobreestimado, no es un número exacto al centavo.
     * Medido con un test dedicado
     * (`weightedAverageCost_repeatedPurchases_accumulateABoundedRoundingBias`).
     *
     * `currentStockQty + purchaseQty` tiene que ser mayor que cero -- si
     * no hay ninguna unidad sobre la que promediar un costo, es un error
     * de quien llama (D-015 prohíbe devolver un centinela silencioso
     * como `Money.ZERO` acá: un costo de Q0.00 es un valor creíble que
     * se **persiste** en `product.cost_cents`, y de ahí en adelante
     * `markupOnCost` empieza a devolver `null`, el valor de inventario
     * queda mal, y la ganancia se calcula sobre un costo inventado sin
     * que nada lo señale). `purchaseQty` siempre es `>= 1` viniendo de
     * la pantalla de compra (`PurchaseLineUiState.isComplete`) y
     * `currentStockQty` nunca es negativo (`ESQUEMA.md`), así que este
     * `require` no debería dispararse nunca con datos reales -- si se
     * dispara, es una señal de que algo llamó a esta función con datos
     * que ya estaban mal, y hay que enterarse en desarrollo, no guardar
     * un cero en producción.
     */
    fun weightedAverageCost(currentStockQty: Int, currentCost: Money, purchaseQty: Int, lineTotalCost: Money): Money {
        val totalQty = currentStockQty.toLong() + purchaseQty.toLong()
        require(totalQty > 0L) {
            "weightedAverageCost: currentStockQty ($currentStockQty) + purchaseQty ($purchaseQty) " +
                "tiene que ser mayor que cero -- no hay ninguna unidad sobre la que promediar un costo."
        }
        val totalActual = currentStockQty.toLong() * currentCost.cents
        val totalNuevo = totalActual + lineTotalCost.cents
        return Money(ceilDiv(totalNuevo, totalQty))
    }

    /**
     * Redondeo hacia arriba para enteros no negativos. `denominator` tiene
     * que ser positivo -- ver D-015/el comentario de `weightedAverageCost`
     * sobre por qué esto falla ruidosamente en vez de devolver `0`.
     */
    private fun ceilDiv(numerator: Long, denominator: Long): Long {
        require(denominator > 0L) { "ceilDiv: denominator ($denominator) tiene que ser positivo." }
        return (numerator + denominator - 1) / denominator
    }
}
