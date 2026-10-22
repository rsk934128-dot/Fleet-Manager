package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a surveillance photo or frame snapshot captured via remote or local camera commands.
 */
@IgnoreExtraProperties
data class CameraSnapshot(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val cameraFacing: String = "back", // "back" or "front"
    val resolution: String = "1920x1080",
    val triggerReason: String = "Manual Remote Trigger", // "Manual Remote Trigger", "Geofence Violation", "Audit Check"
    val storageUrl: String = "",
    val base64Data: String? = null,
    val torchActive: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
