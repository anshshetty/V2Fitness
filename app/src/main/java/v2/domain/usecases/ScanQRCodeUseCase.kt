package v2.domain.usecases

import v2.data.models.ScanResult
import v2.domain.repository.QRCodeRepository
import javax.inject.Inject

class ScanQRCodeUseCase
    @Inject
    constructor(
        private val repository: QRCodeRepository,
    ) {
        suspend operator fun invoke(
            encryptedPayload: String,
            deviceId: String,
        ): Result<ScanResult> {
            if (encryptedPayload.isBlank()) {
                return Result.success(
                    ScanResult.Error(
                        "Invalid QR code format",
                        v2.data.models.ScanErrorType.INVALID_FORMAT,
                    ),
                )
            }

            return repository.scanQRCode(encryptedPayload, deviceId)
        }
    } 
