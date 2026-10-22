package com.example.core.ai

import android.content.Context
import com.example.NovaPulseApp
import com.example.feature.widget.BriefingSession

enum class AiModelMode(val displayName: String, val sizeMb: Int) {
    BASIC_0MB("Self-Learning Model (Native On-Device ML)", 0),
    LIGHT_120MB("Gemma 2B Light (Offline)", 120),
    STANDARD_350MB("Qwen 1.5B (Offline)", 350),
    ADVANCED_500MB("DeepSeek R1 Distill (Offline)", 500)
}

data class OnDeviceUserContext(
    val session: BriefingSession,
    val tempC: Double,
    val weatherCondition: String,
    val stepsCount: Int,
    val sleepDurationFormatted: String = "7h 15m"
)

class OnDeviceModelRunner(private val context: Context) {

    private val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
    private val llmManager: LocalLlmManager?
        get() = (context.applicationContext as? NovaPulseApp)?.llmManager

    suspend fun runInference(ctx: OnDeviceUserContext): String {
        val routineInsight = mlEngine.getRoutineInsight()
        val summaryContext = com.example.core.ml.OnDeviceSummaryContext(
            session = ctx.session,
            tempC = ctx.tempC,
            weatherCondition = ctx.weatherCondition,
            routineInsight = routineInsight
        )
        return mlEngine.generatePersonalizedSummary(summaryContext)
    }

    suspend fun generateResponse(prompt: String): String {
        val manager = llmManager
        if (manager != null) {
            return try {
                manager.executeInference(prompt, prompt)
            } catch (_: Exception) {
                ""
            }
        }
        return runInference(
            OnDeviceUserContext(
                session = BriefingSession.getCurrentSession(),
                tempC = 28.0,
                weatherCondition = "Cool and pleasant",
                stepsCount = 0
            )
        )
    }

    fun generateStandardAiSummary(ctx: OnDeviceUserContext): String {
        return mlEngine.generatePersonalizedSummary(
            com.example.core.ml.OnDeviceSummaryContext(
                session = ctx.session,
                tempC = ctx.tempC,
                weatherCondition = ctx.weatherCondition
            )
        )
    }

    fun generateLightweightAiSummary(ctx: OnDeviceUserContext): String {
        return generateStandardAiSummary(ctx)
    }
}
