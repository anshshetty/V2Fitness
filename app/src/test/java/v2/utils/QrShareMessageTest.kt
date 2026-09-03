package v2.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class QrShareMessageTest {
    @Test
    fun `formatValidUntil adds expiry days to created date`() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(2026, Calendar.JANUARY, 15, 10, 0, 0)

        val formatted = QrShareMessage.formatValidUntil(
            createdAtMillis = calendar.timeInMillis,
            expiryDurationDays = 30,
            locale = Locale.US,
            timeZone = TimeZone.getTimeZone("UTC"),
        )

        assertEquals("Feb 14, 2026", formatted)
    }

    @Test
    fun `build uses organization name so another venue can rebrand`() {
        val message = QrShareMessage.build(
            orgName = "Riverside Studio",
            memberName = "Priya",
            mobileNumber = "9876543210",
            validUntil = "Feb 14, 2026",
        )

        assertEquals(
            "Welcome to Riverside Studio!\n\n" +
                "Here's your check-in QR for Priya.\n\n" +
                "Details:\n" +
                "• Mobile: 9876543210\n" +
                "• Valid until: Feb 14, 2026\n\n" +
                "Please show this QR at the entrance.",
            message,
        )
    }
}
