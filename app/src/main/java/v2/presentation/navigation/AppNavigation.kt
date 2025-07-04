package v2.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import v2.BuildConfig
import v2.domain.usecases.DeviceApprovalStatus
import v2.presentation.ui.screens.BiometricAuthScreen
import v2.presentation.ui.screens.BiometricSetupScreen
import v2.presentation.ui.screens.DashboardScreen
import v2.presentation.ui.screens.DeviceCheckScreen
import v2.presentation.ui.screens.DeviceWaitingScreen
import v2.presentation.ui.screens.GenerateQRScreen
import v2.presentation.ui.screens.QRListScreen
import v2.presentation.ui.screens.ScanQRScreen
import v2.presentation.ui.screens.TestingMenuScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    deviceApprovalStatus: DeviceApprovalStatus?,
    biometricSetupCompleted: Boolean?,
    biometricEnabled: Boolean?,
    onRefreshDeviceApproval: () -> Unit,
) {
    // Determine the correct start destination based on current state
    val startDestination = determineStartDestination(
        deviceApprovalStatus,
        biometricSetupCompleted,
        biometricEnabled,
    )

    // Handle reactive navigation when states change
    LaunchedEffect(deviceApprovalStatus, biometricSetupCompleted, biometricEnabled) {
        Log.d("Navigation", "LaunchedEffect triggered!")
        Log.d(
            "Navigation",
            "States changed - DeviceApproval: $deviceApprovalStatus, BiometricSetup: $biometricSetupCompleted, BiometricEnabled: $biometricEnabled",
        )

        // Get current route
        val currentRoute = navController.currentDestination?.route
        Log.d("Navigation", "Current route: $currentRoute")

        // Only auto-navigate if we're on a "waiting" screen, not if user is actively using the app
        val shouldAutoNavigate = currentRoute in listOf(
            Screen.DeviceCheck.route,
            Screen.DeviceWaiting.route,
        )

        Log.d("Navigation", "Should auto navigate: $shouldAutoNavigate (current route: $currentRoute)")

        if (!shouldAutoNavigate) {
            Log.d("Navigation", "Not on waiting screen, skipping auto-navigation")
            return@LaunchedEffect
        }

        // Handle device approval status changes immediately
        when (deviceApprovalStatus) {
            null -> {
                // Still loading device approval status
                Log.d("Navigation", "Device approval status still loading")
            }

            DeviceApprovalStatus.APPROVED -> {
                // Device is approved, check biometric status
                when {
                    biometricSetupCompleted == null || biometricEnabled == null -> {
                        // Biometric states still loading, navigate to biometric setup as default
                        Log.d("Navigation", "Device approved, biometric states loading - navigating to BiometricSetup")
                        if (currentRoute != Screen.BiometricSetup.route) {
                            navController.navigate(Screen.BiometricSetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    biometricSetupCompleted == false -> {
                        // Need to setup biometric
                        Log.d("Navigation", "Device approved, need biometric setup")
                        if (currentRoute != Screen.BiometricSetup.route) {
                            navController.navigate(Screen.BiometricSetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    biometricEnabled == true -> {
                        // Biometric enabled, require authentication
                        Log.d("Navigation", "Device approved, biometric enabled - navigating to BiometricAuth")
                        if (currentRoute != Screen.BiometricAuth.route) {
                            navController.navigate(Screen.BiometricAuth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    else -> {
                        // Direct to dashboard (biometric disabled or setup completed but not enabled)
                        Log.d("Navigation", "Device approved, biometric disabled - navigating to Dashboard")
                        if (currentRoute != Screen.Dashboard.route) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }

            else -> {
                // Device not approved (pending, rejected, not registered)
                if (currentRoute != Screen.DeviceWaiting.route) {
                    Log.d("Navigation", "Device not approved - navigating to DeviceWaiting")
                    navController.navigate(Screen.DeviceWaiting.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.DeviceCheck.route) {
            DeviceCheckScreen(
                onDeviceApproved = {
                    // Navigation will be handled reactively by LaunchedEffect above
                    Log.d("Navigation", "DeviceCheck.onDeviceApproved - letting reactive navigation handle this")
                },
                onDeviceNotApproved = {
                    // Navigation will be handled reactively by LaunchedEffect above
                    Log.d("Navigation", "DeviceCheck.onDeviceNotApproved - letting reactive navigation handle this")
                },
            )
        }

        composable(Screen.DeviceWaiting.route) {
            DeviceWaitingScreen(
                onDeviceApproved = {
                    // Navigation will be handled reactively by LaunchedEffect above
                    Log.d("Navigation", "DeviceWaiting.onDeviceApproved - letting reactive navigation handle this")
                },
                onRefreshDeviceApproval = onRefreshDeviceApproval,
            )
        }

        composable(Screen.BiometricSetup.route) {
            BiometricSetupScreen(
                onBiometricSetupCompleted = {
                    // Navigate to dashboard after successful setup
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true } // Clear entire back stack
                    }
                },
                onSkipSetup = {
                    // This is no longer used since setup is mandatory
                    // But keeping for compatibility
                },
            )
        }

        composable(Screen.BiometricAuth.route) {
            BiometricAuthScreen(
                onAuthenticationSucceeded = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true } // Clear entire back stack
                    }
                },
                onDeviceCheckRequired = {
                    // When user explicitly requests device verification, disable reactive navigation temporarily
                    navController.navigate(Screen.DeviceCheck.route) {
                        popUpTo(0) { inclusive = true } // Clear entire back stack
                    }
                },
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onGenerateClick = {
                    navController.navigate(Screen.GenerateQR.route)
                },
                onScanClick = {
                    navController.navigate(Screen.ScanQR.route)
                },
                onViewAllQRsClick = {
                    navController.navigate(Screen.QRList.route)
                },
                onDeviceNotApproved = {
                    navController.navigate(Screen.DeviceWaiting.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onTestingMenuClick = if (BuildConfig.ENABLE_TESTING_MENU) {
                    {
                        navController.navigate(Screen.TestingMenu.route)
                    }
                } else {
                    null
                },
            )
        }

        composable(Screen.GenerateQR.route) {
            GenerateQRScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onQRGenerated = {
                    navController.popBackStack()
                },
                onDeviceNotApproved = {
                    navController.navigate(Screen.DeviceWaiting.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ScanQR.route) {
            ScanQRScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onDeviceNotApproved = {
                    navController.navigate(Screen.DeviceWaiting.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.QRList.route) {
            QRListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onDeviceNotApproved = {
                    navController.navigate(Screen.DeviceWaiting.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
            )
        }

        // Testing menu - only available when enabled in BuildConfig
        if (BuildConfig.ENABLE_TESTING_MENU) {
            composable(Screen.TestingMenu.route) {
                TestingMenuScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

sealed class Screen(
    val route: String,
) {
    object DeviceCheck : Screen("device_check")

    object DeviceWaiting : Screen("device_waiting")

    object BiometricAuth : Screen("biometric_auth")

    object BiometricSetup : Screen("biometric_setup")

    object Dashboard : Screen("dashboard")

    object GenerateQR : Screen("generate_qr")

    object ScanQR : Screen("scan_qr")

    object QRList : Screen("qr_list")

    object TestingMenu : Screen("testing_menu")
}

// Helper function to determine the correct start destination
private fun determineStartDestination(
    deviceApprovalStatus: DeviceApprovalStatus?,
    biometricSetupCompleted: Boolean?,
    biometricEnabled: Boolean?,
): String {
    val destination = when {
        // Device not checked or not approved
        deviceApprovalStatus == null -> Screen.DeviceCheck.route
        deviceApprovalStatus != DeviceApprovalStatus.APPROVED -> Screen.DeviceWaiting.route

        // Device approved, check biometric state
        biometricSetupCompleted == null -> Screen.DeviceCheck.route // Still loading
        biometricSetupCompleted == false -> Screen.BiometricSetup.route // Need to setup biometric
        biometricEnabled == true -> Screen.BiometricAuth.route // Biometric enabled, require auth

        // Default to dashboard (biometric setup completed but disabled)
        else -> Screen.Dashboard.route
    }

    Log.d("Navigation", "Start destination: $destination")
    Log.d(
        "Navigation",
        "DeviceApproval: $deviceApprovalStatus, BiometricSetup: $biometricSetupCompleted, BiometricEnabled: $biometricEnabled",
    )

    return destination
}

// Helper function to handle navigation after device approval
private fun handleDeviceApproved(
    navController: NavHostController,
    biometricSetupCompleted: Boolean?,
    biometricEnabled: Boolean?,
) {
    Log.d("Navigation", "Device approved - BiometricSetup: $biometricSetupCompleted, BiometricEnabled: $biometricEnabled")

    when {
        // Need to setup biometric
        biometricSetupCompleted == false -> {
            Log.d("Navigation", "Navigating to BiometricSetup")
            navController.navigate(Screen.BiometricSetup.route) {
                popUpTo(0) { inclusive = true } // Clear entire back stack
            }
        }
        // Biometric enabled, require authentication
        biometricEnabled == true -> {
            Log.d("Navigation", "Navigating to BiometricAuth")
            navController.navigate(Screen.BiometricAuth.route) {
                popUpTo(0) { inclusive = true } // Clear entire back stack
            }
        }
        // Direct to dashboard (biometric disabled or setup completed but not enabled)
        else -> {
            Log.d("Navigation", "Navigating to Dashboard")
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(0) { inclusive = true } // Clear entire back stack
            }
        }
    }
} 
