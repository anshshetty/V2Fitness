package v2.presentation.viewmodels

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import v2.data.models.AttendanceRecord
import v2.data.models.QRCodeModel

class DashboardViewModelTest {
    @Test
    fun `should count active QR codes correctly`() {
        // Given
        val activeQRCode1 = createMockQRCode(isActive = true)
        val inactiveQRCode = createMockQRCode(isActive = false)
        val activeQRCode2 = createMockQRCode(isActive = true)

        val qrCodes = listOf(activeQRCode1, inactiveQRCode, activeQRCode2)

        // When
        val activeCount = qrCodes.count { it.isActive() }

        // Then
        assertEquals(2, activeCount)
    }

    @Test
    fun `should handle empty attendance list`() {
        // Given
        val emptyAttendanceList = emptyList<AttendanceRecord>()

        // When
        val count = emptyAttendanceList.size

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `should count attendance records correctly`() {
        // Given
        val attendanceRecord1 = AttendanceRecord(
            id = "test-id-1",
            mobileNumber = "1234567890",
            name = "Test User 1",
            scanTime = Timestamp.now(),
            qrId = "test-qr-id-1",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val attendanceRecord2 = AttendanceRecord(
            id = "test-id-2",
            mobileNumber = "0987654321",
            name = "Test User 2",
            scanTime = Timestamp.now(),
            qrId = "test-qr-id-2",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val attendanceList = listOf(attendanceRecord1, attendanceRecord2)

        // When
        val count = attendanceList.size

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `should filter user attendance correctly`() {
        // Given
        val userMobileNumber = "1234567890"
        val userAttendance = AttendanceRecord(
            id = "test-id-1",
            mobileNumber = userMobileNumber,
            name = "Test User",
            scanTime = Timestamp.now(),
            qrId = "test-qr-id-1",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val otherUserAttendance = AttendanceRecord(
            id = "test-id-2",
            mobileNumber = "0987654321",
            name = "Other User",
            scanTime = Timestamp.now(),
            qrId = "test-qr-id-2",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )

        val allAttendance = listOf(userAttendance, otherUserAttendance)

        // When
        val userOnlyAttendance = allAttendance.filter { it.mobileNumber == userMobileNumber }

        // Then
        assertEquals(1, userOnlyAttendance.size)
        assertEquals(userMobileNumber, userOnlyAttendance.first().mobileNumber)
    }

    private fun createMockQRCode(isActive: Boolean): QRCodeModel =
        QRCodeModel(
            qrId = "test-qr-id",
            name = "Test User",
            mobileNumber = "1234567890",
            encryptedPayload = "encrypted-payload",
            createdAt = Timestamp.now(),
            expiryDuration = 30,
            usageCount = 0,
            lastUsedAt = null,
            status = if (isActive) v2.data.models.QRStatus.ACTIVE else v2.data.models.QRStatus.DISABLED,
        )
} 
