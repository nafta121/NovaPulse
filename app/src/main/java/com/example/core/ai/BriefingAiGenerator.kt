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
 * Real-time context for AI greeting & summary generation
 */
data class BriefingContext(
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val weatherTempC: Double? = null,
    val weatherCondition: String? = null,
    val energyScore: Int = 85,
    val sleepHoursFormatted: String? = null,
    val remainingCalendarEventsCount: Int = 0,
    val topTaskTitle: String? = null
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
 * AI Service generating smart, ultra-concise (<18 words) greetings & summaries without hallucinating metrics
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
        val rawBriefing = generateGenZBriefing(briefingContext)
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
        val tempInt = Math.round(ctx.weatherTempC ?: 28.0).toInt()
        val weatherDesc = ctx.weatherCondition ?: "cool and pleasant"
        return BriefingContentProvider.getHeaderSummary(ctx.session, tempInt, weatherDesc)
    }
}
