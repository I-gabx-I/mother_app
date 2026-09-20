package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money

data class PurchaseLineInput(
    val productId: Long,
    val qty: Int,
    val unitCost: Money,
)

data class RegisterPurchaseInput(
    val purchasedAt: Long,
    val supplier: String?,
    val extraCost: Money,
    val notes: String?,
    val lines: List<PurchaseLineInput>,
)

/** D-031: dos niveles de aviso, no uno solo. Ninguno cambia `sale_price_cents`. */
enum class PurchaseWarningLevel {
    NONE,

    /** margen sobre venta por debajo de `min_margin_bp`. */
    MARGIN_WARNING,

    /** ganancia unitaria cero o negativa al precio de venta actual -- más grave que MARGIN_WARNING. */
    PROFIT_ALERT,
}

data class PurchaseLineWarning(
    val productId: Long,
    val productName: String,
    val newCost: Money,
    val level: PurchaseWarningLevel,
)

data class RegisterPurchaseResult(
    val purchaseId: Long,
    val lineWarnings: List<PurchaseLineWarning>,
    // D-032: true si purchasedAt es anterior a la última venta no CANCELLED
    // de algún producto de esta compra. Informativo -- no bloquea nada.
    val retroactiveWarning: Boolean,
)
