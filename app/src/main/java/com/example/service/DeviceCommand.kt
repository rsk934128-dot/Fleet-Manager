package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representation of an incoming command received from the fleet management server.
 */
data class DeviceCommand(
    val id: String,
    val action: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        private const val TAG = "DeviceCommand"

        /**
         * Safely parses an incoming Socket.IO payload (JSONObject, String, or Map) into a DeviceCommand.
         */
        fun fromPayload(payload: Any?): DeviceCommand {
            return when (payload) {
                is JSONObject -> fromJsonObject(payload)
                is String -> {
                    try {
                        fromJsonObject(JSONObject(payload))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse command from JSON string: $payload", e)
                        DeviceCommand(
                            id = "cmd-${System.currentTimeMillis()}",
                            action = payload.trim().uppercase()
                        )
                    }
                }
                is Map<*, *> -> {
                    val id = payload["commandId"]?.toString()
                        ?: payload["id"]?.toString()
                        ?: "cmd-${System.currentTimeMillis()}"
                    val action = payload["action"]?.toString()
                        ?: payload["command"]?.toString()
                        ?: "UNKNOWN"
                    val params = (payload["parameters"] as? Map<*, *>)?.entries?.associate {
                        it.key.toString() to it.value
                    } ?: emptyMap()
                    val ts = (payload["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    DeviceCommand(id = id, action = action, parameters = params, timestamp = ts)
                }
                else -> {
                    DeviceCommand(
                        id = "cmd-${System.currentTimeMillis()}",
                        action = "UNKNOWN"
                    )
                }
            }
        }

        private fun fromJsonObject(json: JSONObject): DeviceCommand {
            val id = when {
                json.has("commandId") -> json.optString("commandId")
                json.has("id") -> json.optString("id")
                else -> "cmd-${System.currentTimeMillis()}"
            }
            val action = when {
                json.has("action") -> json.optString("action")
                json.has("command") -> json.optString("command")
                else -> "UNKNOWN"
            }
            val parameters = mutableMapOf<String, Any?>()
            val paramsObj = json.optJSONObject("parameters")
            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    parameters[key] = jsonValueToAny(paramsObj.get(key))
                }
            }
            val ts = json.optLong("timestamp", System.currentTimeMillis())
            return DeviceCommand(id = id, action = action, parameters = parameters, timestamp = ts)
        }

        private fun jsonValueToAny(value: Any?): Any? {
            return when (value) {
                JSONObject.NULL, null -> null
                is JSONObject -> {
                    val map = mutableMapOf<String, Any?>()
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = jsonValueToAny(value.get(k))
                    }
                    map
                }
                is JSONArray -> {
                    val list = mutableListOf<Any?>()
                    for (i in 0 until value.length()) {
                        list.add(jsonValueToAny(value.get(i)))
                    }
                    list
                }
                else -> value
            }
        }
    }
}

/**
 * Result returned after executing a DeviceCommand.
 */
data class CommandResult(
    val commandId: String,
    val action: String,
    val success: Boolean,
    val output: Map<String, Any?> = emptyMap(),
    val errorMessage: String? = null,
    val executedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts the result into a JSONObject ready to be transmitted back via Socket.IO.
     */
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("commandId", commandId)
        json.put("action", action)
        json.put("success", success)
        json.put("executedAt", executedAt)
        if (errorMessage != null) {
            json.put("errorMessage", errorMessage)
        }

        val outputObj = JSONObject()
        output.forEach { (key, value) ->
            outputObj.put(key, value ?: JSONObject.NULL)
        }
        json.put("output", outputObj)
        return json
    }
}

/**
 * Interface for pluggable command handlers in the DeviceMonitorService.
 */
interface CommandHandler {
    /**
     * Returns true if this handler can process the given action string (case-insensitive).
     */
    fun canHandle(action: String): Boolean

    /**
     * Executes the command safely and returns the resulting status and diagnostics data.
     */
    suspend fun execute(command: DeviceCommand, context: Context): CommandResult
}

/**
 * Safe ping handler that echoes heartbeat and device clock offset.
 */
class PingCommandHandler : CommandHandler {
    override fun canHandle(action: String): Boolean {
        return action.equals("PING", ignoreCase = true) || action.equals("HEARTBEAT", ignoreCase = true)
    }

    override suspend fun execute(command: DeviceCommand, context: Context): CommandResult {
        val now = System.currentTimeMillis()
        val uptime = SystemClock.elapsedRealtime()
        val output = mapOf<String, Any?>(
            "status" to "pong",
            "deviceTimestamp" to now,
            "uptimeMs" to uptime,
            "latencyMs" to (now - command.timestamp).coerceAtLeast(0L),
            "echo" to command.parameters
        )
        return CommandResult(
            commandId = command.id,
            action = command.action,
            success = true,
            output = output
        )
    }
}

/**
 * Safe battery telemetry handler for MDM fleet monitoring.
 */
class BatteryCommandHandler : CommandHandler {
    override fun canHandle(action: String): Boolean {
        return action.equals("GET_BATTERY", ignoreCase = true) ||
            action.equals("BATTERY", ignoreCase = true) ||
            action.equals("BATTERY_STATUS", ignoreCase = true)
    }

    override suspend fun execute(command: DeviceCommand, context: Context): CommandResult {
        return try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                -1
            }

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val output = mapOf<String, Any?>(
                "batteryLevel" to batteryPct,
                "isCharging" to isCharging,
                "temperatureCelsius" to temp,
                "voltageMv" to voltage,
                "pluggedType" to plugged,
                "statusRaw" to status
            )

            CommandResult(
                commandId = command.id,
                action = command.action,
                success = true,
                output = output
            )
        } catch (e: Exception) {
            CommandResult(
                commandId = command.id,
                action = command.action,
                success = false,
                errorMessage = "Failed to query battery status: ${e.message}"
            )
        }
    }
}

/**
 * Safe device telemetry handler reporting hardware and operating system status.
 */
class TelemetryCommandHandler : CommandHandler {
    override fun canHandle(action: String): Boolean {
        return action.equals("GET_TELEMETRY", ignoreCase = true) ||
            action.equals("TELEMETRY", ignoreCase = true) ||
            action.equals("GET_STATUS", ignoreCase = true) ||
            action.equals("DEVICE_INFO", ignoreCase = true)
    }

    override suspend fun execute(command: DeviceCommand, context: Context): CommandResult {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.getMemoryInfo(memoryInfo)

            val output = mapOf<String, Any?>(
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "device" to Build.DEVICE,
                "androidVersion" to Build.VERSION.RELEASE,
                "sdkInt" to Build.VERSION.SDK_INT,
                "totalMemoryMb" to (memoryInfo.totalMem / (1024 * 1024)),
                "availMemoryMb" to (memoryInfo.availMem / (1024 * 1024)),
                "isLowMemory" to memoryInfo.lowMemory,
                "systemUptimeSec" to (SystemClock.elapsedRealtime() / 1000)
            )

            CommandResult(
                commandId = command.id,
                action = command.action,
                success = true,
                output = output
            )
        } catch (e: Exception) {
            CommandResult(
                commandId = command.id,
                action = command.action,
                success = false,
                errorMessage = "Failed to gather telemetry: ${e.message}"
            )
        }
    }
}

/**
 * Safe diagnostics handler evaluating system resources and storage integrity.
 */
class DiagnosticsCommandHandler : CommandHandler {
    override fun canHandle(action: String): Boolean {
        return action.equals("RUN_DIAGNOSTICS", ignoreCase = true) ||
            action.equals("DIAGNOSTICS", ignoreCase = true) ||
            action.equals("HEALTH_CHECK", ignoreCase = true)
    }

    override suspend fun execute(command: DeviceCommand, context: Context): CommandResult {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val freeStorageMb = bytesAvailable / (1024 * 1024)
            val totalStorageMb = bytesTotal / (1024 * 1024)

            val runtime = Runtime.getRuntime()
            val jvmFreeMemory = runtime.freeMemory() / (1024 * 1024)
            val jvmTotalMemory = runtime.totalMemory() / (1024 * 1024)

            val output = mapOf<String, Any?>(
                "health" to "HEALTHY",
                "freeStorageMb" to freeStorageMb,
                "totalStorageMb" to totalStorageMb,
                "storageUsagePercent" to if (totalStorageMb > 0) ((totalStorageMb - freeStorageMb) * 100 / totalStorageMb) else 0,
                "jvmFreeMemoryMb" to jvmFreeMemory,
                "jvmTotalMemoryMb" to jvmTotalMemory,
                "timestamp" to System.currentTimeMillis()
            )

            CommandResult(
                commandId = command.id,
                action = command.action,
                success = true,
                output = output
            )
        } catch (e: Exception) {
            CommandResult(
                commandId = command.id,
                action = command.action,
                success = false,
                errorMessage = "Diagnostics execution failed: ${e.message}"
            )
        }
    }
}

/**
 * CommandDispatcher centralizes command routing, execution, and handler registration.
 */
class CommandDispatcher {
    private val handlers = mutableListOf<CommandHandler>()

    init {
        // Register default enterprise diagnostic handlers
        registerHandler(PingCommandHandler())
        registerHandler(BatteryCommandHandler())
        registerHandler(TelemetryCommandHandler())
        registerHandler(DiagnosticsCommandHandler())
    }

    @Synchronized
    fun registerHandler(handler: CommandHandler) {
        if (!handlers.contains(handler)) {
            handlers.add(handler)
        }
    }

    @Synchronized
    fun unregisterHandler(handler: CommandHandler) {
        handlers.remove(handler)
    }

    @Synchronized
    fun clearHandlers() {
        handlers.clear()
    }

    suspend fun dispatch(command: DeviceCommand, context: Context): CommandResult {
        val matchingHandler = synchronized(this) {
            handlers.firstOrNull { it.canHandle(command.action) }
        }

        return if (matchingHandler != null) {
            try {
                matchingHandler.execute(command, context)
            } catch (e: Exception) {
                Log.e("CommandDispatcher", "Error executing command '${command.action}' (${command.id})", e)
                CommandResult(
                    commandId = command.id,
                    action = command.action,
                    success = false,
                    errorMessage = e.message ?: "Execution failed"
                )
            }
        } else {
            Log.w("CommandDispatcher", "No handler registered for action: ${command.action}")
            CommandResult(
                commandId = command.id,
                action = command.action,
                success = false,
                errorMessage = "Unsupported action '${command.action}'. No registered command handler found."
            )
        }
    }
}
