package v2.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import v2.domain.usecases.CheckDeviceApprovalUseCase
import v2.domain.usecases.DeviceApprovalStatus
import v2.utils.UserPreferences
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        private val _deviceApprovalStatus = MutableStateFlow<DeviceApprovalStatus?>(null)
        val deviceApprovalStatus: StateFlow<DeviceApprovalStatus?> = _deviceApprovalStatus.asStateFlow()

        private val _biometricSetupCompleted = MutableStateFlow<Boolean?>(null)
        val biometricSetupCompleted: StateFlow<Boolean?> = _biometricSetupCompleted.asStateFlow()

        private val _biometricEnabled = MutableStateFlow<Boolean?>(null)
        val biometricEnabled: StateFlow<Boolean?> = _biometricEnabled.asStateFlow()

        init {
            // Load biometric preferences immediately when ViewModel is created
            loadBiometricPreferences()
        }

        fun checkDeviceApproval(context: Context) {
            viewModelScope.launch {
                checkDeviceApprovalUseCase(context)
                    .fold(
                        onSuccess = { status ->
                            Log.d("MainViewModel", "Device approval status updated: $status")
                            _deviceApprovalStatus.value = status

                            // Always load biometric preferences when checking device approval
                            loadBiometricPreferences()
                        },
                        onFailure = { error ->
                            Log.d("MainViewModel", "Device approval check failed: ${error.message}")
                            // Handle error - for now, assume not approved
                            _deviceApprovalStatus.value = DeviceApprovalStatus.PENDING

                            // Still load biometric preferences even on failure
                            loadBiometricPreferences()
                        },
                    )
            }
        }

        fun refreshBiometricPreferences() {
            loadBiometricPreferences()
        }

        private fun loadBiometricPreferences() {
            val setupCompleted = userPreferences.isBiometricSetupCompleted()
            val biometricEnabled = userPreferences.isBiometricEnabled()

            Log.d("MainViewModel", "Loading biometric preferences - setupCompleted: $setupCompleted, enabled: $biometricEnabled")

            _biometricSetupCompleted.value = setupCompleted
            _biometricEnabled.value = biometricEnabled
        }
    } 
