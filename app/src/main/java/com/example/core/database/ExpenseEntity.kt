package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String = "VND",
    val category: String = "General",
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String = "Banking",
    val rawSnippet: String = ""
)
