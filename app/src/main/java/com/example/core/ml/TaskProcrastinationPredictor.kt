package com.example.core.ml

import java.util.Calendar

enum class ProcrastinationRisk(
    val label: String,
    val colorHex: Long,
    val tagLabel: String
) {
    LOW("On track", 0xFF10B981, "🟢 On time"),
    MEDIUM("Procrastination risk", 0xFFF59E0B, "🟡 Needs attention"),
    HIGH("High delay risk", 0xFFEF4444, "🔴 Delay risk")
}

data class TaskPredictionResult(
    val estimatedMinutes: Int,
    val durationLabel: String,
    val risk: ProcrastinationRisk,
    val riskReason: String? = null,
    val optimalTimeAdvice: String,
    val microActionStep: String,
    val peakFocusSlot: String,
    val isDeepWork: Boolean,
    val category: TaskCategory = TaskCategory.CHORES_ERRANDS,
    val urgency: TaskUrgency = TaskUrgency.NORMAL
)

object TaskProcrastinationPredictor {

    const val MORNING_PEAK_FOCUS = "09:00 - 11:30"
    const val AFTERNOON_PEAK_FOCUS = "14:00 - 16:30"

    fun predict(
        title: String,
        scheduledTimestampMs: Long = System.currentTimeMillis(),
        createdAtMs: Long = System.currentTimeMillis(),
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): TaskPredictionResult {
        val nlp = VietnameseTaskNlpEngine.analyze(
            title = title,
            scheduledTimestampMs = scheduledTimestampMs,
            createdAtMs = createdAtMs,
            currentHour = currentHour
        )

        val estimatedMins = nlp.estimatedMinutes
        val isDeepWork = nlp.isDeepWork
        val durationLabel = "~$estimatedMins mins"

        val cal = Calendar.getInstance().apply { timeInMillis = scheduledTimestampMs }
        val scheduledHour = cal.get(Calendar.HOUR_OF_DAY)
        val nowMs = System.currentTimeMillis()
        val ageHours = ((nowMs - createdAtMs).coerceAtLeast(0L)) / (1000 * 3600)

        var riskScore = 0
        var riskReason: String? = null

        if (isDeepWork && (scheduledHour >= 21 || scheduledHour in 12..13)) {
            riskScore += 2
            riskReason = "High-focus task scheduled during low bio-energy hours (${String.format("%02d:00", scheduledHour)})."
        }

        if (nlp.urgency == TaskUrgency.CRITICAL) {
            riskScore += 2
            riskReason = "Urgent task nearing deadline — prioritize handling immediately."
        }

        if (ageHours >= 36) {
            riskScore += 2
            riskReason = "This task has been pending for over ${ageHours / 24} days."
        } else if (ageHours >= 18) {
            riskScore += 1
        }

        if (estimatedMins >= 60 && currentHour >= 20) {
            riskScore += 1
            if (riskReason == null) {
                riskReason = "Long task (~$estimatedMins mins) in the evening is prone to procrastination."
            }
        }

        val risk = when {
            riskScore >= 2 -> ProcrastinationRisk.HIGH
            riskScore == 1 -> ProcrastinationRisk.MEDIUM
            else -> ProcrastinationRisk.LOW
        }

        return TaskPredictionResult(
            estimatedMinutes = estimatedMins,
            durationLabel = durationLabel,
            risk = risk,
            riskReason = riskReason,
            optimalTimeAdvice = nlp.timingAdvice,
            microActionStep = nlp.microActionStep,
            peakFocusSlot = nlp.optimalFocusWindow,
            isDeepWork = isDeepWork,
            category = nlp.category,
            urgency = nlp.urgency
        )
    }

    fun getDailyPeakFocusWindow(): String = MORNING_PEAK_FOCUS
}
