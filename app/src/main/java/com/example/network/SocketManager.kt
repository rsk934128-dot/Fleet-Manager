package com.example.network

import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import kotlin.coroutines.resume

/**
 * Connection states for SocketManager.
 */
sealed interface SocketConnectionState {
    data class Disconnected(val reason: String? = null) : SocketConnectionState
    data class Connecting(val serverUrl: String) : SocketConnectionState
    data class Connected(val socketId: String?, val serverUrl: String) : SocketConnectionState
    data class Reconnecting(val attempt: Int) : SocketConnectionState
    data class Error(val message: String, val throwable: Throwable? = null) : SocketConnectionState
}

/**
 * SocketManager manages the lifecycle, authentication tokens, and real-time bidirectional
 * event communication with a remote signaling server using socket.io-client.
 *
 * Implements standard enterprise fleet communication (e.g., status updates, geofence alerts,
 * and security log synchronization) in a safe and transparent manner.
 */
class SocketManager {

    private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected())
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private var socket: Socket? = null
    private var currentServerUrl: String? = null
    private val activeListeners = mutableMapOf<String, MutableList<Emitter.Listener>>()

    companion object {
        private const val TAG = "SocketManager"
        private const val DEFAULT_TIMEOUT_MS = 15000L
        private const val DEFAULT_RECONNECT_ATTEMPTS = 5
        private const val DEFAULT_RECONNECT_DELAY_MS = 2000L
    }

    /**
     * Connect to the signaling server with optional auth token and query parameters.
     *
     * @param serverUrl The URL of the signaling server (e.g., "https://fleet-server.example.com")
     * @param authToken Optional Bearer/auth token for authenticating the handshake
     * @param queryParams Optional query parameters to include in the connection URI
     */
    @Synchronized
    fun connect(
        serverUrl: String,
        authToken: String? = null,
        queryParams: Map<String, String> = emptyMap()
    ) {
        if (socket != null && socket?.connected() == true && currentServerUrl == serverUrl) {
            Log.d(TAG, "Already connected to $serverUrl")
            return
        }

        // Clean up any existing connection
        disconnect()

        currentServerUrl = serverUrl
        _connectionState.value = SocketConnectionState.Connecting(serverUrl)

        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = DEFAULT_RECONNECT_ATTEMPTS
                reconnectionDelay = DEFAULT_RECONNECT_DELAY_MS
                timeout = DEFAULT_TIMEOUT_MS
                transports = arrayOf("websocket", "polling")

                // Provide authentication token in auth handshake payload (Socket.IO v4 convention)
                if (!authToken.isNullOrBlank()) {
                    auth = mapOf("token" to authToken)
                }

                // Query parameters if specified
                if (queryParams.isNotEmpty()) {
                    query = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
                }
            }

            val uri = URI.create(serverUrl)
            val newSocket = IO.socket(uri, options)
            socket = newSocket

            setupLifecycleListeners(newSocket, serverUrl)
            newSocket.connect()
            Log.i(TAG, "Initiated connection to $serverUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize socket connection to $serverUrl", e)
            _connectionState.value = SocketConnectionState.Error(
                message = e.message ?: "Failed to initialize socket connection",
                throwable = e
            )
        }
    }

    /**
     * Sets up connection lifecycle and reconnect event listeners on the socket and its manager.
     */
    private fun setupLifecycleListeners(targetSocket: Socket, serverUrl: String) {
        targetSocket.on(Socket.EVENT_CONNECT) {
            val id = targetSocket.id()
            Log.i(TAG, "Connected to $serverUrl with ID: $id")
            _connectionState.value = SocketConnectionState.Connected(socketId = id, serverUrl = serverUrl)
        }

        targetSocket.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString()
            Log.w(TAG, "Disconnected from $serverUrl. Reason: $reason")
            _connectionState.value = SocketConnectionState.Disconnected(reason = reason)
        }

        targetSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()
            val throwable = error as? Throwable
            val message = throwable?.message ?: error?.toString() ?: "Connection error"
            Log.e(TAG, "Socket connection error: $message", throwable)
            _connectionState.value = SocketConnectionState.Error(message = message, throwable = throwable)
        }

        val manager = targetSocket.io()
        manager?.on(Manager.EVENT_RECONNECT_ATTEMPT) { args ->
            val attempt = (args.firstOrNull() as? Number)?.toInt() ?: 1
            Log.d(TAG, "Reconnection attempt #$attempt to $serverUrl")
            _connectionState.value = SocketConnectionState.Reconnecting(attempt = attempt)
        }

        manager?.on(Manager.EVENT_RECONNECT_FAILED) {
            Log.e(TAG, "Reconnection failed after maximum attempts to $serverUrl")
            _connectionState.value = SocketConnectionState.Error(
                message = "Reconnection failed after maximum attempts",
                throwable = null
            )
        }
    }

    /**
     * Update or refresh the authentication token dynamically.
     * Reconnects the socket if currently connected so the server can re-evaluate credentials.
     */
    @Synchronized
    fun updateAuthToken(newToken: String?) {
        val server = currentServerUrl ?: return
        val wasConnected = socket?.connected() == true
        Log.d(TAG, "Updating auth token (wasConnected=$wasConnected)")
        connect(serverUrl = server, authToken = newToken)
    }

    /**
     * Safely disconnect from the signaling server and clean up socket listeners.
     */
    @Synchronized
    fun disconnect() {
        socket?.let { s ->
            try {
                Log.d(TAG, "Disconnecting socket...")
                s.off() // Remove all registered listeners
                s.disconnect()
                s.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error during socket disconnect", e)
            }
        }
        socket = null
        synchronized(activeListeners) {
            activeListeners.clear()
        }
        _connectionState.value = SocketConnectionState.Disconnected(reason = "Client requested disconnect")
    }

    /**
     * Emit an event to the signaling server.
     *
     * @param event The event name
     * @param args Arguments to pass with the event
     */
    fun emit(event: String, vararg args: Any) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            Log.w(TAG, "Cannot emit '$event': Socket is not connected")
            return
        }
        currentSocket.emit(event, *args)
    }

    /**
     * Emit an event with an acknowledgment callback.
     */
    fun emitWithAck(event: String, vararg args: Any, onAck: (Array<Any>) -> Unit) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            Log.w(TAG, "Cannot emit '$event': Socket is not connected")
            return
        }

        val ackCallback = Ack { ackArgs ->
            onAck(ackArgs ?: emptyArray())
        }
        val combinedArgs: Array<Any> = Array(args.size + 1) { i ->
            if (i < args.size) args[i] else ackCallback
        }
        currentSocket.emit(event, *combinedArgs)
    }

    /**
     * Suspending version of emit with acknowledgment.
     */
    suspend fun emitSuspend(event: String, vararg args: Any): Array<Any> =
        suspendCancellableCoroutine { continuation ->
            val currentSocket = socket
            if (currentSocket == null || !currentSocket.connected()) {
                continuation.resume(emptyArray())
                return@suspendCancellableCoroutine
            }

            val ackCallback = Ack { ackArgs ->
                if (continuation.isActive) {
                    continuation.resume(ackArgs ?: emptyArray())
                }
            }
            val combinedArgs: Array<Any> = Array(args.size + 1) { i ->
                if (i < args.size) args[i] else ackCallback
            }
            currentSocket.emit(event, *combinedArgs)
        }

    /**
     * Register a callback listener for a specific event.
     */
    fun on(event: String, listener: (Array<Any>) -> Unit): Emitter.Listener {
        val emitterListener = Emitter.Listener { args ->
            listener(args ?: emptyArray())
        }
        socket?.on(event, emitterListener)

        synchronized(activeListeners) {
            activeListeners.getOrPut(event) { mutableListOf() }.add(emitterListener)
        }
        return emitterListener
    }

    /**
     * Unregister a listener for a specific event, or all listeners for that event if listener is null.
     */
    fun off(event: String, listener: Emitter.Listener? = null) {
        if (listener != null) {
            socket?.off(event, listener)
            synchronized(activeListeners) {
                activeListeners[event]?.remove(listener)
            }
        } else {
            socket?.off(event)
            synchronized(activeListeners) {
                activeListeners.remove(event)
            }
        }
    }

    /**
     * Observes an event as a reactive Kotlin Coroutine Flow.
     * Automatically registers the listener on collection and unregisters on cancellation.
     */
    fun onEventFlow(event: String): Flow<Array<Any>> = callbackFlow {
        val listener = Emitter.Listener { args ->
            trySend(args ?: emptyArray())
        }

        socket?.on(event, listener)
        Log.d(TAG, "Subscribed flow listener for event: $event")

        awaitClose {
            Log.d(TAG, "Unsubscribing flow listener for event: $event")
            socket?.off(event, listener)
        }
    }

    /**
     * Check if the socket is currently connected.
     */
    fun isConnected(): Boolean = socket?.connected() == true

    /**
     * Get the current active socket ID, if connected.
     */
    fun getSocketId(): String? = socket?.id()

    /**
     * Get current server URL.
     */
    fun getServerUrl(): String? = currentServerUrl
}
