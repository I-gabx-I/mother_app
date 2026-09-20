package gt.marcos.joyeria.ui.sale

import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.data.repository.RegisterSaleResult
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/**
 * Una línea del carrito. `unitPrice`/`unitCost`/`availableStock` son una
 * **vista previa** tomada del catálogo en el momento de agregar la pieza
 * -- el snapshot real que se persiste en `sale_item` se toma de nuevo,
 * fresco, dentro de la transacción de `SaleRepository.register()`
 * (ver ESTADO.md "Fase 06 -- Plan", punto 1); si algo cambiara entre
 * medio, lo que se guarda es lo real, no esta vista previa.
 */
data class SaleLineUiState(
    val productId: Long,
    val productName: String,
    val productUid: String,
    val unitPrice: Money,
    val unitCost: Money,
    val availableStock: Int,
    val qtyText: String = "1",
) {
    val qty: Int? get() = qtyText.toIntOrNull()

    // Cantidad vacía o 0: incompleta, sin mensaje (mismo criterio que un
    // campo de dinero vacío). Cantidad > stock: incompleta CON mensaje
    // (ver ESTADO.md punto 2) -- distinción a propósito, no un descuido.
    val exceedsStock: Boolean get() = (qty ?: 0) > availableStock
    val isValid: Boolean get() = qty != null && qty!! in 1..availableStock

    val subtotal: Money get() = unitPrice * (qty ?: 0)
    val totalCost: Money get() = unitCost * (qty ?: 0)
}

/** Estado único de la pantalla "Vender" (CLAUDE.md sección 5). */
data class RegisterSaleUiState(
    val products: List<ProductSummary> = emptyList(),
    val query: String = "",
    val lines: List<SaleLineUiState> = emptyList(),
    val discountText: String = "",
    val isSaving: Boolean = false,
    val pendingLossConfirmation: Boolean = false,
    val result: RegisterSaleResult? = null,
) {
    /** Solo piezas con stock: vender algo agotado no tiene sentido (punto 3 del plan). */
    val availableProducts: List<ProductSummary>
        get() = products.filter { it.stockQty > 0 }.let { active ->
            if (query.isBlank()) {
                active
            } else {
                active.filter { it.name.contains(query, ignoreCase = true) || it.uid.contains(query, ignoreCase = true) }
            }
        }

    val subtotal: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.subtotal }
    val totalCost: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.totalCost }

    /** `null` si el texto de descuento no está vacío pero tampoco es un monto completo (D-030). */
    val discount: Money?
        get() = if (discountText.isBlank()) Money.ZERO else parseMoneyToCents(discountText)?.let(::Money)

    // Igual que exceedsStock de una línea: no se filtra al tipear, se
    // marca incompleto con un mensaje (el subtotal es un número que
    // cambia con el carrito, ella no lo puede anticipar).
    val discountExceedsSubtotal: Boolean get() = (discount ?: Money.ZERO) > subtotal

    /** `null` mientras el descuento no sea un monto completo o exceda el subtotal. */
    val profit: Money?
        get() {
            val discount = discount ?: return null
            if (discountExceedsSubtotal) return null
            return PricingCalculator.saleProfit(subtotal, discount, totalCost)
        }

    val canSave: Boolean
        get() = !isSaving &&
            lines.isNotEmpty() &&
            lines.all { it.isValid } &&
            profit != null
}
