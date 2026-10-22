package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val datetime: Long,
    val priority: String = "MEDIUM",
    val sourceApp: String = "Unknown",
    val isSynced: Boolean = false,
    val isCompleted: Boolean = false,
    val rawSnippet: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
