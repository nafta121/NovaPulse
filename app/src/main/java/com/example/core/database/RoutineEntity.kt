package com.example.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "user_routine_logs")
data class UserRoutineLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val eventType: String,
    val hourOfDay: Int,
    val minuteOfHour: Int
)

@Dao
interface RoutineDao {
    @Insert
    suspend fun insertLog(log: UserRoutineLogEntity)

    @Query("SELECT * FROM user_routine_logs WHERE eventType = :type ORDER BY timestamp DESC LIMIT 50")
    suspend fun getLogsByType(type: String): List<UserRoutineLogEntity>

    @Query("SELECT * FROM user_routine_logs WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    suspend fun getLogsSince(sinceMillis: Long): List<UserRoutineLogEntity>

    @Query("SELECT * FROM user_routine_logs ORDER BY timestamp DESC LIMIT 200")
    suspend fun getAllLogs(): List<UserRoutineLogEntity>

    @Query("DELETE FROM user_routine_logs")
    suspend fun deleteAllLogs()
}
