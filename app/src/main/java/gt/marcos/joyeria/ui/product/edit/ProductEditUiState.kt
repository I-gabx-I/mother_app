package gt.marcos.joyeria.ui.product.edit

import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/** Estado único de la pantalla de detalle/edición (CLAUDE.md sección 5). */
data class ProductEditUiState(
    val productId: Long,
    val uid: String = "",
    val name: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val costText: String = "",
    val salePriceText: String = "",
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
    // `null` mientras el texto esté vacío o incompleto -- ver
    // AddProductUiState.cost, mismo razonamiento (D-030).
    val cost: Money? get() = parseMoneyToCents(costText)?.let(::Money)
    val salePrice: Money? get() = parseMoneyToCents(salePriceText)?.let(::Money)
    val stockQty: Int get() = quantityText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    /** `null` mientras costo o precio estén incompletos: no hay ganancia que mostrar todavía. */
    val profit: Money?
        get() {
            val cost = cost ?: return null
            val salePrice = salePrice ?: return null
            return PricingCalculator.profit(cost, salePrice)
        }

    val canSave: Boolean get() = !isLoading && !isSaving && name.isNotBlank() && cost != null && salePrice != null
}
