package gt.marcos.joyeria.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.io.File

/**
 * Paso final del catálogo (Fase 12): mirar cómo quedó y compartirlo. Stateless
 * y `@Preview`-able. Las páginas son las mismas imágenes que se van a mandar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogResultScreen(
    pages: List<String>,
    onShareClick: () -> Unit,
    onBackToPickingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackToPickingClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        bottomBar = {
            // Mismo manejo de insets que el botón de "Vender" (ver CatalogPickerScreen).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onShareClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text(
                        text = stringResource(R.string.catalog_share_button),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = onBackToPickingClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.catalog_pick_others))
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(text = stringResource(R.string.catalog_result_hint), style = MaterialTheme.typography.bodyLarge)
            }
            // Clave con prefijo por sección (CLAUDE.md sección 5). La ruta cambia
            // en cada generación, así que una vista previa nueva nunca reusa la
            // fila ni la imagen en memoria de la anterior.
            itemsIndexed(pages, key = { _, path -> "page-$path" }) { index, path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.catalog_page_description, index + 1, pages.size),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogResultScreenPreview() {
    JoyeriaTheme {
        CatalogResultScreen(pages = emptyList(), onShareClick = {}, onBackToPickingClick = {})
    }
}
