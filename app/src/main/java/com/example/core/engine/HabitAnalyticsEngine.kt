package com.example.core.engine

import android.content.Context
import com.example.core.data.RealHealthData
import com.example.core.ml.InternalMlEngine
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class HabitCategoryStat(
    val categoryName: String,
    val percentage: Int,
    val colorHex: Long,
    val subDetail: String = ""
)

data class WeeklyHabitInsightsData(
    val overallEnergyScore: Int,
    val peakEnergyHoursLabel: String,
    val peakEnergyDescription: String,
    val habitStats: List<HabitCategoryStat>,
    val aiWeeklySummary: String
)

class HabitAnalyticsEngine(private val context: Context) {

    fun calculateWeeklyInsights(
        healthData: RealHealthData?,
        taskCount: Int = 0,
        completedTaskCount: Int = 0,
        upcomingEventTitle: String = "",
        upcomingEventTime: String = ""
    ): WeeklyHabitInsightsData {
        val schedulePct: Int
        val scheduleDetail: String
        if (taskCount > 0) {
            schedulePct = ((completedTaskCount.toFloat() / taskCount.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            scheduleDetail = "Completed $completedTaskCount/$taskCount tasks"
        } else {
            schedulePct = 100
            scheduleDetail = "No pending tasks"
        }

        val healthPct: Int
        val healthDetail: String
        if (healthData != null && healthData.isDataAvailable && healthData.stepGoal > 0) {
            val rawPct = ((healthData.currentSteps.toFloat() / healthData.stepGoal.toFloat())
                .coerceIn(0f, 1f) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            healthPct = if (healthData.currentSteps > 0 && rawPct == 0) 1 else rawPct
            healthDetail = "${healthData.currentSteps}/${healthData.stepGoal} steps"
        } else {
            healthPct = 0
            healthDetail = "Steps not connected"
        }

        val restPct: Int
        val restDetail: String
        if (healthData != null && healthData.isDataAvailable && healthData.sleepDurationMinutes > 0L) {
            val sleepHours = healthData.sleepDurationMinutes.toFloat() / 60f
            restPct = ((sleepHours / 8.0f).coerceIn(0f, 1f) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            restDetail = "Reached ${healthData.sleepDurationFormatted} sleep last night"
        } else {
            restPct = 75
            restDetail = "Sleep not synced"
        }

        val stats = listOf(
            HabitCategoryStat(
                categoryName = "Schedule Engagement",
                percentage = schedulePct,
                colorHex = 0xFF5856D6,
                subDetail = scheduleDetail
            ),
            HabitCategoryStat(
                categoryName = "Health Consistency",
                percentage = healthPct,
                colorHex = 0xFF34C759,
                subDetail = healthDetail
            ),
            HabitCategoryStat(
                categoryName = "Rest & Recovery",
                percentage = restPct,
                colorHex = 0xFFFF9500,
                subDetail = restDetail
            )
        )

        val overallScore = ((schedulePct + healthPct + restPct).toFloat() / 3.0f)
            .roundToInt()
            .coerceIn(0, 100)

        val cleanTitle = upcomingEventTitle.trim()
        val cleanTime = upcomingEventTime.trim()
        val (focusWindowLabel, focusDescription) = determineFocusWindowAndDescription(
            cleanTitle = cleanTitle,
            cleanTime = cleanTime,
            taskCount = taskCount,
            completedTaskCount = completedTaskCount
        )

        val streakText = InternalMlEngine.getInstance(context).streakTracker.getStreakBadgeText()

        val summary = buildString {
            when {
                overallScore >= 80 -> {
                    append("Weekly performance is excellent ($overallScore%)! ")
                    if (taskCount > 0 && completedTaskCount > 0) {
                        append("You have completed $completedTaskCount/$taskCount tasks. ")
                    }
                    if (streakText != null) {
                        append("$streakText, keep up the great momentum!")
                    } else {
                        append("Your work and activity rhythm is well balanced.")
                    }
                }
                overallScore >= 50 -> {
                    append("Today's habit score is $overallScore%. ")
                    if (healthPct < 30) {
                        val steps = healthData?.currentSteps ?: 0
                        append("Current activity is low ($steps steps). Take a light walk around $focusWindowLabel to boost your energy score.")
                    } else if (taskCount > completedTaskCount) {
                        val rem = taskCount - completedTaskCount
                        append("$rem tasks remaining, focus on tackling them during $focusWindowLabel to reach your goal.")
                    } else {
                        append("Maintain your momentum and recharge properly during $focusWindowLabel.")
                    }
                }
                else -> {
                    append("Today's energy score is $overallScore%. ")
                    append("Allocate a 25-minute Pomodoro session during $focusWindowLabel and increase light movement to restore vitality.")
                }
            }
        }

        return WeeklyHabitInsightsData(
            overallEnergyScore = overallScore,
            peakEnergyHoursLabel = focusWindowLabel,
            peakEnergyDescription = focusDescription,
            habitStats = stats,
            aiWeeklySummary = summary
        )
    }

    private fun determineFocusWindowAndDescription(
        cleanTitle: String,
        cleanTime: String,
        taskCount: Int,
        completedTaskCount: Int
    ): Pair<String, String> {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val nowMinutes = hour * 60 + minute

        if (cleanTitle.isNotBlank() &&
            cleanTime.isNotBlank() &&
            !cleanTime.equals("All day", ignoreCase = true)
        ) {
            val eventMinutes = parseTimeToMinutes(cleanTime)
            if (eventMinutes != null && eventMinutes >= nowMinutes - 15) {
                val formattedWindow = formatFocusTimeWindow(cleanTime)
                return formattedWindow to "Focus on '$cleanTitle' and priority tasks."
            }
        }

        return when (hour) {
            in 5..10 -> {
                val desc = if (taskCount > 0) {
                    "Golden morning window to complete $taskCount important tasks."
                } else {
                    "Golden morning window with the highest focus and productivity index."
                }
                "08:30 – 11:30" to desc
            }
            in 11..13 -> {
                "12:00 – 13:30" to "Midday rest period to recharge energy for afternoon goals."
            }
            in 14..17 -> {
                val desc = if (taskCount > completedTaskCount) {
                    "Afternoon focus window to wrap up ${taskCount - completedTaskCount} remaining tasks."
                } else {
                    "Golden afternoon window with optimal cognitive performance."
                }
                "14:00 – 16:30" to desc
            }
            in 18..21 -> {
                "19:30 – 21:00" to "Review today's plan, do light exercise, and unwind."
            }
            else -> {
                "22:00 – 23:00" to "Unwind and dim your screens to prepare for deep sleep."
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        return try {
            val firstPart = timeStr.split("–")[0].split("-")[0].trim()
            val parts = firstPart.split(":")
            if (parts.size >= 2) {
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                h * 60 + m
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatFocusTimeWindow(cleanTime: String): String {
        if (cleanTime.contains("–") || cleanTime.contains("-")) {
            return cleanTime
        }
        return try {
            val parts = cleanTime.split(":")
            if (parts.size >= 2) {
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                val endTotal = h * 60 + m + 45
                val endH = (endTotal / 60) % 24
                val endM = endTotal % 60
                val endStr = String.format(Locale.getDefault(), "%02d:%02d", endH, endM)
                "$cleanTime – $endStr"
            } else {
                cleanTime
            }
        } catch (e: Exception) {
            cleanTime
        }
    }
}
