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
import v2.domain.usecases.CheckDeviceApprovalUseCase
import v2.domain.usecases.DeviceApprovalStatus
import v2.utils.BiometricAvailability
import v2.utils.BiometricUtils
import v2.utils.UserPreferences
import javax.inject.Inject

@HiltViewModel
class BiometricAuthViewModel
    @Inject
    constructor(
        private val biometricUtils: BiometricUtils,
        private val userPreferences: UserPreferences,
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BiometricAuthUiState())
        val uiState: StateFlow<BiometricAuthUiState> = _uiState.asStateFlow()

        fun initiateAuthentication(activity: FragmentActivity) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // First check if device is still approved
                val deviceApprovalResult = checkDeviceApprovalUseCase(activity)
                if (deviceApprovalResult.isFailure || deviceApprovalResult.getOrNull() != DeviceApprovalStatus.APPROVED) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requireDeviceCheck = true,
                    )
                    return@launch
                }

                // Check if biometric is enabled
                if (!userPreferences.isBiometricEnabled()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requireDeviceCheck = true,
                    )
                    return@launch
                }

                // Check biometric availability
                val availability = biometricUtils.isBiometricAvailable()
                if (availability != BiometricAvailability.AVAILABLE) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Biometric authentication is not available. Please use device verification.",
                    )
                    return@launch
                }

                // Automatically trigger biometric prompt
                authenticateWithBiometric(activity)
            }
        }

        fun authenticateWithBiometric(activity: FragmentActivity) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)

                val biometricPrompt = biometricUtils.createBiometricPrompt(
                    fragmentActivity = activity,
                    onAuthenticationSucceeded = {
                        handleAuthenticationSuccess()
                    },
                    onAuthenticationError = { errorCode, errorString ->
                        handleAuthenticationError(errorCode, errorString.toString())
                    },
                    onAuthenticationFailed = {
                        handleAuthenticationFailed()
                    },
                )

                val promptInfo = biometricUtils.createPromptInfo(
                    title = "Authenticate",
                    subtitle = "Use biometric authentication to access the app",
                    negativeButtonText = "Cancel",
                )

                biometricPrompt.authenticate(promptInfo)
            }
        }

        fun retryAuthentication(activity: FragmentActivity) {
            _uiState.value = _uiState.value.copy(error = null)
            authenticateWithBiometric(activity)
        }

        private fun handleAuthenticationSuccess() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    authenticationSucceeded = true,
                    error = null,
                )
            }
        }

        private fun handleAuthenticationError(
            errorCode: Int,
            errorString: String,
        ) {
            viewModelScope.launch {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    -> {
                        // User cancelled - stay on biometric screen but show cancelled state
                        _uiState.value = _uiState.value.copy(
                            authenticationCancelled = true,
                            error = null,
                        )
                    }

                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                    -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Biometric authentication is temporarily locked. Please try again later or use device verification.",
                        )
                    }

                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        _uiState.value = _uiState.value.copy(
                            error = "No biometric credentials enrolled. Please use device verification.",
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Authentication failed: $errorString",
                        )
                    }
                }
            }
        }

        private fun handleAuthenticationFailed() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    error = "Authentication failed. Please try again or use device verification.",
                )
            }
        }

        fun resetCancelledState() {
            _uiState.value = _uiState.value.copy(
                authenticationCancelled = false,
                error = null,
            )
        }

        fun requestDeviceVerification() {
            _uiState.value = _uiState.value.copy(
                requireDeviceCheck = true,
            )
        }
    }

data class BiometricAuthUiState(
    val isLoading: Boolean = false,
    val authenticationSucceeded: Boolean = false,
    val authenticationCancelled: Boolean = false,
    val requireDeviceCheck: Boolean = false,
    val error: String? = null,
) 
