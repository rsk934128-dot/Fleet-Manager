package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Device
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DeviceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addDevice_and_getDevices_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = DeviceRepository(firestore)

    val device = Device(
      deviceName = "Warehouse Dispatch ${UUID.randomUUID().toString().take(4)}",
      model = "Zebra TC57",
      status = "online",
      batteryLevel = 95L,
      cpuUsage = 20L,
      ramUsageMb = 2100L,
      ramTotalMb = 4096L,
      complianceStatus = "compliant",
      lastLatitude = 37.7749,
      lastLongitude = -122.4194
    )

    val addResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addDevice(uid, device) }
    assertTrue(addResult.isSuccess)
    val deviceId = addResult.getOrThrow()
    assertTrue(deviceId.isNotEmpty())

    // One-time fetch
    val getResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getDevices(uid) }
    assertTrue(getResult.isSuccess)
    val list = getResult.getOrThrow()
    assertTrue(list.any { it.id == deviceId })

    // Single item fetch
    val singleResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getDeviceById(uid, deviceId) }
    assertTrue(singleResult.isSuccess)
    val fetchedDevice = singleResult.getOrThrow()
    assertNotNull(fetchedDevice)
    assertEquals(device.deviceName, fetchedDevice?.deviceName)
  }

  @Test
  fun observeDevices_streamEmitsNewDevices() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = DeviceRepository(firestore)

    val device = Device(
      deviceName = "Field Sensor ${UUID.randomUUID().toString().take(4)}",
      model = "IoT Gateway",
      status = "online",
      batteryLevel = 75L,
      complianceStatus = "compliant"
    )

    val deviceId = repository.addDevice(uid, device).getOrThrow()

    val devices = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeDevices(uid).first { list -> list.any { it.id == deviceId } }
    }
    assertTrue(devices.any { it.id == deviceId })
  }

  @Test
  fun observeDevice_emitsRealtimeSingleDocumentChanges() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = DeviceRepository(firestore)

    val device = Device(
      deviceName = "Realtime Monitor Test",
      model = "Pixel 8",
      status = "online",
      batteryLevel = 100L,
      complianceStatus = "compliant"
    )
    val deviceId = repository.addDevice(uid, device).getOrThrow()

    val initial = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeDevice(uid, deviceId).first { it != null }
    }
    assertNotNull(initial)
    assertEquals(100L, initial?.batteryLevel)

    repository.updateDevice(uid, initial!!.copy(batteryLevel = 91L)).getOrThrow()

    val updated = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeDevice(uid, deviceId).first { it != null && it.batteryLevel == 91L }
    }
    assertEquals(91L, updated?.batteryLevel)
  }

  @Test
  fun crossUserRead_isBlockedBySecurityRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = DeviceRepository(firestore)

    val device = Device(
      deviceName = "Alice Private Terminal",
      status = "online",
      batteryLevel = 100L,
      complianceStatus = "compliant"
    )
    val deviceId = aliceRepo.addDevice(aliceUid, device).getOrThrow()

    // Sign in as Bob and attempt to read Alice's device
    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = DeviceRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getDeviceById(aliceUid, deviceId)
    }
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull()
    val firestoreException = exception as? FirebaseFirestoreException
      ?: exception?.cause as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_dev@fleettest.com"
    const val BOB_EMAIL = "bob_dev@fleettest.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
