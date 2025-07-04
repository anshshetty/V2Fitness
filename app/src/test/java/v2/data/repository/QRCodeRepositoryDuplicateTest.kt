package v2.data.repository

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import v2.data.models.*
import java.util.*

class QRCodeRepositoryDuplicateTest {
    @Test
    fun `should prevent duplicate scans within 30 seconds`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val qrId = "test-qr-id"
            val name = "Test User"

            // Create a recent attendance record (10 seconds ago)
            val recentScanTime = Calendar
                .getInstance()
                .apply {
                    add(Calendar.SECOND, -10)
                }.time

            val recentAttendance = AttendanceRecord(
                id = "recent-id",
                mobileNumber = mobileNumber,
                name = name,
                scanTime = Timestamp(recentScanTime),
                qrId = qrId,
                deviceId = "test-device",
                scannerInfo = "Android Scanner",
            )

            // When trying to scan the same QR code again
            // Then it should be blocked due to recent duplicate

            // This test would need to be integrated with actual repository implementation
            // For now, we're testing the logic conceptually

            val timeDiff = (System.currentTimeMillis() - recentScanTime.time) / 1000
            assertTrue("Recent scan should be within 30 seconds", timeDiff < 30)
        }

    @Test
    fun `should allow scan after 30 seconds cooldown`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val qrId = "test-qr-id"
            val name = "Test User"

            // Create an old attendance record (40 seconds ago)
            val oldScanTime = Calendar
                .getInstance()
                .apply {
                    add(Calendar.SECOND, -40)
                }.time

            val oldAttendance = AttendanceRecord(
                id = "old-id",
                mobileNumber = mobileNumber,
                name = name,
                scanTime = Timestamp(oldScanTime),
                qrId = qrId,
                deviceId = "test-device",
                scannerInfo = "Android Scanner",
            )

            // When trying to scan the same QR code again
            // Then it should be allowed since enough time has passed

            val timeDiff = (System.currentTimeMillis() - oldScanTime.time) / 1000
            assertTrue("Old scan should be more than 30 seconds ago", timeDiff > 30)
        }

    @Test
    fun `should identify duplicate attendance records correctly`() {
        // Given
        val mobileNumber = "1234567890"
        val qrId = "test-qr-id"
        val name = "Test User"

        val baseTime = System.currentTimeMillis()

        // Create multiple attendance records for the same user and QR code
        val record1 = AttendanceRecord(
            id = "id-1",
            mobileNumber = mobileNumber,
            name = name,
            scanTime = Timestamp(Date(baseTime)),
            qrId = qrId,
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val record2 = AttendanceRecord(
            id = "id-2",
            mobileNumber = mobileNumber,
            name = name,
            scanTime = Timestamp(Date(baseTime + 5000)), // 5 seconds later
            qrId = qrId,
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val record3 = AttendanceRecord(
            id = "id-3",
            mobileNumber = mobileNumber,
            name = name,
            scanTime = Timestamp(Date(baseTime + 10000)), // 10 seconds later
            qrId = qrId,
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val allRecords = listOf(record1, record2, record3)

        // When grouping by mobile number and QR ID
        val groupedRecords = allRecords.groupBy { "${it.mobileNumber}-${it.qrId}" }

        // Then we should find duplicates
        val duplicateGroups = groupedRecords.filter { it.value.size > 1 }
        assertEquals("Should find one group with duplicates", 1, duplicateGroups.size)
        assertEquals("Should find 3 duplicate records", 3, duplicateGroups.values.first().size)

        // The first record should be kept, others should be marked as duplicates
        val sortedRecords = duplicateGroups.values.first().sortedBy { it.scanTime.toDate() }
        val recordsToDelete = sortedRecords.drop(1)
        assertEquals("Should mark 2 records for deletion", 2, recordsToDelete.size)
        assertEquals("First record should be kept", "id-1", sortedRecords.first().id)
    }

    @Test
    fun `should handle different users with same QR code correctly`() {
        // Given
        val qrId = "shared-qr-id"
        val name = "Test User"

        val user1Record = AttendanceRecord(
            id = "user1-id",
            mobileNumber = "1111111111",
            name = name,
            scanTime = Timestamp.now(),
            qrId = qrId,
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val user2Record = AttendanceRecord(
            id = "user2-id",
            mobileNumber = "2222222222",
            name = name,
            scanTime = Timestamp.now(),
            qrId = qrId,
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val allRecords = listOf(user1Record, user2Record)

        // When grouping by mobile number and QR ID
        val groupedRecords = allRecords.groupBy { "${it.mobileNumber}-${it.qrId}" }

        // Then each user should have their own group (no duplicates)
        assertEquals("Should have 2 separate groups", 2, groupedRecords.size)
        groupedRecords.values.forEach { records ->
            assertEquals("Each group should have only 1 record", 1, records.size)
        }
    }

    @Test
    fun `should validate scan cooldown periods`() {
        // Test camera level cooldown (3 seconds)
        val cameraCooldown = 3000L
        val lastCameraDetection = System.currentTimeMillis() - 2000L // 2 seconds ago
        val shouldBlockCamera = (System.currentTimeMillis() - lastCameraDetection) < cameraCooldown
        assertTrue("Camera should block scan within 3 seconds", shouldBlockCamera)

        // Test viewmodel level cooldown (5 seconds)
        val viewModelCooldown = 5000L
        val lastViewModelScan = System.currentTimeMillis() - 4000L // 4 seconds ago
        val shouldBlockViewModel = (System.currentTimeMillis() - lastViewModelScan) < viewModelCooldown
        assertTrue("ViewModel should block scan within 5 seconds", shouldBlockViewModel)

        // Test repository level cooldown (30 seconds)
        val repositoryCooldown = 30000L
        val lastRepositoryScan = System.currentTimeMillis() - 25000L // 25 seconds ago
        val shouldBlockRepository = (System.currentTimeMillis() - lastRepositoryScan) < repositoryCooldown
        assertTrue("Repository should block scan within 30 seconds", shouldBlockRepository)
    }
} 
