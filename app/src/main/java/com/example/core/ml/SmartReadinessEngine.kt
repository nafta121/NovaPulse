package com.example.core.ml

import java.util.Calendar
import kotlin.math.roundToInt

enum class ReadinessLevel(val label: String, val colorHex: Long) {
    PEAK("Peak", 0xFF10B981),
    OPTIMAL("Optimal", 0xFF3B82F6),
    MODERATE("Moderate", 0xFFF59E0B),
    RECOVERY_NEEDED("Recovery Needed", 0xFFEF4444)
}

data class ReadinessFactorBreakdown(
    val sleepScore: Int,
    val sleepHoursFormatted: String,
    val activityScore: Int,
    val weatherScore: Int,
    val taskLoadScore: Int
)

data class DailyReadinessResult(
    val overallScore: Int,
    val level: ReadinessLevel,
    val headline: String,
    val insight: String,
    val breakdown: ReadinessFactorBreakdown,
    val recoveryAdvice: String? = null,
    val tonightSleepRecoveryEstimate: Int = 80
)

object SmartReadinessEngine {

    fun calculateReadiness(
        sleepDurationMinutes: Int?,
        activityCompletionRate: Float = 0.5f,
        streakDays: Int = 0,
        temperatureC: Double? = 28.0,
        humidityPercent: Int? = 65,
        pendingTasksCount: Int = 0,
        completedTasksCount: Int = 0,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): DailyReadinessResult {
        val actualSleepMins = sleepDurationMinutes?.takeIf { it > 0 } ?: 435
        val sleepHours = actualSleepMins / 60.0

        val sleepScore = when {
            sleepHours in 7.0..8.5 -> 95
            sleepHours in 6.5..7.0 -> 85
            sleepHours > 8.5 -> 88
            sleepHours in 5.5..6.5 -> 72
            sleepHours in 4.5..5.5 -> 58
            else -> 45
        }.coerceIn(30, 100)

        val sleepH = actualSleepMins / 60
        val sleepM = actualSleepMins % 60
        val sleepFormatted = "${sleepH}h ${String.format("%02d", sleepM)}m"

        val baseActivity = (activityCompletionRate.coerceIn(0f, 1.2f) * 80).roundToInt()
        val streakBonus = (streakDays.coerceAtMost(7) * 3)
        val activityScore = (baseActivity + streakBonus).coerceIn(45, 100)

        val temp = temperatureC ?: 28.0
        val humidity = humidityPercent ?: 65

        val tempPenalty = when {
            temp in 21.0..27.0 -> 0
            temp in 27.1..31.0 -> 6
            temp in 31.1..35.0 -> 14
            temp > 35.0 -> 22
            temp < 16.0 -> 10
            else -> 4
        }

        val humidityPenalty = if (humidity > 80) 8 else if (humidity < 35) 5 else 0
        val weatherScore = (95 - tempPenalty - humidityPenalty).coerceIn(50, 100)

        val taskPenalty = (pendingTasksCount * 4).coerceAtMost(28)
        val completionBonus = (completedTasksCount * 3).coerceAtMost(15)
        val circadianAdjustment = when (currentHour) {
            in 6..11 -> 5
            in 13..15 -> -4
            in 21..23 -> -8
            in 0..4 -> -15
            else -> 0
        }

        val taskLoadScore = (85 - taskPenalty + completionBonus + circadianAdjustment).coerceIn(40, 100)

        val rawOverall = (sleepScore * 0.45) +
                (activityScore * 0.25) +
                (weatherScore * 0.15) +
                (taskLoadScore * 0.15)

        val overallScore = rawOverall.roundToInt().coerceIn(35, 99)

        val level = when {
            overallScore >= 85 -> ReadinessLevel.PEAK
            overallScore >= 70 -> ReadinessLevel.OPTIMAL
            overallScore >= 55 -> ReadinessLevel.MODERATE
            else -> ReadinessLevel.RECOVERY_NEEDED
        }

        val (headline, insight, recoveryAdvice) = generatePersonalizedCopy(
            score = overallScore,
            level = level,
            sleepHours = sleepHours,
            temp = temp,
            pendingTasks = pendingTasksCount,
            currentHour = currentHour
        )

        val tonightRecoveryEstimate = (overallScore + 15).coerceIn(65, 96)

        return DailyReadinessResult(
            overallScore = overallScore,
            level = level,
            headline = headline,
            insight = insight,
            breakdown = ReadinessFactorBreakdown(
                sleepScore = sleepScore,
                sleepHoursFormatted = sleepFormatted,
                activityScore = activityScore,
                weatherScore = weatherScore,
                taskLoadScore = taskLoadScore
            ),
            recoveryAdvice = recoveryAdvice,
            tonightSleepRecoveryEstimate = tonightRecoveryEstimate
        )
    }

    private fun generatePersonalizedCopy(
        score: Int,
        level: ReadinessLevel,
        sleepHours: Double,
        temp: Double,
        pendingTasks: Int,
        currentHour: Int
    ): Triple<String, String, String?> {
        return when (level) {
            ReadinessLevel.PEAK -> Triple(
                "Peak bio-energy ($score/100)",
                "Restful sleep (${String.format("%.1f", sleepHours)}h) and balanced habits keep your mind sharp. Ideal time for Deep Work or high-intensity workouts.",
                null
            )
            ReadinessLevel.OPTIMAL -> {
                val weatherNote = if (temp > 32.0) "Watch out for outdoor heat (${temp.toInt()}°C), stay hydrated." else "Steady physical state for today's plans."
                Triple(
                    "Ready for an active day ($score/100)",
                    "Your body recovered well (${String.format("%.1f", sleepHours)}h sleep). $weatherNote",
                    if (pendingTasks > 4) "Prioritize your 2 most important tasks this morning to avoid overload." else null
                )
            }
            ReadinessLevel.MODERATE -> {
                val reason = if (sleepHours < 6.5) "last night's sleep was slightly short (${String.format("%.1f", sleepHours)}h)" else "fairly high task load ($pendingTasks tasks)"
                Triple(
                    "Moderate energy — Pace yourself ($score/100)",
                    "Because $reason, alternate 45 minutes of focus with 5-minute breaks.",
                    "Suggested: Take a 15-minute power nap and limit caffeine after 3:00 PM."
                )
            }
            ReadinessLevel.RECOVERY_NEEDED -> Triple(
                "Prioritize rest & recovery ($score/100)",
                "Your body shows signs of sleep deficit or high stress. Avoid overloading on complex tasks today.",
                "Go to bed 45 minutes early tonight to restore ~85% energy for tomorrow."
            )
        }
    }
}
