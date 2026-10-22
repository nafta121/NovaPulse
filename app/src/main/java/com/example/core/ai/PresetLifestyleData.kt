package com.example.core.ai

import com.example.feature.widget.BriefingSession

data class LifestyleTip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String, // "Food & Dining", "Fitness", "Travel", "Relaxation", "Lifestyle"
    val tag: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isDone: Boolean = false
)

object PresetLifestyleData {

    fun getTipsForSession(
        session: BriefingSession,
        weatherCondition: String = "Cool",
        temp: Double = 26.0,
        steps: Long = 4000,
        isWeekend: Boolean = false
    ): List<LifestyleTip> {
        return BriefingContentProvider.getLifestyleTips(
            session = session,
            temp = temp.toInt(),
            weather = weatherCondition,
            count = 3
        )
    }

    fun getTipsForSession(
        timeOfDay: String,
        weatherCondition: String = "Cool",
        temp: Double = 26.0,
        steps: Long = 4000,
        isWeekend: Boolean = false
    ): List<LifestyleTip> {
        val session = BriefingSession.values().firstOrNull { 
            it.sessionTag.equals(timeOfDay, ignoreCase = true) || it.name.equals(timeOfDay, ignoreCase = true)
        } ?: when {
            timeOfDay.contains("early morning", ignoreCase = true) || timeOfDay.contains("dawn", ignoreCase = true) -> BriefingSession.EARLY_MORNING
            timeOfDay.contains("morning", ignoreCase = true) -> BriefingSession.MID_MORNING
            timeOfDay.contains("noon", ignoreCase = true) || timeOfDay.contains("midday", ignoreCase = true) -> BriefingSession.NOON
            timeOfDay.contains("afternoon", ignoreCase = true) -> BriefingSession.AFTERNOON
            timeOfDay.contains("evening", ignoreCase = true) || timeOfDay.contains("sunset", ignoreCase = true) || timeOfDay.contains("golden hour", ignoreCase = true) -> BriefingSession.EVENING
            timeOfDay.contains("late night", ignoreCase = true) -> BriefingSession.LATE_NIGHT
            timeOfDay.contains("night", ignoreCase = true) -> BriefingSession.NIGHT
            else -> BriefingSession.LATE_NIGHT
        }
        return getTipsForSession(session, weatherCondition, temp, steps, isWeekend)
    }
}
