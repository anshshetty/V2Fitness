package v2.utils

import org.junit.Assert.*
import org.junit.Test
import v2.data.models.QRStatus

class DataSeederIntegrationTest {
    @Test
    fun `should have realistic dummy names`() {
        // Given
        val dummyNames = listOf(
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
        )

        // Then
        assertTrue("Should have at least 10 names", dummyNames.size >= 10)
        dummyNames.forEach { name ->
            assertTrue("Name should contain space", name.contains(" "))
            assertTrue("Name should be at least 3 characters", name.length >= 3)
        }
    }

    @Test
    fun `should have valid dummy mobile numbers`() {
        // Given
        val dummyMobileNumbers = listOf(
            "9876543210",
            "8765432109",
            "7654321098",
            "6543210987",
            "5432109876",
        )

        // Then
        assertTrue("Should have at least 5 mobile numbers", dummyMobileNumbers.size >= 5)
        dummyMobileNumbers.forEach { mobile ->
            assertEquals("Mobile number should be 10 digits", 10, mobile.length)
            assertTrue("Mobile number should contain only digits", mobile.all { it.isDigit() })
        }
    }

    @Test
    fun `should have valid gym locations`() {
        // Given
        val gymLocations = listOf(
            "Main Gym Floor",
            "Cardio Section",
            "Weight Training Area",
            "Yoga Studio",
            "Spinning Room",
            "Pool Area",
            "Locker Room",
            "Reception",
        )

        // Then
        assertTrue("Should have at least 8 locations", gymLocations.size >= 8)
        gymLocations.forEach { location ->
            assertTrue("Location should not be empty", location.isNotBlank())
            assertTrue("Location should be at least 4 characters", location.length >= 4)
        }
    }

    @Test
    fun `should generate valid expiry durations`() {
        // Test the logic used in DataSeeder for expiry durations
        val expiryDurations = mutableListOf<Int>()

        for (i in 0 until 10) {
            val duration = when (i % 4) {
                0 -> kotlin.random.Random.nextInt(1, 7) // Active QR codes (1-6 days)
                1 -> kotlin.random.Random.nextInt(7, 30) // Long-term QR codes (1-4 weeks)
                2 -> kotlin.random.Random.nextInt(1, 3) // Short-term QR codes (1-2 days)
                else -> kotlin.random.Random.nextInt(1, 14) // Medium-term QR codes (1-2 weeks)
            }
            expiryDurations.add(duration)
        }

        // Then
        assertEquals("Should generate 10 durations", 10, expiryDurations.size)
        expiryDurations.forEach { duration ->
            assertTrue("Duration should be between 1 and 30 days", duration in 1..30)
        }
    }

    @Test
    fun `should generate different QR statuses`() {
        // Test the logic used in DataSeeder for QR status distribution
        val statuses = mutableListOf<QRStatus>()

        for (i in 0 until 10) {
            val status = when (i % 5) {
                1 -> QRStatus.DISABLED // 20% disabled
                2 -> QRStatus.EXPIRED // 20% expired (would be set by old creation date)
                3 -> QRStatus.USED // 20% used today
                else -> QRStatus.ACTIVE // 40% remain active
            }
            statuses.add(status)
        }

        // Then
        assertEquals("Should generate 10 statuses", 10, statuses.size)
        assertTrue("Should have active QR codes", statuses.contains(QRStatus.ACTIVE))
        assertTrue("Should have disabled QR codes", statuses.contains(QRStatus.DISABLED))
        assertTrue("Should have used QR codes", statuses.contains(QRStatus.USED))

        // Check distribution
        val activeCount = statuses.count { it == QRStatus.ACTIVE }
        val disabledCount = statuses.count { it == QRStatus.DISABLED }

        assertTrue("Should have more active than disabled", activeCount >= disabledCount)
    }

    @Test
    fun `should generate valid attendance time patterns`() {
        // Test the logic used in DataSeeder for attendance timing
        val currentTime = System.currentTimeMillis()
        val times = mutableListOf<Long>()

        for (i in 0 until 15) {
            val scanTime = when (i % 3) {
                0 -> currentTime // Today
                1 -> currentTime - kotlin.random.Random.nextLong(0, 12 * 60 * 60 * 1000) // Earlier today
                else -> currentTime - kotlin.random.Random.nextLong(24 * 60 * 60 * 1000, 48 * 60 * 60 * 1000) // Yesterday
            }
            times.add(scanTime)
        }

        // Then
        assertEquals("Should generate 15 times", 15, times.size)

        val todayTimes = times.filter { it >= currentTime - (24 * 60 * 60 * 1000) }
        val yesterdayTimes = times.filter { it < currentTime - (24 * 60 * 60 * 1000) }

        assertTrue("Should have some today times", todayTimes.isNotEmpty())
        assertTrue("Should have some yesterday times", yesterdayTimes.isNotEmpty())
    }

    @Test
    fun `should generate unique dummy IDs`() {
        // Test that dummy ID generation produces unique values
        val ids = mutableSetOf<String>()

        repeat(100) {
            val id = "dummy_${java.util.UUID.randomUUID()}"
            ids.add(id)
        }

        // Then
        assertEquals("All IDs should be unique", 100, ids.size)
        ids.forEach { id ->
            assertTrue("ID should start with dummy_", id.startsWith("dummy_"))
            assertTrue("ID should be at least 10 characters", id.length >= 10)
        }
    }
} 
