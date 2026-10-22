package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NotificationLogEntity): Long

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_logs")
    fun getTotalInterceptedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_logs WHERE matchedRegex = 1")
    fun getRegexMatchedCount(): Flow<Int>

    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_logs")
    suspend fun clearAll()
}
