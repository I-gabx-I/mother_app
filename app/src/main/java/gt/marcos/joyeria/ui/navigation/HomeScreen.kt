package gt.marcos.joyeria.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Pantalla de inicio definitiva (CLAUDE.md sección 6): dos acciones
 * grandes y obvias -- "Vender" y "Agregar pieza" -- sin ser un
 * dashboard de métricas. D-024 (Fase 04) había registrado una versión
 * provisoria como desviación temporal, con vencimiento explícito en
 * esta fase (D-024, bloque "Cumplida" en `DECISIONES.md`). "Vender" va
 * primero: es la acción que se repite todos los días, agregar piezas
 * nuevas es esporádico. "Inventario", "Ventas de hoy" y "Registrar
 * compra" son enlaces chicos, no compiten con las dos grandes.
 */
@Composable
fun HomeScreen(
    onSellClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRegisterPurchaseClick: () -> Unit,
    onTodaySalesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp),
            )
            Button(
                onClick = onSellClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_sell), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onAddProductClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(top = 16.dp),
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_add_product), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = onInventoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 8.dp),
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_inventory))
            }
            TextButton(
                onClick = onTodaySalesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_today_sales))
            }
            TextButton(
                onClick = onRegisterPurchaseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_register_purchase))
            }
        }
    }
}

@Composable
fun HomeRoute(
    onSellClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRegisterPurchaseClick: () -> Unit,
    onTodaySalesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeScreen(
        onSellClick = onSellClick,
        onAddProductClick = onAddProductClick,
        onInventoryClick = onInventoryClick,
        onRegisterPurchaseClick = onRegisterPurchaseClick,
        onTodaySalesClick = onTodaySalesClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    JoyeriaTheme {
        HomeScreen(
            onSellClick = {},
            onAddProductClick = {},
            onInventoryClick = {},
            onRegisterPurchaseClick = {},
            onTodaySalesClick = {},
        )
    }
}
