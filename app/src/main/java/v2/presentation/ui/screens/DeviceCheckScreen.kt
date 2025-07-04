package v2.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import v2.presentation.viewmodels.DeviceCheckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCheckScreen(
    onDeviceApproved: () -> Unit,
    onDeviceNotApproved: () -> Unit,
    viewModel: DeviceCheckViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkDeviceApproval(context)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is DeviceCheckViewModel.UiState.Approved -> onDeviceApproved()
            is DeviceCheckViewModel.UiState.NotApproved -> onDeviceNotApproved()
            else -> { /* Stay on this screen */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Verification") },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is DeviceCheckViewModel.UiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Verifying device authorization...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                is DeviceCheckViewModel.UiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Verification Failed",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = (uiState as DeviceCheckViewModel.UiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = { viewModel.checkDeviceApproval(context) },
                        ) {
                            Text("Retry")
                        }
                    }
                }

                is DeviceCheckViewModel.UiState.Registering -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Registering device...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "This is a new device. Registering for approval.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> { /* Other states handled by LaunchedEffect */ }
            }
        }
    }
} 
