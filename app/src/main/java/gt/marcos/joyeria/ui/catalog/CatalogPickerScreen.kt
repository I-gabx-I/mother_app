package gt.marcos.joyeria.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.catalog.CatalogItem
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Paso 1 del catálogo (Fase 12): elegir las piezas. Stateless y
 * `@Preview`-able (CLAUDE.md sección 5). Solo conoce [CatalogPiece], que no
 * tiene costo: esta pantalla no puede mostrar una ganancia aunque alguien
 * quisiera (D-058), a diferencia de la lista de Inventario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogPickerScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onPieceToggled: (Long) -> Unit,
    onSelectAllVisibleClick: () -> Unit,
    onDeselectVisibleClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        bottomBar = {
            // Mismo manejo de insets que el botón de "Vender" (ESTADO.md, "Punto
            // 2 -- insets"): `Scaffold` no le pasa insets al `bottomBar`, y con
            // `safeDrawing` (barras + teclado) un solo modificador cubre los dos.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(16.dp),
            ) {
                Button(
                    onClick = onCreateClick,
                    enabled = state.canCreate,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.catalog_create_button), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.catalog_search_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SelectionBar(
                state = state,
                onSelectAllVisibleClick = onSelectAllVisibleClick,
                onDeselectVisibleClick = onDeselectVisibleClick,
                onDeselectAllClick = onDeselectAllClick,
            )
            when {
                !state.isLoaded -> Unit
                state.pieces.isEmpty() -> EmptyMessage(stringResource(R.string.catalog_empty))
                state.visiblePieces.isEmpty() -> EmptyMessage(stringResource(R.string.catalog_no_results))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Clave con prefijo por sección (CLAUDE.md sección 5), y con
                    // el id de la pieza, no con la posición: cada fila tiene su
                    // propio estado de entrada (el check).
                    items(state.visiblePieces, key = { "catalog-${it.item.productId}" }) { piece ->
                        CatalogPieceRow(
                            piece = piece,
                            selected = piece.item.productId in state.selectedIds,
                            onToggle = { onPieceToggled(piece.item.productId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    state: CatalogUiState,
    onSelectAllVisibleClick: () -> Unit,
    onDeselectVisibleClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
) {
    // Columna y no fila: con letra de 18sp (y más con letra grande del sistema)
    // el contador y el botón no caben lado a lado y el contador se partía en
    // tres líneas.
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = if (state.selectedCount == 0) {
                stringResource(R.string.catalog_selected_none)
            } else {
                pluralStringResource(R.plurals.catalog_selected_count, state.selectedCount, state.selectedCount)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        // «Elegir» y «Quitar» actúan sobre lo mismo: las que se ven. Si ella
        // filtra y quita, no pierde en silencio lo que el buscador esconde.
        if (state.allVisibleSelected) {
            TextButton(onClick = onDeselectVisibleClick) { Text(stringResource(R.string.catalog_deselect_visible)) }
        } else if (state.visiblePieces.isNotEmpty()) {
            TextButton(onClick = onSelectAllVisibleClick) { Text(stringResource(R.string.catalog_select_all_visible)) }
        }
        // Vaciar todo es una opción aparte y dice cuántas quita.
        if (state.canClearAll) {
            TextButton(onClick = onDeselectAllClick) {
                Text(pluralStringResource(R.plurals.catalog_deselect_all, state.selectedCount, state.selectedCount))
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CatalogPieceRow(
    piece: CatalogPiece,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            // Toda la fila es el área táctil: el check es solo el indicador.
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (piece.item.photoPath != null) {
                AsyncImage(model = piece.item.photoPath, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (piece.caption.isNotEmpty()) {
                Text(
                    text = piece.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // El código no se parte en dos líneas (con letra grande salía
            // «XP-0000 / 05»): es el dato con el que ella busca la pieza.
            Text(
                text = piece.item.uid,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            // El precio va debajo y no en una columna a la derecha: así el
            // texto conserva todo el ancho con letra grande del sistema. En
            // pantalla: el precio con el formato del teléfono de ella. Lo que
            // sale en la IMAGEN usa otro formato fijo (ver `catalogPriceText`).
            Text(
                text = piece.item.price.format(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogPickerScreenPreview() {
    val pieces = listOf(
        CatalogPiece(CatalogItem(1, "XP-000001", "Anillo con piedra azul", "Anillos", null, Money(12500)), "Anillo con piedra azul"),
        CatalogPiece(CatalogItem(2, "XP-000002", "Pieza sin nombre", "Aretes", null, Money(8000)), "Aretes"),
        CatalogPiece(CatalogItem(3, "XP-000003", "Pieza sin nombre", null, null, Money(15000)), ""),
    )
    JoyeriaTheme {
        CatalogPickerScreen(
            state = CatalogUiState(pieces = pieces, isLoaded = true, selectedIds = setOf(2L)),
            onQueryChange = {},
            onPieceToggled = {},
            onSelectAllVisibleClick = {},
            onDeselectVisibleClick = {},
            onDeselectAllClick = {},
            onCreateClick = {},
            onBackClick = {},
        )
    }
}
