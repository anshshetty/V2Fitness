package v2.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import v2.data.models.DeviceApproval
import v2.domain.repository.DeviceApprovalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceApprovalRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : DeviceApprovalRepository {
        companion object {
            private const val COLLECTION_NAME = "approved_devices"
            private const val TAG = "DeviceApprovalRepo"
        }

        override suspend fun checkDeviceApprovalStatus(deviceId: String): Result<DeviceApproval?> =
            try {
                Log.d(TAG, "Checking approval status for device: $deviceId")

                val document = firestore
                    .collection(COLLECTION_NAME)
                    .document(deviceId)
                    .get()
                    .await()

                if (document.exists()) {
                    val deviceApproval = document.toObject(DeviceApproval::class.java)
                    Log.d(TAG, "Device found with status: ${deviceApproval?.deviceStatus}")
                    Result.success(deviceApproval)
                } else {
                    Log.d(TAG, "Device not found in database")
                    Result.success(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking device approval status", e)
                Result.failure(e)
            }

        override suspend fun registerDevice(deviceApproval: DeviceApproval): Result<Unit> =
            try {
                Log.d(TAG, "Registering device: ${deviceApproval.deviceId}")

                firestore
                    .collection(COLLECTION_NAME)
                    .document(deviceApproval.deviceId)
                    .set(deviceApproval)
                    .await()

                Log.d(TAG, "Device registered successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering device", e)
                Result.failure(e)
            }

        override suspend fun updateLastActiveTime(deviceId: String): Result<Unit> =
            try {
                Log.d(TAG, "Updating last active time for device: $deviceId")

                firestore
                    .collection(COLLECTION_NAME)
                    .document(deviceId)
                    .update("lastActiveDate", Timestamp.now())
                    .await()

                Log.d(TAG, "Last active time updated successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last active time", e)
                Result.failure(e)
            }

        override fun observeDeviceApprovalStatus(deviceId: String): Flow<DeviceApproval?> =
            firestore
                .collection(COLLECTION_NAME)
                .document(deviceId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(DeviceApproval::class.java)
                    } else {
                        null
                    }
                }
    } 
