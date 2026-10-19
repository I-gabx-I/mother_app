package gt.marcos.joyeria.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gt.marcos.joyeria.ui.backup.BackupRoute
import gt.marcos.joyeria.ui.barcode.LabelsRoute
import gt.marcos.joyeria.ui.catalog.CatalogRoute
import gt.marcos.joyeria.ui.credit.CustomerProfileRoute
import gt.marcos.joyeria.ui.credit.CustomersRoute
import gt.marcos.joyeria.ui.product.add.AddProductRoute
import gt.marcos.joyeria.ui.product.category.CategoryManageRoute
import gt.marcos.joyeria.ui.product.edit.ProductEditRoute
import gt.marcos.joyeria.ui.product.list.ProductListRoute
import gt.marcos.joyeria.ui.purchase.RegisterPurchaseRoute
import gt.marcos.joyeria.ui.reports.ReportsRoute
import gt.marcos.joyeria.ui.sale.RegisterSaleRoute
import gt.marcos.joyeria.ui.sale.TodaySalesRoute

/**
 * Único `NavHost` de la app (Fase 04, D-023). Cada pantalla nueva de
 * fases futuras se agrega acá como un destino más.
 */
@Composable
fun JoyeriaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = JoyeriaDestination.Home.route) {
        composable(JoyeriaDestination.Home.route) {
            HomeRoute(
                onSellClick = { navController.navigate(JoyeriaDestination.RegisterSale.createRoute()) },
                onAddProductClick = { navController.navigate(JoyeriaDestination.AddProduct.route) },
                onInventoryClick = { navController.navigate(JoyeriaDestination.ProductList.route) },
                onRegisterPurchaseClick = { navController.navigate(JoyeriaDestination.RegisterPurchase.route) },
                onTodaySalesClick = { navController.navigate(JoyeriaDestination.TodaySales.route) },
                onCustomersClick = { navController.navigate(JoyeriaDestination.Customers.route) },
                onReportsClick = { navController.navigate(JoyeriaDestination.Reports.route) },
                onCatalogClick = { navController.navigate(JoyeriaDestination.Catalog.route) },
                onBackupClick = { navController.navigate(JoyeriaDestination.Backup.route) },
            )
        }
        composable(JoyeriaDestination.Backup.route) {
            BackupRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.Labels.route) {
            LabelsRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.Catalog.route) {
            CatalogRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.Reports.route) {
            ReportsRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.Customers.route) {
            CustomersRoute(
                // Las dos pestañas llevan al mismo perfil (Fase 08). Antes
                // "Todas las clientas" abría el diálogo de editar y una
                // clienta sin deuda no tenía a dónde entrar.
                onCustomerClick = { customerId ->
                    navController.navigate(JoyeriaDestination.CustomerProfile.createRoute(customerId))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = JoyeriaDestination.CustomerProfile.route,
            arguments = listOf(navArgument(JoyeriaDestination.CustomerProfile.ARG_CUSTOMER_ID) { type = NavType.LongType }),
        ) {
            CustomerProfileRoute(
                onSellClick = { customerId ->
                    navController.navigate(JoyeriaDestination.RegisterSale.createRoute(customerId))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = JoyeriaDestination.RegisterSale.route,
            arguments = listOf(
                navArgument(JoyeriaDestination.RegisterSale.ARG_CUSTOMER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            RegisterSaleRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.TodaySales.route) {
            TodaySalesRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.RegisterPurchase.route) {
            RegisterPurchaseRoute(onBackClick = { navController.popBackStack() })
        }
        composable(JoyeriaDestination.AddProduct.route) {
            AddProductRoute()
        }
        composable(JoyeriaDestination.ProductList.route) {
            ProductListRoute(
                onProductClick = { productId ->
                    navController.navigate(JoyeriaDestination.ProductEdit.createRoute(productId))
                },
                onManageCategoriesClick = { navController.navigate(JoyeriaDestination.Categories.route) },
                onLabelsClick = { navController.navigate(JoyeriaDestination.Labels.route) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(JoyeriaDestination.Categories.route) {
            CategoryManageRoute(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = JoyeriaDestination.ProductEdit.route,
            arguments = listOf(navArgument(JoyeriaDestination.ProductEdit.ARG_PRODUCT_ID) { type = NavType.LongType }),
        ) {
            ProductEditRoute(onBackClick = { navController.popBackStack() })
        }
    }
}
