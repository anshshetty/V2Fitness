package v2.data.models

import com.google.firebase.Timestamp

data class DeviceApproval(
    val deviceId: String = "",
    val isApproved: Boolean = false,
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val registrationDate: Timestamp = Timestamp.now(),
    val lastActiveDate: Timestamp = Timestamp.now(),
    val deviceStatus: String = "pending", // "pending", "approved", "rejected"
)

enum class DeviceStatus {
    PENDING,
    APPROVED,
    REJECTED,
} 
