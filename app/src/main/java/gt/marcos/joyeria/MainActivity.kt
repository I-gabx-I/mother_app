package gt.marcos.joyeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import gt.marcos.joyeria.ui.navigation.JoyeriaNavHost
import gt.marcos.joyeria.ui.theme.JoyeriaTheme

// Fase 04: aloja el NavHost único de la app (D-023). Cada pantalla nueva
// se agrega como un destino más de JoyeriaNavHost, no acá.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JoyeriaTheme {
                JoyeriaNavHost()
            }
        }
    }
}
