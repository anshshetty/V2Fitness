package v2.domain.repository

import kotlinx.coroutines.flow.Flow
import v2.data.models.AttendanceRecord
import v2.data.models.DailyUsage
import v2.data.models.QRCodeModel
import v2.data.models.ScanResult

interface QRCodeRepository {
    // QR Code Management
    suspend fun generateQRCode(
        name: String,
        mobileNumber: String,
        expiryDuration: Int,
    ): Result<QRCodeModel>

    suspend fun getQRCodes(mobileNumber: String): Flow<List<QRCodeModel>>

    // NEW: Get all QR codes from all users (for admin/scanner view)
    suspend fun getAllQRCodes(): Flow<List<QRCodeModel>>

    // NEW: Get recent QR codes from all users (for dashboard recent section)
    suspend fun getRecentQRCodes(limit: Int = 5): Flow<List<QRCodeModel>>

    suspend fun getQRCodeById(qrId: String): Result<QRCodeModel?>

    suspend fun updateQRCode(qrCode: QRCodeModel): Result<Unit>

    suspend fun disableQRCode(qrId: String): Result<Unit>

    suspend fun extendQRCode(
        qrId: String,
        additionalDays: Int,
    ): Result<Unit>

    // Scanning & Attendance
    suspend fun scanQRCode(
        encryptedPayload: String,
        deviceId: String,
    ): Result<ScanResult>

    suspend fun getAttendanceRecords(mobileNumber: String): Flow<List<AttendanceRecord>>

    suspend fun getTodayAttendance(mobileNumber: String): Result<List<AttendanceRecord>>

    // NEW: Real-time Flow version for user's today attendance
    suspend fun observeTodayAttendance(mobileNumber: String): Flow<List<AttendanceRecord>>

    // NEW: Get today's attendance records from all users (for dashboard check-ins section)
    suspend fun getTodayAttendanceAllUsers(): Result<List<AttendanceRecord>>

    // NEW: Real-time Flow version for dashboard updates
    suspend fun observeTodayAttendanceAllUsers(): Flow<List<AttendanceRecord>>

    suspend fun addAttendanceRecord(attendanceRecord: AttendanceRecord): Result<Unit>

    // Daily Usage Tracking
    suspend fun getDailyUsage(
        date: String,
        mobileNumber: String,
    ): Result<DailyUsage?>

    suspend fun updateDailyUsage(dailyUsage: DailyUsage): Result<Unit>

    // Analytics
    suspend fun getUsageStats(
        mobileNumber: String,
        days: Int,
    ): Result<Map<String, Int>>

    suspend fun getActiveQRCount(mobileNumber: String): Result<Int>

    // Duplicate Management
    suspend fun cleanupDuplicateAttendance(): Result<Int>

    suspend fun getDuplicateAttendanceRecords(): Result<List<AttendanceRecord>>

    // Testing Support (Debug builds only)
    suspend fun clearDummyData(): Result<Unit>
} 
