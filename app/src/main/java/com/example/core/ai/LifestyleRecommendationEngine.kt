package com.example.core.ai

import android.util.Log
import com.example.feature.widget.BriefingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

/**
 * Engine responsible for generating dynamic data for the "Recommendations You Might Like" carousel
 * using the On-Device AI model (Local LLM Inference).
 */
object LifestyleRecommendationEngine {

    private const val TAG = "LifestyleEngine"
    private val lifestyleCache = ConcurrentHashMap<String, List<LifestyleTip>>()

    fun clearCache() {
        lifestyleCache.clear()
    }

    /**
     * Directly invokes the real on-device AI inference engine (On-Device LLM Inference)
     */
    suspend fun generateLifestyleRecommendations(
        temp: Int,
        weather: String,
        session: String,
        runner: OnDeviceModelRunner? = null
    ): List<LifestyleTip> = withContext(Dispatchers.IO) {
        val cacheKey = "lifestyle_${session}_${temp}_${weather}"

        // Check Memory Cache
        lifestyleCache[cacheKey]?.let { cachedList ->
            Log.d(TAG, "Cache Hit for Lifestyle Tips key $cacheKey")
            return@withContext cachedList
        }

        try {
            val modelRunner = runner ?: throw IllegalStateException("Model runner is not initialized")

            val prompt = """
                <|im_start|>system
                You are on-device Galaxy AI. Return strictly 1 JSON array containing 2 contextual lifestyle recommendations for: temperature ${temp}°C, weather ${weather}, session ${session}.
                Do not write markdown, do not explain.
                Format:
                [
                  {"category": "Food & Dining", "tag": "Delicious", "subTag": "Treats", "title": "...", "desc": "...", "emoji": "🍜"},
                  {"category": "Activity", "tag": "Health", "subTag": "Relaxation", "title": "...", "desc": "...", "emoji": "🌿"}
                ]
                <|im_end|>
                <|im_start|>user
                Recommend now<|im_end|>
                <|im_start|>assistant
            """.trimIndent()

            // CALL REAL LOCAL MODEL RUNNER
            val rawOutput = modelRunner.generateResponse(prompt)

            // Parse output tokens from model
            val jsonString = rawOutput.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
            val tips = parseTipsFromJson(jsonString)

            if (tips.isNotEmpty()) {
                lifestyleCache[cacheKey] = tips
            }
            val resolvedSession = BriefingSession.values().firstOrNull { it.sessionTag == session } ?: BriefingSession.NOON
            return@withContext if (tips.isNotEmpty()) tips else BriefingContentProvider.getLifestyleTips(resolvedSession, temp, weather, 6)
        } catch (e: Exception) {
            Log.e("LOCAL_AI", "Text generation error: ${e.message}")
            val resolvedSession = BriefingSession.values().firstOrNull { it.sessionTag == session } ?: BriefingSession.NOON
            return@withContext BriefingContentProvider.getLifestyleTips(resolvedSession, temp, weather, 6)
        }
    }

    suspend fun generateRecommendations(
        modelMode: AiModelMode,
        userContext: OnDeviceUserContext,
        runner: OnDeviceModelRunner? = null
    ): List<LifestyleTip> = withContext(Dispatchers.Default) {
        if (modelMode == AiModelMode.BASIC_0MB) {
            return@withContext getFallbackTips(userContext)
        }

        val tips = generateLifestyleRecommendations(
            temp = Math.round(userContext.tempC).toInt(),
            weather = userContext.weatherCondition,
            session = userContext.session.sessionTag,
            runner = runner
        )

        if (tips.isNotEmpty()) tips else getFallbackTips(userContext)
    }

    private fun parseTipsFromJson(jsonStr: String): List<LifestyleTip> {
        if (jsonStr.isBlank()) return emptyList()

        val list = mutableListOf<LifestyleTip>()
        try {
            val start = jsonStr.indexOf('[')
            val end = jsonStr.lastIndexOf(']')
            if (start == -1 || end == -1 || start >= end) return emptyList()

            val cleanJson = jsonStr.substring(start, end + 1)
            val jsonArray = JSONArray(cleanJson)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val category = obj.optString("category", "Lifestyle")
                val tag = if (obj.has("subTag") && obj.optString("subTag").isNotBlank()) {
                    obj.optString("subTag")
                } else {
                    obj.optString("tag", "Suggestion")
                }
                val title = obj.optString("title", "Lifestyle Suggestion")
                val desc = if (obj.has("desc") && obj.optString("desc").isNotBlank()) {
                    obj.optString("desc")
                } else {
                    obj.optString("description", "Take time to care for your health every day.")
                }
                val emoji = obj.optString("emoji", "✨")

                list.add(
                    LifestyleTip(
                        category = category,
                        tag = tag,
                        title = title,
                        description = desc,
                        emoji = emoji
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("LOCAL_AI", "Error parsing JSON tips: ${e.message}")
        }
        return list
    }

    private fun getFallbackTips(ctx: OnDeviceUserContext): List<LifestyleTip> {
        return PresetLifestyleData.getTipsForSession(
            session = ctx.session,
            weatherCondition = ctx.weatherCondition,
            temp = ctx.tempC,
            steps = ctx.stepsCount.toLong(),
            isWeekend = false
        )
    }
}
