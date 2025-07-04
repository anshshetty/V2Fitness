package v2.presentation.viewmodels

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import v2.data.models.ScanResult
import v2.domain.usecases.ScanQRCodeUseCase
import javax.inject.Inject

@HiltViewModel
class ScanQRViewModel
    @Inject
    constructor(
        private val scanQRCodeUseCase: ScanQRCodeUseCase,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScanQRUiState())
        val uiState: StateFlow<ScanQRUiState> = _uiState.asStateFlow()

        private val deviceId: String by lazy {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"
        }

        // Duplicate prevention
        private var lastScanTime = 0L
        private var lastScannedPayload: String? = null
        private val scanCooldown = 5000L // 5 seconds between scans
        private val sameScanCooldown = 15000L // 15 seconds for same QR code

        fun scanQRCode(encryptedPayload: String) {
            val currentTime = System.currentTimeMillis()
            val isSamePayload = lastScannedPayload == encryptedPayload
            val requiredCooldown = if (isSamePayload) sameScanCooldown else scanCooldown

            // Check if we're still in cooldown period
            if (currentTime - lastScanTime < requiredCooldown) {
                val remainingTime = (requiredCooldown - (currentTime - lastScanTime)) / 1000
                Log.d("ScanQRViewModel", "Scan blocked - cooldown active (${remainingTime}s remaining)")

                // Show a brief message about cooldown
                _uiState.value = _uiState.value.copy(
                    error = if (isSamePayload) {
                        "Please wait ${remainingTime}s before scanning the same QR code again"
                    } else {
                        "Please wait ${remainingTime}s before scanning another QR code"
                    },
                )
                return
            }

            // Check if already scanning
            if (_uiState.value.isScanning) {
                Log.d("ScanQRViewModel", "Scan blocked - already processing a scan")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isScanning = true, error = null)
                lastScanTime = currentTime
                lastScannedPayload = encryptedPayload

                Log.d("ScanQRViewModel", "Processing QR scan: ${encryptedPayload.take(20)}...")

                val result = scanQRCodeUseCase(encryptedPayload, deviceId)
                result
                    .onSuccess { scanResult ->
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            scanResult = scanResult,
                        )
                        Log.d("ScanQRViewModel", "Scan completed successfully")
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            error = e.message,
                        )
                        Log.e("ScanQRViewModel", "Scan failed: ${e.message}")
                    }
            }
        }

        fun clearResult() {
            _uiState.value = _uiState.value.copy(scanResult = null, error = null)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

data class ScanQRUiState(
    val isScanning: Boolean = false,
    val scanResult: ScanResult? = null,
    val error: String? = null,
) 
