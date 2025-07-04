package v2.presentation.viewmodels

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import v2.utils.BiometricAvailability
import v2.utils.BiometricUtils
import v2.utils.UserPreferences
import javax.inject.Inject

@HiltViewModel
class BiometricSetupViewModel
    @Inject
    constructor(
        private val biometricUtils: BiometricUtils,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BiometricSetupUiState())
        val uiState: StateFlow<BiometricSetupUiState> = _uiState.asStateFlow()

        fun checkBiometricAvailability() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val availability = biometricUtils.isBiometricAvailable()

                when (availability) {
                    BiometricAvailability.AVAILABLE -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = true,
                            error = null,
                        )
                    }

                    BiometricAvailability.NOT_ENROLLED -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "No biometric authentication is enrolled on this device. Please set up fingerprint or face recognition in your device settings first.",
                        )
                    }

                    BiometricAvailability.NO_HARDWARE -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "This device does not have biometric hardware.",
                        )
                    }

                    BiometricAvailability.HARDWARE_UNAVAILABLE -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "Biometric hardware is currently unavailable.",
                        )
                    }

                    BiometricAvailability.SECURITY_UPDATE_REQUIRED -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "A security update is required to use biometric authentication.",
                        )
                    }

                    BiometricAvailability.UNSUPPORTED -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "Biometric authentication is not supported on this device.",
                        )
                    }

                    BiometricAvailability.UNKNOWN -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            biometricAvailable = false,
                            error = "Unable to determine biometric availability.",
                        )
                    }
                }
            }
        }

        fun setupBiometric(activity: FragmentActivity) {
            viewModelScope.launch {
                val biometricPrompt = biometricUtils.createBiometricPrompt(
                    fragmentActivity = activity,
                    onAuthenticationSucceeded = {
                        handleBiometricSetupSuccess()
                    },
                    onAuthenticationError = { errorCode, errorString ->
                        handleBiometricSetupError(errorCode, errorString.toString())
                    },
                    onAuthenticationFailed = {
                        handleBiometricSetupFailed()
                    },
                )

                val promptInfo = biometricUtils.createPromptInfo(
                    title = "Setup Biometric Authentication",
                    subtitle = "Authenticate to enable biometric login",
                    negativeButtonText = "Cancel",
                )

                biometricPrompt.authenticate(promptInfo)
            }
        }

        private fun handleBiometricSetupSuccess() {
            viewModelScope.launch {
                // Mark biometric as set up and enabled
                userPreferences.setBiometricSetupCompleted(true)
                userPreferences.setBiometricEnabled(true)

                _uiState.value = _uiState.value.copy(
                    setupCompleted = true,
                    error = null,
                )
            }
        }

        private fun handleBiometricSetupError(
            errorCode: Int,
            errorString: String,
        ) {
            viewModelScope.launch {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    -> {
                        // User cancelled, don't show error
                        _uiState.value = _uiState.value.copy(error = null)
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Setup failed: $errorString",
                        )
                    }
                }
            }
        }

        private fun handleBiometricSetupFailed() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    error = "Authentication failed. Please try again.",
                )
            }
        }
    }

data class BiometricSetupUiState(
    val isLoading: Boolean = false,
    val biometricAvailable: Boolean = false,
    val setupCompleted: Boolean = false,
    val error: String? = null,
) 
