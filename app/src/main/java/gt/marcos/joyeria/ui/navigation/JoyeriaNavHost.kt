package gt.marcos.joyeria.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gt.marcos.joyeria.ui.product.add.AddProductRoute
import gt.marcos.joyeria.ui.product.edit.ProductEditRoute
import gt.marcos.joyeria.ui.product.list.ProductListRoute
import gt.marcos.joyeria.ui.purchase.RegisterPurchaseRoute

/**
 * Único `NavHost` de la app (Fase 04, D-023). Cada pantalla nueva de
 * fases futuras se agrega acá como un destino más.
 */
@Composable
fun JoyeriaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = JoyeriaDestination.Home.route) {
        composable(JoyeriaDestination.Home.route) {
            HomeRoute(
                onAddProductClick = { navController.navigate(JoyeriaDestination.AddProduct.route) },
                onInventoryClick = { navController.navigate(JoyeriaDestination.ProductList.route) },
                onRegisterPurchaseClick = { navController.navigate(JoyeriaDestination.RegisterPurchase.route) },
            )
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
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = JoyeriaDestination.ProductEdit.route,
            arguments = listOf(navArgument(JoyeriaDestination.ProductEdit.ARG_PRODUCT_ID) { type = NavType.LongType }),
        ) {
            ProductEditRoute(onBackClick = { navController.popBackStack() })
        }
    }
}
