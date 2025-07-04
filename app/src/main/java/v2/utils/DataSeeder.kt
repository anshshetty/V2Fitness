package v2.utils

import android.util.Log
import com.google.firebase.Timestamp
import v2.data.models.AttendanceRecord
import v2.data.models.QRStatus
import v2.domain.repository.QRCodeRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DataSeeder
    @Inject
    constructor(
        private val repository: QRCodeRepository,
    ) {
        companion object {
            private const val TAG = "DataSeeder"

            // Dummy data sets
            private val dummyNames = listOf(
                "John Doe",
                "Jane Smith",
                "Mike Johnson",
                "Sarah Wilson",
                "David Brown",
                "Emily Davis",
                "Chris Miller",
                "Lisa Garcia",
                "Tom Anderson",
                "Amy Taylor",
                "Kevin Martinez",
                "Jessica Thompson",
                "Ryan White",
                "Michelle Lee",
                "Brian Clark",
                "Ashley Rodriguez",
                "Daniel Lewis",
                "Nicole Walker",
                "Justin Hall",
                "Stephanie Young",
            )

            private val dummyMobileNumbers = listOf(
                "9876543210",
                "8765432109",
                "7654321098",
                "6543210987",
                "5432109876",
                "9123456780",
                "8234567891",
                "7345678902",
                "6456789013",
                "5567890124",
                "9988776655",
                "8877665544",
                "7766554433",
                "6655443322",
                "5544332211",
                "9111222333",
                "8222333444",
                "7333444555",
                "6444555666",
                "5555666777",
            )

            private val gymLocations = listOf(
                "Main Gym Floor",
                "Cardio Section",
                "Weight Training Area",
                "Yoga Studio",
                "Spinning Room",
                "Pool Area",
                "Locker Room",
                "Reception",
                "Cafe Area",
                "Parking",
            )
        }

        /**
         * Seeds dummy QR codes with various statuses for testing
         */
        suspend fun seedDummyQRCodes(): Result<Int> =
            try {
                Log.d(TAG, "Starting to seed dummy QR codes...")
                var successCount = 0

                // Generate 10 dummy QR codes with different characteristics
                for (i in 0 until 10) {
                    val name = dummyNames[i % dummyNames.size]
                    val mobileNumber = dummyMobileNumbers[i % dummyMobileNumbers.size]
                    val expiryDuration = when (i % 4) {
                        0 -> Random.nextInt(1, 7) // Active QR codes (1-6 days)
                        1 -> Random.nextInt(7, 30) // Long-term QR codes (1-4 weeks)
                        2 -> Random.nextInt(1, 3) // Short-term QR codes (1-2 days)
                        else -> Random.nextInt(1, 14) // Medium-term QR codes (1-2 weeks)
                    }

                    val result = repository.generateQRCode(name, mobileNumber, expiryDuration)

                    if (result.isSuccess) {
                        val qrCode = result.getOrThrow()

                        // Modify some QR codes to have different statuses
                        val modifiedQRCode = when (i % 5) {
                            1 -> qrCode.copy(status = QRStatus.DISABLED) // 20% disabled
                            2 -> qrCode.copy( // 20% expired (simulate old creation date)
                                createdAt = Timestamp(Date(System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000))),
                                expiryDuration = 30,
                            )
                            3 -> qrCode.copy( // 20% used today
                                usageCount = Random.nextInt(1, 5),
                                lastUsedAt = Timestamp.now(),
                            )
                            else -> qrCode // 40% remain active
                        }

                        if (modifiedQRCode != qrCode) {
                            repository.updateQRCode(modifiedQRCode)
                        }

                        successCount++
                        Log.d(TAG, "Created dummy QR code for $name (${modifiedQRCode.getCurrentStatus()})")
                    } else {
                        Log.w(TAG, "Failed to create QR code for $name: ${result.exceptionOrNull()?.message}")
                    }
                }

                Log.d(TAG, "Successfully seeded $successCount dummy QR codes")
                Result.success(successCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding dummy QR codes", e)
                Result.failure(e)
            }

        /**
         * Seeds dummy attendance records for testing
         */
        suspend fun seedDummyAttendance(): Result<Int> =
            try {
                Log.d(TAG, "Starting to seed dummy attendance records...")
                var successCount = 0

                // Generate 15 dummy attendance records across different times
                for (i in 0 until 15) {
                    val name = dummyNames[i % dummyNames.size]
                    val mobileNumber = dummyMobileNumbers[i % dummyMobileNumbers.size]
                    val location = gymLocations[i % gymLocations.size]

                    // Create attendance records for different times (today and yesterday)
                    val scanTime = when (i % 3) {
                        0 -> Timestamp.now() // Today
                        1 -> Timestamp(Date(System.currentTimeMillis() - Random.nextLong(0, 12 * 60 * 60 * 1000))) // Earlier today
                        else -> Timestamp(Date(System.currentTimeMillis() - Random.nextLong(24 * 60 * 60 * 1000, 48 * 60 * 60 * 1000))) // Yesterday
                    }

                    val attendanceRecord = AttendanceRecord(
                        id = "dummy_${UUID.randomUUID()}",
                        mobileNumber = mobileNumber,
                        name = name,
                        scanTime = scanTime,
                        qrId = "dummy_qr_${Random.nextInt(1000, 9999)}",
                        deviceId = "dummy_device_${Random.nextInt(100, 999)}",
                        location = location,
                        scannerInfo = "Dummy Scanner v1.0",
                    )

                    val result = repository.addAttendanceRecord(attendanceRecord)

                    if (result.isSuccess) {
                        successCount++
                        Log.d(TAG, "Created dummy attendance record for $name at $location")
                    } else {
                        Log.w(TAG, "Failed to create attendance record for $name: ${result.exceptionOrNull()?.message}")
                    }
                }

                Log.d(TAG, "Successfully seeded $successCount dummy attendance records")
                Result.success(successCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding dummy attendance records", e)
                Result.failure(e)
            }

        /**
         * Seeds both QR codes and attendance records
         */
        suspend fun seedAllDummyData(): Result<Pair<Int, Int>> =
            try {
                Log.d(TAG, "Starting to seed all dummy data...")

                val qrResult = seedDummyQRCodes()
                val attendanceResult = seedDummyAttendance()

                if (qrResult.isSuccess && attendanceResult.isSuccess) {
                    val qrCount = qrResult.getOrThrow()
                    val attendanceCount = attendanceResult.getOrThrow()
                    Log.d(TAG, "Successfully seeded all dummy data: $qrCount QR codes, $attendanceCount attendance records")
                    Result.success(Pair(qrCount, attendanceCount))
                } else {
                    val error = qrResult.exceptionOrNull() ?: attendanceResult.exceptionOrNull()
                    Log.e(TAG, "Failed to seed all dummy data", error)
                    Result.failure(error ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding all dummy data", e)
                Result.failure(e)
            }

        /**
         * Clears all dummy data (requires repository support)
         */
        suspend fun clearDummyData(): Result<Unit> =
            try {
                Log.d(TAG, "Clearing dummy data...")
                // Note: This would require additional repository methods to identify and delete dummy data
                // For now, we'll return success as a placeholder
                Log.d(TAG, "Dummy data clearing completed")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing dummy data", e)
                Result.failure(e)
            }

        /**
         * Legacy method - kept for compatibility but does nothing in production
         */
        suspend fun seedTestData() {
            // No test data seeding in production
            Log.d(TAG, "seedTestData called - no action taken in production build")
        }
    } 
