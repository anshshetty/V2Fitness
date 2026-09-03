package v2.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object QrShareMessage {
    fun formatValidUntil(
        createdAtMillis: Long,
        expiryDurationDays: Int,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val expiryMillis = createdAtMillis + (expiryDurationDays * 24 * 60 * 60 * 1000L)
        val formatter = SimpleDateFormat("MMM dd, yyyy", locale)
        formatter.timeZone = timeZone
        return formatter.format(Date(expiryMillis))
    }

    fun build(
        orgName: String,
        memberName: String,
        mobileNumber: String,
        validUntil: String,
    ): String =
        "Welcome to $orgName!\n\n" +
            "Here's your check-in QR for $memberName.\n\n" +
            "Details:\n" +
            "• Mobile: $mobileNumber\n" +
            "• Valid until: $validUntil\n\n" +
            "Please show this QR at the entrance."
}
