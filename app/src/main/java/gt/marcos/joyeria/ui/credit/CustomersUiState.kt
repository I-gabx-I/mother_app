package gt.marcos.joyeria.ui.credit

import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.CustomerDebtSummary
import gt.marcos.joyeria.domain.model.Money

/** Pestaña seleccionada de la pantalla "Clientes" (D-038). */
enum class CustomersTab { WHO_OWES_ME, ALL_CUSTOMERS }

/** El archivado se bloqueó por saldo pendiente (D-039) -- monto y cantidad de ventas, para el mensaje claro. */
data class ArchiveBlockedInfo(val pendingCents: Long, val pendingSaleCount: Int)

/** Estado único de la pantalla "Clientes" (CLAUDE.md sección 5). */
data class CustomersUiState(
    val tab: CustomersTab = CustomersTab.WHO_OWES_ME,
    val debts: List<CustomerDebtSummary> = emptyList(),
    val allCustomers: List<Customer> = emptyList(),
    val showFormDialog: Boolean = false,
    val editingCustomerId: Long? = null,
    val formName: String = "",
    val formPhone: String = "",
    val formNotes: String = "",
    val archiveTarget: Customer? = null,
    val archiveBlocked: ArchiveBlockedInfo? = null,
) {
    val totalDebt: Money get() = debts.fold(Money.ZERO) { acc, debt -> acc + debt.totalBalance }
    val canSaveForm: Boolean get() = formName.isNotBlank()
}
