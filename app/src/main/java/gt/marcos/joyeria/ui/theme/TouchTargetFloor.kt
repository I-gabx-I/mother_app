package gt.marcos.joyeria.ui.theme

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Piso de área táctil de CLAUDE.md sección 6. Único lugar donde vive el número. */
val MinTouchTarget = 56.dp

/**
 * Piso de área táctil de CLAUDE.md sección 6 (56dp), fijado en un solo lugar
 * para toda la app y para las pantallas que todavía no existen. No se pisa con
 * un Modifier.size local. Hacen falta DOS valores, no uno:
 *
 * - `LocalMinimumInteractiveComponentSize` agranda el espacio que ocupa el
 *   componente (Material 3 lo deja en 48dp).
 * - `minimumTouchTargetSize` de `ViewConfiguration` es hasta dónde Compose
 *   extiende la zona que de verdad responde al toque (también 48dp por
 *   defecto). Con solo el primero el componente ocupa 56dp, pero el anillo de
 *   afuera de los 48dp no responde: la regla se vería cumplida en el layout
 *   y no en el dedo.
 *
 * `JoyeriaTheme` lo aplica a toda la ventana principal. Un diálogo es otra
 * ventana: reinicia `ViewConfiguration` y pierde el segundo valor, por eso
 * los diálogos se arman con [JoyeriaAlertDialog] y no con `AlertDialog`.
 */
@Composable
fun TouchTargetFloor(content: @Composable () -> Unit) {
    val viewConfiguration = LocalViewConfiguration.current
    val viewConfigurationConPiso = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize: DpSize = DpSize(MinTouchTarget, MinTouchTarget)
        }
    }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides MinTouchTarget,
        LocalViewConfiguration provides viewConfigurationConPiso,
        content = content,
    )
}

/**
 * `AlertDialog` de Material 3 con el piso de [TouchTargetFloor] puesto en sus
 * botones. Toda pantalla usa este y no `AlertDialog` directo: sin esto, los
 * botones de confirmar y cancelar -- los de las acciones irreversibles --
 * vuelven a medir 48dp porque el diálogo vive en su propia ventana.
 */
@Composable
fun JoyeriaAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TouchTargetFloor(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { boton -> { TouchTargetFloor(boton) } },
        title = title,
        text = text,
    )
}

/**
 * `DropdownMenuItem` de Material 3 con alto mínimo de [MinTouchTarget]. El
 * ítem de Material trae un alto mínimo fijo de 48dp que ni
 * `LocalMinimumInteractiveComponentSize` ni `ViewConfiguration` suben, y el
 * menú vive en otra ventana. Toda lista desplegable usa este y no
 * `DropdownMenuItem` directo.
 */
@Composable
fun JoyeriaDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        leadingIcon = leadingIcon,
    )
}
