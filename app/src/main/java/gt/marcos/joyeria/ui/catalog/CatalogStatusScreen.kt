package gt.marcos.joyeria.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

/**
 * Los dos pasos intermedios del catálogo (Fase 12): «Armando…» y el error.
 * Los dos tienen una salida clara: cancelar o volver a elegir, sin perder lo
 * que ella eligió.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogStatusScreen(
    step: CatalogStep,
    onBackToPickingClick: () -> Unit,
    onRetryClick: () -> Unit,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                is CatalogStep.Generating -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.catalog_generating, step.done, step.total),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = onBackToPickingClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.catalog_cancel))
                    }
                }
                CatalogStep.Failed -> {
                    Text(
                        text = stringResource(R.string.catalog_error),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.catalog_retry))
                    }
                    OutlinedButton(
                        onClick = onBackToPickingClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.catalog_pick_others))
                    }
                }
                else -> Unit
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogStatusGeneratingPreview() {
    JoyeriaTheme {
        CatalogStatusScreen(
            step = CatalogStep.Generating(done = 2, total = 5),
            onBackToPickingClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogStatusFailedPreview() {
    JoyeriaTheme {
        CatalogStatusScreen(step = CatalogStep.Failed, onBackToPickingClick = {}, onRetryClick = {})
    }
}
