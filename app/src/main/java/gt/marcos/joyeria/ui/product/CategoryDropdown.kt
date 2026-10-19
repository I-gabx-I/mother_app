package gt.marcos.joyeria.ui.product

import androidx.compose.foundation.layout.fillMaxWidth
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
import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.ui.theme.JoyeriaDropdownMenuItem

/**
 * Selector de categoría, compartido entre alta rápida (Fase 03/04) y
 * detalle/edición (Fase 04). "Sin categoría" siempre está disponible
 * como primera opción -- `category_id` es `Long?` en `ESQUEMA.md`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedCategoryId }
    val selectedName = selected?.let { it.displayName() } ?: stringResource(R.string.add_product_category_none)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_product_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            JoyeriaDropdownMenuItem(
                text = { Text(stringResource(R.string.add_product_category_none)) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                JoyeriaDropdownMenuItem(
                    text = { Text(category.displayName()) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * "Pulseras (archivada)" para una categoría archivada que sigue siendo
 * seleccionable (Fase 09, Opción A: `CategoryRepository.observeSelectable`)
 * -- ella entiende por qué una pieza vieja todavía la muestra, en vez de
 * ver un nombre suelto sin ninguna marca.
 */
@Composable
internal fun Category.displayName(): String =
    if (archived) stringResource(R.string.category_archived_suffix, name) else name
