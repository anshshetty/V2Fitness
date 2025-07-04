package v2.presentation.ui.components

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRCodeScanner(
    onQRCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var flashEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isBackCamera by remember { mutableStateOf(true) }

    when {
        cameraPermissionState.status.isGranted -> {
            // Camera permission granted, show camera preview
            Box(modifier = modifier) {
                CameraPreview(
                    onQRCodeDetected = onQRCodeDetected,
                    flashEnabled = flashEnabled,
                    isBackCamera = isBackCamera,
                    onCameraReady = { cam -> camera = cam },
                    modifier = Modifier.fillMaxSize(),
                )

                // Overlay with scanning frame and controls
                ScanningOverlay(
                    flashEnabled = flashEnabled,
                    isBackCamera = isBackCamera,
                    onFlashToggle = {
                        flashEnabled = !flashEnabled
                        camera?.cameraControl?.enableTorch(flashEnabled)
                    },
                    onCameraSwitch = {
                        isBackCamera = !isBackCamera
                        // Flash is only available on back camera, so disable it when switching to front
                        if (!isBackCamera && flashEnabled) {
                            flashEnabled = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // Show rationale and request permission
            CameraPermissionRationale(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            )
        }
        else -> {
            // Request permission for the first time
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
            CameraPermissionRequest()
        }
    }
}

@Composable
private fun CameraPreview(
    onQRCodeDetected: (String) -> Unit,
    flashEnabled: Boolean,
    isBackCamera: Boolean,
    onCameraReady: (Camera) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor, QRCodeAnalyzer(onQRCodeDetected))
                    }

                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                    )
                    onCameraReady(camera)
                } catch (exc: Exception) {
                    Log.e("QRCodeScanner", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier,
        update = { previewView ->
            // Trigger camera rebinding when camera selection changes
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor, QRCodeAnalyzer(onQRCodeDetected))
                    }

                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                    )
                    onCameraReady(camera)
                } catch (exc: Exception) {
                    Log.e("QRCodeScanner", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun ScanningOverlay(
    flashEnabled: Boolean,
    isBackCamera: Boolean,
    onFlashToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Scanning frame
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            // Corner indicators
            ScanningCorners()
        }

        // Camera controls row
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Camera switch button
            FloatingActionButton(
                onClick = onCameraSwitch,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = if (isBackCamera) "Switch to front camera" else "Switch to back camera",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Flash toggle button (only show for back camera)
            if (isBackCamera) {
                FloatingActionButton(
                    onClick = onFlashToggle,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                        contentDescription = if (flashEnabled) "Turn off flash" else "Turn on flash",
                        tint = if (flashEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
        ) {
            Text(
                text = "Point your camera at a QR code",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ScanningCorners() {
    val cornerSize = 20.dp
    val cornerThickness = 3.dp
    val cornerColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.TopStart)
                    .background(cornerColor),
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.TopStart)
                    .background(cornerColor),
            )
        }

        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.TopEnd)
                    .background(cornerColor),
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .background(cornerColor),
            )
        }

        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomStart)
                    .background(cornerColor),
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.BottomStart)
                    .background(cornerColor),
            )
        }

        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomEnd)
                    .background(cornerColor),
            )
            Box(
                modifier = Modifier
                    .width(cornerThickness)
                    .fillMaxHeight()
                    .align(Alignment.BottomEnd)
                    .background(cornerColor),
            )
        }
    }
}

@Composable
private fun CameraPermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This app needs camera access to scan QR codes for attendance tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Camera Permission")
        }
    }
}

@Composable
private fun CameraPermissionRequest() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Requesting camera permission...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()
    private var lastDetectionTime = 0L
    private var lastScannedQRCode: String? = null
    private val detectionCooldown = 3000L // Increased to 3 seconds cooldown between detections
    private val sameCooldown = 10000L // 10 seconds cooldown for the same QR code

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner
                .process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                                barcode.rawValue?.let { qrData ->
                                    if (qrData.isNotBlank()) {
                                        val currentTime = System.currentTimeMillis()

                                        // Check if this is the same QR code as last scanned
                                        val isSameQRCode = lastScannedQRCode == qrData
                                        val requiredCooldown = if (isSameQRCode) sameCooldown else detectionCooldown

                                        if (currentTime - lastDetectionTime > requiredCooldown) {
                                            lastDetectionTime = currentTime
                                            lastScannedQRCode = qrData
                                            onQRCodeDetected(qrData)
                                            Log.d("QRCodeAnalyzer", "QR code detected and processed: ${qrData.take(20)}...")
                                        } else {
                                            val remainingTime = (requiredCooldown - (currentTime - lastDetectionTime)) / 1000
                                            Log.d(
                                                "QRCodeAnalyzer",
                                                "QR code detection blocked - cooldown active (${remainingTime}s remaining)",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("QRCodeAnalyzer", "QR code detection failed", exception)
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
} 
