package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SecurityLog(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val severity: String = "info", // "critical", "warning", "info"
    val eventType: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
