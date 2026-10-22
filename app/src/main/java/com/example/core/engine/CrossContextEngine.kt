package com.example.core.engine

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.util.Log
import com.example.core.data.CalendarRepository
import com.example.core.data.RealCalendarEvent
import com.example.core.data.WeatherRepository
import com.example.core.database.AppDatabase
import com.example.core.ml.InternalMlEngine
import com.example.core.ml.ProcrastinationRisk
import com.example.core.ml.TaskProcrastinationPredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CrossContextEngine(private val context: Context) {

    private val feedbackTracker = UserFeedbackTracker(context)
    private val calendarRepo = CalendarRepository(context)
    private val weatherRepo = WeatherRepository(context)
    private val database = AppDatabase.getInstance(context)
    private val mlEngine = InternalMlEngine.getInstance(context)

    companion object {
        private const val TAG = "CrossContextEngine"
        private const val W1_URGENCY = 0.45f
        private const val W2_TIME_RELEVANCE = 0.35f
        private const val W3_HABIT_BASE = 0.2f
    }

    suspend fun evaluateContextCards(): List<ContextActionCard> = withContext(Dispatchers.IO) {
        val rawCards = mutableListOf<ContextActionCard>()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        val (batteryPct, isCharging) = getBatteryInfo()
        val nextAlarmMillis = getNextAlarmMillis()

        val todayEvents = try {
            calendarRepo.getRemainingTodayEvents()
        } catch (e: Exception) {
            emptyList()
        }

        val upcomingTasks = try {
            database.taskDao().getUpcomingTasksSync(10)
        } catch (e: Exception) {
            emptyList()
        }

        val weatherData = try {
            weatherRepo.fetchWeather()
        } catch (e: Exception) {
            null
        }

        // 1. Standard Calendar Events
        for (event in todayEvents) {
            val deltaMin = (event.startTimeMillis - now) / 60000L
            val meetUrl = extractMeetingUrl(event)
            val (actionLabel, actionType, actionIntent) = when {
                meetUrl != null -> Triple(
                    "Join Google Meet",
                    ActionType.JOIN_MEET,
                    Intent(Intent.ACTION_VIEW, Uri.parse(meetUrl))
                )
                event.location.isNotBlank() -> Triple(
                    "Directions on Maps",
                    ActionType.OPEN_MAPS,
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(event.location)}"))
                )
                else -> Triple(null, null, null)
            }

            rawCards.add(
                ContextActionCard(
                    id = "calendar_event_${event.id}",
                    title = event.title,
                    subtitle = "Source: ${event.displaySourceLabel}",
                    category = ContextCardCategory.WEATHER_SCHEDULE,
                    emoji = if (meetUrl != null) "🎥" else "📅",
                    urgency = if (meetUrl != null || deltaMin in 0..60) 8.5f else 7.5f,
                    timeRelevance = calculateTimeRelevance(deltaMin),
                    actionLabel = actionLabel,
                    actionType = actionType,
                    actionIntent = actionIntent,
                    rawTimeMillis = event.startTimeMillis,
                    endTimeMillis = event.endTimeMillis,
                    isAllDay = event.isAllDay,
                    location = event.location,
                    meetUrl = meetUrl
                )
            )
        }

        // 2. Travel Buffer Conflicts
        val timedEvents = todayEvents.filter { !it.isAllDay }.sortedBy { it.startTimeMillis }
        for (i in 0 until timedEvents.size - 1) {
            val ev1 = timedEvents[i]
            val ev2 = timedEvents[i + 1]
            val gapMin = (ev2.startTimeMillis - ev1.endTimeMillis) / 60000L
            val deltaToEv2 = (ev2.startTimeMillis - now) / 60000L
            if (gapMin in 0..30 && deltaToEv2 in 10..240) {
                val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ev1.endTimeMillis))
                val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ev2.startTimeMillis))
                val intent = if (ev2.location.isNotBlank()) {
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(ev2.location)}"))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=traffic"))
                }
                rawCards.add(
                    ContextActionCard(
                        id = "travel_buffer_${ev1.id}_${ev2.id}",
                        title = "🚗 Tight travel buffer (${gapMin}m gap)",
                        subtitle = "'${ev1.title}' ends at $endStr, but '${ev2.title}' starts at $startStr. Leave early!",
                        category = ContextCardCategory.TRAVEL_BUFFER_CONFLICT,
                        emoji = "🚗",
                        urgency = 9.3f,
                        timeRelevance = calculateTimeRelevance(deltaToEv2),
                        actionLabel = if (ev2.location.isNotBlank()) "Directions to next event" else "View traffic route",
                        actionType = ActionType.OPEN_MAPS,
                        actionIntent = intent,
                        rawTimeMillis = ev2.startTimeMillis,
                        location = ev2.location
                    )
                )
            }
        }

        // 3. Energy & Task Match
        val pendingTasks = upcomingTasks.filter { !it.isCompleted }
        val heavyTask = pendingTasks.firstOrNull { it.priority == "HIGH" || it.title.length > 15 }
            ?: pendingTasks.firstOrNull()
        if (heavyTask != null) {
            val prediction = mlEngine.behaviorPredictor.predictTomorrow()
            val recoveryPct = prediction.estimatedRecoveryPercent.coerceIn(35, 95)
            if (recoveryPct < 60 && currentHour in 14..21) {
                rawCards.add(
                    ContextActionCard(
                        id = "energy_task_low_${heavyTask.id}",
                        title = "⚡ Low energy ($recoveryPct%) • Adjustment suggested",
                        subtitle = "Recovery index is low. Consider rescheduling heavy task '${heavyTask.title}' to tomorrow morning and prioritizing light tasks now.",
                        category = ContextCardCategory.ENERGY_TASK_MATCH,
                        emoji = "⚡",
                        urgency = 8.8f,
                        timeRelevance = 8.6f,
                        actionLabel = "View light tasks",
                        actionType = ActionType.CUSTOM_INTENT
                    )
                )
            } else if (recoveryPct >= 80 && (currentHour in 9..11 || currentHour in 14..16)) {
                rawCards.add(
                    ContextActionCard(
                        id = "energy_task_high_${heavyTask.id}",
                        title = "🔥 Peak focus window ($recoveryPct%)",
                        subtitle = "Your biorhythm is at peak energy. Ideal time to finish '${heavyTask.title}'.",
                        category = ContextCardCategory.ENERGY_TASK_MATCH,
                        emoji = "🔥",
                        urgency = 8.9f,
                        timeRelevance = 8.8f,
                        actionLabel = "Start focusing now",
                        actionType = ActionType.CUSTOM_INTENT
                    )
                )
            }
        }

        // 4. Procrastination Micro-Action
        for (task in pendingTasks.take(3)) {
            val pred = TaskProcrastinationPredictor.predict(
                task.title,
                task.createdAt,
                task.datetime,
                currentHour
            )
            if (pred.risk == ProcrastinationRisk.HIGH || pred.risk == ProcrastinationRisk.MEDIUM) {
                rawCards.add(
                    ContextActionCard(
                        id = "procrastinate_micro_${task.id}",
                        title = "🎯 2-minute start: ${task.title}",
                        subtitle = "Kickoff step: \"${pred.microActionStep}\" (${pred.durationLabel})",
                        category = ContextCardCategory.PROCRASTINATION_MICRO_ACTION,
                        emoji = "🎯",
                        urgency = if (pred.risk == ProcrastinationRisk.HIGH) 9.1f else 8.4f,
                        timeRelevance = 8.5f,
                        actionLabel = "Start 2 minutes now",
                        actionType = ActionType.START_MICRO_ACTION,
                        rawTimeMillis = task.datetime
                    )
                )
            }
        }

        // 5. Weather + Schedule
        for (event in todayEvents) {
            val deltaMin = (event.startTimeMillis - now) / 60000L
            if (deltaMin in 5..360 && !event.isAllDay) {
                val hourlyItem = weatherData?.getWeatherForTimestamp(event.startTimeMillis)
                val code = hourlyItem?.weatherCode ?: weatherData?.weatherCode ?: 0
                val tempInt = when {
                    hourlyItem != null -> Math.round(hourlyItem.temperatureC).toInt()
                    weatherData != null -> Math.round(weatherData.currentTempC).toInt()
                    else -> 30
                }
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startTimeMillis))
                val isRainy = code in 51..67 || code in 80..99
                val isHot = tempInt >= 33 || (hourlyItem?.apparentTemperatureC ?: 0.0) >= 36.0

                if (isRainy) {
                    val intent = if (event.location.isNotBlank()) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(event.location)}"))
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=traffic"))
                    }
                    rawCards.add(
                        ContextActionCard(
                            id = "weather_sched_${event.id}",
                            title = "🌧️ Rain forecast at $timeStr before event",
                            subtitle = "Event '${event.title}' at $timeStr overlaps with rain/thunderstorms. Bring an umbrella/raincoat & leave 15m early!",
                            category = ContextCardCategory.WEATHER_SCHEDULE,
                            emoji = "🌧️",
                            urgency = 9.4f,
                            timeRelevance = calculateTimeRelevance(deltaMin),
                            actionLabel = if (event.location.isNotBlank()) "Directions on Maps" else "Check traffic",
                            actionType = ActionType.LEAVE_EARLY,
                            actionIntent = intent,
                            rawTimeMillis = event.startTimeMillis,
                            location = event.location
                        )
                    )
                } else if (isHot && deltaMin in 10..180) {
                    val intent = if (event.location.isNotBlank()) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(event.location)}"))
                    } else {
                        null
                    }
                    rawCards.add(
                        ContextActionCard(
                            id = "weather_heat_${event.id}",
                            title = "☀️ Intense heat ${tempInt}°C at $timeStr",
                            subtitle = "Commuting to '${event.title}' during peak heat hours. Stay covered & bring cold water.",
                            category = ContextCardCategory.WEATHER_SCHEDULE,
                            emoji = "☀️",
                            urgency = 8.6f,
                            timeRelevance = calculateTimeRelevance(deltaMin),
                            actionLabel = if (intent != null) "Directions on Maps" else "Check weather",
                            actionType = if (intent != null) ActionType.OPEN_MAPS else null,
                            actionIntent = intent,
                            rawTimeMillis = event.startTimeMillis,
                            location = event.location
                        )
                    )
                }
            }
        }

        // 6. Battery + Meeting
        if (batteryPct <= 20 && !isCharging) {
            val upcomingEvent = todayEvents.firstOrNull {
                val d = (it.startTimeMillis - now) / 60000L
                d in 0..120 && !it.isAllDay
            }
            if (upcomingEvent != null) {
                rawCards.add(
                    ContextActionCard(
                        id = "batt_meeting_${upcomingEvent.id}",
                        title = "🔋 Low battery ($batteryPct%) before meeting",
                        subtitle = "You have '${upcomingEvent.title}' coming up soon. Plug in your charger now!",
                        category = ContextCardCategory.BATTERY_MEETING,
                        emoji = "🔋",
                        urgency = 9.6f,
                        timeRelevance = 9.5f,
                        actionLabel = "Plugged in",
                        actionType = ActionType.CHARGE_REMINDER,
                        rawTimeMillis = upcomingEvent.startTimeMillis
                    )
                )
            }
        }

        // 7. Meet Shortcut
        for (event in todayEvents) {
            val deltaMin = (event.startTimeMillis - now) / 60000L
            val meetUrl = extractMeetingUrl(event)
            if (meetUrl != null && deltaMin in -5..10) {
                rawCards.add(
                    ContextActionCard(
                        id = "meet_shortcut_${event.id}",
                        title = "🎥 Join meeting now: ${event.title}",
                        subtitle = "Online meeting is starting/ongoing. Tap for 1-click join.",
                        category = ContextCardCategory.MEET_SHORTCUT,
                        emoji = "🎥",
                        urgency = 9.8f,
                        timeRelevance = 10.0f,
                        actionLabel = "Join Google Meet",
                        actionType = ActionType.JOIN_MEET,
                        actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetUrl)),
                        rawTimeMillis = event.startTimeMillis,
                        meetUrl = meetUrl
                    )
                )
            }
        }

        // 8. Alarm Check
        if (currentHour >= 20 || currentHour <= 4) {
            val earlyEvents = getTomorrowEarlyEvents()
            if (earlyEvents.isNotEmpty()) {
                val earlyEvent = earlyEvents.first()
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(earlyEvent.startTimeMillis))
                val hasAlarmBeforeEvent = nextAlarmMillis > 0L && nextAlarmMillis < earlyEvent.startTimeMillis
                if (!hasAlarmBeforeEvent) {
                    val alarmCal = Calendar.getInstance().apply {
                        timeInMillis = earlyEvent.startTimeMillis
                        add(Calendar.MINUTE, -60)
                    }
                    val alarmH = alarmCal.get(Calendar.HOUR_OF_DAY)
                    val alarmM = alarmCal.get(Calendar.MINUTE)
                    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, alarmH)
                        putExtra(AlarmClock.EXTRA_MINUTES, alarmM)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Meeting: ${earlyEvent.title}")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    val alarmTimeFormatted = String.format("%02d:%02d", alarmH, alarmM)
                    rawCards.add(
                        ContextActionCard(
                            id = "alarm_check_${earlyEvent.id}",
                            title = "⏰ No alarm set for tomorrow morning",
                            subtitle = "Event '${earlyEvent.title}' starts at $timeStr.",
                            category = ContextCardCategory.ALARM_CHECK,
                            emoji = "⏰",
                            urgency = 8.2f,
                            timeRelevance = 8.5f,
                            actionLabel = "Set alarm $alarmTimeFormatted",
                            actionType = ActionType.SET_ALARM,
                            actionIntent = alarmIntent,
                            rawTimeMillis = earlyEvent.startTimeMillis
                        )
                    )
                }
            }
        }

        // 9. Score and sort
        val scored = rawCards.map { card ->
            val w = feedbackTracker.getCategoryWeight(card.category)
            val score = card.urgency * W1_URGENCY + card.timeRelevance * W2_TIME_RELEVANCE + 1.0f * w
            card.copy(calculatedScore = score)
        }.sortedByDescending { it.calculatedScore }

        Log.d(
            TAG,
            "Evaluated ${scored.size} context cards. Top 1: ${scored.firstOrNull()?.title} (Score: ${scored.firstOrNull()?.calculatedScore})"
        )

        scored
    }

    private fun calculateTimeRelevance(deltaMinutes: Long): Float {
        return when {
            deltaMinutes <= 15L -> 10.0f
            deltaMinutes <= 30L -> 9.0f
            deltaMinutes <= 60L -> 8.0f
            deltaMinutes <= 120L -> 6.5f
            deltaMinutes <= 240L -> 5.0f
            else -> 3.0f
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
            pct to isCharging
        } catch (e: Exception) {
            100 to false
        }
    }

    private fun getNextAlarmMillis(): Long {
        return try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.nextAlarmClock?.triggerTime ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTomorrowEarlyEvents(): List<RealCalendarEvent> {
        return try {
            val tomorrowEvents = calendarRepo.getTomorrowEvents()
            tomorrowEvents.filter { event ->
                val lowerTitle = event.title.lowercase()
                val isHolidayKeyword = lowerTitle.contains("holiday") ||
                    lowerTitle.contains("off") ||
                    lowerTitle.contains("vacation") ||
                    lowerTitle.contains("leave") ||
                    lowerTitle.contains("festival") ||
                    lowerTitle.contains("new year")
                if (event.isAllDay || isHolidayKeyword) {
                    false
                } else {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = event.startTimeMillis
                    }
                    val startHour = cal.get(Calendar.HOUR_OF_DAY)
                    startHour < 10
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractMeetingUrl(event: RealCalendarEvent): String? {
        val combined = "${event.title} ${event.location}"
        val regex = Regex(
            "(https?://[^\\s]*(meet\\.google\\.com|zoom\\.us|teams\\.microsoft\\.com)[^\\s]*)",
            RegexOption.IGNORE_CASE
        )
        return regex.find(combined)?.value
    }
}
