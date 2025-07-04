package v2.data.models

import com.google.firebase.Timestamp
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: String = "",
    val mobileNumber: String = "",
    val name: String = "",
    val scanTime: Timestamp = Timestamp.now(),
    val qrId: String = "",
    val deviceId: String = "",
    val location: String? = null,
    val scannerInfo: String = "",
) 
