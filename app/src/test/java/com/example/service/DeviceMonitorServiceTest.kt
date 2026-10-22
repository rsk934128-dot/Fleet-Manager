package com.example.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceMonitorServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun parseCommand_fromJsonObject() {
        val json = JSONObject().apply {
            put("commandId", "cmd-999")
            put("action", "PING")
            put("parameters", JSONObject().apply {
                put("echoMessage", "hello-fleet")
            })
            put("timestamp", 1700000000000L)
        }

        val command = DeviceCommand.fromPayload(json)
        assertEquals("cmd-999", command.id)
        assertEquals("PING", command.action)
        assertEquals("hello-fleet", command.parameters["echoMessage"])
        assertEquals(1700000000000L, command.timestamp)
    }

    @Test
    fun parseCommand_fromJsonString() {
        val jsonStr = """
            {
                "id": "cmd-888",
                "command": "BATTERY",
                "parameters": {
                    "detailed": true
                }
            }
        """.trimIndent()

        val command = DeviceCommand.fromPayload(jsonStr)
        assertEquals("cmd-888", command.id)
        assertEquals("BATTERY", command.action)
        assertEquals(true, command.parameters["detailed"])
    }

    @Test
    fun parseCommand_fromMap() {
        val map = mapOf(
            "commandId" to "cmd-777",
            "action" to "GET_TELEMETRY",
            "parameters" to mapOf("interval" to 30)
        )

        val command = DeviceCommand.fromPayload(map)
        assertEquals("cmd-777", command.id)
        assertEquals("GET_TELEMETRY", command.action)
        assertEquals(30, command.parameters["interval"])
    }

    @Test
    fun commandDispatcher_executesPingCommand() = runBlocking {
        val dispatcher = CommandDispatcher()
        val command = DeviceCommand(
            id = "cmd-101",
            action = "PING",
            parameters = mapOf("nonce" to "abc123xyz")
        )

        val result = dispatcher.dispatch(command, context)

        assertEquals("cmd-101", result.commandId)
        assertEquals("PING", result.action)
        assertTrue("Ping command should succeed", result.success)
        assertEquals("pong", result.output["status"])
        assertTrue("Uptime should be non-negative", (result.output["uptimeMs"] as Number).toLong() >= 0)

        val json = result.toJsonObject()
        assertEquals("cmd-101", json.getString("commandId"))
        assertTrue(json.getBoolean("success"))
        assertEquals("pong", json.getJSONObject("output").getString("status"))
    }

    @Test
    fun commandDispatcher_executesBatteryCommand() = runBlocking {
        val dispatcher = CommandDispatcher()
        val command = DeviceCommand(id = "cmd-102", action = "GET_BATTERY")

        val result = dispatcher.dispatch(command, context)

        assertEquals("cmd-102", result.commandId)
        assertTrue(result.success)
        assertTrue("Output should contain battery info", result.output.containsKey("batteryLevel"))
        assertTrue("Output should contain charging status", result.output.containsKey("isCharging"))
    }

    @Test
    fun commandDispatcher_executesTelemetryCommand() = runBlocking {
        val dispatcher = CommandDispatcher()
        val command = DeviceCommand(id = "cmd-103", action = "TELEMETRY")

        val result = dispatcher.dispatch(command, context)

        assertEquals("cmd-103", result.commandId)
        assertTrue(result.success)
        assertTrue(result.output.containsKey("manufacturer"))
        assertTrue(result.output.containsKey("model"))
        assertTrue(result.output.containsKey("androidVersion"))
    }

    @Test
    fun commandDispatcher_executesDiagnosticsCommand() = runBlocking {
        val dispatcher = CommandDispatcher()
        val command = DeviceCommand(id = "cmd-104", action = "RUN_DIAGNOSTICS")

        val result = dispatcher.dispatch(command, context)

        assertEquals("cmd-104", result.commandId)
        assertTrue(result.success)
        assertEquals("HEALTHY", result.output["health"])
        assertTrue(result.output.containsKey("jvmFreeMemoryMb"))
    }

    @Test
    fun commandDispatcher_handlesUnsupportedCommandGracefully() = runBlocking {
        val dispatcher = CommandDispatcher()
        val command = DeviceCommand(id = "cmd-999", action = "UNSUPPORTED_OPERATION")

        val result = dispatcher.dispatch(command, context)

        assertEquals("cmd-999", result.commandId)
        assertFalse("Unsupported command should fail gracefully", result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Unsupported action"))
    }

    @Test
    fun commandDispatcher_customHandlerRegistration() = runBlocking {
        val dispatcher = CommandDispatcher()
        val customHandler = object : CommandHandler {
            override fun canHandle(action: String): Boolean = action.equals("REBOOT_SIMULATION", ignoreCase = true)
            override suspend fun execute(command: DeviceCommand, context: Context): CommandResult {
                return CommandResult(
                    commandId = command.id,
                    action = command.action,
                    success = true,
                    output = mapOf("rebootScheduledInSec" to 5)
                )
            }
        }

        dispatcher.registerHandler(customHandler)
        val command = DeviceCommand(id = "cmd-custom-01", action = "REBOOT_SIMULATION")

        val result = dispatcher.dispatch(command, context)
        assertTrue(result.success)
        assertEquals(5, result.output["rebootScheduledInSec"])

        dispatcher.unregisterHandler(customHandler)
        val afterUnregister = dispatcher.dispatch(command, context)
        assertFalse(afterUnregister.success)
    }

    @Test
    fun serviceLifecycle_startAndBind() {
        val serviceController = Robolectric.buildService(DeviceMonitorService::class.java)
        val service = serviceController.create().get()

        val startIntent = Intent(context, DeviceMonitorService::class.java).apply {
            action = DeviceMonitorService.ACTION_START
            putExtra(DeviceMonitorService.EXTRA_SERVER_URL, "https://fleet-server.example.com")
            putExtra(DeviceMonitorService.EXTRA_DEVICE_ID, "test-device-42")
        }

        service.onStartCommand(startIntent, 0, 1)
        assertTrue("Service should indicate running state", DeviceMonitorService.isServiceRunning.value)

        val binder = service.onBind(startIntent)
        assertNotNull("Binder should not be null", binder)
        assertTrue("Binder should be LocalBinder", binder is DeviceMonitorService.LocalBinder)
        assertEquals(service, (binder as DeviceMonitorService.LocalBinder).getService())

        // Stop action
        val stopIntent = Intent(context, DeviceMonitorService::class.java).apply {
            action = DeviceMonitorService.ACTION_STOP
        }
        service.onStartCommand(stopIntent, 0, 2)
        assertFalse("Service should indicate stopped state", DeviceMonitorService.isServiceRunning.value)

        serviceController.destroy()
    }
}
