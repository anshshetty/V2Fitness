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
import v2.domain.usecases.RegisterDeviceUseCase
import javax.inject.Inject

@HiltViewModel
class DeviceCheckViewModel
    @Inject
    constructor(
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
        private val registerDeviceUseCase: RegisterDeviceUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        fun checkDeviceApproval(context: Context) {
            viewModelScope.launch {
                _uiState.value = UiState.Loading

                checkDeviceApprovalUseCase(context)
                    .fold(
                        onSuccess = { status ->
                            when (status) {
                                DeviceApprovalStatus.APPROVED -> {
                                    _uiState.value = UiState.Approved
                                }
                                DeviceApprovalStatus.PENDING -> {
                                    _uiState.value = UiState.NotApproved
                                }
                                DeviceApprovalStatus.REJECTED -> {
                                    _uiState.value = UiState.NotApproved
                                }
                                DeviceApprovalStatus.NOT_REGISTERED -> {
                                    registerNewDevice(context)
                                }
                            }
                        },
                        onFailure = { exception ->
                            _uiState.value = UiState.Error(
                                exception.message ?: "Failed to check device approval status",
                            )
                        },
                    )
            }
        }

        private fun registerNewDevice(context: Context) {
            viewModelScope.launch {
                _uiState.value = UiState.Registering

                registerDeviceUseCase(context)
                    .fold(
                        onSuccess = {
                            _uiState.value = UiState.NotApproved
                        },
                        onFailure = { exception ->
                            _uiState.value = UiState.Error(
                                "Failed to register device: ${exception.message}",
                            )
                        },
                    )
            }
        }

        sealed class UiState {
            object Loading : UiState()

            object Registering : UiState()

            object Approved : UiState()

            object NotApproved : UiState()

            data class Error(
                val message: String,
            ) : UiState()
        }
    } 
