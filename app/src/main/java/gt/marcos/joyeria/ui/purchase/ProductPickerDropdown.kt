package gt.marcos.joyeria.ui.purchase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.ProductSummary

/** Selector de pieza para una línea de compra, entre las piezas activas del inventario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerDropdown(
    products: List<ProductSummary>,
    selectedProductId: Long?,
    onProductSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = products.find { it.id == selectedProductId }
    val label = selected?.let { "${it.name} · ${it.uid}" }
        ?: stringResource(R.string.purchase_line_product_placeholder)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.purchase_line_product_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            products.forEach { product ->
                DropdownMenuItem(
                    text = { Text("${product.name} · ${product.uid}") },
                    onClick = {
                        onProductSelected(product.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
