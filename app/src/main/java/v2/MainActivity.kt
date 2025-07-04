package v2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import v2.domain.usecases.DeviceApprovalStatus
import v2.presentation.navigation.AppNavigation
import v2.presentation.viewmodels.MainViewModel
import v2.theme.TemplateTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display and proper window insets handling
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TemplateTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val navController = rememberNavController()

                    val deviceApprovalStatus by mainViewModel.deviceApprovalStatus.collectAsState()
                    val biometricSetupCompleted by mainViewModel.biometricSetupCompleted.collectAsState()
                    val biometricEnabled by mainViewModel.biometricEnabled.collectAsState()

                    // Check device approval when the activity resumes
                    LaunchedEffect(Unit) {
                        mainViewModel.checkDeviceApproval(this@MainActivity)
                    }

                    // Refresh biometric preferences when device approval status changes to APPROVED
                    LaunchedEffect(deviceApprovalStatus) {
                        if (deviceApprovalStatus == DeviceApprovalStatus.APPROVED) {
                            // Force refresh biometric preferences when device gets approved
                            mainViewModel.refreshBiometricPreferences()
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        deviceApprovalStatus = deviceApprovalStatus,
                        biometricSetupCompleted = biometricSetupCompleted,
                        biometricEnabled = biometricEnabled,
                        onRefreshDeviceApproval = {
                            mainViewModel.checkDeviceApproval(this@MainActivity)
                        },
                    )
                }
            }
        }
    }
}
