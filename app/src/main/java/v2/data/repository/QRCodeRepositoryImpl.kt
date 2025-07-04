package v2.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import v2.data.models.*
import v2.domain.repository.QRCodeRepository
import v2.utils.CryptoUtils
import v2.utils.DeviceUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val moshi: Moshi,
        @ApplicationContext private val context: Context,
    ) : QRCodeRepository {
        private val qrCodesCollection = firestore.collection("qr_codes")
        private val attendanceCollection = firestore.collection("attendance")
        private val dailyUsageCollection = firestore.collection("daily_usage")

        private val deviceId: String by lazy {
            DeviceUtils.getDeviceId(context)
        }

        override suspend fun generateQRCode(
            name: String,
            mobileNumber: String,
            expiryDuration: Int,
        ): Result<QRCodeModel> {
            return try {
                // Validate mobile number exists and is not empty
                if (mobileNumber.isBlank() || !mobileNumber.matches(Regex("^[0-9]{10}$"))) {
                    return Result.failure(Exception("Invalid mobile number. Must be 10 digits."))
                }

                // Check for existing QR codes for this mobile number
                val existingQRCodesSnapshot = qrCodesCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .get()
                    .await()

                val existingQRCodes = existingQRCodesSnapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(QRCodeModel::class.java)
                    }.sortedByDescending { it.createdAt.toDate() } // Sort in memory instead of in query

                // Check if there's already an active QR code
                val activeQRCode = existingQRCodes.find { it.isActive() }
                if (activeQRCode != null) {
                    return Result.failure(
                        Exception(
                            "An active QR code already exists for this mobile number. Please disable it first or wait for it to expire.",
                        ),
                    )
                }

                // Check if there's a disabled QR code that can be reactivated
                val disabledQRCode = existingQRCodes.find { it.status == QRStatus.DISABLED }
                if (disabledQRCode != null) {
                    // Update existing disabled QR code to active with new expiry
                    val updatedQRCode = disabledQRCode.copy(
                        name = name, // Update name in case it changed
                        status = QRStatus.ACTIVE,
                        expiryDuration = expiryDuration,
                        createdAt = Timestamp.now(), // Reset creation time for new expiry calculation
                        usageCount = 0, // Reset usage count
                        lastUsedAt = null, // Reset last used time
                    )

                    qrCodesCollection.document(disabledQRCode.qrId).set(updatedQRCode).await()
                    return Result.success(updatedQRCode)
                }

                // No existing QR code found, create a new one
                val qrId = UUID.randomUUID().toString()
                val salt = CryptoUtils.generateSalt()
                val password = CryptoUtils.generateSecurePassword()

                // Create payload
                val payload = QRPayload(
                    name = name,
                    mobileNumber = mobileNumber,
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                    expiryDuration = expiryDuration,
                    qrId = qrId,
                )

                // Serialize and encrypt payload
                val payloadJson = moshi.adapter(QRPayload::class.java).toJson(payload)
                val encryptedPayload = CryptoUtils.encrypt(payloadJson, password, salt).getOrThrow()

                val qrCode = QRCodeModel(
                    qrId = qrId,
                    name = name,
                    mobileNumber = mobileNumber,
                    createdAt = Timestamp.now(),
                    expiryDuration = expiryDuration,
                    encryptedPayload = encryptedPayload,
                    salt = salt,
                    deviceId = deviceId,
                )

                qrCodesCollection.document(qrId).set(qrCode).await()
                Result.success(qrCode)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getQRCodes(mobileNumber: String): Flow<List<QRCodeModel>> =
            callbackFlow {
                val listener = qrCodesCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val qrCodes = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(QRCodeModel::class.java)
                            }
                            trySend(qrCodes)
                        }
                    }

                awaitClose { listener.remove() }
            }

        override suspend fun getAllQRCodes(): Flow<List<QRCodeModel>> =
            callbackFlow {
                val listener = qrCodesCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val qrCodes = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(QRCodeModel::class.java)
                            }
                            trySend(qrCodes)
                        }
                    }

                awaitClose { listener.remove() }
            }

        override suspend fun getRecentQRCodes(limit: Int): Flow<List<QRCodeModel>> =
            callbackFlow {
                val listener = qrCodesCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val qrCodes = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(QRCodeModel::class.java)
                            }
                            trySend(qrCodes)
                        }
                    }

                awaitClose { listener.remove() }
            }

        override suspend fun getQRCodeById(qrId: String): Result<QRCodeModel?> =
            try {
                val document = qrCodesCollection.document(qrId).get().await()
                val qrCode = document.toObject(QRCodeModel::class.java)
                Result.success(qrCode)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun updateQRCode(qrCode: QRCodeModel): Result<Unit> =
            try {
                qrCodesCollection.document(qrCode.qrId).set(qrCode).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun disableQRCode(qrId: String): Result<Unit> =
            try {
                qrCodesCollection
                    .document(qrId)
                    .update("status", "DISABLED")
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun extendQRCode(
            qrId: String,
            additionalDays: Int,
        ): Result<Unit> {
            return try {
                val qrCodeResult = getQRCodeById(qrId)
                if (qrCodeResult.isFailure) {
                    return Result.failure(qrCodeResult.exceptionOrNull()!!)
                }

                val qrCode = qrCodeResult.getOrNull()
                if (qrCode == null) {
                    return Result.failure(Exception("QR Code not found"))
                }

                val updatedQRCode = qrCode.copy(
                    expiryDuration = qrCode.expiryDuration + additionalDays,
                )

                return updateQRCode(updatedQRCode)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun scanQRCode(
            encryptedPayload: String,
            deviceId: String,
        ): Result<ScanResult> {
            return try {
                Log.d("QRCodeRepository", "Scanning QR code with payload: $encryptedPayload")

                // Try to find QR code by encrypted payload
                val snapshot = qrCodesCollection
                    .whereEqualTo("encryptedPayload", encryptedPayload)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    Log.w("QRCodeRepository", "No QR code found for payload: $encryptedPayload")
                    return Result.success(ScanResult.Error("Invalid QR code", ScanErrorType.INVALID_FORMAT))
                }

                val qrCode = snapshot.documents.first().toObject(QRCodeModel::class.java)
                    ?: return Result.success(ScanResult.Error("Invalid QR code", ScanErrorType.INVALID_FORMAT))

                Log.d("QRCodeRepository", "Found QR code: ${qrCode.name} (${qrCode.mobileNumber})")

                // Check if QR code is active
                if (!qrCode.isActive()) {
                    Log.w("QRCodeRepository", "QR code is not active: ${qrCode.status}")
                    return when {
                        qrCode.status == QRStatus.DISABLED -> Result.success(
                            ScanResult.Error("QR code is disabled", ScanErrorType.DISABLED),
                        )
                        qrCode.isExpired() -> Result.success(ScanResult.Error("QR code has expired", ScanErrorType.EXPIRED))
                        else -> Result.success(ScanResult.Error("QR code is not active", ScanErrorType.UNKNOWN))
                    }
                }

                // Check for recent duplicate scans (within last 30 seconds)
                val recentScanCheck = checkRecentDuplicateScans(qrCode.mobileNumber, qrCode.qrId)
                if (recentScanCheck.isFailure) {
                    val errorMessage = recentScanCheck.exceptionOrNull()?.message ?: "Recent duplicate scan detected"
                    Log.w("QRCodeRepository", errorMessage)
                    return Result.success(ScanResult.Error(errorMessage, ScanErrorType.RATE_LIMITED))
                }

                // Check if already used today
                val todayAttendance = getTodayAttendance(qrCode.mobileNumber)
                if (todayAttendance.isSuccess) {
                    val todayRecords = todayAttendance.getOrNull() ?: emptyList()
                    Log.d("QRCodeRepository", "Found ${todayRecords.size} today's attendance records for ${qrCode.mobileNumber}")
                    if (todayRecords.any { it.qrId == qrCode.qrId }) {
                        Log.w("QRCodeRepository", "QR code already used today")
                        return Result.success(ScanResult.Error("QR code already used today", ScanErrorType.ALREADY_USED))
                    }
                }

                // Create attendance record
                val attendanceRecord = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    mobileNumber = qrCode.mobileNumber,
                    name = qrCode.name,
                    scanTime = Timestamp.now(),
                    qrId = qrCode.qrId,
                    deviceId = deviceId,
                    scannerInfo = "Android Scanner",
                )

                Log.d("QRCodeRepository", "Creating attendance record: ${attendanceRecord.name} at ${attendanceRecord.scanTime}")

                // Save attendance record
                attendanceCollection.document(attendanceRecord.id).set(attendanceRecord).await()

                Log.d("QRCodeRepository", "Attendance record saved successfully with ID: ${attendanceRecord.id}")

                // Update QR code usage
                val updatedQRCode = qrCode.copy(
                    usageCount = qrCode.usageCount + 1,
                    lastUsedAt = Timestamp.now(),
                )
                updateQRCode(updatedQRCode)

                Log.d("QRCodeRepository", "QR code usage updated, returning success")
                Result.success(ScanResult.Success(attendanceRecord))
            } catch (e: Exception) {
                Log.e("QRCodeRepository", "Error scanning QR code", e)
                Result.success(ScanResult.Error("Network error: ${e.message}", ScanErrorType.NETWORK_ERROR))
            }
        }

        /**
         * Check for recent duplicate scans within the last 30 seconds
         * This prevents rapid successive scans of the same QR code
         */
        private suspend fun checkRecentDuplicateScans(
            mobileNumber: String,
            qrId: String,
        ): Result<Unit> {
            return try {
                val thirtySecondsAgo = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.SECOND, -30)
                    }.time

                val recentScans = attendanceCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .whereEqualTo("qrId", qrId)
                    .whereGreaterThan("scanTime", Timestamp(thirtySecondsAgo))
                    .get()
                    .await()

                if (!recentScans.isEmpty) {
                    val lastScan = recentScans.documents.first().toObject(AttendanceRecord::class.java)
                    val timeDiff = (System.currentTimeMillis() - lastScan?.scanTime?.toDate()?.time!!) / 1000
                    Log.w("QRCodeRepository", "Recent duplicate scan detected - last scan was ${timeDiff}s ago")
                    return Result.failure(Exception("Please wait before scanning again (last scan was ${timeDiff}s ago)"))
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("QRCodeRepository", "Error checking recent duplicates", e)
                // If we can't check, allow the scan to proceed
                Result.success(Unit)
            }
        }

        override suspend fun getAttendanceRecords(mobileNumber: String): Flow<List<AttendanceRecord>> =
            flow {
                try {
                    val snapshot = attendanceCollection
                        .whereEqualTo("mobileNumber", mobileNumber)
                        .orderBy("scanTime", Query.Direction.DESCENDING)
                        .get()
                        .await()

                    val records = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AttendanceRecord::class.java)
                    }
                    emit(records)
                } catch (e: Exception) {
                    emit(emptyList())
                }
            }

        override suspend fun addAttendanceRecord(attendanceRecord: AttendanceRecord): Result<Unit> =
            try {
                attendanceCollection.document(attendanceRecord.id).set(attendanceRecord).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getTodayAttendance(mobileNumber: String): Result<List<AttendanceRecord>> =
            try {
                val today = Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val tomorrow = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val snapshot = attendanceCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .whereGreaterThanOrEqualTo("scanTime", Timestamp(today))
                    .whereLessThan("scanTime", Timestamp(tomorrow))
                    .get()
                    .await()

                val records = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                }
                Result.success(records)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getTodayAttendanceAllUsers(): Result<List<AttendanceRecord>> =
            try {
                val today = Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val tomorrow = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val snapshot = attendanceCollection
                    .orderBy("scanTime", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val allRecords = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                }

                val todayRecords = allRecords.filter { record ->
                    val recordDate = record.scanTime.toDate()
                    recordDate.after(today) && recordDate.before(tomorrow)
                }

                Result.success(todayRecords)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun observeTodayAttendance(mobileNumber: String): Flow<List<AttendanceRecord>> =
            callbackFlow {
                val today = Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val tomorrow = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val listener = attendanceCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .whereGreaterThanOrEqualTo("scanTime", Timestamp(today))
                    .whereLessThan("scanTime", Timestamp(tomorrow))
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val records = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(AttendanceRecord::class.java)
                            }
                            trySend(records)
                        }
                    }

                awaitClose { listener.remove() }
            }

        override suspend fun observeTodayAttendanceAllUsers(): Flow<List<AttendanceRecord>> =
            callbackFlow {
                Log.d("QRCodeRepository", "Setting up attendance listener (simplified)")

                // For debugging, let's first try to get ALL attendance records and filter them locally
                val listener = attendanceCollection
                    .orderBy("scanTime", Query.Direction.DESCENDING)
                    .limit(50) // Limit to recent 50 records for performance
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("QRCodeRepository", "Error in attendance listener", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            Log.d("QRCodeRepository", "Received attendance snapshot with ${snapshot.documents.size} documents")

                            val allRecords = snapshot.documents.mapNotNull { doc ->
                                val record = doc.toObject(AttendanceRecord::class.java)
                                if (record != null) {
                                    Log.d(
                                        "QRCodeRepository",
                                        "Found attendance: ${record.name} (${record.mobileNumber}) at ${record.scanTime}",
                                    )
                                } else {
                                    Log.w("QRCodeRepository", "Failed to parse document: ${doc.id}")
                                }
                                record
                            }

                            // Filter for today's records locally
                            val today = Calendar
                                .getInstance()
                                .apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.time

                            val tomorrow = Calendar
                                .getInstance()
                                .apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.time

                            val todayRecords = allRecords.filter { record ->
                                val recordDate = record.scanTime.toDate()
                                recordDate.after(today) && recordDate.before(tomorrow)
                            }

                            Log.d("QRCodeRepository", "Filtered to ${todayRecords.size} today's records out of ${allRecords.size} total")
                            Log.d("QRCodeRepository", "Today start: $today, Tomorrow start: $tomorrow")

                            trySend(todayRecords)
                        } else {
                            Log.w("QRCodeRepository", "Received null snapshot")
                            trySend(emptyList())
                        }
                    }

                awaitClose {
                    Log.d("QRCodeRepository", "Removing attendance listener")
                    listener.remove()
                }
            }

        override suspend fun getDailyUsage(
            date: String,
            mobileNumber: String,
        ): Result<DailyUsage?> =
            try {
                val id = "$date-$mobileNumber"
                val document = dailyUsageCollection.document(id).get().await()
                val usage = document.toObject(DailyUsage::class.java)
                Result.success(usage)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun updateDailyUsage(dailyUsage: DailyUsage): Result<Unit> =
            try {
                dailyUsageCollection.document(dailyUsage.id).set(dailyUsage).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getUsageStats(
            mobileNumber: String,
            days: Int,
        ): Result<Map<String, Int>> =
            try {
                val calendar = Calendar.getInstance()
                val endDate = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, -days)
                val startDate = calendar.time

                val snapshot = attendanceCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .whereGreaterThanOrEqualTo("scanTime", Timestamp(startDate))
                    .whereLessThanOrEqualTo("scanTime", Timestamp(endDate))
                    .get()
                    .await()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val stats = mutableMapOf<String, Int>()

                snapshot.documents.forEach { doc ->
                    val record = doc.toObject(AttendanceRecord::class.java)
                    if (record != null) {
                        val dateKey = dateFormat.format(record.scanTime.toDate())
                        stats[dateKey] = stats.getOrDefault(dateKey, 0) + 1
                    }
                }

                Result.success(stats)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getActiveQRCount(mobileNumber: String): Result<Int> =
            try {
                val snapshot = qrCodesCollection
                    .whereEqualTo("mobileNumber", mobileNumber)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .await()

                val activeCount = snapshot.documents.count { doc ->
                    val qrCode = doc.toObject(QRCodeModel::class.java)
                    qrCode?.isActive() == true
                }

                Result.success(activeCount)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun cleanupDuplicateAttendance(): Result<Int> =
            try {
                Log.d("QRCodeRepository", "Starting duplicate attendance cleanup")

                // Get all attendance records from today
                val today = Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val tomorrow = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val snapshot = attendanceCollection
                    .whereGreaterThanOrEqualTo("scanTime", Timestamp(today))
                    .whereLessThan("scanTime", Timestamp(tomorrow))
                    .orderBy("scanTime")
                    .get()
                    .await()

                val allRecords = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                }

                // Group by mobile number and QR ID to find duplicates
                val groupedRecords = allRecords.groupBy { "${it.mobileNumber}-${it.qrId}" }
                var deletedCount = 0

                for ((key, records) in groupedRecords) {
                    if (records.size > 1) {
                        Log.d("QRCodeRepository", "Found ${records.size} duplicate records for $key")

                        // Sort by scan time and keep only the first one
                        val sortedRecords = records.sortedBy { it.scanTime.toDate() }
                        val recordsToDelete = sortedRecords.drop(1) // Keep first, delete rest

                        for (recordToDelete in recordsToDelete) {
                            try {
                                attendanceCollection.document(recordToDelete.id).delete().await()
                                deletedCount++
                                Log.d("QRCodeRepository", "Deleted duplicate record: ${recordToDelete.id}")
                            } catch (e: Exception) {
                                Log.e("QRCodeRepository", "Failed to delete duplicate record: ${recordToDelete.id}", e)
                            }
                        }
                    }
                }

                Log.d("QRCodeRepository", "Cleanup completed. Deleted $deletedCount duplicate records")
                Result.success(deletedCount)
            } catch (e: Exception) {
                Log.e("QRCodeRepository", "Error during duplicate cleanup", e)
                Result.failure(e)
            }

        override suspend fun getDuplicateAttendanceRecords(): Result<List<AttendanceRecord>> =
            try {
                // Get all attendance records from today
                val today = Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val tomorrow = Calendar
                    .getInstance()
                    .apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                val snapshot = attendanceCollection
                    .whereGreaterThanOrEqualTo("scanTime", Timestamp(today))
                    .whereLessThan("scanTime", Timestamp(tomorrow))
                    .orderBy("scanTime")
                    .get()
                    .await()

                val allRecords = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                }

                // Find duplicates
                val groupedRecords = allRecords.groupBy { "${it.mobileNumber}-${it.qrId}" }
                val duplicates = mutableListOf<AttendanceRecord>()

                for ((_, records) in groupedRecords) {
                    if (records.size > 1) {
                        // Add all but the first record as duplicates
                        val sortedRecords = records.sortedBy { it.scanTime.toDate() }
                        duplicates.addAll(sortedRecords.drop(1))
                    }
                }

                Result.success(duplicates)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun clearDummyData(): Result<Unit> =
            try {
                Log.d("QRCodeRepository", "Starting to clear dummy data...")

                // Note: In a real implementation, we would identify dummy data by specific patterns
                // For now, this is a placeholder that logs the action
                // In production, you might want to:
                // 1. Delete QR codes with specific dummy patterns in their IDs
                // 2. Delete attendance records with dummy device IDs or scanner info
                // 3. Clear specific test user data

                Log.d("QRCodeRepository", "Dummy data clearing completed (placeholder implementation)")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("QRCodeRepository", "Error clearing dummy data", e)
                Result.failure(e)
            }
    }
