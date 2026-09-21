package gt.marcos.joyeria.ui.navigation

/**
 * Rutas del `NavHost` (Fase 04, D-023). `Home` es provisoria (D-024):
 * se reemplaza en Fase 06 cuando "Vender" exista de verdad.
 */
sealed class JoyeriaDestination(val route: String) {
    data object Home : JoyeriaDestination("home")
    data object AddProduct : JoyeriaDestination("add_product")
    data object ProductList : JoyeriaDestination("product_list")
    data object ProductEdit : JoyeriaDestination("product_edit/{productId}") {
        const val ARG_PRODUCT_ID = "productId"
        fun createRoute(productId: Long) = "product_edit/$productId"
    }
    data object RegisterPurchase : JoyeriaDestination("register_purchase")
    data object RegisterSale : JoyeriaDestination("register_sale")
    data object TodaySales : JoyeriaDestination("today_sales")
    data object Customers : JoyeriaDestination("customers")
    data object AccountStatement : JoyeriaDestination("account_statement/{customerId}") {
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(customerId: Long) = "account_statement/$customerId"
    }
}
