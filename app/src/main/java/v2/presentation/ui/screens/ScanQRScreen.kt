package v2.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import v2.presentation.ui.components.DeviceApprovalValidator
import v2.presentation.ui.components.QRCodeScanner
import v2.presentation.viewmodels.ScanQRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQRScreen(
    onBackClick: () -> Unit,
    onDeviceNotApproved: () -> Unit,
    viewModel: ScanQRViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DeviceApprovalValidator(
        onDeviceNotApproved = onDeviceNotApproved,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full screen camera
                QRCodeScanner(
                    onQRCodeDetected = { qrData ->
                        viewModel.scanQRCode(qrData)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Top bar overlay
                TopAppBar(
                    title = {
                        Text(
                            text = "Scan QR Code",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                // Bottom overlay for status messages
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Scanning status indicator
                    if (uiState.isScanning) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.8f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Processing QR code...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    // Error display
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.9f),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                // Success popup overlay - centered and auto-dismissing
                AnimatedVisibility(
                    visible = uiState.scanResult is v2.data.models.ScanResult.Success,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ) + fadeIn(
                        animationSpec = tween(300),
                    ) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(300),
                    ) + fadeOut(
                        animationSpec = tween(300),
                    ) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(300),
                    ),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    uiState.scanResult?.let { result ->
                        if (result is v2.data.models.ScanResult.Success) {
                            SuccessPopup(
                                memberName = result.attendanceRecord.name,
                                onDismiss = { viewModel.clearResult() },
                            )
                        }
                    }
                }

                // Error popup overlay - centered
                AnimatedVisibility(
                    visible = uiState.scanResult is v2.data.models.ScanResult.Error,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ) + fadeIn(
                        animationSpec = tween(300),
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(200),
                    ) + fadeOut(
                        animationSpec = tween(200),
                    ),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    uiState.scanResult?.let { result ->
                        if (result is v2.data.models.ScanResult.Error) {
                            ErrorPopup(
                                errorMessage = result.message,
                                onDismiss = { viewModel.clearResult() },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SuccessPopup(
    memberName: String,
    onDismiss: () -> Unit,
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    // Animation for the success icon
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "success_icon_scale",
    )

    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Animated success icon with pulse effect
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale * pulse),
                tint = Color(0xFF4CAF50),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Welcome message
            Text(
                text = "Welcome, $memberName! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your attendance has been recorded successfully!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF388E3C),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Have a great workout! 💪",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF66BB6A),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-dismiss indicator
            Text(
                text = "Auto-closing in 3 seconds...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorPopup(
    errorMessage: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFE53E3E),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53E3E),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Try Again",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
