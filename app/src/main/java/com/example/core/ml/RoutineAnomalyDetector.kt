package com.example.core.ml

enum class AnomalyType {
    NONE,
    LATE_WAKEUP,
    EARLY_WAKEUP,
    LATE_BEDTIME
}

data class RoutineAnomalyResult(
    val type: AnomalyType = AnomalyType.NONE,
    val deviationMinutes: Int = 0,
    val recoveryAdvice: String? = null
)

object RoutineAnomalyDetector {

    fun detectWakeAnomaly(
        currentHour: Int,
        currentMinute: Int,
        predictedHour: Int,
        predictedMinute: Int
    ): RoutineAnomalyResult {
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val predictedTotalMinutes = predictedHour * 60 + predictedMinute
        val diffMinutes = currentTotalMinutes - predictedTotalMinutes

        return when {
            diffMinutes >= 45 -> RoutineAnomalyResult(
                type = AnomalyType.LATE_WAKEUP,
                deviationMinutes = diffMinutes,
                recoveryAdvice = "You woke up ~$diffMinutes minutes later than usual today. Start your day at a relaxed pace and drink a glass of water!"
            )
            diffMinutes <= -45 -> RoutineAnomalyResult(
                type = AnomalyType.EARLY_WAKEUP,
                deviationMinutes = -diffMinutes,
                recoveryAdvice = "You woke up earlier than usual! Enjoy the quiet early morning atmosphere."
            )
            else -> RoutineAnomalyResult()
        }
    }
}
