package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Device
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class DeviceRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  /**
   * Real-time stream of all fleet devices belonging to the authenticated user.
   */
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

  /**
   * Real-time stream of a single device document using Firestore DocumentReference.snapshots().
   * Automatically updates when document fields (status, battery, metrics) mutate in Firestore.
   */
  fun observeDevice(userId: String, deviceId: String): Flow<Device?> {
    val docRef = db.collection("users").document(userId).collection("devices").document(deviceId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(Device::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }
  }

  /**
   * One-time fetch of all fleet devices from Firestore.
   */
  suspend fun getDevices(userId: String): Result<List<Device>> {
    val path = "users/$userId/devices"
    return try {
      val snapshot = db.collection("users").document(userId).collection("devices")
        .get()
        .await()
      val list = snapshot.toObjects(Device::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  /**
   * Retrieve a specific device by its document ID.
   */
  suspend fun getDeviceById(userId: String, deviceId: String): Result<Device?> {
    val docRef = db.collection("users").document(userId).collection("devices").document(deviceId)
    return try {
      val snapshot = docRef.get().await()
      val device = snapshot.toObject(Device::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      Result.success(device)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Add a new device to the cloud database.
   * Generates a new unique document ID and sets server timestamps.
   */
  suspend fun addDevice(userId: String, device: Device): Result<String> {
    val collectionRef = db.collection("users").document(userId).collection("devices")
    val docRef = if (device.id.isNotBlank()) collectionRef.document(device.id) else collectionRef.document()
    val generatedId = docRef.id

    return try {
      val payload = device.copy(
        id = generatedId,
        createdAt = null, // Replaced by @ServerTimestamp
        updatedAt = null
      )
      docRef.set(payload).await()
      Result.success(generatedId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Update an existing device document.
   * Preserves existing createdAt and updates updatedAt.
   */
  suspend fun updateDevice(userId: String, device: Device): Result<Unit> {
    require(device.id.isNotBlank()) { "Device ID must not be blank for update" }
    val docRef = db.collection("users").document(userId).collection("devices").document(device.id)

    return try {
      val payload = device.copy(updatedAt = null) // Triggers fresh @ServerTimestamp
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Delete a device from the cloud database.
   */
  suspend fun deleteDevice(userId: String, deviceId: String): Result<Unit> {
    require(deviceId.isNotBlank()) { "Device ID must not be blank for deletion" }
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
}
