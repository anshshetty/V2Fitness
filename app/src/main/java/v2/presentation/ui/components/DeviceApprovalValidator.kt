package v2.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import v2.domain.usecases.DeviceApprovalStatus
import v2.presentation.viewmodels.DeviceApprovalValidatorViewModel

@Composable
fun DeviceApprovalValidator(
    onDeviceNotApproved: () -> Unit,
    content: @Composable () -> Unit,
    viewModel: DeviceApprovalValidatorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val approvalStatus by viewModel.approvalStatus.collectAsState()

    // Initial check and periodic check every 30 seconds for faster detection
    LaunchedEffect(Unit) {
        viewModel.checkApprovalStatus(context) // Initial check
        while (true) {
            delay(30 * 1000) // 30 seconds for faster detection of status changes
            viewModel.checkApprovalStatus(context)
        }
    }

    LaunchedEffect(approvalStatus) {
        if (approvalStatus != null && approvalStatus != DeviceApprovalStatus.APPROVED) {
            onDeviceNotApproved()
        }
    }

    content()
} 
