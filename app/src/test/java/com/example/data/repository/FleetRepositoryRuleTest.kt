package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Device
import com.example.data.model.SecurityLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FleetRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveDevice_validPayload_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FleetRepository(firestore)

    val device = Device(
      id = "device_${UUID.randomUUID().toString().take(8)}",
      deviceName = "Pixel 8 Pro",
      model = "GC3VE",
      status = "online",
      batteryLevel = 92L,
      cpuUsage = 18L,
      ramUsageMb = 3100L,
      ramTotalMb = 12288L,
      complianceStatus = "compliant",
      lastLatitude = 37.4220,
      lastLongitude = -122.0841
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveDevice(uid, device) }
    assertTrue(result.isSuccess)
    assertEquals(device.id, result.getOrThrow())
  }

  @Test
  fun observeDevices_authenticatedOwner_receivesUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FleetRepository(firestore)

    val device = Device(
      id = "device_${UUID.randomUUID().toString().take(8)}",
      deviceName = "Galaxy Tab S9",
      model = "SM-X710",
      status = "online",
      batteryLevel = 88L,
      complianceStatus = "compliant"
    )

    repository.saveDevice(uid, device).getOrThrow()

    val devices = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeDevices(uid).first { list -> list.any { it.id == device.id } }
    }
    assertTrue(devices.any { it.id == device.id && it.deviceName == "Galaxy Tab S9" })
  }

  @Test
  fun crossUserAccess_isRejectedByRules() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FleetRepository(firestore)
    val device = Device(
      id = "device_${UUID.randomUUID().toString().take(8)}",
      deviceName = "Confidential Device",
      status = "online",
      batteryLevel = 99L,
      complianceStatus = "compliant"
    )
    aliceRepo.saveDevice(aliceUid, device).getOrThrow()

    // Sign in as Bob and attempt to read Alice's device
    val bobUid = signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("devices").document(device.id).get().await()
      }
      fail("Expected PERMISSION_DENIED on cross-user read")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun addSecurityLog_validPayload_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FleetRepository(firestore)

    val log = SecurityLog(
      deviceId = "device_test",
      deviceName = "Field Sensor",
      severity = "warning",
      eventType = "geofence-alert",
      description = "Device exited corporate perimeter"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addSecurityLog(uid, log) }
    assertTrue(result.isSuccess)
  }

  @Test
  fun unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = FleetRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeDevices("some_random_uid").first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated caller")
    } catch (e: Exception) {
      val firestoreException = (e as? FirebaseFirestoreException)
        ?: (e.cause as? FirebaseFirestoreException)
        ?: (e.cause?.cause as? FirebaseFirestoreException)
      assertTrue(
        "Expected PERMISSION_DENIED, got $e with cause ${e.cause}",
        firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
        e.toString().contains("PERMISSION_DENIED")
      )
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@fleettest.com"
    const val BOB_EMAIL = "bob@fleettest.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
