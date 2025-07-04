package v2.presentation.viewmodels

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import v2.utils.DataSeeder
import v2.utils.UserPreferences

class TestingMenuViewModelTest {
    @Mock
    private lateinit var dataSeeder: DataSeeder

    @Mock
    private lateinit var userPreferences: UserPreferences

    @Mock
    private lateinit var context: Context

    private lateinit var viewModel: TestingMenuViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = TestingMenuViewModel(dataSeeder, userPreferences, context)
    }

    @Test
    fun `should seed dummy QR codes successfully`() =
        runTest {
            // Given
            `when`(dataSeeder.seedDummyQRCodes()).thenReturn(Result.success(10))

            // When
            viewModel.seedDummyQRCodes()

            // Then
            verify(dataSeeder).seedDummyQRCodes()

            // Wait for coroutine to complete and check state
            kotlinx.coroutines.delay(100)
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isSeedingQRCodes)
            assertEquals("Successfully created 10 dummy QR codes", uiState.successMessage)
            assertNull(uiState.error)
        }

    @Test
    fun `should handle QR codes seeding failure`() =
        runTest {
            // Given
            val error = Exception("Database error")
            `when`(dataSeeder.seedDummyQRCodes()).thenReturn(Result.failure(error))

            // When
            viewModel.seedDummyQRCodes()

            // Then
            verify(dataSeeder).seedDummyQRCodes()

            // Wait for coroutine to complete and check state
            kotlinx.coroutines.delay(100)
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isSeedingQRCodes)
            assertNull(uiState.successMessage)
            assertEquals("Failed to create dummy QR codes: Database error", uiState.error)
        }

    @Test
    fun `should seed dummy attendance successfully`() =
        runTest {
            // Given
            `when`(dataSeeder.seedDummyAttendance()).thenReturn(Result.success(15))

            // When
            viewModel.seedDummyAttendance()

            // Then
            verify(dataSeeder).seedDummyAttendance()

            // Wait for coroutine to complete and check state
            kotlinx.coroutines.delay(100)
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isSeedingAttendance)
            assertEquals("Successfully created 15 dummy attendance records", uiState.successMessage)
            assertNull(uiState.error)
        }

    @Test
    fun `should seed all dummy data successfully`() =
        runTest {
            // Given
            `when`(dataSeeder.seedAllDummyData()).thenReturn(Result.success(Pair(10, 15)))

            // When
            viewModel.seedAllDummyData()

            // Then
            verify(dataSeeder).seedAllDummyData()

            // Wait for coroutine to complete and check state
            kotlinx.coroutines.delay(100)
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isSeedingAllData)
            assertEquals("Successfully created 10 QR codes and 15 attendance records", uiState.successMessage)
            assertNull(uiState.error)
        }

    @Test
    fun `should clear dummy data successfully`() =
        runTest {
            // Given
            `when`(dataSeeder.clearDummyData()).thenReturn(Result.success(Unit))

            // When
            viewModel.clearDummyData()

            // Then
            verify(dataSeeder).clearDummyData()

            // Wait for coroutine to complete and check state
            kotlinx.coroutines.delay(100)
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isClearingData)
            assertEquals("Dummy data cleared successfully", uiState.successMessage)
            assertNull(uiState.error)
        }

    @Test
    fun `should clear messages`() {
        // Given
        viewModel.clearMessages()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.error)
        assertNull(uiState.successMessage)
    }

    @Test
    fun `should clear error only`() {
        // When
        viewModel.clearError()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.error)
    }

    @Test
    fun `should clear success message only`() {
        // When
        viewModel.clearSuccessMessage()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.successMessage)
    }

    @Test
    fun `should track operation in progress correctly`() =
        runTest {
            // Given
            `when`(dataSeeder.seedDummyQRCodes()).thenReturn(Result.success(10))

            // When
            viewModel.seedDummyQRCodes()

            // Then - initially should be in progress
            assertTrue(viewModel.uiState.value.isSeedingQRCodes)
            assertTrue(viewModel.uiState.value.isAnyOperationInProgress)

            // Wait for completion
            kotlinx.coroutines.delay(100)

            // Then - should no longer be in progress
            assertFalse(viewModel.uiState.value.isSeedingQRCodes)
            assertFalse(viewModel.uiState.value.isAnyOperationInProgress)
        }

    @Test
    fun `should prevent multiple operations simultaneously`() {
        // Given
        val initialState = viewModel.uiState.value

        // When
        val stateWithQRSeeding = initialState.copy(isSeedingQRCodes = true)

        // Then
        assertTrue(stateWithQRSeeding.isAnyOperationInProgress)

        // When
        val stateWithAttendanceSeeding = initialState.copy(isSeedingAttendance = true)

        // Then
        assertTrue(stateWithAttendanceSeeding.isAnyOperationInProgress)

        // When
        val stateWithAllDataSeeding = initialState.copy(isSeedingAllData = true)

        // Then
        assertTrue(stateWithAllDataSeeding.isAnyOperationInProgress)

        // When
        val stateWithClearing = initialState.copy(isClearingData = true)

        // Then
        assertTrue(stateWithClearing.isAnyOperationInProgress)
    }
} 
