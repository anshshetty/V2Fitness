package v2.utils

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import v2.data.models.QRCodeModel
import v2.data.models.QRStatus
import v2.domain.repository.QRCodeRepository

class DataSeederTest {
    @Mock
    private lateinit var repository: QRCodeRepository

    private lateinit var dataSeeder: DataSeeder

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        dataSeeder = DataSeeder(repository)
    }

    @Test
    fun `should generate dummy QR codes with different statuses`() =
        runTest {
            // Given
            `when`(repository.generateQRCode(anyString(), anyString(), anyInt())).thenReturn(Result.success(createMockQRCode()))
            `when`(repository.updateQRCode(any())).thenReturn(Result.success(Unit))

            // When
            val result = dataSeeder.seedDummyQRCodes()

            // Then
            assertTrue(result.isSuccess)
            verify(repository, times(10)).generateQRCode(anyString(), anyString(), anyInt()) // 10 dummy QR codes
        }

    @Test
    fun `should generate dummy attendance records`() =
        runTest {
            // Given
            `when`(repository.addAttendanceRecord(any())).thenReturn(Result.success(Unit))

            // When
            val result = dataSeeder.seedDummyAttendance()

            // Then
            assertTrue(result.isSuccess)
            verify(repository, times(15)).addAttendanceRecord(any()) // 15 dummy attendance records
        }

    @Test
    fun `should clear all dummy data`() =
        runTest {
            // Given
            `when`(repository.clearDummyData()).thenReturn(Result.success(Unit))

            // When
            val result = dataSeeder.clearDummyData()

            // Then
            assertTrue(result.isSuccess)
            verify(repository).clearDummyData()
        }

    @Test
    fun `should handle repository errors gracefully`() =
        runTest {
            // Given
            val error = Exception("Database error")
            `when`(repository.generateQRCode(anyString(), anyString(), anyInt())).thenReturn(Result.failure(error))

            // When
            val result = dataSeeder.seedDummyQRCodes()

            // Then
            assertTrue(result.isFailure)
            assertEquals("Database error", result.exceptionOrNull()?.message)
        }

    @Test
    fun `should generate realistic dummy data`() =
        runTest {
            // Given
            `when`(repository.generateQRCode(anyString(), anyString(), anyInt())).thenReturn(Result.success(createMockQRCode()))
            `when`(repository.updateQRCode(any())).thenReturn(Result.success(Unit))

            // When
            val result = dataSeeder.seedDummyQRCodes()

            // Then
            assertTrue(result.isSuccess)

            // Verify realistic data patterns
            verify(repository, atLeastOnce()).generateQRCode(
                argThat { name -> name.contains(" ") }, // Names should have spaces
                argThat { mobile -> mobile.length == 10 }, // Mobile numbers should be 10 digits
                argThat { duration -> duration in 1..30 }, // Expiry should be reasonable
            )
        }

    private fun createMockQRCode(): QRCodeModel =
        QRCodeModel(
            qrId = "test-qr-id",
            name = "Test User",
            mobileNumber = "1234567890",
            expiryDuration = 7,
            status = QRStatus.ACTIVE,
            encryptedPayload = "encrypted-payload",
        )
} 
