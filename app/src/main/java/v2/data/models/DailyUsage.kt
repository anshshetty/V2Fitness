package v2.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DailyUsage(
    val id: String = "", // format: "YYYY-MM-DD-mobileNumber"
    val date: String = "", // YYYY-MM-DD format
    val mobileNumber: String = "",
    val usedQRIds: List<String> = emptyList(),
    val scanCount: Int = 0,
    val deviceId: String = "", // Device that created this usage record
) 
