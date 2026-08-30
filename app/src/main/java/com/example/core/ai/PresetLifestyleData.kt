package com.example.core.ai

import com.example.feature.widget.BriefingSession

data class LifestyleTip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String, // "Ẩm thực", "Thể thao", "Du lịch", "Thư giãn", "Lối sống"
    val tag: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isDone: Boolean = false
)

object PresetLifestyleData {

    fun getTipsForSession(
        session: BriefingSession,
        weatherCondition: String = "Mát mẻ",
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
        weatherCondition: String = "Mát mẻ",
        temp: Double = 26.0,
        steps: Long = 4000,
        isWeekend: Boolean = false
    ): List<LifestyleTip> {
        val session = BriefingSession.values().firstOrNull { 
            it.sessionTag.equals(timeOfDay, ignoreCase = true) || it.name.equals(timeOfDay, ignoreCase = true)
        } ?: when {
            timeOfDay.contains("sáng", ignoreCase = true) -> BriefingSession.MID_MORNING
            timeOfDay.contains("trưa", ignoreCase = true) -> BriefingSession.NOON
            timeOfDay.contains("chiều", ignoreCase = true) -> BriefingSession.AFTERNOON
            timeOfDay.contains("tối", ignoreCase = true) -> BriefingSession.NIGHT
            else -> BriefingSession.LATE_NIGHT
        }
        return getTipsForSession(session, weatherCondition, temp, steps, isWeekend)
    }
}
