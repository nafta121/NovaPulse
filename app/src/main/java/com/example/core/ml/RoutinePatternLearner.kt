package com.example.core.ml

import android.content.Context
import android.util.Log
import com.example.core.database.AppDatabase
import com.example.core.database.UserRoutineLogEntity
import com.example.core.repository.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.pow

data class SingleSegmentRoutine(
    val wakeTimeFormatted: String = "07:00",
    val bedtimeFormatted: String = "23:00",
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val bedHour: Int = 23,
    val bedMinute: Int = 0,
    val sampleCount: Int = 0
)

data class RoutineInsight(
    val weekday: SingleSegmentRoutine = SingleSegmentRoutine("06:45", "23:00", 6, 45, 23, 0, 0),
    val weekend: SingleSegmentRoutine = SingleSegmentRoutine("08:00", "23:45", 8, 0, 23, 45, 0),
    val isTodayWeekend: Boolean = false,
    val predictedWakeTimeFormatted: String = "07:00",
    val predictedBedtimeFormatted: String = "23:00",
    val predictedWakeHour: Int = 7,
    val predictedWakeMinute: Int = 0,
    val predictedBedHour: Int = 23,
    val predictedBedMinute: Int = 0,
    val isUserEarlyRiser: Boolean = false,
    val isNightOwl: Boolean = false,
    val confidenceLevel: String = "Initializing",
    val totalSampleCount: Int = 0
)

class RoutinePatternLearner(private val context: Context) {

    companion object {
        private const val TAG = "RoutinePatternLearner"
        private const val DECAY_FACTOR = 0.85
        private const val LOOKBACK_DAYS = 14
    }

    private val database = AppDatabase.getInstance(context)
    private val routineDao = database.routineDao()

    suspend fun analyzeRoutinePatterns(): RoutineInsight = withContext(Dispatchers.IO) {
        try {
            val nowMs = System.currentTimeMillis()
            val lookbackMs = nowMs - (LOOKBACK_DAYS * 24 * 3600 * 1000L)
            val logs = routineDao.getLogsSince(lookbackMs)

            val weekdayLogs = mutableListOf<UserRoutineLogEntity>()
            val weekendLogs = mutableListOf<UserRoutineLogEntity>()

            for (log in logs) {
                val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
                if (isWeekend) {
                    weekendLogs.add(log)
                } else {
                    weekdayLogs.add(log)
                }
            }

            val weekdaySegment = computeWeightedSegment(weekdayLogs, nowMs, defaultWakeMins = 405, defaultBedMins = 180)
            val weekendSegment = computeWeightedSegment(weekendLogs, nowMs, defaultWakeMins = 480, defaultBedMins = 225)

            val todayCal = Calendar.getInstance()
            val todayDow = todayCal.get(Calendar.DAY_OF_WEEK)
            val isTodayWeekend = (todayDow == Calendar.SATURDAY || todayDow == Calendar.SUNDAY)

            val activeSegment = if (isTodayWeekend) weekendSegment else weekdaySegment
            val totalSamples = logs.size

            val confidence = when {
                totalSamples >= 20 -> "High (Personalized)"
                totalSamples >= 6 -> "Learning Habits"
                else -> "Initializing"
            }

            val isEarlyRiser = activeSegment.wakeHour < 6 || (activeSegment.wakeHour == 6 && activeSegment.wakeMinute <= 30)
            val isNightOwl = activeSegment.bedHour in 0..4 || (activeSegment.bedHour == 23 && activeSegment.bedMinute >= 45)

            RoutineInsight(
                weekday = weekdaySegment,
                weekend = weekendSegment,
                isTodayWeekend = isTodayWeekend,
                predictedWakeTimeFormatted = activeSegment.wakeTimeFormatted,
                predictedBedtimeFormatted = activeSegment.bedtimeFormatted,
                predictedWakeHour = activeSegment.wakeHour,
                predictedWakeMinute = activeSegment.wakeMinute,
                predictedBedHour = activeSegment.bedHour,
                predictedBedMinute = activeSegment.bedMinute,
                isUserEarlyRiser = isEarlyRiser,
                isNightOwl = isNightOwl,
                confidenceLevel = confidence,
                totalSampleCount = totalSamples
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing routine patterns: ${e.message}")
            RoutineInsight()
        }
    }

    private fun computeWeightedSegment(
        logs: List<UserRoutineLogEntity>,
        nowMs: Long,
        defaultWakeMins: Int,
        defaultBedMins: Int
    ): SingleSegmentRoutine {
        var weightedWakeSum = 0.0
        var wakeWeightTotal = 0.0
        var weightedBedSum = 0.0
        var bedWeightTotal = 0.0

        for (log in logs) {
            val daysAgo = ((nowMs - log.timestamp).coerceAtLeast(0L) / (24 * 3600 * 1000.0))
            val weight = DECAY_FACTOR.pow(daysAgo)

            val h = log.hourOfDay
            val m = log.minuteOfHour

            if (log.eventType in listOf(
                    RoutineRepository.EVENT_SCREEN_OFF,
                    RoutineRepository.EVENT_POWER_CONNECTED,
                    RoutineRepository.EVENT_HEALTH_SLEEP
                )
            ) {
                if (h >= 20 || h <= 3) {
                    val minsFrom20 = if (h >= 20) (h - 20) * 60 + m else (h + 4) * 60 + m
                    weightedBedSum += minsFrom20 * weight
                    bedWeightTotal += weight
                }
            }

            if (log.eventType in listOf(
                    RoutineRepository.EVENT_USER_PRESENT,
                    RoutineRepository.EVENT_POWER_DISCONNECTED
                )
            ) {
                if (h in 5..10) {
                    val minsFromMidnight = h * 60 + m
                    weightedWakeSum += minsFromMidnight * weight
                    wakeWeightTotal += weight
                }
            }
        }

        val finalWakeMins = if (wakeWeightTotal > 0.0) (weightedWakeSum / wakeWeightTotal).toInt() else defaultWakeMins
        val finalBedMinsFrom20 = if (bedWeightTotal > 0.0) (weightedBedSum / bedWeightTotal).toInt() else defaultBedMins

        val wakeH = (finalWakeMins / 60).coerceIn(0, 23)
        val wakeM = (finalWakeMins % 60).coerceIn(0, 59)

        val bedH = if (finalBedMinsFrom20 >= 240) (finalBedMinsFrom20 - 240) / 60 else (finalBedMinsFrom20 / 60) + 20
        val bedM = (finalBedMinsFrom20 % 60).coerceIn(0, 59)

        return SingleSegmentRoutine(
            wakeTimeFormatted = String.format("%02d:%02d", wakeH, wakeM),
            bedtimeFormatted = String.format("%02d:%02d", bedH, bedM),
            wakeHour = wakeH,
            wakeMinute = wakeM,
            bedHour = bedH,
            bedMinute = bedM,
            sampleCount = logs.size
        )
    }
}
