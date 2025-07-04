package v2.presentation.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import v2.data.models.QRCodeModel
import v2.data.models.QRStatus
import v2.domain.repository.QRCodeRepository
import v2.utils.QRCodeGenerator
import v2.utils.UserPreferences
import v2.utils.WhatsAppShareHelper
import javax.inject.Inject

@HiltViewModel
class QRListViewModel
    @Inject
    constructor(
        private val repository: QRCodeRepository,
        private val userPreferences: UserPreferences,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(QRListUiState())
        val uiState: StateFlow<QRListUiState> = _uiState.asStateFlow()

        private val userMobileNumber: String
            get() = userPreferences.getMobileNumber() ?: ""

        init {
            observeQRCodes()
        }

        private fun observeQRCodes() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Observe ALL QR codes from ALL users (not filtered by mobile number)
                repository
                    .getAllQRCodes()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            qrCodes = emptyList(),
                            error = "Failed to load QR codes: ${e.message}",
                        )
                    }.collect { qrCodes ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            qrCodes = qrCodes,
                            error = null,
                        )
                        applyFilters()
                    }
            }
        }

        fun showExtendDialog(qrId: String) {
            val qrCode = _uiState.value.qrCodes.find { it.qrId == qrId }
            if (qrCode != null) {
                _uiState.value = _uiState.value.copy(
                    showExtendDialog = true,
                    selectedQRCode = qrCode,
                )
            }
        }

        fun hideExtendDialog() {
            _uiState.value = _uiState.value.copy(
                showExtendDialog = false,
                selectedQRCode = null,
            )
        }

        fun extendQRCode(additionalDays: Int) {
            val qrCode = _uiState.value.selectedQRCode ?: return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isExtending = true)

                repository
                    .extendQRCode(qrCode.qrId, additionalDays)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isExtending = false,
                            showExtendDialog = false,
                            selectedQRCode = null,
                        )
                        // QR codes will be automatically updated via Flow
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isExtending = false,
                            error = e.message,
                        )
                    }
            }
        }

        fun showDisableDialog(qrId: String) {
            val qrCode = _uiState.value.qrCodes.find { it.qrId == qrId }
            if (qrCode != null) {
                _uiState.value = _uiState.value.copy(
                    showDisableDialog = true,
                    selectedQRCode = qrCode,
                )
            }
        }

        fun hideDisableDialog() {
            _uiState.value = _uiState.value.copy(
                showDisableDialog = false,
                selectedQRCode = null,
            )
        }

        fun disableQRCode() {
            val qrCode = _uiState.value.selectedQRCode ?: return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isDisabling = true)

                repository
                    .disableQRCode(qrCode.qrId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isDisabling = false,
                            showDisableDialog = false,
                            selectedQRCode = null,
                        )
                        // QR codes will be automatically updated via Flow
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isDisabling = false,
                            error = e.message,
                        )
                    }
            }
        }

        fun shareQRCode(qrId: String) {
            viewModelScope.launch {
                val qrCode = _uiState.value.qrCodes.find { it.qrId == qrId } ?: return@launch

                _uiState.value = _uiState.value.copy(isSharing = true)

                try {
                    // Generate QR code bitmap
                    val bitmapResult = QRCodeGenerator.generateQRCodeBitmap(qrCode.encryptedPayload, 1024, 1024)

                    bitmapResult
                        .onSuccess { bitmap ->
                            // Create share intent
                            shareQRCodeBitmap(qrCode, bitmap)
                            _uiState.value = _uiState.value.copy(isSharing = false)
                        }.onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                isSharing = false,
                                error = "Failed to generate QR code for sharing: ${e.message}",
                            )
                        }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        error = "Failed to share QR code: ${e.message}",
                    )
                }
            }
        }

        private fun shareQRCodeBitmap(
            qrCode: QRCodeModel,
            bitmap: Bitmap,
        ) {
            try {
                // Save bitmap to cache directory
                val cachePath = java.io.File(context.cacheDir, "qr_codes")
                cachePath.mkdirs()
                val file = java.io.File(cachePath, "qr_${qrCode.qrId}.png")

                val fileOutputStream = java.io.FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                // Create content URI
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

                // Create share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "QR Code for ${qrCode.name}\nValid until: ${java.text.SimpleDateFormat(
                            "MMM dd, yyyy",
                            java.util.Locale.getDefault(),
                        ).format(java.util.Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L)))}",
                    )
                    putExtra(Intent.EXTRA_SUBJECT, "Gym QR Code - ${qrCode.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share QR Code")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to share QR code: ${e.message}",
                )
            }
        }

        fun shareQRCodeOnWhatsApp(qrId: String) {
            viewModelScope.launch {
                val qrCode = _uiState.value.qrCodes.find { it.qrId == qrId } ?: return@launch

                _uiState.value = _uiState.value.copy(isSharingWhatsApp = true)

                try {
                    // Generate QR code bitmap
                    val bitmapResult = QRCodeGenerator.generateQRCodeBitmap(qrCode.encryptedPayload, 1024, 1024)

                    bitmapResult
                        .onSuccess { bitmap ->
                            // Share on WhatsApp
                            shareQRCodeOnWhatsAppBitmap(qrCode, bitmap)
                            _uiState.value = _uiState.value.copy(isSharingWhatsApp = false)
                        }.onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                isSharingWhatsApp = false,
                                error = "Failed to generate QR code for WhatsApp sharing: ${e.message}",
                            )
                        }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isSharingWhatsApp = false,
                        error = "Failed to share QR code on WhatsApp: ${e.message}",
                    )
                }
            }
        }

        private fun shareQRCodeOnWhatsAppBitmap(
            qrCode: QRCodeModel,
            bitmap: Bitmap,
        ) {
            try {
                // Create WhatsApp share text with V2 Fitness welcome message
                val welcomeMessage = "Welcome to V2 Fitness! 💪\n\nHere's your gym QR code for ${qrCode.name}.\n\nYour membership details:\n• Mobile: ${qrCode.mobileNumber}\n• Valid until: ${java.text.SimpleDateFormat(
                    "MMM dd, yyyy",
                    java.util.Locale.getDefault(),
                ).format(
                    java.util.Date(qrCode.createdAt.toDate().time + (qrCode.expiryDuration * 24 * 60 * 60 * 1000L)),
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
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to initiate WhatsApp sharing",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to share QR code on WhatsApp: ${e.message}",
                )
            }
        }

        fun refreshData() {
            // Data is automatically refreshed via Flow observer, but we can trigger a manual refresh if needed
            observeQRCodes()
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun updateSearchQuery(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            applyFilters()
        }

        fun updateStatusFilter(filter: QRStatusFilter) {
            _uiState.value = _uiState.value.copy(selectedStatusFilter = filter)
            applyFilters()
        }

        fun showFilterDialog() {
            _uiState.value = _uiState.value.copy(showFilterDialog = true)
        }

        fun hideFilterDialog() {
            _uiState.value = _uiState.value.copy(showFilterDialog = false)
        }

        private fun applyFilters() {
            val currentState = _uiState.value
            var filteredList = currentState.qrCodes

            // Apply search filter
            if (currentState.searchQuery.isNotBlank()) {
                filteredList = filteredList.filter { qrCode ->
                    qrCode.name.contains(currentState.searchQuery, ignoreCase = true) ||
                        qrCode.mobileNumber.contains(currentState.searchQuery, ignoreCase = true)
                }
            }

            // Apply status filter
            filteredList = when (currentState.selectedStatusFilter) {
                QRStatusFilter.ALL -> filteredList
                QRStatusFilter.ACTIVE -> filteredList.filter {
                    it.getCurrentStatus() == QRStatus.ACTIVE ||
                        it.getCurrentStatus() == QRStatus.USED
                }
                QRStatusFilter.EXPIRED -> filteredList.filter { it.getCurrentStatus() == QRStatus.EXPIRED }
                QRStatusFilter.DISABLED -> filteredList.filter { it.getCurrentStatus() == QRStatus.DISABLED }
                QRStatusFilter.USED -> filteredList.filter { it.getCurrentStatus() == QRStatus.USED }
            }

            _uiState.value = currentState.copy(filteredQrCodes = filteredList)
        }
    }

data class QRListUiState(
    val isLoading: Boolean = false,
    val qrCodes: List<QRCodeModel> = emptyList(),
    val filteredQrCodes: List<QRCodeModel> = emptyList(),
    val error: String? = null,
    val showExtendDialog: Boolean = false,
    val showDisableDialog: Boolean = false,
    val selectedQRCode: QRCodeModel? = null,
    val isExtending: Boolean = false,
    val isDisabling: Boolean = false,
    val isSharing: Boolean = false,
    val isSharingWhatsApp: Boolean = false,
    val searchQuery: String = "",
    val selectedStatusFilter: QRStatusFilter = QRStatusFilter.ALL,
    val showFilterDialog: Boolean = false,
)

enum class QRStatusFilter {
    ALL,
    ACTIVE,
    EXPIRED,
    DISABLED,
    USED,
}
