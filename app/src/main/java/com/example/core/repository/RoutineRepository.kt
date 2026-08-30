package com.example.core.repository

import android.content.Context
import android.util.Log
import com.example.core.database.AppDatabase
import com.example.core.database.UserRoutineLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class PredictedSleepWindow(
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val bedtimeFormatted: String = "23:00",
    val wakeTimeFormatted: String = "07:00",
    val sampleCount: Int = 0
)

class RoutineRepository(private val context: Context) {

    companion object {
        private const val TAG = "RoutineRepository"
        const val EVENT_SCREEN_OFF = "SCREEN_OFF"
        const val EVENT_USER_PRESENT = "USER_PRESENT"
        const val EVENT_POWER_CONNECTED = "POWER_CONNECTED"
        const val EVENT_POWER_DISCONNECTED = "POWER_DISCONNECTED"
        const val EVENT_HEALTH_SLEEP = "HEALTH_CONNECT_SLEEP"
    }

    private val database = AppDatabase.getInstance(context)
    private val routineDao = database.routineDao()

    suspend fun recordEvent(eventType: String) = withContext(Dispatchers.IO) {
        try {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            val log = UserRoutineLogEntity(
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                hourOfDay = hour,
                minuteOfHour = minute
            )
            routineDao.insertLog(log)
            Log.d(TAG, "Recorded routine event '$eventType' at ${String.format("%02d:%02d", hour, minute)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record routine event: ${e.message}")
        }
    }

    /**
     * Predictive Sleep Window algorithm calculated over historical routine logs
     */
    suspend fun predictSleepWindow(): PredictedSleepWindow = withContext(Dispatchers.IO) {
        try {
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 3600 * 1000L)
            val recentLogs = routineDao.getLogsSince(threeDaysAgo)

            // Bedtime candidates: SCREEN_OFF or POWER_CONNECTED or HEALTH_SLEEP between 20:00 and 03:00
            val bedtimeMinutesList = mutableListOf<Int>()
            for (log in recentLogs) {
                if (log.eventType in listOf(EVENT_SCREEN_OFF, EVENT_POWER_CONNECTED, EVENT_HEALTH_SLEEP)) {
                    val h = log.hourOfDay
                    val m = log.minuteOfHour
                    if (h >= 20 || h <= 3) {
                        val totalMins = if (h >= 20) (h - 20) * 60 + m else (h + 4) * 60 + m
                        bedtimeMinutesList.add(totalMins)
                    }
                }
            }

            // Wake time candidates: USER_PRESENT or POWER_DISCONNECTED between 05:00 and 10:00
            val wakeMinutesList = mutableListOf<Int>()
            for (log in recentLogs) {
                if (log.eventType in listOf(EVENT_USER_PRESENT, EVENT_POWER_DISCONNECTED)) {
                    val h = log.hourOfDay
                    val m = log.minuteOfHour
                    if (h in 5..10) {
                        wakeMinutesList.add(h * 60 + m)
                    }
                }
            }

            val avgBedtimeMins = if (bedtimeMinutesList.isNotEmpty()) bedtimeMinutesList.average().toInt() else 180 // Default ~23:00
            val avgWakeMins = if (wakeMinutesList.isNotEmpty()) wakeMinutesList.average().toInt() else 420 // Default ~07:00

            // Convert bedtime back to 24h
            val predictedBedHour = if (avgBedtimeMins >= 240) (avgBedtimeMins - 240) / 60 else (avgBedtimeMins / 60) + 20
            val predictedBedMin = (avgBedtimeMins % 60).coerceIn(0, 59)

            val predictedWakeHour = (avgWakeMins / 60).coerceIn(0, 23)
            val predictedWakeMin = (avgWakeMins % 60).coerceIn(0, 59)

            PredictedSleepWindow(
                bedtimeHour = predictedBedHour,
                bedtimeMinute = predictedBedMin,
                wakeHour = predictedWakeHour,
                wakeMinute = predictedWakeMin,
                bedtimeFormatted = String.format("%02d:%02d", predictedBedHour, predictedBedMin),
                wakeTimeFormatted = String.format("%02d:%02d", predictedWakeHour, predictedWakeMin),
                sampleCount = recentLogs.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating sleep window prediction: ${e.message}")
            PredictedSleepWindow()
        }
    }

    /**
     * Checks if current USER_PRESENT unlock is the first unlock of today (after 04:00 AM)
     */
    suspend fun isFirstUnlockOfToday(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 4)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis

            val todayUnlocks = routineDao.getLogsSince(startOfToday)
                .filter { it.eventType == EVENT_USER_PRESENT }

            // If there's only 1 unlock recorded since 4 AM, it's the first unlock of the day!
            todayUnlocks.size <= 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if current time is within 30 minutes before predicted bedtime
     */
    suspend fun isWithinPreBedtimeWindow(windowMinutes: Int = 30): Boolean = withContext(Dispatchers.IO) {
        val window = predictSleepWindow()
        val nowCal = Calendar.getInstance()
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val currentMin = nowCal.get(Calendar.MINUTE)
        val currentTotalMins = currentHour * 60 + currentMin

        val bedTotalMins = window.bedtimeHour * 60 + window.bedtimeMinute
        val diff = bedTotalMins - currentTotalMins

        diff in 0..windowMinutes
    }

    suspend fun clearAllRoutineLogs() = withContext(Dispatchers.IO) {
        try {
            routineDao.deleteAllLogs()
            Log.d(TAG, "Cleared all routine logs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear routine logs: ${e.message}")
        }
    }
}
