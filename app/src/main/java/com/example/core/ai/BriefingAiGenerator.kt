package com.example.core.ai

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.example.feature.widget.BriefingSession
import com.example.feature.widget.BriefingSessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 5 standard time phases of the day (Morning / Noon / Afternoon / Evening / Night)
 */
enum class DiurnalPhaseType(val label: String) {
    MORNING("Morning"),
    NOON("Noon"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

/**
 * Real-time Samsung Personal Data Engine (PDE) context for AI greeting & summary generation.
 * Pre-correlates Calendar, Health/Sleep/Energy Score, Weather, Screen Time, and Tasks before prompting.
 */
data class BriefingContext(
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val weatherTempC: Double? = null,
    val weatherCondition: String? = null,
    val energyScore: Int = 85,
    val sleepHoursFormatted: String? = null,
    val sleepHoursLastNight: Double? = null,
    val remainingCalendarEventsCount: Int = 0,
    val upcomingEventsSummary: String? = null,
    val topTaskTitle: String? = null,
    val screenTimeFormatted: String? = null,
    val screenTimeMinutesToday: Int? = null,
    val stepsCount: Int = 0,
    val stepsToday: Int? = null
) {
    val session: BriefingSession
        get() = BriefingSession.fromHourAndMinute(hourOfDay, 0)

    val phase: DiurnalPhaseType
        get() = when (session) {
            BriefingSession.EARLY_MORNING, BriefingSession.MID_MORNING -> DiurnalPhaseType.MORNING
            BriefingSession.NOON -> DiurnalPhaseType.NOON
            BriefingSession.AFTERNOON -> DiurnalPhaseType.AFTERNOON
            BriefingSession.EVENING -> DiurnalPhaseType.EVENING
            BriefingSession.NIGHT, BriefingSession.LATE_NIGHT -> DiurnalPhaseType.NIGHT
        }

    /**
     * Deterministic Samsung PDE Cross-Signal Correlation Engine:
     * Synthesizes top cross-domain insights (Sleep/Energy vs Schedule, Weather vs Activity, Screen Time vs Wind-down)
     * inside paired XML delimiters (`<personal_data_signals>`) for ML Kit GenAI Prompt API.
     */
    fun toPdeXmlSignals(): String {
        val tempInt = Math.round(weatherTempC ?: 28.0).toInt()
        val cond = weatherCondition ?: "pleasant"
        val effectiveSteps = stepsToday ?: stepsCount
        val effectiveSleepStr = sleepHoursFormatted?.takeIf { it.isNotBlank() }
            ?: sleepHoursLastNight?.let { String.format(java.util.Locale.US, "%.1f", it) }
            ?: "7.2"
        val effectiveScreenMins = screenTimeMinutesToday ?: 0
        val screenInfo = screenTimeFormatted?.takeIf { it.isNotBlank() }
            ?: if (effectiveScreenMins > 0) "${effectiveScreenMins}m" else "moderate"
        val correlation = buildString {
            when (phase) {
                DiurnalPhaseType.MORNING -> {
                    append("Recovery energy $energyScore% ($effectiveSleepStr sleep) with $remainingCalendarEventsCount calendar events today")
                    if (!topTaskTitle.isNullOrBlank()) append("; priority task: $topTaskTitle")
                }
                DiurnalPhaseType.NOON -> {
                    append("Midday recharge at ${tempInt}°C ($cond) after $effectiveSteps steps; $remainingCalendarEventsCount events remaining")
                }
                DiurnalPhaseType.AFTERNOON -> {
                    append("Afternoon focus window with $remainingCalendarEventsCount events and ${topTaskTitle ?: "steady momentum"}")
                }
                DiurnalPhaseType.EVENING, DiurnalPhaseType.NIGHT -> {
                    append("Evening wind-down after $screenInfo screen time and $effectiveSteps steps; energy at $energyScore%")
                }
            }
        }
        return """
            <personal_data_signals>
            <session>${session.name}</session>
            <weather>${tempInt}°C, $cond</weather>
            <energy_score>$energyScore</energy_score>
            <calendar remaining_events="$remainingCalendarEventsCount">${upcomingEventsSummary ?: "None"}</calendar>
            <top_task>${topTaskTitle ?: "None"}</top_task>
            <activity steps="$effectiveSteps" screen_time_min="$effectiveScreenMins" sleep_hours="$effectiveSleepStr" />
            <correlation>$correlation</correlation>
            </personal_data_signals>
        """.trimIndent()
    }
}

/**
 * Output result from AI Service
 */
data class BriefingAiResult(
    val greetingHeadline: String,
    val dynamicBriefing: String,
    val timePhase: DiurnalPhaseType,
    val energyScore: Int
)

/**
 * AI Service generating smart, ultra-concise (<18 words) greetings & summaries without hallucinating metrics.
 * Uses ML Kit GenAI Prompt API (Gemini Nano with Prefix Caching) when available, with deterministic PDE fallback.
 */
class BriefingAiGenerator(
    private val context: Context,
    private val llmManager: LocalLlmManager? = null
) {
    companion object {
        private const val TAG = "BriefingAiGenerator"

        /**
         * Strict System Prompt sent to Local AI / LLM
         */
        const val SYSTEM_PROMPT_TEMPLATE =
            "Generate a natural, ultra-concise briefing summary sentence (max 18 words) in English. STRICTLY DO NOT invent percentages, battery levels, or metrics not provided in the context. Morning sessions (5:00-11:29) MUST strictly focus on morning and work productivity, NEVER generating the words 'afternoon' or 'evening'. Noon sessions (11:30-13:59) MUST use 'Lunch break' or 'Midday', NEVER using 'Afternoon'."

        /**
         * Read actual battery percentage from the Android system via BatteryManager
         */
        fun getRealSystemBatteryPercentage(context: Context): Int {
            return try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            } catch (e: Exception) {
                -1
            }
        }
    }

    suspend fun generate(briefingContext: BriefingContext): BriefingAiResult = withContext(Dispatchers.Default) {
        val session = briefingContext.session
        val phase = briefingContext.phase
        val headline = session.title
        val tempInt = Math.round(briefingContext.weatherTempC ?: 28.0).toInt()

        // 1. Try ML Kit GenAI Prompt API (Gemini Nano on-device with Prefix Caching)
        val nanoSummary = llmManager?.generateWithPromptApi(
            staticPrefix = LocalLlmManager.PDE_BRIEFING_PROMPT_PREFIX,
            dynamicXmlSuffix = briefingContext.toPdeXmlSignals(),
            maxTokens = 80
        )

        val rawBriefing = if (!nanoSummary.isNullOrBlank()) {
            nanoSummary.replace("\"", "").replace("*", "").lineSequence().first().trim()
        } else {
            generateGenZBriefing(briefingContext)
        }
        val dynamicBriefing = BriefingSessionHelper.sanitizeWidgetText(rawBriefing, session, tempInt)

        Log.d(TAG, "Generated briefing for session ${session.name} (${headline}): '$dynamicBriefing'")

        BriefingAiResult(
            greetingHeadline = headline,
            dynamicBriefing = dynamicBriefing,
            timePhase = phase,
            energyScore = briefingContext.energyScore
        )
    }

    private fun generateGenZBriefing(ctx: BriefingContext): String {
        val parts = mutableListOf<String>()
        val tempInt = ctx.weatherTempC?.let { Math.round(it).toInt() }
        val weatherDesc = ctx.weatherCondition?.takeIf { it.isNotBlank() }
        if (tempInt != null) {
            parts.add(if (weatherDesc != null) "${tempInt}°C ($weatherDesc)" else "${tempInt}°C")
        }
        when {
            !ctx.upcomingEventsSummary.isNullOrBlank() -> parts.add("Jadwal: ${ctx.upcomingEventsSummary}")
            !ctx.topTaskTitle.isNullOrBlank() -> parts.add("Tugas: ${ctx.topTaskTitle}")
            ctx.remainingCalendarEventsCount > 0 -> parts.add("${ctx.remainingCalendarEventsCount} agenda hari ini")
        }
        if ((ctx.stepsToday ?: 0) > 0) {
            parts.add("${ctx.stepsToday} langkah")
        }
        val screenMins = ctx.screenTimeMinutesToday ?: 0
        if (screenMins > 0) {
            val h = screenMins / 60
            val m = screenMins % 60
            parts.add(if (h > 0) "Layar ${h}j ${m}m" else "Layar ${m}m")
        }
        val batteryPct = getRealSystemBatteryPercentage(context)
        if (batteryPct in 1..100) {
            parts.add("Baterai $batteryPct%")
        }
        return if (parts.isNotEmpty()) {
            parts.joinToString(" • ")
        } else {
            BriefingContentProvider.getHeaderSummary(ctx.session, tempInt ?: 28, weatherDesc ?: "cerah")
        }
    }
}
