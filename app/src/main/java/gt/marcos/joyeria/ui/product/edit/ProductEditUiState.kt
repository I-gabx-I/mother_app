package gt.marcos.joyeria.ui.product.edit

import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.ui.product.add.digitsToCents

/** Estado único de la pantalla de detalle/edición (CLAUDE.md sección 5). */
data class ProductEditUiState(
    val productId: Long,
    val uid: String = "",
    val name: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val costDigits: String = "",
    val salePriceDigits: String = "",
    val quantityText: String = "",
    val supplier: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedConfirmationVisible: Boolean = false,
    val showArchiveConfirmation: Boolean = false,
    val archived: Boolean = false,
) {
    val cost: Money get() = Money(digitsToCents(costDigits))
    val salePrice: Money get() = Money(digitsToCents(salePriceDigits))
    val stockQty: Int get() = quantityText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    // profit() nunca es null: es una resta, no divide (D-015, Fase 02).
    val profit: Money get() = PricingCalculator.profit(cost, salePrice)

    val canSave: Boolean get() = !isLoading && !isSaving && name.isNotBlank()
}
