package gt.marcos.joyeria.ui.product.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/**
 * Pantalla completa (superpuesta sobre `AddProductScreen`) que pide el
 * permiso de cámara si hace falta, muestra la vista previa de CameraX y
 * dispara la captura. `onImageCaptured` recibe el `Bitmap` decodificado y
 * la rotación (`imageInfo.rotationDegrees`) por separado — quien la reciba
 * (el ViewModel, vía `ImageStorage`) es responsable de aplicarla antes de
 * guardar; acá no se rota nada, solo se captura y se decodifica.
 */
@Composable
fun CameraCaptureOverlay(
    onImageCaptured: (bitmap: Bitmap, rotationDegrees: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)) {
        if (hasPermission) {
            CameraPreviewWithShutter(onImageCaptured = onImageCaptured)
        } else {
            PermissionRationale(
                denied = permissionDenied,
                onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun PermissionRationale(denied: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.add_product_camera_permission_rationale),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (denied) {
            Text(
                text = stringResource(R.string.add_product_camera_permission_denied),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Button(onClick = onRetry, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.add_product_take_photo))
        }
        Button(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.add_product_camera_cancel))
        }
    }
}

@Composable
private fun CameraPreviewWithShutter(
    onImageCaptured: (bitmap: Bitmap, rotationDegrees: Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            },
            mainExecutor,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        FloatingActionButton(
            onClick = {
                imageCapture.takePicture(
                    mainExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            image.close()
                            onImageCaptured(bitmap, rotationDegrees)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Sin crash: si falla la captura, la usuaria simplemente
                            // vuelve a tocar el disparador. No hay nada que persistir.
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.add_product_take_photo),
            )
        }
    }
}
