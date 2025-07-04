package v2.domain.usecases

import android.content.Context
import v2.data.models.QRCodeModel
import v2.domain.repository.QRCodeRepository
import javax.inject.Inject

class GenerateQRCodeUseCase
    @Inject
    constructor(
        private val repository: QRCodeRepository,
        private val checkDeviceApprovalUseCase: CheckDeviceApprovalUseCase,
    ) {
        suspend operator fun invoke(
            context: Context,
            name: String,
            mobileNumber: String,
            expiryDuration: Int,
        ): Result<QRCodeModel> {
            // DOUBLE CHECK: Verify device approval before generating QR code
            // Force refresh for critical operations like QR code generation
            val deviceApprovalResult = checkDeviceApprovalUseCase(context, forceRefresh = true)
            if (deviceApprovalResult.isFailure) {
                return Result.failure(
                    Exception("Unable to verify device approval. Please check your connection and try again."),
                )
            }

            val approvalStatus = deviceApprovalResult.getOrThrow()
            when (approvalStatus) {
                DeviceApprovalStatus.APPROVED -> {
                    // Device is approved, continue with QR code generation
                }
                DeviceApprovalStatus.PENDING -> {
                    return Result.failure(
                        Exception("Device is not approved for QR code generation. Please wait for admin approval."),
                    )
                }
                DeviceApprovalStatus.REJECTED -> {
                    return Result.failure(
                        Exception("Device access has been rejected. Contact administrator for assistance."),
                    )
                }
                DeviceApprovalStatus.NOT_REGISTERED -> {
                    return Result.failure(
                        Exception("Device is not registered. Please restart the app to register your device."),
                    )
                }
            }

            // Validate input
            val validationError = validateInput(name, mobileNumber, expiryDuration)
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }

            // Check rate limiting (max 5 QR codes per hour)
            val activeCount = repository.getActiveQRCount(mobileNumber).getOrElse { 0 }
            if (activeCount >= 5) {
                return Result.failure(
                    Exception("Rate limit exceeded. Maximum 5 active QR codes allowed per mobile number."),
                )
            }

            // Generate QR code
            return repository.generateQRCode(name, mobileNumber, expiryDuration)
        }

        private fun validateInput(
            name: String,
            mobileNumber: String,
            expiryDuration: Int,
        ): String? {
            if (name.isBlank() || name.length < 2) {
                return "Name must be at least 2 characters long"
            }

            if (!isValidMobileNumber(mobileNumber)) {
                return "Invalid mobile number. Must be 10 digits."
            }

            if (expiryDuration <= 0 || expiryDuration > 90) { // Max 90 days
                return "Expiry duration must be between 1 and 90 days"
            }

            return null
        }

        private fun isValidMobileNumber(mobileNumber: String): Boolean = mobileNumber.matches(Regex("^[0-9]{10}$"))
    } 
