package com.example.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SocketManagerTest {

    @Test
    fun initialState_isDisconnected() = runBlocking {
        val socketManager = SocketManager()
        val state = socketManager.connectionState.first()

        assertTrue("Initial state should be Disconnected", state is SocketConnectionState.Disconnected)
        assertFalse("Initial isConnected should be false", socketManager.isConnected())
    }

    @Test
    fun connect_initiatesConnectionState() = runBlocking {
        val socketManager = SocketManager()
        socketManager.connect(
            serverUrl = "https://signaling.example.com",
            authToken = "test-token-12345"
        )

        assertEquals("https://signaling.example.com", socketManager.getServerUrl())
        val state = socketManager.connectionState.value
        // State should transition to Connecting or Error (since remote URL is not active in test)
        assertTrue(
            "State should be Connecting or Disconnected or Error",
            state is SocketConnectionState.Connecting ||
                state is SocketConnectionState.Disconnected ||
                state is SocketConnectionState.Error
        )

        socketManager.disconnect()
        val afterDisconnect = socketManager.connectionState.value
        assertTrue("State should be Disconnected after calling disconnect()", afterDisconnect is SocketConnectionState.Disconnected)
    }

    @Test
    fun eventListeners_canBeRegisteredAndRemoved() {
        val socketManager = SocketManager()
        socketManager.connect("https://signaling.example.com")

        var receivedEvent = false
        val listener = socketManager.on("fleet:alert") {
            receivedEvent = true
        }
        assertNotNull(listener)

        socketManager.off("fleet:alert", listener)
        socketManager.off("fleet:alert")
        socketManager.disconnect()
    }

    @Test
    fun emitWithoutConnection_handlesGracefully() {
        val socketManager = SocketManager()
        // Should not throw exception when emitting while disconnected
        socketManager.emit("heartbeat", "device-001")
        socketManager.emitWithAck("ping", "payload") { /* ack */ }
    }
}
