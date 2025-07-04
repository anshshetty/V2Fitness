package v2.domain.usecases

import android.content.Context
import v2.domain.repository.DeviceApprovalRepository
import v2.utils.DeviceUtils
import javax.inject.Inject

class RegisterDeviceUseCase
    @Inject
    constructor(
        private val deviceApprovalRepository: DeviceApprovalRepository,
    ) {
        suspend operator fun invoke(context: Context): Result<Unit> {
            val deviceApproval = DeviceUtils.createDeviceApproval(context)
            return deviceApprovalRepository.registerDevice(deviceApproval)
        }
    } 
