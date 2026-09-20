package gt.marcos.joyeria.ui.purchase

import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.data.repository.RegisterPurchaseResult
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/** Una línea del formulario, tal como ella la está tecleando (D-030: texto plano, sin transformar). */
data class PurchaseLineUiState(
    val productId: Long? = null,
    val qtyText: String = "1",
    val unitCostText: String = "",
) {
    val qty: Int? get() = qtyText.toIntOrNull()?.takeIf { it > 0 }
    val unitCost: Money? get() = parseMoneyToCents(unitCostText)?.let(::Money)
    val isComplete: Boolean get() = productId != null && qty != null && unitCost != null
}

/** Estado único de la pantalla de registro de compra (CLAUDE.md sección 5). */
data class RegisterPurchaseUiState(
    val products: List<ProductSummary> = emptyList(),
    val lines: List<PurchaseLineUiState> = listOf(PurchaseLineUiState()),
    val extraCostText: String = "",
    val supplier: String = "",
    val purchasedAtMillis: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val result: RegisterPurchaseResult? = null,
) {
    /** `null` si el texto de transporte no está vacío pero tampoco es un monto completo. */
    val extraCost: Money?
        get() = if (extraCostText.isBlank()) Money.ZERO else parseMoneyToCents(extraCostText)?.let(::Money)

    val canSave: Boolean
        get() = !isSaving && lines.isNotEmpty() && lines.all { it.isComplete } && extraCost != null
}
