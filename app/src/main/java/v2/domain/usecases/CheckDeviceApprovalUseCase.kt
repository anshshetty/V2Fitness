package v2.domain.usecases

import android.content.Context
import v2.domain.repository.DeviceApprovalRepository
import v2.utils.DeviceUtils
import v2.utils.UserPreferences
import javax.inject.Inject

class CheckDeviceApprovalUseCase
    @Inject
    constructor(
        private val deviceApprovalRepository: DeviceApprovalRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(
            context: Context,
            forceRefresh: Boolean = false,
        ): Result<DeviceApprovalStatus> {
            val deviceId = DeviceUtils.getDeviceId(context)

            // For critical operations (like QR generation), always check Firebase
            // For periodic checks, use cache if available and recent
            if (!forceRefresh) {
                val cachedStatus = userPreferences.getCachedDeviceApprovalStatus()
                val cacheTimestamp = userPreferences.getDeviceApprovalCacheTimestamp()
                val currentTime = System.currentTimeMillis()

                // Use cached status if it's less than 5 minutes old and device was approved
                // Reduced cache time for better real-time detection of status changes
                if (cachedStatus == "approved" && (currentTime - cacheTimestamp) < 5 * 60 * 1000) {
                    return Result.success(DeviceApprovalStatus.APPROVED)
                }
            }

            // Check with Firebase
            return deviceApprovalRepository
                .checkDeviceApprovalStatus(deviceId)
                .fold(
                    onSuccess = { deviceApproval ->
                        val currentTime = System.currentTimeMillis()
                        when {
                            deviceApproval == null -> {
                                // Device not registered, clear any cached status
                                userPreferences.clearDeviceApprovalCache()
                                Result.success(DeviceApprovalStatus.NOT_REGISTERED)
                            }

                            deviceApproval.deviceStatus == "approved" -> {
                                // Cache the approval status
                                userPreferences.cacheDeviceApprovalStatus("approved", currentTime)
                                // Update last active time
                                deviceApprovalRepository.updateLastActiveTime(deviceId)
                                Result.success(DeviceApprovalStatus.APPROVED)
                            }
                            deviceApproval.deviceStatus == "rejected" -> {
                                // Clear cache for rejected devices to ensure immediate detection
                                userPreferences.clearDeviceApprovalCache()
                                Result.success(DeviceApprovalStatus.REJECTED)
                            }
                            else -> {
                                // Clear cache for pending devices
                                userPreferences.clearDeviceApprovalCache()
                                Result.success(DeviceApprovalStatus.PENDING)
                            }
                        }
                    },
                    onFailure = { exception ->
                        // If network fails and we have cached approval, use it
                        val cachedStatus = userPreferences.getCachedDeviceApprovalStatus()
                        if (cachedStatus == "approved") {
                            Result.success(DeviceApprovalStatus.APPROVED)
                        } else {
                            Result.failure(exception)
                        }
                    },
                )
        }
    }

enum class DeviceApprovalStatus {
    NOT_REGISTERED,
    PENDING,
    APPROVED,
    REJECTED,
}
