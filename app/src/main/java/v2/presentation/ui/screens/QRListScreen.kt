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
import v2.presentation.ui.components.DeviceApprovalValidator
import v2.presentation.ui.components.DisableQRDialog
import v2.presentation.ui.components.ExtendQRDialog
import v2.presentation.ui.components.QRCodeListItem
import v2.presentation.ui.components.QRFilterDialog
import v2.presentation.viewmodels.QRListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRListScreen(
    onBackClick: () -> Unit,
    onDeviceNotApproved: () -> Unit,
    viewModel: QRListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DeviceApprovalValidator(
        onDeviceNotApproved = onDeviceNotApproved,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "All QR Codes",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                            IconButton(onClick = { viewModel.showFilterDialog() }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        placeholder = { Text("Search by name or mobile number...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                    )

                    // Filter Status Indicator
                    if (uiState.selectedStatusFilter != v2.presentation.viewmodels.QRStatusFilter.ALL) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Filtered by: ${uiState.selectedStatusFilter.name.lowercase().replaceFirstChar {
                                        it.uppercase()
                                    }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = { viewModel.updateStatusFilter(v2.presentation.viewmodels.QRStatusFilter.ALL) },
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
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

                        // QR Codes List or Empty State
                        if (uiState.filteredQrCodes.isEmpty() && !uiState.isLoading) {
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
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.outline,
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (uiState.searchQuery.isNotEmpty() ||
                                                uiState.selectedStatusFilter != v2.presentation.viewmodels.QRStatusFilter.ALL
                                            ) {
                                                "No QR codes match your filters"
                                            } else {
                                                "No QR codes found"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = if (uiState.searchQuery.isNotEmpty() ||
                                                uiState.selectedStatusFilter != v2.presentation.viewmodels.QRStatusFilter.ALL
                                            ) {
                                                "Try adjusting your search or filter criteria"
                                            } else {
                                                "QR codes from all users will appear here"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = uiState.filteredQrCodes,
                                key = { qrCode -> qrCode.qrId },
                            ) { qrCode ->
                                QRCodeListItem(
                                    qrCode = qrCode,
                                    onExtendClick = { viewModel.showExtendDialog(it) },
                                    onDisableClick = { viewModel.showDisableDialog(it) },
                                    onShareClick = { viewModel.shareQRCode(it) },
                                    onWhatsAppShareClick = { viewModel.shareQRCodeOnWhatsApp(it) },
                                )
                            }
                        }
                    }
                }
            }

            // Dialogs
            uiState.selectedQRCode?.let { selectedQRCode ->
                if (uiState.showExtendDialog) {
                    ExtendQRDialog(
                        qrCode = selectedQRCode,
                        onDismiss = { viewModel.hideExtendDialog() },
                        onConfirm = { days -> viewModel.extendQRCode(days) },
                    )
                }

                if (uiState.showDisableDialog) {
                    DisableQRDialog(
                        qrCode = selectedQRCode,
                        onDismiss = { viewModel.hideDisableDialog() },
                        onConfirm = { viewModel.disableQRCode() },
                    )
                }
            }

            // Filter Dialog
            if (uiState.showFilterDialog) {
                QRFilterDialog(
                    selectedFilter = uiState.selectedStatusFilter,
                    onFilterSelected = { filter ->
                        viewModel.updateStatusFilter(filter)
                        viewModel.hideFilterDialog()
                    },
                    onDismiss = { viewModel.hideFilterDialog() },
                )
            }
        },
    )
} 
