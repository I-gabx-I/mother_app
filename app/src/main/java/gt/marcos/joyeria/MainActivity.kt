package gt.marcos.joyeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import gt.marcos.joyeria.ui.product.add.AddProductRoute
import gt.marcos.joyeria.ui.product.add.AddProductViewModel
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

// Fase 03: muestra la pantalla de alta rápida de pieza. Todavía no hay
// pantalla de inicio con "Vender"/"Agregar pieza" (CLAUDE.md sección 6) --
// esa llega cuando exista algo real detrás del botón "Vender" (Fase 05);
// hasta entonces, mostrar un botón que no hace nada sería un stub que miente
// (CLAUDE.md sección 5).
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val addProductViewModel: AddProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JoyeriaTheme {
                AddProductRoute(viewModel = addProductViewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun HomePlaceholder(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.app_name))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePlaceholderPreview() {
    JoyeriaTheme {
        HomePlaceholder()
    }
}
