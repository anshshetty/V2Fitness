package v2.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import v2.data.models.DeviceApproval

object DeviceUtils {
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    fun createDeviceApproval(context: Context): DeviceApproval =
        DeviceApproval(
            deviceId = getDeviceId(context),
            isApproved = false,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = "1.0",
            deviceStatus = "pending",
        )

    fun getDeviceInfo(context: Context): Map<String, String> =
        mapOf(
            "Device ID" to getDeviceId(context),
            "Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Android Version" to Build.VERSION.RELEASE,
            "App Version" to "1.0",
        )
} 
