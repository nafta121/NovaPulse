package com.example.core.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

data class UserContext(
    val timeOfDay: String,
    val weatherCondition: String,
    val temperature: Double,
    val steps: Long,
    val isWeekend: Boolean = false,
    val dayOfWeek: String = "Friday"
)

class LifestyleAiGenerator(
    private val context: Context,
    private val llmManager: LocalLlmManager? = null
) {
    companion object {
        private const val TAG = "LifestyleAiGenerator"
    }

    suspend fun generateLifestyleTips(contextData: UserContext): List<LifestyleTip> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Try AI Generation with 4-second timeout limit
        val result = withTimeoutOrNull(4000L) {
            try {
                val prompt = buildPrompt(contextData)
                val rawOutput = llmManager?.executeInference(prompt, prompt) ?: ""
                parseJsonArray(rawOutput)
            } catch (e: Exception) {
                Log.w(TAG, "AI Generation failed or threw exception: ${e.message}")
                null
            }
        }

        // 2. Smart Fallback Strategy if AI generation failed, timed out, or returned empty JSON
        if (result.isNullOrEmpty()) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Using Preset Fallback Lifestyle Data (elapsed: ${elapsed}ms)")
            PresetLifestyleData.getTipsForSession(
                timeOfDay = contextData.timeOfDay,
                weatherCondition = contextData.weatherCondition,
                temp = contextData.temperature,
                steps = contextData.steps,
                isWeekend = contextData.isWeekend
            )
        } else {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Successfully generated ${result.size} AI Lifestyle Tips (elapsed: ${elapsed}ms)")
            result
        }
    }

    private fun buildPrompt(contextData: UserContext): String {
        val weekendContext = if (contextData.isWeekend) "Today is the Weekend (${contextData.dayOfWeek}), great for outdoor trips, short getaways, picnics, or coffee." else "Today is a Workday (${contextData.dayOfWeek}), stay focused and maintain your energy."
        val weatherType = when {
            contextData.temperature >= 32.0 -> "Hot weather (${contextData.temperature.toInt()}°C), stay cool, hydrate, and remain in shaded or ventilated areas."
            contextData.temperature <= 22.0 -> "Cool weather (${contextData.temperature.toInt()}°C), enjoy warm food or drinks and keep your body warm."
            else -> "Ideal weather (${contextData.temperature.toInt()}°C, ${contextData.weatherCondition}), great for outdoor activities."
        }

        return """
            Based on the following context:
            - Time: ${contextData.timeOfDay}, ${contextData.dayOfWeek} (${if (contextData.isWeekend) "Weekend" else "Weekday"}).
            - Weather: ${contextData.weatherCondition}, ${contextData.temperature}°C ($weatherType).
            - Context: $weekendContext
            - Activity: ${contextData.steps} steps today.

            Recommend 3 to 4 rich lifestyle/mindfulness suggestion cards across these categories:
            1. "Fitness & Travel" (Tag: "Outdoors", "Weekend", "Explore"): Based on weather and day off.
            2. "Food & Dining" (Tag: "Treats", "Nutrition", "Cooling"): Based on time of day and temperature.
            3. "Relaxation & Recovery" (Tag: "Recovery", "Mindfulness"): Based on step count and circadian rhythm.
            4. "Health Tips" (Tag: "Wellness", "Care"): Reminders to hydrate, apply sunscreen, or stretch.

            Return ONLY a JSON array of objects with the following fields:
            [
              {
                "category": "Food & Dining",
                "tag": "Treats",
                "title": "Morning Coffee & Warm Noodle Soup",
                "description": "Start your new day with a fragrant bowl of noodle soup and a rich cup of coffee.",
                "emoji": "🍜"
              }
            ]
        """.trimIndent()
    }

    private fun parseJsonArray(jsonString: String): List<LifestyleTip> {
        val list = mutableListOf<LifestyleTip>()
        try {
            val cleanJson = extractJsonArrayString(jsonString)
            if (cleanJson.isBlank()) return emptyList()

            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val category = obj.optString("category", "Lifestyle")
                val tag = obj.optString("tag", "Suggestion")
                val title = obj.optString("title", "Healthy Lifestyle")
                val description = obj.optString("description", "Take time to care for yourself every day.")
                val emoji = obj.optString("emoji", "💡")

                list.add(
                    LifestyleTip(
                        category = category,
                        tag = tag,
                        title = title,
                        description = description,
                        emoji = emoji
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON Array from AI output: ${e.message}")
        }
        return list
    }

    private fun extractJsonArrayString(input: String): String {
        val start = input.indexOf('[')
        val end = input.lastIndexOf(']')
        return if (start != -1 && end != -1 && start < end) {
            input.substring(start, end + 1)
        } else {
            ""
        }
    }
}
