package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money

data class SaleLineInput(
    val productId: Long,
    val qty: Int,
)

data class RegisterSaleInput(
    val soldAt: Long,
    val discount: Money,
    val notes: String?,
    val lines: List<SaleLineInput>,
)

data class RegisterSaleResult(
    val saleId: Long,
)

/** Una venta ya registrada, tal como la ve "Ventas de hoy" -- nunca incluye CANCELLED (D-006). */
data class SaleSummary(
    val id: Long,
    val soldAt: Long,
    val total: Money,
    val discount: Money,
    val profit: Money,
) {
    /** Lo que ella cobró de verdad por esta venta (total menos el descuento). */
    val net: Money get() = total - discount
}

data class SaleItemDetail(
    val productUidSnapshot: String,
    val productNameSnapshot: String,
    val qty: Int,
    val unitPrice: Money,
    val unitCost: Money,
)

data class SaleDetail(
    val id: Long,
    val soldAt: Long,
    val total: Money,
    val discount: Money,
    val profit: Money,
    val items: List<SaleItemDetail>,
)
