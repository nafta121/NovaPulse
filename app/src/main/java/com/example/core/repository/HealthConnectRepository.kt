package com.example.core.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.ui.components.DailyHeartRingData
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class RealHealthData(
    val sleepDurationMinutes: Long = 0L,
    val sleepDurationFormatted: String = "0h 00m",
    val sleepStageSummary: String = "Last night's sleep",
    val averageHeartRateBpm: Int = 0,
    val minHeartRateBpm: Int = 0,
    val maxHeartRateBpm: Int = 0,
    val currentSteps: Int = 0,
    val stepGoal: Int = 6000,
    val currentActiveMinutes: Int = 0,
    val activeGoalMinutes: Int = 60,
    val currentCalories: Int = 0,
    val calorieGoal: Int = 500,
    val weeklyHistory: List<DailyHeartRingData> = emptyList(),
    val isDataAvailable: Boolean = false
)

class HealthConnectRepository(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectRepo"

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )
    }

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Health Connect is not available on this device: ${e.message}")
            null
        }
    }

    fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            // Granting either Sleep, Steps, or HeartRate permission allows querying granted data
            granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class)) ||
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class)) ||
            granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))
        } catch (e: Throwable) {
            Log.e(TAG, "Error checking Health Connect permissions: ${e.message}")
            false
        }
    }

    /**
     * Query real records from Health Connect using flexible aggregation queries
     */
    suspend fun fetchHealthData(): RealHealthData? {
        val client = healthConnectClient ?: return null
        val granted = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Throwable) {
            emptySet()
        }

        if (granted.isEmpty()) {
            Log.w(TAG, "No Health Connect permissions granted.")
            return null
        }

        return try {
            val now = Instant.now()
            val zoneId = ZoneId.systemDefault()
            val todayLocalDate = LocalDate.now(zoneId)
            val todayStart = todayLocalDate.atStartOfDay(zoneId).toInstant()

            // 1. Query SleepSessionRecord (last 7 days to ensure capturing last night's sleep from Samsung Health)
            val hasSleepPerm = granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))
            var totalSleepMinutes = 0L

            if (hasSleepPerm) {
                val sleepStartWindow = now.minus(7, ChronoUnit.DAYS)
                val sleepRequest = ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(sleepStartWindow, now)
                )
                val sleepResponse = client.readRecords(sleepRequest)
                
                // Get the latest sleep session
                val latestSleepSession = sleepResponse.records.maxByOrNull { it.endTime }
                if (latestSleepSession != null) {
                    totalSleepMinutes = Duration.between(latestSleepSession.startTime, latestSleepSession.endTime).toMinutes()
                    Log.d(TAG, "Fetched real sleep session: start=${latestSleepSession.startTime}, end=${latestSleepSession.endTime}, minutes=$totalSleepMinutes")
                }
            }

            val sleepHours = totalSleepMinutes / 60
            val sleepMins = totalSleepMinutes % 60
            val sleepFormatted = if (totalSleepMinutes > 0) {
                if (sleepMins > 0) "${sleepHours}h ${sleepMins.toString().padStart(2, '0')}m" else "${sleepHours}h"
            } else {
                ""
            }

            val sleepSummary = when {
                totalSleepMinutes >= 420 -> "Deep & optimal sleep"
                totalSleepMinutes in 300..419 -> "Steady sleep"
                totalSleepMinutes > 0 -> "Less sleep than recommended"
                else -> "Sleep not synced yet"
            }

            // 2. Query HeartRateRecord
            val hasHeartPerm = granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))
            val bpmList = mutableListOf<Long>()

            if (hasHeartPerm) {
                val heartRateStartWindow = now.minus(24, ChronoUnit.HOURS)
                val heartRateRequest = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(heartRateStartWindow, now)
                )
                val heartRateResponse = client.readRecords(heartRateRequest)
                for (record in heartRateResponse.records) {
                    for (sample in record.samples) {
                        bpmList.add(sample.beatsPerMinute)
                    }
                }
            }

            val avgBpm = if (bpmList.isNotEmpty()) bpmList.average().toInt() else 68
            val minBpm = if (bpmList.isNotEmpty()) bpmList.minOrNull()?.toInt() ?: 58 else 58
            val maxBpm = if (bpmList.isNotEmpty()) bpmList.maxOrNull()?.toInt() ?: 86 else 86

            // 3. Query aggregation for TODAY (Steps, Active Calories, Active Duration)
            val (realTodaySteps, realTodayCalories, realTodayActiveMins) = queryDayAggregation(client, todayStart, now, granted)

            // 4. Query 7 days of the current week (Monday -> Sunday)
            val mondayDate = todayLocalDate.with(DayOfWeek.MONDAY)
            val weeklyHistory = mutableListOf<DailyHeartRingData>()
            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val dayFullNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

            for (i in 0..6) {
                val dayDate = mondayDate.plusDays(i.toLong())
                val dayStart = dayDate.atStartOfDay(zoneId).toInstant()
                val dayEnd = dayDate.plusDays(1).atStartOfDay(zoneId).toInstant()

                if (dayDate.isAfter(todayLocalDate)) {
                    weeklyHistory.add(
                        DailyHeartRingData(
                            dayLabel = dayLabels[i],
                            isSunday = i == 6,
                            stepsProgress = 0f,
                            activeProgress = 0f,
                            calProgress = 0f,
                            stepsCount = 0,
                            activeMinutes = 0,
                            caloriesBurned = 0,
                            fullDayName = dayFullNames[i]
                        )
                    )
                } else if (dayDate == todayLocalDate) {
                    // Today: Use latest real-time data
                    weeklyHistory.add(
                        DailyHeartRingData(
                            dayLabel = dayLabels[i],
                            isSunday = i == 6,
                            stepsProgress = (realTodaySteps.toFloat() / 6000f).coerceIn(0f, 1f),
                            activeProgress = (realTodayActiveMins.toFloat() / 60f).coerceIn(0f, 1f),
                            calProgress = (realTodayCalories.toFloat() / 500f).coerceIn(0f, 1f),
                            stepsCount = realTodaySteps,
                            activeMinutes = realTodayActiveMins,
                            caloriesBurned = realTodayCalories,
                            fullDayName = "Today (${dayFullNames[i]})"
                        )
                    )
                } else {
                    val (dSteps, dCal, dActive) = queryDayAggregation(client, dayStart, dayEnd, granted)

                    weeklyHistory.add(
                        DailyHeartRingData(
                            dayLabel = dayLabels[i],
                            isSunday = i == 6,
                            stepsProgress = (dSteps.toFloat() / 6000f).coerceIn(0f, 1f),
                            activeProgress = (dActive.toFloat() / 60f).coerceIn(0f, 1f),
                            calProgress = (dCal.toFloat() / 500f).coerceIn(0f, 1f),
                            stepsCount = dSteps,
                            activeMinutes = dActive,
                            caloriesBurned = dCal,
                            fullDayName = dayFullNames[i]
                        )
                    )
                }
            }

            RealHealthData(
                sleepDurationMinutes = totalSleepMinutes,
                sleepDurationFormatted = sleepFormatted,
                sleepStageSummary = sleepSummary,
                averageHeartRateBpm = avgBpm,
                minHeartRateBpm = minBpm,
                maxHeartRateBpm = maxBpm,
                currentSteps = realTodaySteps,
                stepGoal = 6000,
                currentActiveMinutes = realTodayActiveMins,
                activeGoalMinutes = 60,
                currentCalories = realTodayCalories,
                calorieGoal = 500,
                weeklyHistory = weeklyHistory,
                isDataAvailable = true
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error querying Health Connect records: ${e.message}", e)
            null
        }
    }

    /**
     * Perform flexible aggregation query based on granted permissions
     */
    private suspend fun queryDayAggregation(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant,
        granted: Set<String>
    ): Triple<Int, Int, Int> {
        return try {
            val hasSteps = granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
            val hasActiveCal = granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
            val hasTotalCal = granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            val hasExercise = granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))

            val metrics = mutableSetOf<androidx.health.connect.client.aggregate.AggregateMetric<*>>()
            if (hasSteps) metrics.add(StepsRecord.COUNT_TOTAL)
            if (hasActiveCal) metrics.add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (hasTotalCal) metrics.add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (hasExercise) metrics.add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)

            if (metrics.isEmpty()) {
                return Triple(0, 0, 0)
            }

            val response = client.aggregate(
                AggregateRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            var steps = if (hasSteps) response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0 else 0

            // Fallback: If steps aggregation returns 0, read raw records directly
            if (steps == 0 && hasSteps) {
                val rawStepsReq = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val rawStepsResp = client.readRecords(rawStepsReq)
                steps = rawStepsResp.records.sumOf { it.count }.toInt()
            }

            val activeEnergy = if (hasActiveCal) response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() else null

            // Active Calories:
            // - Prefer actual ActiveCaloriesBurnedRecord from Health Connect (>0) if available
            // - If ActiveCalories is null or 0, estimate from actual step count (0.04 kcal/step)
            // - Never use TotalCaloriesBurnedRecord (which includes BMR ~775-2000 kcal) against the 500 kcal active goal
            val calories = if (activeEnergy != null && activeEnergy > 0) {
                activeEnergy
            } else {
                (steps * 0.04f).toInt()
            }

            val durationMins = if (hasExercise) response[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toInt() ?: 0 else (steps / 100).coerceAtLeast(0)

            Triple(steps, calories, durationMins)
        } catch (e: Exception) {
            Log.w(TAG, "Aggregation query failed: ${e.message}")
            Triple(0, 0, 0)
        }
    }

    fun openHealthConnectSettings(context: Context) {
        try {
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback: Open Samsung Health
                val shealthIntent = context.packageManager.getLaunchIntentForPackage("com.sec.android.app.shealth")
                if (shealthIntent != null) {
                    context.startActivity(shealthIntent)
                } else {
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(playStoreIntent)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Cannot launch Health Connect or Samsung Health settings: ${ex.message}")
            }
        }
    }
}
