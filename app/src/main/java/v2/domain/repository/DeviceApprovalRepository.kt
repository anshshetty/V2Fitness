package v2.domain.repository

import kotlinx.coroutines.flow.Flow
import v2.data.models.DeviceApproval

interface DeviceApprovalRepository {
    suspend fun checkDeviceApprovalStatus(deviceId: String): Result<DeviceApproval?>

    suspend fun registerDevice(deviceApproval: DeviceApproval): Result<Unit>

    suspend fun updateLastActiveTime(deviceId: String): Result<Unit>

    fun observeDeviceApprovalStatus(deviceId: String): Flow<DeviceApproval?>
} 
