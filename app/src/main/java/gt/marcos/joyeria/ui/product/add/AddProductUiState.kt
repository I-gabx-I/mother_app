package gt.marcos.joyeria.ui.product.add

import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/**
 * Estado único de la pantalla de alta rápida (CLAUDE.md sección 5: los
 * ViewModels exponen un único `StateFlow<UiState>`). Los tres campos
 * obligatorios son `photoPath`, `costText` y `salePriceText` — el
 * resto tiene su valor por defecto ya aplicado o se resuelve al guardar.
 */
data class AddProductUiState(
    val photoPath: String? = null,
    val costText: String = "",
    val salePriceText: String = "",
    val salePriceManuallyEdited: Boolean = false,
    val name: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val quantityText: String = "1",
    val notes: String = "",
    val defaultMarkupBp: Int? = null,
    val roundingStep: Money? = null,
    val isSaving: Boolean = false,
    val savedUid: String? = null,
    val cameraPermissionDeniedMessage: String? = null,
) {
    // `null` mientras el texto esté vacío o a mitad de escribir (ej. "20."
    // recién tecleado el separador) -- MoneyTextField ya garantiza que
    // nunca llega acá un texto con letras, dos separadores o un tercer
    // decimal (D-030), así que la única invalidez posible es "incompleto".
    val cost: Money? get() = parseMoneyToCents(costText)?.let(::Money)
    val salePrice: Money? get() = parseMoneyToCents(salePriceText)?.let(::Money)
    val stockQty: Int get() = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    /** `null` mientras no haya costo completo o todavía no se cargó la configuración de precios. */
    val suggestedPrice: Money?
        get() {
            val cost = cost ?: return null
            val bp = defaultMarkupBp ?: return null
            val step = roundingStep ?: return null
            return PricingCalculator.suggestedPrice(cost, bp, step)
        }

    /** `null` mientras falte costo o precio completos: no hay ganancia que mostrar todavía. */
    val profit: Money?
        get() {
            val cost = cost ?: return null
            val salePrice = salePrice ?: return null
            return PricingCalculator.profit(cost, salePrice)
        }

    /**
     * Los cuatro campos obligatorios (D-026, reemplaza a los tres de la
     * Fase 03 original): foto, costo, precio y categoría.
     */
    val canSave: Boolean
        get() = photoPath != null &&
            cost != null &&
            salePrice != null &&
            categoryId != null &&
            !isSaving
}
