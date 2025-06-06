package com.familystalking.app.presentation.family

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.hilt.navigation.compose.hiltViewModel
import com.familystalking.app.ui.theme.PrimaryGreen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedCode by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsState()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (cameraPermissionState.status is PermissionStatus.Denied) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (cameraPermissionState.status) {
                is PermissionStatus.Granted -> {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            startCamera(
                                context = ctx,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                cameraExecutor = cameraExecutor,
                                onQrCodeScanned = { result ->
                                    if (result != null && scannedCode == null) {
                                        scannedCode = result
                                        val parts = result.split("|")
                                        if (parts.size == 2) {
                                            viewModel.handleScannedQrCode(parts[0], parts[1])
                                        }
                                    }
                                }
                            )
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
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

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        // Add Friend Dialog
        if (state.showAddFriendDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAddFriendDialog() },
                title = { Text("Add Friend") },
                text = { Text("Would you like to add ${state.scannedUserName} as friend?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.sendFriendshipRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAddFriendDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error Snackbar
        state.error?.let { error ->
            LaunchedEffect(error) {
                // Show error snackbar
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

private fun startCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    cameraExecutor: ExecutorService,
    onQrCodeScanned: (String?) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            Log.d("CameraScreen", "Camera provider future complete.")
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy, onQrCodeScanned)
            }

            cameraProvider.unbindAll()
            Log.d("CameraScreen", "Binding camera to lifecycle.")
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            Log.d("CameraScreen", "Camera bound successfully.")
        } catch (e: Exception) {
            Log.e("CameraScreen", "Use case binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
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