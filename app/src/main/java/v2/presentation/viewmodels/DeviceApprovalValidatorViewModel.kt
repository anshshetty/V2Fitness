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
class DeviceApprovalValidatorViewModel
    @Inject
    constructor(
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
    ) : ViewModel() {
        private val _approvalStatus = MutableStateFlow<DeviceApprovalStatus?>(null)
        val approvalStatus: StateFlow<DeviceApprovalStatus?> = _approvalStatus.asStateFlow()

        fun checkApprovalStatus(context: Context) {
            viewModelScope.launch {
                checkDeviceApprovalUseCase(context)
                    .fold(
                        onSuccess = { status ->
                            _approvalStatus.value = status
                        },
                        onFailure = {
                            // On error, assume device is not approved for security
                            _approvalStatus.value = DeviceApprovalStatus.PENDING
                        },
                    )
            }
        }
    } 
