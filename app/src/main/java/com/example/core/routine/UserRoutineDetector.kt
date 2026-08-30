package com.example.core.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.core.ai.BriefingAiGenerator
import com.example.core.ai.BriefingContext
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.RoutineRepository
import com.example.core.repository.WeatherRepository
import com.example.feature.widget.NowBriefWidgetReceiver
import com.example.feature.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class UserRoutineDetector : BroadcastReceiver() {

    companion object {
        private const val TAG = "UserRoutineDetector"
        private var isRegistered = false

        fun registerReceiver(context: Context) {
            if (isRegistered) return
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                context.applicationContext.registerReceiver(UserRoutineDetector(), filter)
                isRegistered = true
                Log.d(TAG, "UserRoutineDetector dynamically registered for system signals")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register UserRoutineDetector: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received system routine signal: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routineRepo = RoutineRepository(context)

                when (action) {
                    Intent.ACTION_USER_PRESENT -> {
                        routineRepo.recordEvent(RoutineRepository.EVENT_USER_PRESENT)

                        // Check if this unlock is the first unlock of the day
                        if (routineRepo.isFirstUnlockOfToday()) {
                            Log.d(TAG, "First unlock of the day detected! Triggering Morning Briefing...")
                            triggerContextAwareBriefing(context, isMorningFirstUnlock = true)
                        } else {
                            // Enqueue background worker check
                            WidgetUpdateWorker.enqueueImmediateUpdate(context)
                        }
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        routineRepo.recordEvent(RoutineRepository.EVENT_SCREEN_OFF)

                        if (routineRepo.isWithinPreBedtimeWindow(30)) {
                            Log.d(TAG, "Pre-bedtime screen off detected! Triggering Wind-down Late Night Briefing...")
                            triggerContextAwareBriefing(context, isPreBedtimeWindDown = true)
                        }
                    }

                    Intent.ACTION_POWER_CONNECTED -> {
                        routineRepo.recordEvent(RoutineRepository.EVENT_POWER_CONNECTED)

                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        if (hour >= 21 || hour <= 2) {
                            Log.d(TAG, "Late night charging detected! Triggering Late Night Recap...")
                            triggerContextAwareBriefing(context, isPreBedtimeWindDown = true)
                        }
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {
                        routineRepo.recordEvent(RoutineRepository.EVENT_POWER_DISCONNECTED)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing routine signal in detector: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun triggerContextAwareBriefing(
        context: Context,
        isMorningFirstUnlock: Boolean = false,
        isPreBedtimeWindDown: Boolean = false
    ) {
        try {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val effectiveHour = when {
                isMorningFirstUnlock -> 8 // Force morning context
                isPreBedtimeWindDown -> 23 // Force late night context
                else -> hour
            }

            val weatherRepo = WeatherRepository(context)
            val calendarRepo = CalendarRepository(context)
            val prefsManager = PreferencesManager(context)

            val currentWeather = try { weatherRepo.fetchWeather() } catch (e: Exception) { null }
            val todayEvents = try { calendarRepo.getTodayEvents() } catch (e: Exception) { emptyList() }

            val briefingGenerator = BriefingAiGenerator(context)
            val briefingContext = BriefingContext(
                hourOfDay = effectiveHour,
                weatherTempC = currentWeather?.currentTempC,
                weatherCondition = currentWeather?.conditionDescription,
                energyScore = if (isMorningFirstUnlock) 95 else if (isPreBedtimeWindDown) 60 else 85,
                remainingCalendarEventsCount = todayEvents.size
            )

            val result = briefingGenerator.generate(briefingContext)

            prefsManager.setWidgetAiBriefing(
                briefing = result.dynamicBriefing,
                energyScore = result.energyScore,
                timestamp = System.currentTimeMillis()
            )

            NowBriefWidgetReceiver.triggerUpdate(context)
            Log.d(TAG, "Context-aware briefing triggered & synced to Glance widgets: '${result.dynamicBriefing}'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger context-aware briefing: ${e.message}")
        }
    }
}
