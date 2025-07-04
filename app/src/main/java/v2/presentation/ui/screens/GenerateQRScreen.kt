package v2.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import v2.presentation.ui.components.DeviceApprovalValidator
import v2.presentation.viewmodels.GenerateQRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQRScreen(
    onBackClick: () -> Unit,
    onQRGenerated: () -> Unit,
    onDeviceNotApproved: () -> Unit,
    viewModel: GenerateQRViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Remove auto-navigation to let user see the generated QR code
    // LaunchedEffect(uiState.isSuccess) {
    //     if (uiState.isSuccess) {
    //         onQRGenerated()
    //     }
    // }

    DeviceApprovalValidator(
        onDeviceNotApproved = onDeviceNotApproved,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Generate QR Code",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Remove test data button for production
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Form Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "QR Code Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )

                            // Name Field
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::updateName,
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.nameError != null,
                                supportingText = uiState.nameError?.let { { Text(it) } },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                            )

                            // Mobile Number Field
                            OutlinedTextField(
                                value = uiState.mobileNumber,
                                onValueChange = viewModel::updateMobileNumber,
                                label = { Text("Mobile Number") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = uiState.mobileNumberError != null,
                                supportingText = uiState.mobileNumberError?.let { { Text(it) } },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                },
                                singleLine = true,
                            )

                            // Expiry Duration Field
                            OutlinedTextField(
                                value = uiState.expiryDuration.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { viewModel.updateExpiryDuration(it) }
                                },
                                label = { Text("Expiry Duration (days)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = uiState.expiryError != null,
                                supportingText = uiState.expiryError?.let { { Text(it) } },
                                leadingIcon = {
                                    Icon(Icons.Default.AccessTime, contentDescription = null)
                                },
                                singleLine = true,
                            )

                            // Quick Duration Buttons
                            Text(
                                text = "Quick Select:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                val durations = listOf(
                                    "30 Days" to 30,
                                    "60 Days" to 60,
                                    "90 Days" to 90,
                                    "120 Days" to 120,
                                    "1 Year" to 365,
                                )

                                items(durations) { (label, days) ->
                                    FilterChip(
                                        onClick = { viewModel.updateExpiryDuration(days) },
                                        label = {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                            )
                                        },
                                        selected = uiState.expiryDuration == days,
                                        modifier = Modifier.height(40.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = { viewModel.generateQRCode(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate QR Code")
                        }
                    }

                    // Error Display
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
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
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }

                    // QR Code Display
                    uiState.qrCodeBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Generated QR Code",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier.size(200.dp),
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                uiState.generatedQRCode?.let { qrCode ->
                                    Text(
                                        text = "Name: ${qrCode.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Mobile: ${qrCode.mobileNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Valid for: ${qrCode.expiryDuration} days",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.saveQRCode() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.isSaving && !uiState.isSharing && !uiState.isSharingWhatsApp,
                                        ) {
                                            if (uiState.isSaving) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Saving...")
                                            } else {
                                                Icon(Icons.Default.Save, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save")
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.shareQRCode() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.isSaving && !uiState.isSharing && !uiState.isSharingWhatsApp,
                                        ) {
                                            if (uiState.isSharing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sharing...")
                                            } else {
                                                Icon(Icons.Default.Share, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Share")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // WhatsApp Share Button
                                    Button(
                                        onClick = { viewModel.shareQRCodeOnWhatsApp() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isSaving && !uiState.isSharing && !uiState.isSharingWhatsApp,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = androidx.compose.ui.graphics
                                                .Color(0xFF25D366),
                                        ),
                                    ) {
                                        if (uiState.isSharingWhatsApp) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = androidx.compose.ui.graphics.Color.White,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Sharing on WhatsApp...",
                                                color = androidx.compose.ui.graphics.Color.White,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = androidx.compose.ui.graphics.Color.White,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Share on WhatsApp",
                                                color = androidx.compose.ui.graphics.Color.White,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = onQRGenerated,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Back to Dashboard")
                                }
                            }
                        }
                    }

                    // Add bottom padding to ensure content is not cut off
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
    )
} 
