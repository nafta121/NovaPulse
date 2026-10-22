package com.example.feature.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.ai.LocalLlmManager
import com.example.core.ai.ModelDownloadManager
import com.example.core.database.AppDatabase
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.RealCalendarEvent
import com.example.core.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val PERIODIC_WORK_NAME = "NovaPulseWidgetPeriodicUpdate"
        private const val IMMEDIATE_WORK_NAME = "NovaPulseWidgetImmediateUpdate"

        /**
         * Schedules periodic background widget updates via WorkManager.
         * Runs every 15 minutes with CONNECTED network constraint.
         */
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()

                val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                Log.d(TAG, "Scheduled periodic widget update worker (15 min interval, network required)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic widget update worker: ${e.message}")
            }
        }

        /**
         * Triggers an immediate one-time WorkManager update task
         * for when notifications are parsed, tasks are modified, or settings change.
         */
        fun enqueueImmediateUpdate(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
                Log.d(TAG, "Enqueued immediate widget update task")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue immediate widget update: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "WidgetUpdateWorker starting background execution...")

            val database = AppDatabase.getInstance(context)
            val prefsManager = PreferencesManager(context)
            val downloadManager = ModelDownloadManager(context)
            val llmManager = LocalLlmManager(context, downloadManager)
            val weatherRepository = WeatherRepository(context)
            val calendarRepository = CalendarRepository(context)
            val briefingGenerator = com.example.core.ai.BriefingAiGenerator(context, llmManager)

            // AdaptiveBackgroundSync: Automatically skip nighttime network fetches during learned sleep hours to save battery
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            val routineInsight = mlEngine.getRoutineInsight()
            val isSleepHour = if (routineInsight.predictedBedHour > routineInsight.predictedWakeHour) {
                currentHour >= routineInsight.predictedBedHour || currentHour < routineInsight.predictedWakeHour
            } else {
                currentHour in routineInsight.predictedBedHour until routineInsight.predictedWakeHour
            }

            val currentWeather = if (isSleepHour) {
                Log.d(
                    TAG,
                    "AdaptiveBackgroundSync: Currently within learned sleep window ($currentHour h). Skipping network fetch for battery saving."
                )
                com.example.core.repository.WeatherCacheManager.load(context)
            } else {
                try {
                    weatherRepository.fetchWeather()
                } catch (e: Exception) {
                    Log.w(TAG, "Background weather fetch skipped/failed: ${e.message}")
                    null
                }
            }

            val upcomingTasks = try {
                database.taskDao().getUpcomingTasksSync(5)
            } catch (e: Exception) {
                emptyList()
            }

            val todayEvents: List<RealCalendarEvent> = try {
                calendarRepository.getTodayEvents()
            } catch (e: Exception) {
                emptyList()
            }

            val topTaskTitle = upcomingTasks.firstOrNull()?.title
            val startOfTodayMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayExpenses = try {
                database.expenseDao().getSpentSinceSync(startOfTodayMs) ?: 0.0
            } catch (_: Exception) {
                0.0
            }
            val screenTimeStats = try {
                com.example.core.repository.ScreenTimeRepository(context).getScreenTimeStats()
            } catch (_: Exception) {
                null
            }
            val healthData = try {
                com.example.core.repository.HealthConnectRepository(context).fetchHealthData()
            } catch (_: Exception) {
                null
            }

            // 2. Synthesize AI Briefing for 4 Time Phases
            val smartBriefing = llmManager.generateSmartWidgetBriefing(
                topTaskTitle = topTaskTitle,
                pendingTasksCount = upcomingTasks.size,
                totalExpensesToday = todayExpenses,
                hourOfDay = currentHour
            )

            val briefingContext = com.example.core.ai.BriefingContext(
                hourOfDay = currentHour,
                weatherTempC = currentWeather?.currentTempC,
                weatherCondition = currentWeather?.conditionDescription,
                energyScore = smartBriefing.energyScore,
                remainingCalendarEventsCount = todayEvents.size,
                upcomingEventsSummary = todayEvents.firstOrNull()?.title,
                topTaskTitle = topTaskTitle,
                sleepHoursLastNight = healthData?.sleepDurationMinutes?.let { it / 60.0 },
                screenTimeMinutesToday = screenTimeStats?.totalScreenTimeTodayMillis?.let { (it / 60_000L).toInt() },
                stepsToday = healthData?.currentSteps
            )

            val aiBriefingResult = briefingGenerator.generate(briefingContext)

            // 3. Persist AI Output to DataStore
            val currentSession =
                BriefingSession.fromHourAndMinute(currentHour, Calendar.getInstance().get(Calendar.MINUTE))
            val currentTempRounded = Math.round(currentWeather?.currentTempC ?: 29.0).toInt()
            val sanitizedBriefing = BriefingSessionHelper.sanitizeWidgetText(
                aiBriefingResult.dynamicBriefing,
                currentSession,
                currentTempRounded
            )

            prefsManager.setWidgetAiBriefing(
                briefing = sanitizedBriefing,
                energyScore = aiBriefingResult.energyScore,
                timestamp = System.currentTimeMillis()
            )

            // 4. Update Glance Widgets
            NowBriefWidget().updateAll(context)
            GlanceContextWidget().updateAll(context)

            // 5. Broadcast to receivers for instant OS reflection
            NowBriefWidgetReceiver.triggerUpdate(context)
            NovaPulseGlanceWidgetReceiver.triggerUpdate(context)
            if (com.example.feature.island.NowBarLiveService.isRunning) {
                com.example.feature.island.NowBarLiveService.startOrRefresh(context)
            }

            Log.d(
                TAG,
                "WidgetUpdateWorker completed successfully: Briefing='${smartBriefing.subtext}', Energy=${smartBriefing.energyScore}%"
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "WidgetUpdateWorker error: ${e.message}", e)
            Result.retry()
        }
    }
}
