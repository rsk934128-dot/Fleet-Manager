package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Device(
    @DocumentId val id: String = "",
    val deviceName: String = "",
    val model: String = "",
    val status: String = "online", // "online", "offline", "warning"
    val batteryLevel: Long = 100L,
    val cpuUsage: Long = 0L,
    val ramUsageMb: Long = 0L,
    val ramTotalMb: Long = 8192L,
    val complianceStatus: String = "compliant", // "compliant", "violation", "pending"
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
