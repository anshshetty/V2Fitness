package v2.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import v2.utils.DataSeeder
import v2.utils.UserPreferences
import javax.inject.Inject

@HiltViewModel
class TestingMenuViewModel
    @Inject
    constructor(
        private val dataSeeder: DataSeeder,
        private val userPreferences: UserPreferences,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TestingMenuUiState())
        val uiState: StateFlow<TestingMenuUiState> = _uiState.asStateFlow()

        companion object {
            private const val TAG = "TestingMenuViewModel"
        }

        fun seedDummyQRCodes() {
            viewModelScope.launch {
                Log.d(TAG, "Starting to seed dummy QR codes...")
                _uiState.value = _uiState.value.copy(
                    isSeedingQRCodes = true,
                    error = null,
                    successMessage = null,
                )

                try {
                    val result = dataSeeder.seedDummyQRCodes()

                    if (result.isSuccess) {
                        val count = result.getOrThrow()
                        Log.d(TAG, "Successfully seeded $count dummy QR codes")
                        _uiState.value = _uiState.value.copy(
                            isSeedingQRCodes = false,
                            successMessage = "Successfully created $count dummy QR codes",
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e(TAG, "Failed to seed dummy QR codes: $error")
                        _uiState.value = _uiState.value.copy(
                            isSeedingQRCodes = false,
                            error = "Failed to create dummy QR codes: $error",
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while seeding dummy QR codes", e)
                    _uiState.value = _uiState.value.copy(
                        isSeedingQRCodes = false,
                        error = "Error creating dummy QR codes: ${e.message}",
                    )
                }
            }
        }

        fun seedDummyAttendance() {
            viewModelScope.launch {
                Log.d(TAG, "Starting to seed dummy attendance records...")
                _uiState.value = _uiState.value.copy(
                    isSeedingAttendance = true,
                    error = null,
                    successMessage = null,
                )

                try {
                    val result = dataSeeder.seedDummyAttendance()

                    if (result.isSuccess) {
                        val count = result.getOrThrow()
                        Log.d(TAG, "Successfully seeded $count dummy attendance records")
                        _uiState.value = _uiState.value.copy(
                            isSeedingAttendance = false,
                            successMessage = "Successfully created $count dummy attendance records",
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e(TAG, "Failed to seed dummy attendance: $error")
                        _uiState.value = _uiState.value.copy(
                            isSeedingAttendance = false,
                            error = "Failed to create dummy attendance: $error",
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while seeding dummy attendance", e)
                    _uiState.value = _uiState.value.copy(
                        isSeedingAttendance = false,
                        error = "Error creating dummy attendance: ${e.message}",
                    )
                }
            }
        }

        fun seedAllDummyData() {
            viewModelScope.launch {
                Log.d(TAG, "Starting to seed all dummy data...")
                _uiState.value = _uiState.value.copy(
                    isSeedingAllData = true,
                    error = null,
                    successMessage = null,
                )

                try {
                    val result = dataSeeder.seedAllDummyData()

                    if (result.isSuccess) {
                        val (qrCount, attendanceCount) = result.getOrThrow()
                        Log.d(TAG, "Successfully seeded all dummy data: $qrCount QR codes, $attendanceCount attendance records")
                        _uiState.value = _uiState.value.copy(
                            isSeedingAllData = false,
                            successMessage = "Successfully created $qrCount QR codes and $attendanceCount attendance records",
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e(TAG, "Failed to seed all dummy data: $error")
                        _uiState.value = _uiState.value.copy(
                            isSeedingAllData = false,
                            error = "Failed to create dummy data: $error",
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while seeding all dummy data", e)
                    _uiState.value = _uiState.value.copy(
                        isSeedingAllData = false,
                        error = "Error creating dummy data: ${e.message}",
                    )
                }
            }
        }

        fun clearDummyData() {
            viewModelScope.launch {
                Log.d(TAG, "Starting to clear dummy data...")
                _uiState.value = _uiState.value.copy(
                    isClearingData = true,
                    error = null,
                    successMessage = null,
                )

                try {
                    val result = dataSeeder.clearDummyData()

                    if (result.isSuccess) {
                        Log.d(TAG, "Successfully cleared dummy data")
                        _uiState.value = _uiState.value.copy(
                            isClearingData = false,
                            successMessage = "Dummy data cleared successfully",
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e(TAG, "Failed to clear dummy data: $error")
                        _uiState.value = _uiState.value.copy(
                            isClearingData = false,
                            error = "Failed to clear dummy data: $error",
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while clearing dummy data", e)
                    _uiState.value = _uiState.value.copy(
                        isClearingData = false,
                        error = "Error clearing dummy data: ${e.message}",
                    )
                }
            }
        }

        fun clearMessages() {
            _uiState.value = _uiState.value.copy(
                error = null,
                successMessage = null,
            )
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun clearSuccessMessage() {
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }

data class TestingMenuUiState(
    val isSeedingQRCodes: Boolean = false,
    val isSeedingAttendance: Boolean = false,
    val isSeedingAllData: Boolean = false,
    val isClearingData: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
) {
    val isAnyOperationInProgress: Boolean
        get() = isSeedingQRCodes || isSeedingAttendance || isSeedingAllData || isClearingData
} 
