package v2.domain.usecases

import v2.data.models.AttendanceRecord
import v2.domain.repository.QRCodeRepository
import javax.inject.Inject

class CleanupDuplicateAttendanceUseCase
    @Inject
    constructor(
        private val repository: QRCodeRepository,
    ) {
        suspend fun cleanupDuplicates(): Result<Int> = repository.cleanupDuplicateAttendance()

        suspend fun getDuplicates(): Result<List<AttendanceRecord>> = repository.getDuplicateAttendanceRecords()
    } 
