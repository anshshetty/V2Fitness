package v2.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File

object ImageShareDebugUtils {
    private const val TAG = "ImageShareDebug"

    fun verifyImageCreation(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): String {
        val debugInfo = StringBuilder()

        try {
            // Check cache directory
            val cachePath = File(context.cacheDir, "qr_codes")
            debugInfo.appendLine("Cache directory path: ${cachePath.absolutePath}")
            debugInfo.appendLine("Cache directory exists: ${cachePath.exists()}")
            debugInfo.appendLine("Cache directory can write: ${cachePath.canWrite()}")

            if (!cachePath.exists()) {
                cachePath.mkdirs()
                debugInfo.appendLine("Created cache directory: ${cachePath.exists()}")
            }

            // Create test file
            val file = File(cachePath, fileName)
            debugInfo.appendLine("Target file path: ${file.absolutePath}")

            // Write bitmap
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            // Verify file creation
            debugInfo.appendLine("File exists after creation: ${file.exists()}")
            debugInfo.appendLine("File size: ${file.length()} bytes")
            debugInfo.appendLine("File can read: ${file.canRead()}")

            // Test URI creation
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            debugInfo.appendLine("Content URI: $contentUri")

            // Test URI permissions
            try {
                context.grantUriPermission("com.whatsapp", contentUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                debugInfo.appendLine("URI permission granted to WhatsApp: SUCCESS")
            } catch (e: Exception) {
                debugInfo.appendLine("URI permission grant failed: ${e.message}")
            }

            Log.d(TAG, debugInfo.toString())
        } catch (e: Exception) {
            debugInfo.appendLine("ERROR: ${e.message}")
            Log.e(TAG, "Image verification failed", e)
        }

        return debugInfo.toString()
    }

    fun logWhatsAppIntentDetails(
        context: Context,
        contentUri: android.net.Uri,
    ) {
        val debugInfo = StringBuilder()

        // Check WhatsApp installation
        val whatsAppIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage("com.whatsapp")
        }

        val isWhatsAppInstalled = whatsAppIntent.resolveActivity(context.packageManager) != null
        debugInfo.appendLine("WhatsApp installed: $isWhatsAppInstalled")

        // Check WhatsApp Business installation
        val whatsAppBusinessIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage("com.whatsapp.w4b")
        }

        val isWhatsAppBusinessInstalled = whatsAppBusinessIntent.resolveActivity(context.packageManager) != null
        debugInfo.appendLine("WhatsApp Business installed: $isWhatsAppBusinessInstalled")

        // Check FileProvider authority
        debugInfo.appendLine("FileProvider authority: ${context.packageName}.fileprovider")
        debugInfo.appendLine("Content URI scheme: ${contentUri.scheme}")
        debugInfo.appendLine("Content URI authority: ${contentUri.authority}")
        debugInfo.appendLine("Content URI path: ${contentUri.path}")

        Log.d(TAG, debugInfo.toString())
    }
} 
