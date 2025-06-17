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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.ar.core.ImageFormat

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var scannedCode by remember { mutableStateOf<String?>(null) } // To prevent multiple scans triggering dialog
    val state by viewModel.state.collectAsState()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (cameraPermissionState.status is PermissionStatus.Denied && cameraPermissionState.status != PermissionStatus.Granted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (cameraPermissionState.status) {
                PermissionStatus.Granted -> {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            startCamera(
                                context = ctx,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                cameraExecutor = cameraExecutor,
                                onQrCodeScanned = { resultText ->
                                    if (resultText != null && scannedCode == null) { // Process only once
                                        scannedCode = resultText // Mark as processed for this scan session
                                        val parts = resultText.split("|")
                                        if (parts.size == 2) {
                                            Log.d("CameraScreen", "QR Scanned: ID=${parts[0]}, Name=${parts[1]}")
                                            viewModel.handleScannedQrCode(parts[0], parts[1])
                                        } else {
                                            Log.w("CameraScreen", "Scanned QR code has unexpected format: $resultText")
                                            // Optionally show a snackbar for bad QR format
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
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Camera permission is required to scan QR codes.", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape) // Slightly darker background for visibility
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White // White icon for better contrast on darkish background
            )
        }

        if (state.showAddFriendDialog && state.scannedUserId != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!state.isSendingFriendRequest) {
                        viewModel.dismissAddFriendDialog()
                        scannedCode = null // Allow new scan after dismissing
                    }
                },
                title = { Text("Add Friend") },
                text = { Text("Send a friend request to ${state.scannedUserName ?: "this user"}?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.sendFriendshipRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = !state.isSendingFriendRequest
                    ) {
                        if (state.isSendingFriendRequest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Request")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissAddFriendDialog()
                            scannedCode = null // Allow new scan after dismissing
                        },
                        enabled = !state.isSendingFriendRequest
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (state.showRequestAlreadyPendingDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.dismissRequestAlreadyPendingDialog()
                    scannedCode = null // Allow new scan after dismissing this dialog too
                },
                title = { Text("Request Status") },
                text = { Text(state.requestAlreadyPendingMessage) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.dismissRequestAlreadyPendingDialog()
                        scannedCode = null // Allow new scan
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        state.error?.let { error ->
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError() // Clear error after showing
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
        }
        state.successMessage?.let { message ->
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSuccessMessage()
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraScreen", "Disposing camera resources.")
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
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(imageProxy, onQrCodeScanned)
            })

            cameraProvider.unbindAll()
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
    imageProxy.use { imgProxy -> // Use 'use' block for automatic closing
        if (imgProxy.format == android.graphics.ImageFormat.YUV_420_888 ||
            imgProxy.planes.size >= 3) { // A common check for YUV plane availability
            val yBuffer = imgProxy.planes[0].buffer // Y
            val uBuffer = imgProxy.planes[1].buffer // U
            val vBuffer = imgProxy.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize) // This might not be correct for PlanarYUVLuminanceSource directly

            val buffer = imgProxy.planes[0].buffer // Get the Y-plane (luminance)
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val width = imgProxy.width
            val height = imgProxy.height

            // Create a LuminanceSource from the Y-plane data
            val source = PlanarYUVLuminanceSource(
                bytes,         // yuvData
                width,         // dataWidth
                height,        // dataHeight
                0,             // left
                0,             // top
                width,         // width
                height,        // height
                false          // reverseHorizontal
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val reader = MultiFormatReader().apply {
                    val hints = HashMap<DecodeHintType, Any>()
                    hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
                    // You could also add TRY_HARDER if scans are difficult, but it impacts performance
                    // hints[DecodeHintType.TRY_HARDER] = true
                    setHints(hints)
                }
                val result = reader.decode(binaryBitmap)
                onQrCodeScanned(result.text)
            } catch (e: NotFoundException) {
            } catch (e: Exception) {
                Log.e("CameraScreen", "QR Code decoding error: ${e.message}", e)
                onQrCodeScanned(null) // Signal error or no QR code found
            }
        } else {
            Log.w("CameraScreen", "Image format not YUV_420_888 or unexpected plane count. Format: ${imgProxy.format}")
            onQrCodeScanned(null) // Signal that we couldn't process this frame
        }
    }
}