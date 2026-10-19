package gt.marcos.joyeria.ui.barcode

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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.barcode.LabelPiece
import gt.marcos.joyeria.domain.barcode.ProductUid
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Elegir piezas para imprimir sus etiquetas (Fase 13). Stateless y `@Preview`-able.
 * Solo conoce [LabelListItem], que no tiene ningún monto: igual que el catálogo,
 * esta pantalla no puede mostrar un precio ni un costo aunque alguien quisiera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    state: LabelsUiState,
    onQueryChange: (String) -> Unit,
    onPieceToggled: (String) -> Unit,
    onSelectAllVisibleClick: () -> Unit,
    onDeselectVisibleClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit,
    onTestSheetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.labels_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        bottomBar = {
            // Mismo manejo de insets que el botón de "Vender" (ESTADO.md, "Punto 2 -- insets").
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.failed) {
                    Text(
                        text = stringResource(R.string.labels_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onPrintClick,
                    enabled = state.canPrint,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(
                        text = if (state.working) stringResource(R.string.labels_working) else stringResource(R.string.labels_print),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
                    onClick = onShareClick,
                    enabled = state.canPrint,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.labels_share))
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                text = stringResource(R.string.labels_hint),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.catalog_search_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = if (state.selectedCount == 0) {
                        stringResource(R.string.catalog_selected_none)
                    } else {
                        pluralStringResource(R.plurals.catalog_selected_count, state.selectedCount, state.selectedCount)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                // «Elegir» y «Quitar» actúan sobre lo mismo: las que se ven (igual que el catálogo).
                if (state.allVisibleSelected) {
                    TextButton(onClick = onDeselectVisibleClick) { Text(stringResource(R.string.catalog_deselect_visible)) }
                } else if (state.visibleItems.isNotEmpty()) {
                    TextButton(onClick = onSelectAllVisibleClick) { Text(stringResource(R.string.catalog_select_all_visible)) }
                }
                if (state.canClearAll) {
                    TextButton(onClick = onDeselectAllClick) {
                        Text(pluralStringResource(R.plurals.catalog_deselect_all, state.selectedCount, state.selectedCount))
                    }
                }
                TextButton(onClick = onTestSheetClick, enabled = !state.working) {
                    Text(stringResource(R.string.labels_test_sheet))
                }
            }
            when {
                !state.isLoaded -> Unit
                state.items.isEmpty() -> Message(stringResource(R.string.labels_empty))
                state.visibleItems.isEmpty() -> Message(stringResource(R.string.catalog_no_results))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Clave con prefijo por sección (CLAUDE.md sección 5), con el código de la pieza y no la posición.
                    items(state.visibleItems, key = { "label-${it.piece.uid.value}" }) { item ->
                        LabelRow(
                            item = item,
                            selected = item.piece.uid.value in state.selectedUids,
                            onToggle = { onPieceToggled(item.piece.uid.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabelRow(item: LabelListItem, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (item.piece.photoPath != null) {
                AsyncImage(model = item.piece.photoPath, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (item.caption.isNotEmpty()) {
                Text(text = item.caption, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            // El código no se parte en dos líneas: es lo que va en la etiqueta.
            Text(
                text = item.piece.uid.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    fun item(raw: String, name: String, caption: String) =
        LabelListItem(LabelPiece(checkNotNull(ProductUid.parse(raw)), name, null, null), caption)
    JoyeriaTheme {
        LabelsScreen(
            state = LabelsUiState(
                items = listOf(item("XP-000042", "Anillo azul", "Anillo azul"), item("XP-000041", "Pieza sin nombre", "Aretes")),
                isLoaded = true,
                selectedUids = setOf("XP-000042"),
            ),
            onQueryChange = {}, onPieceToggled = {}, onSelectAllVisibleClick = {}, onDeselectVisibleClick = {},
            onDeselectAllClick = {}, onPrintClick = {}, onShareClick = {}, onTestSheetClick = {}, onBackClick = {},
        )
    }
}
