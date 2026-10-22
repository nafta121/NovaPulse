package com.example.core.engine

import android.content.Intent

enum class ContextCardCategory {
    WEATHER_SCHEDULE,
    BATTERY_MEETING,
    ALARM_CHECK,
    COMMUTE_TRAFFIC,
    HOME_RELAX,
    MEET_SHORTCUT,
    SPOTIFY_SHORTCUT,
    GENERAL_TASK,
    EMPTY_SCHEDULE,
    TRAVEL_BUFFER_CONFLICT,
    ENERGY_TASK_MATCH,
    PROCRASTINATION_MICRO_ACTION
}

enum class ActionType {
    JOIN_MEET,
    OPEN_MAPS,
    SET_ALARM,
    PLAY_MUSIC,
    COPY_CODE,
    CHARGE_REMINDER,
    LEAVE_EARLY,
    START_MICRO_ACTION,
    RESCHEDULE_TASK,
    CUSTOM_INTENT
}

data class ContextActionCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ContextCardCategory,
    val emoji: String = "📅",
    val urgency: Float = 5.0f,
    val timeRelevance: Float = 5.0f,
    val calculatedScore: Float = 0.0f,
    val actionLabel: String? = null,
    val actionType: ActionType? = null,
    val actionIntent: Intent? = null,
    val rawTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis() + 3600000L,
    val isAllDay: Boolean = false,
    val location: String = "",
    val meetUrl: String? = null
)
