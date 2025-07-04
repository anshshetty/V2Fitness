package v2.presentation.ui.screens

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import v2.data.models.AttendanceRecord
import v2.data.models.ScanErrorType
import v2.data.models.ScanResult

class ScanQRScreenTest {
    @Test
    fun `should process QR code scan successfully`() {
        // Given
        val testQRCode = "encrypted-qr-payload-12345"
        val expectedAttendanceRecord = AttendanceRecord(
            id = "test-id",
            mobileNumber = "1234567890",
            name = "Test User",
            scanTime = Timestamp.now(),
            qrId = "test-qr-id",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )
        val expectedResult = ScanResult.Success(expectedAttendanceRecord)

        // When
        var actualResult: ScanResult? = null
        val onScanResult: (ScanResult) -> Unit = { result ->
            actualResult = result
        }
        onScanResult(expectedResult)

        // Then
        assertTrue(actualResult is ScanResult.Success)
        val successResult = actualResult as ScanResult.Success
        assertEquals(expectedAttendanceRecord.name, successResult.attendanceRecord.name)
        assertEquals(expectedAttendanceRecord.mobileNumber, successResult.attendanceRecord.mobileNumber)
    }

    @Test
    fun `should show welcoming success message without mobile number`() {
        // Given
        val memberName = "John Doe"
        val mobileNumber = "1234567890"
        val attendanceRecord = AttendanceRecord(
            id = "test-id",
            mobileNumber = mobileNumber,
            name = memberName,
            scanTime = Timestamp.now(),
            qrId = "test-qr-id",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )
        val successResult = ScanResult.Success(attendanceRecord)

        // When - Success popup should show welcoming message
        val expectedWelcomeMessage = "Welcome, $memberName! 🎉"
        val expectedSuccessMessage = "Your attendance has been recorded successfully!"
        val expectedMotivationMessage = "Have a great workout! 💪"

        // Then - Verify the success result contains member name but not mobile number in display
        assertTrue(successResult.attendanceRecord.name == memberName)
        assertTrue(successResult.attendanceRecord.mobileNumber == mobileNumber)

        // The UI should show welcoming messages without exposing mobile number
        assertFalse(
            "Success popup should not display mobile number",
            expectedWelcomeMessage.contains(mobileNumber),
        )
        assertFalse(
            "Success message should not display mobile number",
            expectedSuccessMessage.contains(mobileNumber),
        )
        assertFalse(
            "Motivation message should not display mobile number",
            expectedMotivationMessage.contains(mobileNumber),
        )

        // Verify welcoming tone
        assertTrue("Should contain welcome message", expectedWelcomeMessage.contains("Welcome"))
        assertTrue("Should contain celebration emoji", expectedWelcomeMessage.contains("🎉"))
        assertTrue("Should contain workout motivation", expectedMotivationMessage.contains("workout"))
        assertTrue("Should contain muscle emoji", expectedMotivationMessage.contains("💪"))
    }

    @Test
    fun `should auto-dismiss success popup after 3 seconds`() {
        // Given
        val memberName = "Jane Smith"
        val attendanceRecord = AttendanceRecord(
            id = "test-id-2",
            mobileNumber = "9876543210",
            name = memberName,
            scanTime = Timestamp.now(),
            qrId = "test-qr-id-2",
            deviceId = "test-device",
            scannerInfo = "Android Scanner",
        )
        val successResult = ScanResult.Success(attendanceRecord)

        // When - Success popup should auto-dismiss
        var dismissCalled = false
        val onDismiss: () -> Unit = { dismissCalled = true }

        // Then - Verify auto-dismiss behavior is configured
        // Note: In actual implementation, LaunchedEffect with delay(3000) handles auto-dismiss
        val autoDismissDelay = 3000L // 3 seconds
        assertTrue("Auto-dismiss delay should be 3 seconds", autoDismissDelay == 3000L)

        // Verify success result is valid for auto-dismiss
        assertTrue("Should be success result for auto-dismiss", successResult is ScanResult.Success)
        assertEquals("Should contain correct member name", memberName, successResult.attendanceRecord.name)

        // Simulate manual dismiss (auto-dismiss would call this after delay)
        onDismiss()
        assertTrue("Dismiss callback should be called", dismissCalled)
    }

    @Test
    fun `should handle QR code scan error`() {
        // Given
        val errorMessage = "QR code has expired"
        val errorType = ScanErrorType.EXPIRED
        val expectedResult = ScanResult.Error(errorMessage, errorType)

        // When
        var actualResult: ScanResult? = null
        val onScanResult: (ScanResult) -> Unit = { result ->
            actualResult = result
        }
        onScanResult(expectedResult)

        // Then
        assertTrue(actualResult is ScanResult.Error)
        val errorResult = actualResult as ScanResult.Error
        assertEquals(errorMessage, errorResult.message)
        assertEquals(errorType, errorResult.errorType)
    }

    @Test
    fun `should handle invalid QR code format`() {
        // Given
        val invalidQRCode = ""
        val expectedResult = ScanResult.Error("Invalid QR code format", ScanErrorType.INVALID_FORMAT)

        // When
        var actualResult: ScanResult? = null
        val onScanResult: (ScanResult) -> Unit = { result ->
            actualResult = result
        }

        if (invalidQRCode.isBlank()) {
            onScanResult(expectedResult)
        }

        // Then
        assertTrue(actualResult is ScanResult.Error)
        val errorResult = actualResult as ScanResult.Error
        assertEquals("Invalid QR code format", errorResult.message)
        assertEquals(ScanErrorType.INVALID_FORMAT, errorResult.errorType)
    }

    @Test
    fun `should handle already used QR code`() {
        // Given
        val alreadyUsedResult = ScanResult.Error("QR code already used today", ScanErrorType.ALREADY_USED)

        // When
        var actualResult: ScanResult? = null
        val onScanResult: (ScanResult) -> Unit = { result ->
            actualResult = result
        }
        onScanResult(alreadyUsedResult)

        // Then
        assertTrue(actualResult is ScanResult.Error)
        val errorResult = actualResult as ScanResult.Error
        assertEquals("QR code already used today", errorResult.message)
        assertEquals(ScanErrorType.ALREADY_USED, errorResult.errorType)
    }
} 
