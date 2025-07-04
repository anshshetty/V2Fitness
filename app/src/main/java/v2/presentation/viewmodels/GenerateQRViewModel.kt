package v2.presentation.viewmodels

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import v2.data.models.QRCodeModel
import v2.domain.usecases.GenerateQRCodeUseCase
import v2.utils.QRCodeGenerator
import v2.utils.UserPreferences
import v2.utils.WhatsAppShareHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GenerateQRViewModel
    @Inject
    constructor(
        private val generateQRCodeUseCase: GenerateQRCodeUseCase,
        private val userPreferences: UserPreferences,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(
            GenerateQRUiState(
                name = userPreferences.getUserName() ?: "",
                mobileNumber = userPreferences.getMobileNumber() ?: "",
            ),
        )
        val uiState: StateFlow<GenerateQRUiState> = _uiState.asStateFlow()

        fun updateName(name: String) {
            _uiState.value = _uiState.value.copy(name = name, nameError = null)
        }

        fun updateMobileNumber(mobileNumber: String) {
            _uiState.value = _uiState.value.copy(mobileNumber = mobileNumber, mobileNumberError = null)
        }

        fun updateExpiryDuration(duration: Int) {
            _uiState.value = _uiState.value.copy(expiryDuration = duration, expiryError = null)
        }

        fun generateQRCode(context: Context) {
            viewModelScope.launch {
                val currentState = _uiState.value

                // Validate inputs
                if (!validateInputs(currentState)) {
                    return@launch
                }

                _uiState.value = currentState.copy(isLoading = true, error = null)

                try {
                    val result = generateQRCodeUseCase(
                        context = context,
                        name = currentState.name,
                        mobileNumber = currentState.mobileNumber,
                        expiryDuration = currentState.expiryDuration,
                    )

                    result
                        .onSuccess { qrCode ->
                            // Generate QR code bitmap
                            val bitmapResult = QRCodeGenerator.generateQRCodeBitmap(qrCode.encryptedPayload)
                            bitmapResult
                                .onSuccess { bitmap ->
                                    _uiState.value = currentState.copy(
                                        isLoading = false,
                                        generatedQRCode = qrCode,
                                        qrCodeBitmap = bitmap,
                                        isSuccess = true,
                                    )
                                }.onFailure { e ->
                                    _uiState.value = currentState.copy(
                                        isLoading = false,
                                        error = "Failed to generate QR code image: ${e.message}",
                                    )
                                }
                        }.onFailure { e ->
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = e.message,
                            )
                        }
                } catch (e: Exception) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = e.message,
                    )
                }
            }
        }

        private fun validateInputs(state: GenerateQRUiState): Boolean {
            var isValid = true
            var nameError: String? = null
            var mobileNumberError: String? = null
            var expiryError: String? = null

            if (state.name.isBlank() || state.name.length < 2) {
                nameError = "Name must be at least 2 characters"
                isValid = false
            }

            if (!state.mobileNumber.matches(Regex("^[0-9]{10}$"))) {
                mobileNumberError = "Mobile number must be 10 digits"
                isValid = false
            }

            if (state.expiryDuration <= 0 || state.expiryDuration > 90) {
                expiryError = "Duration must be between 1 and 90 days"
                isValid = false
            }

            if (!isValid) {
                _uiState.value = state.copy(
                    nameError = nameError,
                    mobileNumberError = mobileNumberError,
                    expiryError = expiryError,
                )
            }

            return isValid
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun resetForm() {
            _uiState.value = GenerateQRUiState(
                name = userPreferences.getUserName() ?: "",
                mobileNumber = userPreferences.getMobileNumber() ?: "",
            )
        }

        fun saveQRCode() {
            viewModelScope.launch {
                val currentState = _uiState.value
                if (currentState.qrCodeBitmap != null && currentState.generatedQRCode != null) {
                    _uiState.value = currentState.copy(isSaving = true)

                    try {
                        val result = saveQRCodeToGallery(currentState.qrCodeBitmap, currentState.generatedQRCode)
                        if (result) {
                            _uiState.value = currentState.copy(
                                isSaving = false,
                                error = "QR Code saved to gallery successfully!",
                            )
                        } else {
                            _uiState.value = currentState.copy(
                                isSaving = false,
                                error = "Failed to save QR code to gallery",
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.value = currentState.copy(
                            isSaving = false,
                            error = "Failed to save QR code: ${e.message}",
                        )
                    }
                }
            }
        }

        fun shareQRCode() {
            viewModelScope.launch {
                val currentState = _uiState.value
                if (currentState.qrCodeBitmap != null && currentState.generatedQRCode != null) {
                    _uiState.value = currentState.copy(isSharing = true)

                    try {
                        shareQRCodeBitmap(currentState.generatedQRCode, currentState.qrCodeBitmap)
                        _uiState.value = currentState.copy(isSharing = false)
                    } catch (e: Exception) {
                        _uiState.value = currentState.copy(
                            isSharing = false,
                            error = "Failed to share QR code: ${e.message}",
                        )
                    }
                }
            }
        }

        private fun saveQRCodeToGallery(
            bitmap: Bitmap,
            qrCode: QRCodeModel,
        ): Boolean =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    saveToMediaStore(bitmap, qrCode)
                } else {
                    // Use legacy storage for older versions
                    saveLegacy(bitmap, qrCode)
                }
            } catch (e: Exception) {
                false
            }

        private fun saveToMediaStore(
            bitmap: Bitmap,
            qrCode: QRCodeModel,
        ): Boolean =
            try {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                        "QR_${qrCode.name.replace(
                            " ",
                            "_",
                        )}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png",
                    )
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QR Codes")
                }

                val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    true
                } ?: false
            } catch (e: Exception) {
                false
            }

        private fun saveLegacy(
            bitmap: Bitmap,
            qrCode: QRCodeModel,
        ): Boolean {
            return try {
                // Check for write permission
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }

                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val qrCodesDir = File(picturesDir, "QR Codes")

                if (!qrCodesDir.exists()) {
                    qrCodesDir.mkdirs()
                }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "QR_${qrCode.name.replace(" ", "_")}_$timeStamp.png"
                val file = File(qrCodesDir, fileName)

                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                // Notify media scanner to make the image visible in gallery
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/png"),
                    null,
                )

                true
            } catch (e: Exception) {
                false
            }
        }

        private fun shareQRCodeBitmap(
            qrCode: QRCodeModel,
            bitmap: Bitmap,
        ) {
            try {
                // Save bitmap to cache directory
                val cachePath = File(context.cacheDir, "qr_codes")
                cachePath.mkdirs()
                val file = File(cachePath, "qr_${qrCode.qrId}.png")

                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                // Create content URI
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

                // Create share text with QR code details
                val expiryDate = Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L))
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val shareText = "QR Code for ${qrCode.name}\nMobile: ${qrCode.mobileNumber}\nValid until: ${dateFormat.format(expiryDate)}"

                // Create share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Gym QR Code - ${qrCode.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share QR Code")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                throw e
            }
        }

        fun shareQRCodeOnWhatsApp() {
            viewModelScope.launch {
                val currentState = _uiState.value
                if (currentState.qrCodeBitmap != null && currentState.generatedQRCode != null) {
                    _uiState.value = currentState.copy(isSharingWhatsApp = true)

                    try {
                        shareQRCodeOnWhatsAppBitmap(currentState.generatedQRCode, currentState.qrCodeBitmap)
                        _uiState.value = currentState.copy(isSharingWhatsApp = false)
                    } catch (e: Exception) {
                        _uiState.value = currentState.copy(
                            isSharingWhatsApp = false,
                            error = "Failed to share QR code on WhatsApp: ${e.message}",
                        )
                    }
                }
            }
        }

        private fun shareQRCodeOnWhatsAppBitmap(
            qrCode: QRCodeModel,
            bitmap: Bitmap,
        ) {
            try {
                // Create WhatsApp share text with V2 Fitness welcome message
                val welcomeMessage = "Welcome to V2 Fitness! 💪\n\nHere's your gym QR code for ${qrCode.name}.\n\nYour membership details:\n• Mobile: ${qrCode.mobileNumber}\n• Valid until: ${SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault(),
                ).format(
                    Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L)),
                )}\n\nPlease show this QR code at the gym entrance. We look forward to helping you achieve your fitness goals!"

                // Use the specialized WhatsApp helper
                val success = WhatsAppShareHelper.shareQRCodeWithMessage(
                    context = context,
                    bitmap = bitmap,
                    phoneNumber = qrCode.mobileNumber,
                    message = welcomeMessage,
                    qrId = qrCode.qrId,
                )

                if (!success) {
                    throw Exception("Failed to initiate WhatsApp sharing")
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

data class GenerateQRUiState(
    val name: String = "",
    val mobileNumber: String = "",
    val expiryDuration: Int = 1, // Default 1 day
    val nameError: String? = null,
    val mobileNumberError: String? = null,
    val expiryError: String? = null,
    val isLoading: Boolean = false,
    val generatedQRCode: QRCodeModel? = null,
    val qrCodeBitmap: Bitmap? = null,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val isSharing: Boolean = false,
    val isSharingWhatsApp: Boolean = false,
) 
