package com.example.core.engine

enum class NowNudgeActionType {
    START_FOCUS,
    OPEN_SPOTIFY,
    DRINK_WATER,
    START_BREATHING,
    VIEW_SCHEDULE
}

data class NowNudgeItem(
    val emoji: String,
    val badge: String = "Now Nudge • One UI",
    val headline: String,
    val subtext: String,
    val actionLabel: String,
    val actionType: NowNudgeActionType
)

object NowNudgeEngine {

    fun generateNudge(
        currentHour: Int,
        pendingTasksCount: Int = 0,
        currentSteps: Int = 0,
        waterDrunkMl: Int = 0
    ): NowNudgeItem {
        return when (currentHour) {
            in 5..8 -> NowNudgeItem(
                emoji = "🌅",
                badge = "Now Nudge • Morning",
                headline = "Start your day with inspiration",
                subtext = "Play an energizing morning playlist to build momentum for productive work.",
                actionLabel = "Play on Spotify",
                actionType = NowNudgeActionType.OPEN_SPOTIFY
            )
            in 9..11 -> {
                if (pendingTasksCount > 0) {
                    NowNudgeItem(
                        emoji = "⚡",
                        badge = "Now Nudge • Productivity",
                        headline = "Golden window for deep focus",
                        subtext = "You have $pendingTasksCount tasks today. Activate 25 minutes of distraction-free Deep Work.",
                        actionLabel = "Start Focus",
                        actionType = NowNudgeActionType.START_FOCUS
                    )
                } else {
                    NowNudgeItem(
                        emoji = "🎯",
                        badge = "Now Nudge • Planning",
                        headline = "Check your morning progress",
                        subtext = "Review your event schedule or quickly set an important goal to accomplish.",
                        actionLabel = "View Schedule",
                        actionType = NowNudgeActionType.VIEW_SCHEDULE
                    )
                }
            }
            in 12..13 -> NowNudgeItem(
                emoji = "🍱",
                badge = "Now Nudge • Recharge",
                headline = "Time to rest & recover",
                subtext = "Take 3 minutes to relax your mind and prepare for an alert afternoon.",
                actionLabel = "Mindful Breathing",
                actionType = NowNudgeActionType.START_BREATHING
            )
            in 14..16 -> {
                if (waterDrunkMl < 1500) {
                    NowNudgeItem(
                        emoji = "💧",
                        badge = "Now Nudge • Wellness",
                        headline = "Hydrate for the afternoon",
                        subtext = "Staying hydrated keeps your brain sharp and reduces eye strain while working.",
                        actionLabel = "Log 250ml",
                        actionType = NowNudgeActionType.DRINK_WATER
                    )
                } else {
                    NowNudgeItem(
                        emoji = "⏱️",
                        badge = "Now Nudge • Wrap-up",
                        headline = "Power through your daily goals",
                        subtext = "Run a 25-minute Pomodoro sprint to finish up your remaining tasks.",
                        actionLabel = "Run 25m Focus",
                        actionType = NowNudgeActionType.START_FOCUS
                    )
                }
            }
            in 17..21 -> NowNudgeItem(
                emoji = "🌇",
                badge = "Now Nudge • Unwind",
                headline = "Set aside work worries",
                subtext = "Enjoy relaxing melodies after a day of effort and light movement.",
                actionLabel = "Play Relaxing Music",
                actionType = NowNudgeActionType.OPEN_SPOTIFY
            )
            else -> NowNudgeItem(
                emoji = "🌙",
                badge = "Now Nudge • Sleep",
                headline = "Prepare for deep sleep",
                subtext = "Relax your body with a 4-7-8 breathing exercise to fall asleep easily.",
                actionLabel = "4-7-8 Breathing",
                actionType = NowNudgeActionType.START_BREATHING
            )
        }
    }
}
