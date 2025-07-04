package v2.data.models

import com.google.firebase.Timestamp
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QRCodeModel(
    val qrId: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiryDuration: Int = 0, // days
    val status: QRStatus = QRStatus.ACTIVE,
    val usageCount: Int = 0,
    val lastUsedAt: Timestamp? = null,
    val encryptedPayload: String = "",
    val salt: String = "",
    val deviceId: String = "", // Device that created this QR code
) {
    fun isExpired(): Boolean {
        val expiryTime = createdAt.toDate().time + (expiryDuration * 24 * 60 * 60 * 1000L)
        return System.currentTimeMillis() > expiryTime
    }

    fun isActive(): Boolean = status == QRStatus.ACTIVE && !isExpired()

    fun getCurrentStatus(): QRStatus =
        when {
            status == QRStatus.DISABLED -> QRStatus.DISABLED
            isExpired() -> QRStatus.EXPIRED
            usageCount > 0 && isSameDay(lastUsedAt) -> QRStatus.USED
            else -> status
        }

    private fun isSameDay(timestamp: Timestamp?): Boolean {
        if (timestamp == null) return false
        val today = System.currentTimeMillis()
        val lastUsed = timestamp.toDate().time
        val dayInMillis = 24 * 60 * 60 * 1000
        return (today - lastUsed) < dayInMillis
    }
}

enum class QRStatus {
    ACTIVE,
    USED,
    EXPIRED,
    DISABLED,
} 
