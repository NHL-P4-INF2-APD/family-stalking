package com.familystalking.app.presentation.family

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavController
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(onQrScanned: (String) -> Unit, navController: NavController? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (cameraPermissionState.status is PermissionStatus.Denied) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Back-knop linksboven
        if (navController != null) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (cameraPermissionState.status) {
                is PermissionStatus.Granted -> {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    processImageProxy(imageProxy) { result ->
                                        if (result != null && scannedCode == null) {
                                            scannedCode = result
                                            onQrScanned(result)
                                        }
                                    }
                                }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalyzer
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (scannedCode != null) {
                        Text(
                            text = "QR Code: $scannedCode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is PermissionStatus.Denied -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera permission required", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }
        }
    }
}

private fun processImageProxy(imageProxy: ImageProxy, onQrCodeScanned: (String?) -> Unit) {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val width = imageProxy.width
    val height = imageProxy.height
    val source = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    try {
        val result = MultiFormatReader().decode(bitmap)
        onQrCodeScanned(result.text)
    } catch (e: Exception) {
        onQrCodeScanned(null)
    } finally {
        imageProxy.close()
    }
} 