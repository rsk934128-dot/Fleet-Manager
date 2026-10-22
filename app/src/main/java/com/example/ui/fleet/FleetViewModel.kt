package com.example.ui.fleet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Device
import com.example.data.model.Geofence
import com.example.data.model.SecurityLog
import com.example.data.repository.FleetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class FleetViewModel(
  private val repository: FleetRepository,
  private val currentUserId: String
) : ViewModel() {

  val devices: StateFlow<UiState<List<Device>>> = repository.observeDevices(currentUserId)
    .map<List<Device>, UiState<List<Device>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing devices", error)
      emit(UiState.Error(error.localizedMessage ?: "Failed to observe devices"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val securityLogs: StateFlow<UiState<List<SecurityLog>>> = repository.observeSecurityLogs(currentUserId)
    .map<List<SecurityLog>, UiState<List<SecurityLog>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing security logs", error)
      emit(UiState.Error(error.localizedMessage ?: "Failed to observe security logs"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val geofences: StateFlow<UiState<List<Geofence>>> = repository.observeGeofences(currentUserId)
    .map<List<Geofence>, UiState<List<Geofence>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing geofences", error)
      emit(UiState.Error(error.localizedMessage ?: "Failed to observe geofences"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val cameraSnapshots: StateFlow<UiState<List<com.example.data.model.CameraSnapshot>>> = repository.observeCameraSnapshots(currentUserId)
    .map<List<com.example.data.model.CameraSnapshot>, UiState<List<com.example.data.model.CameraSnapshot>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing camera snapshots", error)
      emit(UiState.Error(error.localizedMessage ?: "Failed to observe snapshots"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  fun saveCameraSnapshot(snapshot: com.example.data.model.CameraSnapshot) {
    viewModelScope.launch {
      val result = repository.saveCameraSnapshot(currentUserId, snapshot)
      if (result.isSuccess) {
        _actionMessage.value = "Snapshot saved to surveillance logs."
      } else {
        _actionMessage.value = "Failed to save snapshot: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun deleteCameraSnapshot(snapshotId: String) {
    viewModelScope.launch {
      val result = repository.deleteCameraSnapshot(currentUserId, snapshotId)
      if (result.isSuccess) {
        _actionMessage.value = "Surveillance snapshot deleted."
      } else {
        _actionMessage.value = "Failed to delete snapshot: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun triggerRemoteSnapshot(device: Device, lens: String = "back", reason: String = "Manual Remote Trigger") {
    viewModelScope.launch {
      val newSnapshot = com.example.data.model.CameraSnapshot(
        deviceId = device.id,
        deviceName = device.deviceName,
        timestamp = System.currentTimeMillis(),
        cameraFacing = lens,
        resolution = "1920x1080",
        triggerReason = reason,
        torchActive = false,
        notes = "Remote frame captured from ${device.deviceName} (${device.model})"
      )
      repository.saveCameraSnapshot(currentUserId, newSnapshot)

      // Also record in security audit logs
      repository.addSecurityLog(
        currentUserId,
        SecurityLog(
          deviceId = device.id,
          deviceName = device.deviceName,
          severity = "info",
          eventType = "camera-snapshot",
          description = "Remote camera snapshot triggered ($lens lens) - $reason"
        )
      )
      _actionMessage.value = "Remote capture triggered for '${device.deviceName}'"
    }
  }

  fun saveDevice(device: Device) {
    viewModelScope.launch {
      val result = repository.saveDevice(currentUserId, device)
      if (result.isSuccess) {
        _actionMessage.value = "Device '${device.deviceName}' updated successfully."
      } else {
        _actionMessage.value = "Failed to save device: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun deleteDevice(deviceId: String) {
    viewModelScope.launch {
      val result = repository.deleteDevice(currentUserId, deviceId)
      if (result.isSuccess) {
        _actionMessage.value = "Device removed from fleet."
      } else {
        _actionMessage.value = "Failed to remove device: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun addSecurityLog(log: SecurityLog) {
    viewModelScope.launch {
      val result = repository.addSecurityLog(currentUserId, log)
      if (result.isSuccess) {
        _actionMessage.value = "Security event logged."
      } else {
        _actionMessage.value = "Failed to record log: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun saveGeofence(geofence: Geofence) {
    viewModelScope.launch {
      val result = repository.saveGeofence(currentUserId, geofence)
      if (result.isSuccess) {
        _actionMessage.value = "Geofence perimeter '${geofence.name}' saved."
      } else {
        _actionMessage.value = "Failed to save geofence: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun populateSampleFleet() {
    viewModelScope.launch {
      val sampleDevices = listOf(
        Device(
          deviceName = "HQ Dispatch Tablet",
          model = "Pixel Tablet",
          status = "online",
          batteryLevel = 94L,
          cpuUsage = 22L,
          ramUsageMb = 2340L,
          ramTotalMb = 8192L,
          complianceStatus = "compliant",
          lastLatitude = 37.7749,
          lastLongitude = -122.4194
        ),
        Device(
          deviceName = "Warehouse Scanner A",
          model = "Zebra TC52",
          status = "online",
          batteryLevel = 68L,
          cpuUsage = 45L,
          ramUsageMb = 1890L,
          ramTotalMb = 4096L,
          complianceStatus = "compliant",
          lastLatitude = 37.7833,
          lastLongitude = -122.4167
        ),
        Device(
          deviceName = "Executive Field Unit",
          model = "Galaxy S24 Ultra",
          status = "warning",
          batteryLevel = 14L,
          cpuUsage = 78L,
          ramUsageMb = 6500L,
          ramTotalMb = 12288L,
          complianceStatus = "violation",
          lastLatitude = 37.7600,
          lastLongitude = -122.4400
        )
      )

      sampleDevices.forEach { dev ->
        repository.saveDevice(currentUserId, dev)
      }

      repository.addSecurityLog(
        currentUserId,
        SecurityLog(
          deviceId = "sample-1",
          deviceName = "Executive Field Unit",
          severity = "critical",
          eventType = "critical-battery",
          description = "Battery dropped below 15% threshold during field dispatch."
        )
      )

      repository.addSecurityLog(
        currentUserId,
        SecurityLog(
          deviceId = "sample-2",
          deviceName = "Warehouse Scanner A",
          severity = "info",
          eventType = "policy-check",
          description = "Routine endpoint posture assessment completed."
        )
      )

      repository.saveGeofence(
        currentUserId,
        Geofence(
          name = "Corporate HQ Perimeter",
          latitude = 37.7749,
          longitude = -122.4194,
          radiusMeters = 800L
        )
      )

      repository.saveCameraSnapshot(
        currentUserId,
        com.example.data.model.CameraSnapshot(
          deviceId = "sample-1",
          deviceName = "Executive Field Unit",
          timestamp = System.currentTimeMillis() - 3600000,
          cameraFacing = "back",
          resolution = "1920x1080",
          triggerReason = "Geofence Violation",
          notes = "Automated surveillance snapshot triggered upon perimeter crossing."
        )
      )

      repository.saveCameraSnapshot(
        currentUserId,
        com.example.data.model.CameraSnapshot(
          deviceId = "sample-3",
          deviceName = "Delivery Dispatch Pad",
          timestamp = System.currentTimeMillis() - 7200000,
          cameraFacing = "front",
          resolution = "1280x720",
          triggerReason = "Manual Operator Trigger",
          notes = "Operator requested visual driver verification frame."
        )
      )

      _actionMessage.value = "Sample enterprise fleet loaded."
    }
  }

  private companion object {
    const val TAG = "FleetVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
