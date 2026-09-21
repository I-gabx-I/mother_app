package gt.marcos.joyeria.ui.credit

import gt.marcos.joyeria.data.repository.SaleAccountStatement
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/** Estado único del estado de cuenta de una clienta (CLAUDE.md sección 5). */
data class AccountStatementUiState(
    val customerId: Long,
    val customerName: String = "",
    val statements: List<SaleAccountStatement> = emptyList(),
    val paymentDialogSaleId: Long? = null,
    val amountText: String = "",
    val method: String = "CASH",
    val isSaving: Boolean = false,
) {
    val totalBalance: Money get() = statements.fold(Money.ZERO) { acc, statement -> acc + statement.balance }

    val paymentDialogSale: SaleAccountStatement?
        get() = statements.find { it.saleId == paymentDialogSaleId }

    /** `null` mientras el texto esté vacío o incompleto (D-030) -- nunca se exige un monto hasta confirmar. */
    val amount: Money? get() = if (amountText.isBlank()) null else parseMoneyToCents(amountText)?.let(::Money)

    // Mismo tratamiento que el descuento/abono inicial de "Vender" (D-030):
    // no se filtra al tipear, se marca incompleto con un mensaje -- el
    // saldo es un número que cambia por venta, ella no lo puede anticipar.
    val amountExceedsBalance: Boolean
        get() {
            val sale = paymentDialogSale ?: return false
            return (amount ?: Money.ZERO) > sale.balance
        }

    val canConfirmPayment: Boolean
        get() {
            val amount = amount ?: return false
            return !isSaving && amount.cents > 0L && !amountExceedsBalance
        }
}
