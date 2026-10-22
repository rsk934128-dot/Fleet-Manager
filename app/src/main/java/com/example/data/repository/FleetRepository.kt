package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Device
import com.example.data.model.Geofence
import com.example.data.model.SecurityLog
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FleetRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeDevices(userId: String): Flow<List<Device>> {
        val path = "users/$userId/devices"
        return db.collection("users").document(userId).collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Device::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveDevice(userId: String, device: Device): Result<String> {
        val docRef = if (device.id.isNotBlank()) {
            db.collection("users").document(userId).collection("devices").document(device.id)
        } else {
            db.collection("users").document(userId).collection("devices").document()
        }
        val targetId = docRef.id
        return try {
            if (device.id.isNotBlank() && device.createdAt != null) {
                docRef.set(device.copy(updatedAt = null)).await()
            } else {
                docRef.set(device.copy(id = targetId, createdAt = null, updatedAt = null)).await()
            }
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (device.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(userId: String, deviceId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("devices").document(deviceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSecurityLogs(userId: String): Flow<List<SecurityLog>> {
        val path = "users/$userId/security_logs"
        return db.collection("users").document(userId).collection("security_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SecurityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addSecurityLog(userId: String, log: SecurityLog): Result<String> {
        val docRef = db.collection("users").document(userId).collection("security_logs").document()
        return try {
            docRef.set(log.copy(id = docRef.id, createdAt = null)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGeofences(userId: String): Flow<List<Geofence>> {
        val path = "users/$userId/geofences"
        return db.collection("users").document(userId).collection("geofences")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Geofence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveGeofence(userId: String, geofence: Geofence): Result<String> {
        val docRef = if (geofence.id.isNotBlank()) {
            db.collection("users").document(userId).collection("geofences").document(geofence.id)
        } else {
            db.collection("users").document(userId).collection("geofences").document()
        }
        val targetId = docRef.id
        return try {
            if (geofence.id.isNotBlank() && geofence.createdAt != null) {
                docRef.set(geofence.copy(updatedAt = null)).await()
            } else {
                docRef.set(geofence.copy(id = targetId, createdAt = null, updatedAt = null)).await()
            }
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (geofence.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCameraSnapshots(userId: String): Flow<List<com.example.data.model.CameraSnapshot>> {
        val path = "users/$userId/camera_snapshots"
        return db.collection("users").document(userId).collection("camera_snapshots")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(com.example.data.model.CameraSnapshot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.timestamp }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveCameraSnapshot(userId: String, snapshot: com.example.data.model.CameraSnapshot): Result<String> {
        val docRef = if (snapshot.id.isNotBlank()) {
            db.collection("users").document(userId).collection("camera_snapshots").document(snapshot.id)
        } else {
            db.collection("users").document(userId).collection("camera_snapshots").document()
        }
        val targetId = docRef.id
        return try {
            docRef.set(snapshot.copy(id = targetId, createdAt = null)).await()
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCameraSnapshot(userId: String, snapshotId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("camera_snapshots").document(snapshotId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
