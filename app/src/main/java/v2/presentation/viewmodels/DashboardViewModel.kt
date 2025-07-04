package v2.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import v2.data.models.AttendanceRecord
import v2.domain.repository.QRCodeRepository
import v2.domain.usecases.CleanupDuplicateAttendanceUseCase
import v2.utils.UserPreferences
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val repository: QRCodeRepository,
        private val userPreferences: UserPreferences,
        private val cleanupDuplicateAttendanceUseCase: CleanupDuplicateAttendanceUseCase,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DashboardUiState())
        val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

        init {
            observeDashboardData()
        }

        private fun observeDashboardData() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    Log.d("DashboardViewModel", "Loading all users' check-ins for gym staff")

                    // Only observe today's check-ins from ALL users - no personal stats needed
                    repository
                        .observeTodayAttendanceAllUsers()
                        .catch { e ->
                            Log.e("DashboardViewModel", "Error observing check-ins", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load check-ins: ${e.message}",
                            )
                        }.collect { checkIns ->
                            Log.d("DashboardViewModel", "Received ${checkIns.size} check-ins for gym staff")

                            checkIns.forEach { record ->
                                Log.d("DashboardViewModel", "Check-in: ${record.name} (${record.mobileNumber}) at ${record.scanTime}")
                            }

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                todayCheckIns = checkIns,
                                error = null,
                            )
                        }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Exception in observeDashboardData", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message,
                    )
                }
            }
        }

        fun cleanupDuplicates() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isCleaningDuplicates = true)

                try {
                    Log.d("DashboardViewModel", "Starting duplicate cleanup")
                    val result = cleanupDuplicateAttendanceUseCase.cleanupDuplicates()

                    result
                        .onSuccess { deletedCount ->
                            Log.d("DashboardViewModel", "Cleanup completed: $deletedCount duplicates removed")
                            _uiState.value = _uiState.value.copy(
                                isCleaningDuplicates = false,
                                cleanupMessage = "Removed $deletedCount duplicate attendance records",
                            )
                            // Refresh data to show updated list
                            refreshData()
                        }.onFailure { e ->
                            Log.e("DashboardViewModel", "Cleanup failed", e)
                            _uiState.value = _uiState.value.copy(
                                isCleaningDuplicates = false,
                                error = "Failed to cleanup duplicates: ${e.message}",
                            )
                        }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Exception during cleanup", e)
                    _uiState.value = _uiState.value.copy(
                        isCleaningDuplicates = false,
                        error = "Cleanup error: ${e.message}",
                    )
                }
            }
        }

        fun getDuplicates() {
            viewModelScope.launch {
                try {
                    val result = cleanupDuplicateAttendanceUseCase.getDuplicates()
                    result
                        .onSuccess { duplicates ->
                            Log.d("DashboardViewModel", "Found ${duplicates.size} duplicate records")
                            _uiState.value = _uiState.value.copy(duplicateRecords = duplicates)
                        }.onFailure { e ->
                            Log.e("DashboardViewModel", "Failed to get duplicates", e)
                            _uiState.value = _uiState.value.copy(error = "Failed to get duplicates: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Exception getting duplicates", e)
                    _uiState.value = _uiState.value.copy(error = "Error: ${e.message}")
                }
            }
        }

        // Remove test data seeding for production

        fun refreshData() {
            Log.d("DashboardViewModel", "Manual refresh triggered")
            // Data is automatically refreshed via Flow observers, but we can trigger a manual refresh if needed
            observeDashboardData()
        }

        fun clearCleanupMessage() {
            _uiState.value = _uiState.value.copy(cleanupMessage = null)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

data class DashboardUiState(
    val isLoading: Boolean = false,
    val todayCheckIns: List<AttendanceRecord> = emptyList(),
    val duplicateRecords: List<AttendanceRecord> = emptyList(),
    val isCleaningDuplicates: Boolean = false,
    val cleanupMessage: String? = null,
    val error: String? = null,
) 
