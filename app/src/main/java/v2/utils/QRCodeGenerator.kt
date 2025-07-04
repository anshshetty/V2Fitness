package v2.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {
    fun generateQRCodeBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
    ): Result<Bitmap> =
        try {
            val writer = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bitmap = createBitmap(bitMatrix)

            Result.success(bitmap)
        } catch (e: WriterException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun createBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    fun generateQRCodeWithLogo(
        content: String,
        logo: Bitmap? = null,
        width: Int = 512,
        height: Int = 512,
    ): Result<Bitmap> {
        return try {
            val qrResult = generateQRCodeBitmap(content, width, height)
            if (qrResult.isFailure) {
                return qrResult
            }

            val qrBitmap = qrResult.getOrThrow()

            if (logo != null) {
                val combinedBitmap = addLogoToQRCode(qrBitmap, logo)
                Result.success(combinedBitmap)
            } else {
                Result.success(qrBitmap)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addLogoToQRCode(
        qrBitmap: Bitmap,
        logo: Bitmap,
    ): Bitmap {
        val combinedBitmap = qrBitmap.copy(qrBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(combinedBitmap)

        val logoSize = (qrBitmap.width * 0.2f).toInt() // Logo is 20% of QR code size
        val logoX = (qrBitmap.width - logoSize) / 2f
        val logoY = (qrBitmap.height - logoSize) / 2f

        val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)

        // Draw white background for logo
        val paint = android.graphics.Paint().apply {
            color = Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        val logoRect = android.graphics.RectF(
            logoX - 10f,
            logoY - 10f,
            logoX + logoSize + 10f,
            logoY + logoSize + 10f,
        )
        canvas.drawRoundRect(logoRect, 10f, 10f, paint)

        // Draw logo
        canvas.drawBitmap(scaledLogo, logoX, logoY, null)

        return combinedBitmap
    }
} 
