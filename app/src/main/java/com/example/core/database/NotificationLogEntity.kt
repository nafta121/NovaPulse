package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedRegex: Boolean = false,
    val regexTrigger: String = "",
    val extractedType: String = "NONE",
    val summary: String = "",
    val processingTimeMs: Long = 0
)
