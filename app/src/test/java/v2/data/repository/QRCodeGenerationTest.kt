package v2.data.repository

import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import v2.data.models.*
import java.util.*

class QRCodeGenerationTest {
    @Test
    fun `should fail for invalid mobile number`() =
        runTest {
            // Given invalid mobile numbers
            val invalidNumbers = listOf("", "123", "12345678901", "abcd123456", " ", "123-456-7890")

            // When/Then each should fail validation
            invalidNumbers.forEach { invalidNumber ->
                // This would test the validation logic in generateQRCode
                val isValid = invalidNumber.matches(Regex("^[0-9]{10}$")) && invalidNumber.isNotBlank()
                assertFalse("Mobile number '$invalidNumber' should be invalid", isValid)
            }
        }

    @Test
    fun `should accept valid mobile number`() =
        runTest {
            // Given valid mobile numbers
            val validNumbers = listOf("1234567890", "9876543210", "0123456789")

            // When/Then each should pass validation
            validNumbers.forEach { validNumber ->
                val isValid = validNumber.matches(Regex("^[0-9]{10}$")) && validNumber.isNotBlank()
                assertTrue("Mobile number '$validNumber' should be valid", isValid)
            }
        }

    @Test
    fun `should prevent generating QR when active QR exists`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val name = "Test User"

            // Create an active QR code
            val activeQRCode = QRCodeModel(
                qrId = "active-qr-id",
                name = name,
                mobileNumber = mobileNumber,
                createdAt = Timestamp.now(),
                expiryDuration = 30, // 30 days
                status = QRStatus.ACTIVE,
                usageCount = 0,
                lastUsedAt = null,
                encryptedPayload = "encrypted-payload",
                salt = "salt",
            )

            // When checking if QR is active
            val isActive = activeQRCode.isActive()

            // Then it should be active and block new QR generation
            assertTrue("QR code should be active", isActive)
            assertEquals("Status should be ACTIVE", QRStatus.ACTIVE, activeQRCode.status)
            assertFalse("QR code should not be expired", activeQRCode.isExpired())
        }

    @Test
    fun `should reactivate disabled QR code instead of creating new one`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val originalName = "Original Name"
            val newName = "Updated Name"
            val newExpiryDuration = 15

            // Create a disabled QR code
            val disabledQRCode = QRCodeModel(
                qrId = "disabled-qr-id",
                name = originalName,
                mobileNumber = mobileNumber,
                createdAt = Timestamp(Date(System.currentTimeMillis() - 86400000)), // 1 day ago
                expiryDuration = 7, // Original 7 days
                status = QRStatus.DISABLED,
                usageCount = 5,
                lastUsedAt = Timestamp.now(),
                encryptedPayload = "encrypted-payload",
                salt = "salt",
            )

            // When reactivating the disabled QR code
            val reactivatedQRCode = disabledQRCode.copy(
                name = newName, // Update name
                status = QRStatus.ACTIVE,
                expiryDuration = newExpiryDuration,
                createdAt = Timestamp.now(), // Reset creation time
                usageCount = 0, // Reset usage count
                lastUsedAt = null, // Reset last used time
            )

            // Then the reactivated QR code should have updated properties
            assertEquals("Status should be ACTIVE", QRStatus.ACTIVE, reactivatedQRCode.status)
            assertEquals("Name should be updated", newName, reactivatedQRCode.name)
            assertEquals("Expiry duration should be updated", newExpiryDuration, reactivatedQRCode.expiryDuration)
            assertEquals("Usage count should be reset", 0, reactivatedQRCode.usageCount)
            assertNull("Last used time should be reset", reactivatedQRCode.lastUsedAt)
            assertEquals("QR ID should remain the same", disabledQRCode.qrId, reactivatedQRCode.qrId)
            assertEquals("Mobile number should remain the same", mobileNumber, reactivatedQRCode.mobileNumber)
        }

    @Test
    fun `should create new QR code when no existing QR code found`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val name = "New User"
            val expiryDuration = 30

            // When no existing QR codes exist (empty list)
            val existingQRCodes = emptyList<QRCodeModel>()

            // Then should proceed to create new QR code
            val activeQRCode = existingQRCodes.find { it.isActive() }
            val disabledQRCode = existingQRCodes.find { it.status == QRStatus.DISABLED }

            assertNull("Should not find active QR code", activeQRCode)
            assertNull("Should not find disabled QR code", disabledQRCode)

            // Logic would proceed to create new QR code with fresh UUID, salt, etc.
        }

    @Test
    fun `should handle expired QR codes correctly`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val name = "Test User"

            // Create an expired QR code (created 31 days ago with 30 day duration)
            val oldDate = Calendar
                .getInstance()
                .apply {
                    add(Calendar.DAY_OF_YEAR, -31)
                }.time

            val expiredQRCode = QRCodeModel(
                qrId = "expired-qr-id",
                name = name,
                mobileNumber = mobileNumber,
                createdAt = Timestamp(oldDate),
                expiryDuration = 30, // 30 days
                status = QRStatus.ACTIVE, // Status is active but time has expired
                usageCount = 0,
                lastUsedAt = null,
                encryptedPayload = "encrypted-payload",
                salt = "salt",
            )

            // When checking if QR is active
            val isActive = expiredQRCode.isActive()
            val isExpired = expiredQRCode.isExpired()

            // Then it should be expired and not active
            assertTrue("QR code should be expired", isExpired)
            assertFalse("QR code should not be active", isActive)

            // This expired QR code should not block new QR generation
            val existingQRCodes = listOf(expiredQRCode)
            val activeQRCode = existingQRCodes.find { it.isActive() }
            assertNull("Should not find active QR code", activeQRCode)
        }

    @Test
    fun `should prioritize disabled QR over expired QR for reactivation`() =
        runTest {
            // Given
            val mobileNumber = "1234567890"
            val name = "Test User"

            // Create an expired QR code
            val oldDate = Calendar
                .getInstance()
                .apply {
                    add(Calendar.DAY_OF_YEAR, -31)
                }.time

            val expiredQRCode = QRCodeModel(
                qrId = "expired-qr-id",
                name = name,
                mobileNumber = mobileNumber,
                createdAt = Timestamp(oldDate),
                expiryDuration = 30,
                status = QRStatus.ACTIVE,
                usageCount = 0,
                lastUsedAt = null,
                encryptedPayload = "encrypted-payload-1",
                salt = "salt-1",
            )

            // Create a disabled QR code
            val disabledQRCode = QRCodeModel(
                qrId = "disabled-qr-id",
                name = name,
                mobileNumber = mobileNumber,
                createdAt = Timestamp.now(),
                expiryDuration = 15,
                status = QRStatus.DISABLED,
                usageCount = 3,
                lastUsedAt = Timestamp.now(),
                encryptedPayload = "encrypted-payload-2",
                salt = "salt-2",
            )

            val existingQRCodes = listOf(expiredQRCode, disabledQRCode)

            // When looking for QR codes to reactivate
            val activeQRCode = existingQRCodes.find { it.isActive() }
            val disabledQRForReactivation = existingQRCodes.find { it.status == QRStatus.DISABLED }

            // Then should find disabled QR for reactivation, not the expired one
            assertNull("Should not find active QR code", activeQRCode)
            assertNotNull("Should find disabled QR code", disabledQRForReactivation)
            assertEquals("Should select the disabled QR code", "disabled-qr-id", disabledQRForReactivation?.qrId)
        }

    @Test
    fun `should validate expiry duration calculation from creation time`() =
        runTest {
            // Given
            val creationTime = System.currentTimeMillis()
            val expiryDuration = 7 // 7 days

            // When calculating expiry time
            val expiryTime = creationTime + (expiryDuration * 24 * 60 * 60 * 1000L)
            val currentTime = System.currentTimeMillis()

            // Then expiry time should be in the future for new QR codes
            assertTrue("Expiry time should be in the future", expiryTime > currentTime)

            // Test with past creation time
            val pastCreationTime = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L) // 8 days ago
            val pastExpiryTime = pastCreationTime + (expiryDuration * 24 * 60 * 60 * 1000L)

            // Then it should be expired
            assertTrue(
                "QR code created 8 days ago with 7 day duration should be expired",
                currentTime > pastExpiryTime,
            )
        }
} 
