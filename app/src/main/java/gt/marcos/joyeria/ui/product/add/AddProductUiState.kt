package gt.marcos.joyeria.ui.product.add

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator

/**
 * Estado único de la pantalla de alta rápida (CLAUDE.md sección 5: los
 * ViewModels exponen un único `StateFlow<UiState>`). Los tres campos
 * obligatorios son `photoPath`, `costDigits` y `salePriceDigits` — el
 * resto tiene su valor por defecto ya aplicado o se resuelve al guardar.
 */
data class AddProductUiState(
    val photoPath: String? = null,
    val costDigits: String = "",
    val salePriceDigits: String = "",
    val salePriceManuallyEdited: Boolean = false,
    val name: String = "",
    val categoryId: Long? = null,
    val quantityText: String = "",
    val notes: String = "",
    val defaultMarkupBp: Int? = null,
    val roundingStep: Money? = null,
    val isSaving: Boolean = false,
    val savedUid: String? = null,
    val cameraPermissionDeniedMessage: String? = null,
) {
    val cost: Money get() = Money(digitsToCents(costDigits))
    val salePrice: Money get() = Money(digitsToCents(salePriceDigits))
    val stockQty: Int get() = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    /** `null` mientras no haya costo o todavía no se cargó la configuración de precios. */
    val suggestedPrice: Money?
        get() {
            if (costDigits.isEmpty()) return null
            val bp = defaultMarkupBp ?: return null
            val step = roundingStep ?: return null
            return PricingCalculator.suggestedPrice(cost, bp, step)
        }

    /** `null` mientras falte costo o precio: no hay ganancia que mostrar todavía. */
    val profit: Money?
        get() {
            if (costDigits.isEmpty() || salePriceDigits.isEmpty()) return null
            return PricingCalculator.profit(cost, salePrice)
        }

    /** Los tres campos obligatorios de FASES.md Fase 03: foto, costo, precio. */
    val canSave: Boolean
        get() = photoPath != null && costDigits.isNotEmpty() && salePriceDigits.isNotEmpty() && !isSaving
}
