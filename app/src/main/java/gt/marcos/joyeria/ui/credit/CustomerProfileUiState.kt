package gt.marcos.joyeria.ui.credit

import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.CustomerPurchaseHistory
import gt.marcos.joyeria.data.repository.SaleAccountStatement
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/**
 * Estado único del perfil de una clienta (CLAUDE.md sección 5).
 *
 * Fase 08 (B-05): esta pantalla era el estado de cuenta y ahora es el
 * perfil completo. **No se hizo una pantalla nueva a propósito**: dos
 * pantallas mostrando saldos son dos lugares que se desincronizan. El
 * estado de cuenta pasó a ser una sección de acá, con el mismo
 * `totalBalance` de siempre.
 */
data class CustomerProfileUiState(
    val customerId: Long,
    val customer: Customer? = null,
    val statements: List<SaleAccountStatement> = emptyList(),
    val history: CustomerPurchaseHistory = CustomerPurchaseHistory(),
    val paymentDialogSaleId: Long? = null,
    val amountText: String = "",
    val method: String = "CASH",
    val isSaving: Boolean = false,
    // B-09: el mismo diálogo que usa "Clientes", no uno nuevo.
    val showEditDialog: Boolean = false,
    val formName: String = "",
    val formPhone: String = "",
    val formNotes: String = "",
) {
    val customerName: String get() = customer?.name.orEmpty()

    val totalBalance: Money get() = statements.fold(Money.ZERO) { acc, statement -> acc + statement.balance }

    /** Sin deuda no se muestra un "Q0.00": se omite la sección entera. */
    val hasDebt: Boolean get() = totalBalance > Money.ZERO

    val canSaveForm: Boolean get() = formName.isNotBlank()

    val paymentDialogSale: SaleAccountStatement?
        get() = statements.find { it.saleId == paymentDialogSaleId }

    /** `null` mientras el texto esté vacío o incompleto (D-030) -- nunca se exige un monto hasta confirmar. */
    val amount: Money? get() = if (amountText.isBlank()) null else parseMoneyToCents(amountText)?.let(::Money)

    // Mismo tratamiento que cualquier campo de dinero (D-030): no se filtra
    // al tipear, se marca incompleto con un mensaje -- el saldo es un número
    // que cambia por venta, ella no lo puede anticipar.
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
