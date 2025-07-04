package v2.domain.usecases

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import v2.data.models.QRCodeModel
import v2.domain.repository.QRCodeRepository

class GenerateQRCodeUseCaseTest {
    @Mock
    private lateinit var repository: QRCodeRepository

    @Mock
    private lateinit var checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase

    @Mock
    private lateinit var context: Context

    private lateinit var useCase: GenerateQRCodeUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GenerateQRCodeUseCase(repository, checkDeviceApprovalUseCase)
    }

    @Test
    fun `invoke with valid inputs and approved device should return success`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "1234567890"
            val expiryDuration = 7 // 7 days
            val mockQRCode = QRCodeModel(
                qrId = "test-id",
                name = name,
                mobileNumber = mobileNumber,
                expiryDuration = expiryDuration,
            )

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.APPROVED))
            `when`(repository.getActiveQRCount(mobileNumber)).thenReturn(Result.success(2))
            `when`(repository.generateQRCode(name, mobileNumber, expiryDuration))
                .thenReturn(Result.success(mockQRCode))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mockQRCode, result.getOrNull())
            verify(checkDeviceApprovalUseCase).invoke(context, true)
        }

    @Test
    fun `invoke with device not approved should return failure`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "1234567890"
            val expiryDuration = 7

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.PENDING))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Device is not approved") == true)
            verify(checkDeviceApprovalUseCase).invoke(context, true)
            verifyNoInteractions(repository)
        }

    @Test
    fun `invoke with device rejected should return failure`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "1234567890"
            val expiryDuration = 7

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.REJECTED))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Device access has been rejected") == true)
            verify(checkDeviceApprovalUseCase).invoke(context, true)
            verifyNoInteractions(repository)
        }

    @Test
    fun `invoke with device approval check failure should return failure`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "1234567890"
            val expiryDuration = 7
            val approvalError = Exception("Network error")

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.failure(approvalError))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Unable to verify device approval") == true)
            verify(checkDeviceApprovalUseCase).invoke(context, true)
            verifyNoInteractions(repository)
        }

    @Test
    fun `invoke with invalid name should return failure`() =
        runTest {
            // Given
            val name = "A" // Too short
            val mobileNumber = "1234567890"
            val expiryDuration = 7 // 7 days

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.APPROVED))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Name must be at least 2 characters") == true)
        }

    @Test
    fun `invoke with invalid mobile number should return failure`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "123" // Too short
            val expiryDuration = 7 // 7 days

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.APPROVED))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Invalid mobile number") == true)
        }

    @Test
    fun `invoke with rate limit exceeded should return failure`() =
        runTest {
            // Given
            val name = "John Doe"
            val mobileNumber = "1234567890"
            val expiryDuration = 7 // 7 days

            `when`(checkDeviceApprovalUseCase(context, true)).thenReturn(Result.success(DeviceApprovalStatus.APPROVED))
            `when`(repository.getActiveQRCount(mobileNumber)).thenReturn(Result.success(5))

            // When
            val result = useCase(context, name, mobileNumber, expiryDuration)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
        }
} 
