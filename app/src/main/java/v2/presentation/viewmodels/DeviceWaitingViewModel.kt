package v2.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import v2.domain.usecases.CheckDeviceApprovalUseCase
import v2.domain.usecases.DeviceApprovalStatus
import javax.inject.Inject

@HiltViewModel
class DeviceWaitingViewModel
    @Inject
    constructor(
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState>(UiState.Waiting)
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        fun refreshApprovalStatus(context: Context) {
            viewModelScope.launch {
                _uiState.value = UiState.Loading

                checkDeviceApprovalUseCase(context)
                    .fold(
                        onSuccess = { status ->
                            when (status) {
                                DeviceApprovalStatus.APPROVED -> {
                                    _uiState.value = UiState.Approved
                                }
                                DeviceApprovalStatus.REJECTED -> {
                                    _uiState.value = UiState.Rejected
                                }
                                DeviceApprovalStatus.PENDING,
                                DeviceApprovalStatus.NOT_REGISTERED,
                                -> {
                                    _uiState.value = UiState.Waiting
                                }
                            }
                        },
                        onFailure = { exception ->
                            _uiState.value = UiState.Error(
                                exception.message ?: "Failed to check approval status",
                            )
                        },
                    )
            }
        }

        sealed class UiState {
            object Waiting : UiState()

            object Loading : UiState()

            object Approved : UiState()

            object Rejected : UiState()

            data class Error(
                val message: String,
            ) : UiState()
        }
    } 
