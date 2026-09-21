package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator

data class SaleLineInput(
    val productId: Long,
    val qty: Int,
)

/**
 * `credit == null` es una venta al contado -- el camino de Fase 06, sin
 * ningún cambio de comportamiento (D-042). `credit != null` es una venta a
 * crédito: nace `PENDING` siempre (FASES.md), con o sin abono inicial.
 */
data class RegisterSaleInput(
    val soldAt: Long,
    val discount: Money,
    val notes: String?,
    val lines: List<SaleLineInput>,
    val credit: CreditSaleDetails? = null,
)

/** `initialPayment` en `null` o `Money.ZERO` significa "sin abono inicial" (FASES.md, Fase 07, punto 5 del pedido). */
data class CreditSaleDetails(
    val customerId: Long,
    val initialPayment: Money?,
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
    val type: String = "CASH",
    val status: String = "PAID",
) {
    /** Lo que ella cobró de verdad por esta venta (total menos el descuento). */
    val net: Money get() = total - discount

    // D-042: sin esto, una venta a crédito recién hecha se ve en "Ventas de
    // hoy" igual que una de contado ya cobrada -- ella podría creer que
    // tiene un dinero que en realidad no cobró.
    val isPendingCredit: Boolean get() = type == "CREDIT" && status == "PENDING"
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
    val type: String = "CASH",
    val status: String = "PAID",
    val hasPayments: Boolean = false,
) {
    val isPendingCredit: Boolean get() = type == "CREDIT" && status == "PENDING"

    // D-043: una venta con abonos ya registrados no se puede anular sin
    // dejar esos abonos huérfanos -- "Ventas de hoy" oculta el botón en vez
    // de esperar a que el repositorio la rechace.
    val canCancel: Boolean get() = status != "CANCELLED" && !hasPayments
}

data class RegisterPaymentInput(
    val saleId: Long,
    val amount: Money,
    val paidAt: Long,
    val method: String,
    val notes: String?,
)

data class RegisterPaymentResult(
    val paymentId: Long,
    val newBalance: Money,
    val saleNowPaid: Boolean,
)

/** Un abono ya registrado, tal como lo ve el estado de cuenta de una venta. */
data class PaymentSummary(
    val id: Long,
    val paidAt: Long,
    val amount: Money,
    val method: String,
    val notes: String?,
)

/** Estado de cuenta de una sola venta a crédito: total, abonos y saldo (FASES.md, Fase 07). */
data class SaleAccountStatement(
    val saleId: Long,
    val soldAt: Long,
    val total: Money,
    val discount: Money,
    val status: String,
    val payments: List<PaymentSummary>,
) {
    val balance: Money
        get() = PricingCalculator.saleBalance(
            total,
            discount,
            payments.fold(Money.ZERO) { acc, payment -> acc + payment.amount },
        )
}

/**
 * Lo que le debe una clienta en total, para "¿Quién me debe?" (D-038):
 * la suma de `saleBalance` de todas sus ventas `PENDING`, y la fecha de la
 * más vieja de ellas (para distinguir una deuda vieja de una reciente).
 * Toda fila que aparece acá tiene `totalBalance > Money.ZERO` por
 * construcción -- una venta con saldo cero ya pasó a `PAID` y deja de
 * contar (ver `SaleRepositoryTest.observeCustomerDebts_everyRowHasPositiveBalance`).
 */
data class CustomerDebtSummary(
    val customerId: Long,
    val customerName: String,
    val customerPhone: String?,
    val totalBalance: Money,
    val oldestPendingSaleAt: Long,
)
