package gt.marcos.joyeria.ui.product.add

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.Category

/**
 * Selector de categoría del alta rápida: chips de un solo toque, sin
 * opción "sin categoría" -- acá la categoría es obligatoria (D-026), a
 * diferencia de `CategoryDropdown`, que sigue usando la pantalla de
 * edición y sí la permite. Altura mínima 56dp y texto en `bodyLarge`
 * (18sp, CLAUDE.md sección 6) en vez del tamaño de chip estándar de
 * Material3, que es más chico que el mínimo que exige el proyecto.
 */
@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.add_product_category_label),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategorySelected(category.id) },
                    label = {
                        Text(category.name, style = MaterialTheme.typography.bodyLarge)
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
    }
}
