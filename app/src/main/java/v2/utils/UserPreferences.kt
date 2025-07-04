package v2.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs: SharedPreferences = context.getSharedPreferences(
            "v2_fitness_prefs",
            Context.MODE_PRIVATE,
        )

        companion object {
            private const val KEY_MOBILE_NUMBER = "mobile_number"
            private const val KEY_USER_NAME = "user_name"
            private const val KEY_DEVICE_APPROVAL_STATUS = "device_approval_status"
            private const val KEY_DEVICE_APPROVAL_CACHE_TIMESTAMP = "device_approval_cache_timestamp"
            private const val KEY_BIOMETRIC_SETUP_COMPLETED = "biometric_setup_completed"
            private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        }

        fun getMobileNumber(): String? = prefs.getString(KEY_MOBILE_NUMBER, null)

        fun setMobileNumber(mobileNumber: String) {
            prefs
                .edit()
                .putString(KEY_MOBILE_NUMBER, mobileNumber)
                .apply()
        }

        fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

        fun setUserName(name: String) {
            prefs
                .edit()
                .putString(KEY_USER_NAME, name)
                .apply()
        }

        fun isValidMobileNumber(mobileNumber: String): Boolean = mobileNumber.matches(Regex("^[0-9]{10}$"))

        fun clearUserData() {
            prefs
                .edit()
                .remove(KEY_MOBILE_NUMBER)
                .remove(KEY_USER_NAME)
                .apply()
        }

        fun getCachedDeviceApprovalStatus(): String? = prefs.getString(KEY_DEVICE_APPROVAL_STATUS, null)

        fun getDeviceApprovalCacheTimestamp(): Long = prefs.getLong(KEY_DEVICE_APPROVAL_CACHE_TIMESTAMP, 0L)

        fun cacheDeviceApprovalStatus(
            status: String,
            timestamp: Long,
        ) {
            prefs
                .edit()
                .putString(KEY_DEVICE_APPROVAL_STATUS, status)
                .putLong(KEY_DEVICE_APPROVAL_CACHE_TIMESTAMP, timestamp)
                .apply()
        }

        fun clearDeviceApprovalCache() {
            prefs
                .edit()
                .remove(KEY_DEVICE_APPROVAL_STATUS)
                .remove(KEY_DEVICE_APPROVAL_CACHE_TIMESTAMP)
                .apply()
        }

        // Method to manually clear cache for testing/debugging
        fun clearAllCache() {
            prefs
                .edit()
                .remove(KEY_DEVICE_APPROVAL_STATUS)
                .remove(KEY_DEVICE_APPROVAL_CACHE_TIMESTAMP)
                .apply()
        }

        fun isBiometricSetupCompleted(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_SETUP_COMPLETED, false)

        fun setBiometricSetupCompleted(completed: Boolean) {
            prefs
                .edit()
                .putBoolean(KEY_BIOMETRIC_SETUP_COMPLETED, completed)
                .apply()
        }

        fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

        fun setBiometricEnabled(enabled: Boolean) {
            prefs
                .edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
                .apply()
        }

        fun setCachedDeviceApprovalStatus(status: String) {
            prefs
                .edit()
                .putString(KEY_DEVICE_APPROVAL_STATUS, status)
                .apply()
        }
    } 
