package v2.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object WhatsAppShareHelper {
    private const val TAG = "WhatsAppShareHelper"

    fun shareQRCodeWithMessage(
        context: Context,
        bitmap: Bitmap,
        phoneNumber: String,
        message: String,
        qrId: String,
    ): Boolean {
        return try {
            val imageFile = saveImageToCacheAsFile(context, bitmap, qrId)
            if (imageFile != null) {
                val imageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile,
                )

                // Method 1: Try direct WhatsApp sharing with ACTION_SEND
                if (shareDirectlyToWhatsApp(context, imageUri, message, phoneNumber)) {
                    Log.d(TAG, "Direct WhatsApp sharing successful")
                    return true
                }

                // Method 2: Try WhatsApp URL scheme approach
                if (shareViaWhatsAppUrlScheme(context, imageUri, message, phoneNumber)) {
                    Log.d(TAG, "WhatsApp URL scheme sharing successful")
                    return true
                }

                // Method 3: Try WhatsApp sendto scheme
                if (shareViaWhatsAppSendTo(context, imageUri, message, phoneNumber)) {
                    Log.d(TAG, "WhatsApp sendto sharing successful")
                    return true
                }

                // Method 4: If all WhatsApp-specific methods fail, fall back to generic chooser
                Log.w(TAG, "All WhatsApp-specific methods failed, falling back to generic chooser")
                return shareViaGenericChooser(context, imageUri, message)
            } else {
                Log.e(TAG, "Failed to create image file")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via WhatsApp", e)
            false
        }
    }

    private fun shareDirectlyToWhatsApp(
        context: Context,
        imageUri: Uri,
        message: String,
        phoneNumber: String,
    ): Boolean {
        val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

        for (packageName in whatsAppPackages) {
            try {
                val whatsAppIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra("jid", "91$phoneNumber@s.whatsapp.net")
                    setPackage(packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                context.grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(whatsAppIntent)
                Log.d(TAG, "Successfully shared to $packageName")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share directly to $packageName", e)
            }
        }
        return false
    }

    private fun shareViaWhatsAppUrlScheme(
        context: Context,
        imageUri: Uri,
        message: String,
        phoneNumber: String,
    ): Boolean {
        try {
            // Clean phone number - remove any non-digits
            val cleanedPhoneNumber = phoneNumber.replace(Regex("[^\\d]"), "")
            val whatsAppUrl = "https://wa.me/$cleanedPhoneNumber?text=${Uri.encode(message)}"

            val urlIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(whatsAppUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (urlIntent.resolveActivity(context.packageManager) != null) {
                Log.d(TAG, "Opening WhatsApp chat via URL: $whatsAppUrl")
                context.startActivity(urlIntent)

                // After opening the chat, try to share the image via a delayed intent
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    {
                        shareImageToActiveWhatsApp(context, imageUri)
                    },
                    2000,
                ) // 2 second delay to let WhatsApp open

                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp URL scheme sharing failed", e)
        }
        return false
    }

    private fun shareViaWhatsAppSendTo(
        context: Context,
        imageUri: Uri,
        message: String,
        phoneNumber: String,
    ): Boolean {
        try {
            val cleanedPhoneNumber = phoneNumber.replace(Regex("[^\\d]"), "")
            val whatsAppUri = "whatsapp://send?phone=91$cleanedPhoneNumber&text=${Uri.encode(message)}"

            val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

            for (packageName in whatsAppPackages) {
                if (isAppInstalled(context, packageName)) {
                    try {
                        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(whatsAppUri)
                            setPackage(packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }

                        if (sendToIntent.resolveActivity(context.packageManager) != null) {
                            Log.d(TAG, "Using $packageName sendto with phone: $cleanedPhoneNumber")
                            context.startActivity(sendToIntent)

                            // Share image after opening the chat
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                {
                                    shareImageToActiveWhatsApp(context, imageUri, packageName)
                                },
                                1500,
                            )

                            return true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to use sendto with $packageName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp sendto sharing failed", e)
        }
        return false
    }

    private fun shareImageToActiveWhatsApp(
        context: Context,
        imageUri: Uri,
        specificPackage: String? = null,
    ) {
        try {
            val whatsAppPackages = if (specificPackage != null) {
                listOf(specificPackage)
            } else {
                listOf("com.whatsapp", "com.whatsapp.w4b")
            }

            for (packageName in whatsAppPackages) {
                if (isAppInstalled(context, packageName)) {
                    try {
                        val imageIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            setPackage(packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        context.grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(imageIntent)
                        Log.d(TAG, "Shared image to active $packageName chat")
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to share image to $packageName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share image to active WhatsApp", e)
        }
    }

    private fun saveImageToCacheAsFile(
        context: Context,
        bitmap: Bitmap,
        qrId: String,
    ): File? {
        return try {
            // Create cache directory
            val cacheDir = File(context.cacheDir, "qr_codes")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Create unique filename
            val fileName = "qr_${qrId}_${System.currentTimeMillis()}.png"
            val imageFile = File(cacheDir, fileName)

            // Save bitmap to file
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            // Verify file was created
            if (!imageFile.exists() || imageFile.length() == 0L) {
                Log.e(TAG, "File creation failed or file is empty")
                return null
            }

            Log.d(TAG, "Image saved: ${imageFile.absolutePath}, size: ${imageFile.length()}")
            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image to cache", e)
            null
        }
    }

    private fun shareViaGenericChooser(
        context: Context,
        imageUri: Uri,
        message: String,
    ): Boolean =
        try {
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(genericIntent, "Share QR Code")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            Log.d(TAG, "Using generic share chooser")
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Generic share chooser failed", e)
            false
        }

    private fun isAppInstalled(
        context: Context,
        packageName: String,
    ): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
}
