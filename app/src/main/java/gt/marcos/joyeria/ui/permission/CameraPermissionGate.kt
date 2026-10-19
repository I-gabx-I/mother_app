package gt.marcos.joyeria.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import gt.marcos.joyeria.R

/**
 * El permiso de cámara de TODA la app (la foto de la pieza y el escáner), en un solo
 * lugar. Android otorga el permiso a la app, no a una función: lo que acepte acá
 * vale para las dos cosas, y no hay un segundo diálogo.
 *
 * - **Ya otorgado:** muestra [content] directo, sin explicación ni diálogo.
 * - **Sin pedir:** primero la [explanation] en español; el diálogo del sistema
 *   aparece **recién cuando ella toca «Permitir la cámara»**, no al abrir la pantalla.
 * - **Dijo que no una vez:** mensaje y puede volver a intentarlo.
 * - **Dijo que no para siempre:** Android ya no muestra el diálogo y tocar el botón
 *   no hacía nada (el callejón sin salida que tenía el alta rápida); ahora ofrece
 *   «Abrir ajustes» y, al volver de ahí, revisa el permiso solo.
 *
 * [onDismiss] es «Ahora no»: ella siempre puede salir.
 */
@Composable
fun CameraPermissionGate(
    explanation: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    // Cuenta cuántas veces respondió ella al diálogo. Hace falta porque negar la 2.a vez no
    // cambia `granted` ni `askedBefore`: sin esto la pantalla no se vuelve a calcular, se queda
    // con «todavía puede intentar» y el botón no hace nada (se vio en el emulador).
    var answers by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        askedBefore = true
        answers++
    }

    // Al volver de los ajustes de la app, revisa el permiso: puede haberlo activado ahí.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasCameraPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) {
        content()
        return
    }

    // Se vuelve a preguntar a Android después de cada respuesta (`answers`) y al volver de los ajustes.
    val canAskAgain = remember(answers, granted) {
        context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
    }
    val step = CameraPermissionLogic.step(granted = false, askedBefore = askedBefore, canAskAgain = canAskAgain)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
        ) {
            Text(text = explanation, style = MaterialTheme.typography.bodyLarge)
            when (step) {
                CameraPermissionStep.DENIED -> Text(
                    text = stringResource(R.string.camera_permission_denied),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                CameraPermissionStep.DENIED_FOR_GOOD -> Text(
                    text = stringResource(R.string.camera_permission_denied_for_good),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }
            if (step == CameraPermissionStep.DENIED_FOR_GOOD) {
                Button(
                    onClick = { context.startActivity(appSettingsIntent(context)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.camera_permission_open_settings)) }
            } else {
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.camera_permission_allow)) }
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.camera_permission_dismiss)) }
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun appSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
