package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.network.SocketConnectionState
import com.example.network.SocketManager
import io.socket.client.Ack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DeviceMonitorService is a background foreground service that maintains a real-time
 * Socket.IO connection with a fleet management Node.js server.
 *
 * It listens for 'execute-command' events sent from the server, runs safe enterprise diagnostics
 * or telemetry gathering actions via a pluggable CommandHandler framework, and emits execution results
 * back to the server with acknowledgments.
 */
class DeviceMonitorService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var socketManager: SocketManager = SocketManager()
    private val commandDispatcher = CommandDispatcher()
    private var notificationManager: NotificationManager? = null

    private var currentServerUrl: String = DEFAULT_SERVER_URL
    private var currentDeviceId: String? = null
    private var currentAuthToken: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): DeviceMonitorService = this@DeviceMonitorService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DeviceMonitorService onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()

        // Copy any registered global handlers
        synchronized(globalHandlers) {
            globalHandlers.forEach { commandDispatcher.registerHandler(it) }
        }

        // Monitor socket connection state and reflect it in service state
        serviceScope.launch {
            socketManager.connectionState.collect { state ->
                _connectionState.value = state
                handleConnectionStateChange(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL) ?: DEFAULT_SERVER_URL
                val authToken = intent?.getStringExtra(EXTRA_AUTH_TOKEN)
                val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)

                currentServerUrl = serverUrl
                currentAuthToken = authToken
                currentDeviceId = deviceId

                startForegroundServiceWithNotification("Connecting to $serverUrl...")
                connectToNodeServer(serverUrl, authToken, deviceId)
                _isServiceRunning.value = true
                return START_STICKY
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
                return START_NOT_STICKY
            }
        }
    }

    /**
     * Initializes Socket.IO connection and registers the 'execute-command' event listener.
     */
    private fun connectToNodeServer(serverUrl: String, authToken: String?, deviceId: String?) {
        val queryParams = mutableMapOf<String, String>()
        if (!deviceId.isNullOrBlank()) {
            queryParams["deviceId"] = deviceId
        }
        queryParams["role"] = "device-agent"

        socketManager.connect(
            serverUrl = serverUrl,
            authToken = authToken,
            queryParams = queryParams
        )

        // Setup listener for the 'execute-command' event from the Node server
        setupCommandListener()
    }

    /**
     * Sets up the Socket.IO listener for incoming 'execute-command' events.
     */
    private fun setupCommandListener() {
        socketManager.on(EVENT_EXECUTE_COMMAND) { args ->
            Log.d(TAG, "Received '$EVENT_EXECUTE_COMMAND' event. Args count: ${args.size}")

            val ackCallback = args.firstOrNull { it is Ack } as? Ack
            val payload = args.firstOrNull { it !is Ack }

            val command = DeviceCommand.fromPayload(payload)
            Log.i(TAG, "Parsed command: id=${command.id}, action=${command.action}, params=${command.parameters}")

            _latestCommand.value = command

            serviceScope.launch {
                handleExecuteCommand(command, ackCallback)
            }
        }
    }

    /**
     * Executes the incoming command using the CommandDispatcher framework and transmits
     * the results back to the server.
     */
    private suspend fun handleExecuteCommand(command: DeviceCommand, ack: Ack?) {
        Log.d(TAG, "Dispatching command '${command.action}' (${command.id})")
        val result = commandDispatcher.dispatch(command, applicationContext)

        _latestResult.value = result
        val updatedHistory = (_commandHistory.value + result).takeLast(MAX_HISTORY_ENTRIES)
        _commandHistory.value = updatedHistory

        val resultJson = result.toJsonObject()

        // 1. Send result event to Node.js server
        socketManager.emit(EVENT_COMMAND_RESULT, resultJson)

        // 2. If server provided a Socket.IO acknowledgment callback, call it with the result
        try {
            ack?.call(resultJson)
        } catch (e: Exception) {
            Log.w(TAG, "Error invoking Socket.io ack callback for command ${command.id}", e)
        }

        // 3. Update ongoing notification with command status
        val statusText = if (result.success) "Success" else "Failed"
        updateNotification("Executed ${command.action} ($statusText)")
    }

    /**
     * Updates notification when socket connection state transitions.
     */
    private fun handleConnectionStateChange(state: SocketConnectionState) {
        val message = when (state) {
            is SocketConnectionState.Connected -> "Connected to ${state.serverUrl}"
            is SocketConnectionState.Connecting -> "Connecting to ${state.serverUrl}..."
            is SocketConnectionState.Reconnecting -> "Reconnecting (attempt ${state.attempt})..."
            is SocketConnectionState.Disconnected -> "Disconnected from server"
            is SocketConnectionState.Error -> "Connection error: ${state.message}"
        }
        updateNotification(message)
    }

    /**
     * Registers a custom CommandHandler on this service instance.
     */
    fun registerCommandHandler(handler: CommandHandler) {
        commandDispatcher.registerHandler(handler)
    }

    /**
     * Unregisters a CommandHandler from this service instance.
     */
    fun unregisterCommandHandler(handler: CommandHandler) {
        commandDispatcher.unregisterHandler(handler)
    }

    /**
     * Allows setting a custom SocketManager for unit testing purposes.
     */
    internal fun setSocketManagerForTesting(manager: SocketManager) {
        this.socketManager.disconnect()
        this.socketManager = manager
        setupCommandListener()
    }

    private fun startForegroundServiceWithNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DeviceMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name) + " - Monitor Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Monitor", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Fleet Device Monitor"
            val descriptionText = "Monitors device status and diagnostic commands from fleet server"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "Stopping DeviceMonitorService...")
        socketManager.disconnect()
        _isServiceRunning.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DeviceMonitorService onDestroy")
        socketManager.disconnect()
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    companion object {
        private const val TAG = "DeviceMonitorService"

        const val CHANNEL_ID = "fleet_device_monitor_channel"
        const val NOTIFICATION_ID = 4001
        private const val MAX_HISTORY_ENTRIES = 50

        // Socket.IO event names
        const val EVENT_EXECUTE_COMMAND = "execute-command"
        const val EVENT_COMMAND_RESULT = "command-result"

        // Intent actions & extras
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val EXTRA_SERVER_URL = "com.example.service.extra.SERVER_URL"
        const val EXTRA_AUTH_TOKEN = "com.example.service.extra.AUTH_TOKEN"
        const val EXTRA_DEVICE_ID = "com.example.service.extra.DEVICE_ID"

        const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"

        // Global state flows for UI observers
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected())
        val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

        private val _latestCommand = MutableStateFlow<DeviceCommand?>(null)
        val latestCommand: StateFlow<DeviceCommand?> = _latestCommand.asStateFlow()

        private val _latestResult = MutableStateFlow<CommandResult?>(null)
        val latestResult: StateFlow<CommandResult?> = _latestResult.asStateFlow()

        private val _commandHistory = MutableStateFlow<List<CommandResult>>(emptyList())
        val commandHistory: StateFlow<List<CommandResult>> = _commandHistory.asStateFlow()

        private val globalHandlers = mutableListOf<CommandHandler>()

        /**
         * Registers a global handler that will be applied to all service instances.
         */
        @Synchronized
        fun registerGlobalHandler(handler: CommandHandler) {
            if (!globalHandlers.contains(handler)) {
                globalHandlers.add(handler)
            }
        }

        /**
         * Starts the DeviceMonitorService foreground service.
         */
        fun start(
            context: Context,
            serverUrl: String = DEFAULT_SERVER_URL,
            authToken: String? = null,
            deviceId: String? = null
        ) {
            val intent = Intent(context, DeviceMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_AUTH_TOKEN, authToken)
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the DeviceMonitorService.
         */
        fun stop(context: Context) {
            val intent = Intent(context, DeviceMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
