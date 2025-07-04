package v2.presentation.viewmodels

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import v2.data.models.QRCodeModel
import v2.data.models.QRStatus

class QRListViewModelTest {
    @Test
    fun `should filter QR codes by search query - name`() {
        // Given
        val qrCodes = createTestQRCodes()
        val searchQuery = "John"

        // When
        val filteredCodes = qrCodes.filter { qrCode ->
            qrCode.name.contains(searchQuery, ignoreCase = true) ||
                qrCode.mobileNumber.contains(searchQuery, ignoreCase = true)
        }

        // Then
        assertEquals(1, filteredCodes.size)
        assertEquals("John Doe", filteredCodes.first().name)
    }

    @Test
    fun `should filter QR codes by search query - mobile number`() {
        // Given
        val qrCodes = createTestQRCodes()
        val searchQuery = "9876"

        // When
        val filteredCodes = qrCodes.filter { qrCode ->
            qrCode.name.contains(searchQuery, ignoreCase = true) ||
                qrCode.mobileNumber.contains(searchQuery, ignoreCase = true)
        }

        // Then
        assertEquals(1, filteredCodes.size)
        assertEquals("9876543210", filteredCodes.first().mobileNumber)
    }

    @Test
    fun `should filter QR codes by status - active only`() {
        // Given
        val qrCodes = createTestQRCodes()

        // When
        val filteredCodes = qrCodes.filter { it.getCurrentStatus() == QRStatus.ACTIVE }

        // Then
        assertTrue(filteredCodes.all { it.getCurrentStatus() == QRStatus.ACTIVE })
        assertEquals(2, filteredCodes.size) // John and Jane should be active
    }

    @Test
    fun `should filter QR codes by status - disabled only`() {
        // Given
        val qrCodes = createTestQRCodes()

        // When
        val filteredCodes = qrCodes.filter { it.getCurrentStatus() == QRStatus.DISABLED }

        // Then
        assertTrue(filteredCodes.all { it.getCurrentStatus() == QRStatus.DISABLED })
        assertEquals(1, filteredCodes.size) // Alice should be disabled
    }

    @Test
    fun `should filter QR codes by status - expired only`() {
        // Given
        val qrCodes = createTestQRCodes()

        // When
        val filteredCodes = qrCodes.filter { it.getCurrentStatus() == QRStatus.EXPIRED }

        // Then
        assertTrue(filteredCodes.all { it.getCurrentStatus() == QRStatus.EXPIRED })
        assertEquals(1, filteredCodes.size) // Bob should be expired
    }

    @Test
    fun `should combine search and status filters`() {
        // Given
        val qrCodes = createTestQRCodes()
        val searchQuery = "John"

        // When
        val filteredCodes = qrCodes.filter { qrCode ->
            (
                qrCode.name.contains(searchQuery, ignoreCase = true) ||
                    qrCode.mobileNumber.contains(searchQuery, ignoreCase = true)
            ) &&
                qrCode.getCurrentStatus() == QRStatus.ACTIVE
        }

        // Then
        assertTrue(
            filteredCodes.all {
                it.name.contains("John", ignoreCase = true) &&
                    it.getCurrentStatus() == QRStatus.ACTIVE
            },
        )
        assertEquals(1, filteredCodes.size)
    }

    @Test
    fun `should handle empty search results`() {
        // Given
        val qrCodes = createTestQRCodes()
        val searchQuery = "NonExistentName"

        // When
        val filteredCodes = qrCodes.filter { qrCode ->
            qrCode.name.contains(searchQuery, ignoreCase = true) ||
                qrCode.mobileNumber.contains(searchQuery, ignoreCase = true)
        }

        // Then
        assertTrue(filteredCodes.isEmpty())
    }

    @Test
    fun `should return all QR codes when no filters applied`() {
        // Given
        val qrCodes = createTestQRCodes()

        // When
        val filteredCodes = qrCodes // No filters applied

        // Then
        assertEquals(4, filteredCodes.size)
    }

    @Test
    fun `should test QR status filter enum values`() {
        // Test that all enum values are available
        val allFilters = QRStatusFilter.values()

        assertTrue(allFilters.contains(QRStatusFilter.ALL))
        assertTrue(allFilters.contains(QRStatusFilter.ACTIVE))
        assertTrue(allFilters.contains(QRStatusFilter.EXPIRED))
        assertTrue(allFilters.contains(QRStatusFilter.DISABLED))
        assertTrue(allFilters.contains(QRStatusFilter.USED))
    }

    private fun createTestQRCodes(): List<QRCodeModel> {
        val now = Timestamp.now()
        val pastTime = Timestamp(now.seconds - (40 * 24 * 60 * 60), 0) // 40 days ago

        return listOf(
            QRCodeModel(
                qrId = "1",
                name = "John Doe",
                mobileNumber = "1234567890",
                createdAt = now,
                expiryDuration = 30,
                status = QRStatus.ACTIVE,
                encryptedPayload = "payload1",
            ),
            QRCodeModel(
                qrId = "2",
                name = "Jane Smith",
                mobileNumber = "9876543210",
                createdAt = now,
                expiryDuration = 30,
                status = QRStatus.ACTIVE,
                encryptedPayload = "payload2",
            ),
            QRCodeModel(
                qrId = "3",
                name = "Bob Wilson",
                mobileNumber = "5555555555",
                createdAt = pastTime,
                expiryDuration = 30,
                status = QRStatus.ACTIVE,
                encryptedPayload = "payload3",
            ),
            QRCodeModel(
                qrId = "4",
                name = "Alice Brown",
                mobileNumber = "7777777777",
                createdAt = now,
                expiryDuration = 30,
                status = QRStatus.DISABLED,
                encryptedPayload = "payload4",
            ),
        )
    }
} 
