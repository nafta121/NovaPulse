package com.example.feature.widget

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * Matching greeting title and subtitle pair
 */
data class BriefingGreeting(
    val title: String,
    val subtitle: String
)

/**
 * 7 detailed daily time sessions following One UI 9 standards
 */
enum class BriefingSession(
    val greetings: List<BriefingGreeting>,
    val sessionTag: String,
    val emoji: String,
    val iconGradient: List<Color>
) {
    EARLY_MORNING(
        greetings = listOf(
            BriefingGreeting("Good morning", "Start your new day filled with excitement and energy."),
            BriefingGreeting("Have a good day", "Wishing you a smooth and productive day ahead."),
            BriefingGreeting("Rise and shine", "Wake up to the dawn light and enjoy a glass of warm water."),
            BriefingGreeting("Start your day", "Organize your key goals and kick off a brilliant day."),
            BriefingGreeting("Morning sunshine", "Enjoy the fresh air and get ready for a wonderful day."),
            BriefingGreeting("Fresh start", "A brand new day with fresh opportunities and experiences."),
            BriefingGreeting("Ready for today", "Confident and ready to conquer all your plans today."),
            BriefingGreeting("Early bird", "Rise early with the dawn to take charge of an active day."),
            BriefingGreeting("Wake up & thrive", "Awaken your body, breathe deeply, and get into the rhythm of the day.")
        ),
        sessionTag = "Dawn",
        emoji = "🌅",
        iconGradient = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    ),
    MID_MORNING(
        greetings = listOf(
            BriefingGreeting("Morning focus", "Golden hours for deep focus and peak productivity."),
            BriefingGreeting("Stay productive", "Maintain your work pace and accomplish your set goals."),
            BriefingGreeting("Keep the momentum", "Keep up the momentum to conquer today's tasks."),
            BriefingGreeting("Deep work", "Dedicate your full attention to your most important work."),
            BriefingGreeting("In the zone", "Stay in a state of high concentration for breakthrough results."),
            BriefingGreeting("Crush your goals", "Step by step, excellently complete your to-do list."),
            BriefingGreeting("Stay sharp", "Keep a clear mind and tackle your tasks efficiently."),
            BriefingGreeting("Focus mode", "Set aside distractions and maximize focus for the best outcomes."),
            BriefingGreeting("Peak performance", "Prime time to achieve outstanding work productivity.")
        ),
        sessionTag = "Morning",
        emoji = "☀️",
        iconGradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
    ),
    NOON(
        greetings = listOf(
            BriefingGreeting("Midday recap", "Review your morning and recharge your energy for midday."),
            BriefingGreeting("Lunch break", "Enjoy a delicious lunch and give your eyes a rest."),
            BriefingGreeting("Time to recharge", "Take a 15-20 minute power nap to get ready for a productive afternoon."),
            BriefingGreeting("Midday breath", "Step away from the screen, relax your body, and unwind a bit."),
            BriefingGreeting("Refuel & relax", "Refuel with a nutritious meal and a relaxed mindset."),
            BriefingGreeting("Halfway there", "You're halfway through a successful day."),
            BriefingGreeting("Take a pause", "Take a short pause to refresh your mind."),
            BriefingGreeting("Noon refresh", "Take a sip of cool water and reboot your alertness.")
        ),
        sessionTag = "Noon",
        emoji = "🌤",
        iconGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
    ),
    AFTERNOON(
        greetings = listOf(
            BriefingGreeting("Afternoon focus", "Stay alert and accelerate your work progress."),
            BriefingGreeting("Stay hydrated", "Drink more water or afternoon tea to keep your spirits refreshed."),
            BriefingGreeting("Power through", "Push past afternoon sluggishness and finish your remaining tasks."),
            BriefingGreeting("Good afternoon", "Maintain an effective workflow and stay on top of your game."),
            BriefingGreeting("Afternoon boost", "Grab a light cup of tea or coffee to regain your enthusiasm."),
            BriefingGreeting("Keep going", "Just a little further to fully accomplish today's goals."),
            BriefingGreeting("Second wind", "Find fresh inspiration for your afternoon projects."),
            BriefingGreeting("Sprint finish", "Focus and speed up to cross the finish line of a successful workday.")
        ),
        sessionTag = "Afternoon",
        emoji = "⛅",
        iconGradient = listOf(Color(0xFF22D3EE), Color(0xFF0D9488))
    ),
    EVENING(
        greetings = listOf(
            BriefingGreeting("Evening wind-down", "Set work aside, enjoy the sunset, and do some light exercise."),
            BriefingGreeting("Wrap up your day", "Review today's achievements and get ready to leave your desk."),
            BriefingGreeting("Heading home", "Relax on your way home and look forward to a cozy dinner."),
            BriefingGreeting("Evening brief", "Unwind your mind after a hard day's work."),
            BriefingGreeting("Sunset vibes", "Watch the sunset and feel the sense of peace."),
            BriefingGreeting("Day completed", "Be proud of giving your best effort throughout the day."),
            BriefingGreeting("Clocking out", "Close out work and open up quality time for family."),
            BriefingGreeting("Time to unwind", "Step outside and breathe in the cool evening air.")
        ),
        sessionTag = "Sunset",
        emoji = "🌇",
        iconGradient = listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFBBF24))
    ),
    NIGHT(
        greetings = listOf(
            BriefingGreeting("Night unwind", "Relax, have dinner, and dedicate quality time to yourself."),
            BriefingGreeting("Relax and recharge", "Relieve stress with your favorite music or a good book."),
            BriefingGreeting("Personal time", "Warm moments with loved ones and self-care."),
            BriefingGreeting("Cozy evening", "Enjoy the warm, comfortable atmosphere of your home."),
            BriefingGreeting("Mindful evening", "Quiet your mind and be grateful for the good things today."),
            BriefingGreeting("Dinner & chill", "Enjoy a delicious dinner and chat with family."),
            BriefingGreeting("Peace of mind", "Leave all pressures behind and peacefully enjoy the night."),
            BriefingGreeting("Slow down", "Slow down a beat, listen to your body, and take care of yourself.")
        ),
        sessionTag = "Night",
        emoji = "🌙",
        iconGradient = listOf(Color(0xFF818CF8), Color(0xFF6366F1))
    ),
    LATE_NIGHT(
        greetings = listOf(
            BriefingGreeting("Sleep well", "Relax your mind and prepare for a deep, restful sleep."),
            BriefingGreeting("Late night", "It's getting late—let your body rest and recover."),
            BriefingGreeting("Sweet dreams", "Turn off electronic devices and drift into sweet dreams."),
            BriefingGreeting("Time to rest", "Put all worries aside; wishing you a peaceful night's sleep."),
            BriefingGreeting("Late night recap", "Close out a long day and recharge your energy for tomorrow."),
            BriefingGreeting("Night owl", "If you're still up, stay warm and get ready for bed soon."),
            BriefingGreeting("Quiet hours", "Quiet hours—the perfect time to restore your energy."),
            BriefingGreeting("Drift away", "Breathe slowly, relax your whole body, and fall into a sound sleep."),
            BriefingGreeting("Restful sleep", "Wishing you a full night's sleep to wake up refreshed tomorrow.")
        ),
        sessionTag = "Late Night",
        emoji = "🌌",
        iconGradient = listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
    );

    val titles: List<String>
        get() = greetings.map { it.title }

    val subtitles: List<String>
        get() = greetings.map { it.subtitle }

    val title: String
        get() = greetings.first().title

    val subtitle: String
        get() = greetings.first().subtitle

    fun getRandomGreeting(): BriefingGreeting = greetings.random()
    fun getRandomTitle(): String = titles.random()
    fun getSubtitleForTitle(title: String): String =
        greetings.firstOrNull { it.title.equals(title, ignoreCase = true) }?.subtitle ?: subtitle

    companion object {
        fun fromHourAndMinute(hour: Int, minute: Int = 0): BriefingSession {
            val totalMins = hour * 60 + minute
            return when {
                totalMins in (5 * 60)..(8 * 60 + 29) -> EARLY_MORNING   // 05:00 - 08:29
                totalMins in (8 * 60 + 30)..(11 * 60 + 29) -> MID_MORNING // 08:30 - 11:29
                totalMins in (11 * 60 + 30)..(13 * 60 + 59) -> NOON       // 11:30 - 13:59
                totalMins in (14 * 60)..(16 * 60 + 59) -> AFTERNOON       // 14:00 - 16:59
                totalMins in (17 * 60)..(19 * 60 + 29) -> EVENING         // 17:00 - 19:29
                totalMins in (19 * 60 + 30)..(22 * 60 + 29) -> NIGHT      // 19:30 - 22:29
                else -> LATE_NIGHT                                        // 22:30 - 04:59
            }
        }

        fun getCurrentSession(): BriefingSession {
            val now = LocalTime.now()
            return fromHourAndMinute(now.hour, now.minute)
        }
    }
}

/**
 * Information data structure for Briefing Session
 */
data class BriefingSessionInfo(
    val session: BriefingSession,
    val title: String,
    val subtitle: String,
    val hour: Int,
    val titles: List<String> = session.titles,
    val greetings: List<BriefingGreeting> = session.greetings
)

/**
 * Helper managing dynamic greeting titles & subtitles across 7 daily time sessions
 */
object BriefingSessionHelper {

    fun getSessionInfo(hour: Int, minute: Int = 0): BriefingSessionInfo {
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return BriefingSessionInfo(
            session = session,
            title = session.title,
            subtitle = session.subtitle,
            hour = hour,
            titles = session.titles,
            greetings = session.greetings
        )
    }

    fun getCurrentSessionInfo(): BriefingSessionInfo {
        val now = LocalTime.now()
        return getSessionInfo(now.hour, now.minute)
    }

    /**
     * Guard function to sanitize and align widget text between title and content
     */
    fun sanitizeWidgetText(rawText: String, session: BriefingSession, currentTemp: Int = 28): String {
        // Automatically replace outdated temperature numbers in text with the actual current temperature
        val updatedTempText = rawText
            .replace("\\d+°C".toRegex(), "${currentTemp}°C")
            .replace("\\d+°".toRegex(), "${currentTemp}°")

        if (session == BriefingSession.MID_MORNING || session == BriefingSession.EARLY_MORNING) {
            if (updatedTempText.contains(
                    "Temperature dropping",
                    ignoreCase = true
                ) || updatedTempText.contains("Night is falling", ignoreCase = true)
            ) {
                return "Mild sunshine at $currentTemp°C. Stay focused on your key goals this morning."
            }
        }
        if (session == BriefingSession.NOON) {
            if (updatedTempText.contains("Get ready to rest", ignoreCase = true)) {
                return "Lunch break at $currentTemp°C. Recharge with a good meal and some light rest."
            }
        }
        return updatedTempText
    }
}
