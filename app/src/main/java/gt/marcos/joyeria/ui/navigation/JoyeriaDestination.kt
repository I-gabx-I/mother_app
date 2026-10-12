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
    /** Administración de categorías (Fase 09), desde el menú de Inventario. */
    data object Categories : JoyeriaDestination("categories")
    /**
     * `customerId` es opcional (Fase 08): sin él, "Vender" se comporta
     * exactamente igual que en Fase 06/07. Con él, la venta llega con esa
     * clienta ya elegida -- se entra desde su perfil.
     *
     * Viaja como `String` y no como `Long` a propósito: `NavType.LongType`
     * no admite `null` en Navigation Compose, y "sin clienta" tiene que ser
     * un estado representable. El `ViewModel` lo convierte con
     * `toLongOrNull()`, así que un valor corrupto se comporta como "sin
     * clienta" en vez de tirar la app.
     */
    data object RegisterSale : JoyeriaDestination("register_sale?customerId={customerId}") {
        const val ARG_CUSTOMER_ID = "customerId"

        /** Sin clienta: el camino de siempre, desde el inicio. */
        fun createRoute(customerId: Long? = null): String =
            if (customerId == null) "register_sale" else "register_sale?customerId=$customerId"
    }
    data object TodaySales : JoyeriaDestination("today_sales")
    data object Customers : JoyeriaDestination("customers")
    data object CustomerProfile : JoyeriaDestination("customer_profile/{customerId}") {
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(customerId: Long) = "customer_profile/$customerId"
    }
    /** Reportes (Fase 11), enlace chico desde el inicio -- no una de las dos acciones grandes (CLAUDE.md sección 6). */
    data object Reports : JoyeriaDestination("reports")
}
