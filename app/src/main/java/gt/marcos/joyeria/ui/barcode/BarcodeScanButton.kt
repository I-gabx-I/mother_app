package gt.marcos.joyeria.ui.barcode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.barcode.ScanResult
import gt.marcos.joyeria.ui.theme.TouchTargetFloor
import kotlinx.coroutines.launch

/**
 * El botón «Escanear» que va dentro del campo de búsqueda (Inventario y el selector
 * de piezas de Vender) y el escáner que abre. Stateful (usa [ScanViewModel]): las
 * pantallas lo reciben como un `@Composable` en una ranura, no el ViewModel.
 *
 * [onFound] recibe una pieza activa de su inventario. Devuelve `null` si la aceptó
 * (el escáner se cierra) o un texto en español si no la puede usar (por ejemplo,
 * «sin existencias»): se le muestra y el escáner sigue abierto para probar con otra.
 *
 * Lo que no es una pieza activa (un código ajeno, uno que no existe, una pieza
 * archivada) tiene su mensaje y nunca cierra ni tira la app.
 *
 * El escáner es una ventana aparte (`Dialog`): como todo diálogo, pierde el piso
 * táctil de 56dp del tema, así que se le vuelve a poner con [TouchTargetFloor].
 */
@Composable
fun BarcodeScanButton(
    onFound: (ScanResult.Found) -> String?,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    var open by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val notAPiece = stringResource(R.string.scanner_not_a_piece)
    val archived = stringResource(R.string.scanner_archived)
    val notFoundTemplate = stringResource(R.string.scanner_not_found)

    IconButton(onClick = { message = null; open = true }, modifier = modifier) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scanner_button_description))
    }

    if (open) {
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            TouchTargetFloor {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BarcodeScannerOverlay(
                        onCodeRead = { raw ->
                            scope.launch {
                                message = when (val result = viewModel.resolve(raw)) {
                                    is ScanResult.Found -> onFound(result).also { error -> if (error == null) open = false }
                                    ScanResult.Archived -> archived
                                    is ScanResult.NotFound -> String.format(java.util.Locale.ROOT, notFoundTemplate, result.uid.value)
                                    ScanResult.NotAPiece -> notAPiece
                                }
                            }
                        },
                        message = message,
                        onDismiss = { open = false },
                    )
                }
            }
        }
    }
}
