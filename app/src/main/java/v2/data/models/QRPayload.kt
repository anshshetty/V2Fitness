package v2.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QRPayload(
    val name: String,
    val mobileNumber: String,
    val timestamp: String, // ISO8601 format
    val expiryDuration: Int, // days
    val qrId: String,
    val version: String = "1.0",
)

sealed class ScanResult {
    data class Success(
        val attendanceRecord: AttendanceRecord,
    ) : ScanResult()

    data class Error(
        val message: String,
        val errorType: ScanErrorType,
    ) : ScanResult()
}

enum class ScanErrorType {
    EXPIRED,
    DISABLED,
    ALREADY_USED,
    INVALID_FORMAT,
    TAMPERED,
    NETWORK_ERROR,
    RATE_LIMITED,
    UNKNOWN,
} 
