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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Pantalla de inicio **provisoria** (D-024): las dos acciones que
 * existen hoy, "Agregar pieza" e "Inventario". CLAUDE.md sección 6 pide
 * "Vender" y "Agregar pieza" -- esta pantalla se reemplaza en Fase 06,
 * cuando "Vender" exista de verdad. Stateless (no tiene estado propio
 * que gestionar todavía).
 */
@Composable
fun HomeScreen(onAddProductClick: () -> Unit, onInventoryClick: () -> Unit, modifier: Modifier = Modifier) {
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
                onClick = onAddProductClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_add_product), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onInventoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(top = 16.dp),
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.home_inventory), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun HomeRoute(onAddProductClick: () -> Unit, onInventoryClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeScreen(onAddProductClick = onAddProductClick, onInventoryClick = onInventoryClick, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    JoyeriaTheme {
        HomeScreen(onAddProductClick = {}, onInventoryClick = {})
    }
}
