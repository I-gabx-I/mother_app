package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator

/**
 * `unitPrice` es el precio que la usuaria tecleó para **esta** línea, no el
 * precio de lista del producto (D-045). Llega precargado con el precio de
 * lista y ella lo puede mover hacia arriba o hacia abajo: a veces vende más
 * caro que la lista, y un descuento solo podía restar.
 */
data class SaleLineInput(
    val productId: Long,
    val qty: Int,
    val unitPrice: Money,
)

/**
 * `credit == null` es una venta al contado -- el camino de Fase 06, sin
 * ningún cambio de comportamiento (D-042). `credit != null` es una venta a
 * crédito: nace `PENDING` siempre (FASES.md), con o sin abono inicial.
 */
data class RegisterSaleInput(
    val soldAt: Long,
    /**
     * **Legado (D-047): las ventas nuevas mandan siempre `Money.ZERO`.** El
     * descuento general se eliminó de la app y lo reemplazó el precio
     * editable por línea. El parámetro y la columna se conservan porque las
     * ventas viejas tienen descuentos reales y sus fórmulas de ganancia y
     * saldo los siguen usando -- ver la advertencia de `discount_cents` en
     * `ESQUEMA.md`.
     */
    val discount: Money,
    val notes: String?,
    val lines: List<SaleLineInput>,
    val credit: CreditSaleDetails? = null,
    /**
     * Clienta de una venta **de contado** (Fase 08): la venta queda
     * asociada a ella y aparece en su perfil, pero sigue naciendo
     * `CASH`/`PAID` y **no** es una deuda.
     *
     * `null` es el caso común -- vender de contado a quien pasa por ahí --
     * y es exactamente el comportamiento de Fase 06, sin cambios.
     *
     * En una venta a crédito este campo se ignora: la clienta sale de
     * `credit.customerId`, que es la única que puede deber plata. No hay
     * forma de pedir una venta a crédito "a otra clienta" que la que la
     * debe.
     */
    val customerId: Long? = null,
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

/**
 * El historial de compras de una clienta, tal como lo ve su perfil
 * (Fase 08).
 *
 * `mayBeMissingCashSales` (B-10) es `true` cuando esta clienta **pudo**
 * haber comprado de contado en la época en que la app no guardaba a quién
 * se le vendía. No afirma que falte algo: afirma que no se puede saber. La
 * pantalla solo avisa cuando esto es `true` -- a una clienta cuyo
 * historial entero es posterior no se le muestra nada, para no crearle una
 * duda a quien no la tenía.
 */
data class CustomerPurchaseHistory(
    val sales: List<SaleSummary> = emptyList(),
    val mayBeMissingCashSales: Boolean = false,
) {
    /** Las anuladas no suman a ningún total (B-06): se ven, no se cuentan. */
    val activeSales: List<SaleSummary> get() = sales.filter { it.status != "CANCELLED" }

    val totalPurchased: Money get() = activeSales.fold(Money.ZERO) { acc, sale -> acc + sale.net }
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
