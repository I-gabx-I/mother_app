package gt.marcos.joyeria.ui.barcode

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import gt.marcos.joyeria.R
import gt.marcos.joyeria.ui.permission.CameraPermissionGate
import java.util.concurrent.Executors

/** Mismo texto dos veces en menos de esto no vuelve a avisar: la etiqueta sigue frente a la cámara. */
private const val SAME_CODE_PAUSE_MILLIS = 2_000L

/**
 * El escáner de códigos de barras (Fase 13): vista previa de CameraX y un
 * `ImageAnalysis` que le pasa cada cuadro a ZXing ([BarcodeDecoder]).
 *
 * - El análisis va a **1080p** a propósito: con la cámara a ~10 cm una etiqueta de
 *   0.254 mm por módulo da ~3.5 píxeles por módulo; a 720p serían ~2.3, justo en el
 *   límite de lo que ZXing lee (D-060).
 * - El permiso lo maneja [CameraPermissionGate]: explicación primero, diálogo del
 *   sistema recién al tocar el botón.
 * - Libera la cámara al salir (`unbindAll`).
 *
 * [onCodeRead] recibe el texto leído (si es el mismo, no más de una vez cada
 * [SAME_CODE_PAUSE_MILLIS]); decidir qué es lo hace quien llama. [message] es el
 * aviso de la última lectura que no sirvió, mientras el escáner sigue abierto.
 */
@Composable
fun BarcodeScannerOverlay(
    onCodeRead: (String) -> Unit,
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraPermissionGate(
        explanation = stringResource(R.string.scanner_permission_explanation),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        ScannerCamera(onCodeRead = onCodeRead, message = message, onDismiss = onDismiss)
    }
}

@Composable
private fun ScannerCamera(
    onCodeRead: (String) -> Unit,
    message: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val currentOnCodeRead by rememberUpdatedStateCompat(onCodeRead)

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        val decoder = BarcodeDecoder()
        var lastText: String? = null
        var lastAt = 0L

        providerFuture.addListener(
            {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image ->
                    try {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                        val text = decoder.decode(bytes, image.width, image.height, plane.rowStride)
                        val now = System.currentTimeMillis()
                        if (text != null && !(text == lastText && now - lastAt < SAME_CODE_PAUSE_MILLIS)) {
                            lastText = text
                            lastAt = now
                            mainExecutor.execute { currentOnCodeRead(text) }
                        }
                    } finally {
                        image.close()
                    }
                }
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            },
            mainExecutor,
        )
        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (camera?.cameraInfo?.hasFlashUnit() == true) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                            IconButton(onClick = {
                                torchOn = !torchOn
                                camera?.cameraControl?.enableTorch(torchOn)
                            }) {
                                Icon(
                                    imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = stringResource(
                                        if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on,
                                    ),
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(stringResource(R.string.scanner_close))
                    }
                }
            }
        }
    }
}

/** `rememberUpdatedState` con otro nombre para no pelearse con el import de `getValue` de arriba. */
@Composable
private fun <T> rememberUpdatedStateCompat(value: T) = androidx.compose.runtime.rememberUpdatedState(value)
