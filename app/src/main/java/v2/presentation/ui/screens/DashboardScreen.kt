package v2.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import v2.BuildConfig
import v2.presentation.ui.components.AttendanceListItem
import v2.presentation.ui.components.DeviceApprovalValidator
import v2.presentation.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGenerateClick: () -> Unit,
    onScanClick: () -> Unit,
    onViewAllQRsClick: () -> Unit,
    onDeviceNotApproved: () -> Unit,
    onTestingMenuClick: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh data when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    DeviceApprovalValidator(
        onDeviceNotApproved = onDeviceNotApproved,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "V2 Fitness",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            // Testing menu button (debug builds only)
                            if (BuildConfig.ENABLE_TESTING_MENU && onTestingMenuClick != null) {
                                IconButton(onClick = onTestingMenuClick) {
                                    Icon(Icons.Default.BugReport, contentDescription = "Testing Menu")
                                }
                            }
                            // Cleanup duplicates button
                            IconButton(
                                onClick = { viewModel.cleanupDuplicates() },
                                enabled = !uiState.isCleaningDuplicates,
                            ) {
                                if (uiState.isCleaningDuplicates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(Icons.Default.CleaningServices, contentDescription = "Cleanup Duplicates")
                                }
                            }
                            IconButton(onClick = { viewModel.refreshData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    // Cleanup Success Message
                    uiState.cleanupMessage?.let { message ->
                        item(key = "cleanup_message") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.clearCleanupMessage() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons
                    item(key = "action_buttons") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = onGenerateClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate QR")
                            }

                            OutlinedButton(
                                onClick = onScanClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan QR")
                            }
                        }
                    }

                    // Today's Check-ins Section Header
                    item(key = "checkins_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Today's Check-ins",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${uiState.todayCheckIns.size} members checked in today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            TextButton(onClick = onViewAllQRsClick) {
                                Text("View All QRs")
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Loading State
                    if (uiState.isLoading) {
                        item(key = "loading_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // Error State
                    uiState.error?.let { error ->
                        item(key = "error_state") {
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
                    }

                    // Today's Check-ins List or Empty State
                    if (uiState.todayCheckIns.isEmpty() && !uiState.isLoading) {
                        item(key = "empty_state") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No members checked in today",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Member check-ins will appear here when they scan QR codes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }

                    // Today's Check-ins List
                    items(
                        items = uiState.todayCheckIns,
                        key = { attendanceRecord -> attendanceRecord.id },
                    ) { attendanceRecord ->
                        AttendanceListItem(
                            attendanceRecord = attendanceRecord,
                        )
                    }
                }
            }
        },
    )
} 
