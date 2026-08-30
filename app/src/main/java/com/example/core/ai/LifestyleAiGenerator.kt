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
    val dayOfWeek: String = "Thứ Sáu"
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
        val weekendContext = if (contextData.isWeekend) "Hôm nay là Cuối tuần (${contextData.dayOfWeek}), thích hợp dã ngoại, du lịch ngắn ngày, picnic hoặc cà phê." else "Hôm nay là Ngày làm việc (${contextData.dayOfWeek}), cần giữ tập trung và duy trì năng lượng."
        val weatherType = when {
            contextData.temperature >= 32.0 -> "Trời oi nóng (${contextData.temperature.toInt()}°C), nên giải nhiệt, bù nước và ở nơi thoáng mát."
            contextData.temperature <= 22.0 -> "Trời lạnh (${contextData.temperature.toInt()}°C), nên dùng món nóng ấm và giữ ấm cơ thể."
            else -> "Thời tiết lý tưởng (${contextData.temperature.toInt()}°C, ${contextData.weatherCondition}), rất thích hợp cho vận động ngoài trời."
        }

        return """
            Dựa vào thông tin:
            - Thời gian: Buổi ${contextData.timeOfDay}, ${contextData.dayOfWeek} (${if (contextData.isWeekend) "Cuối tuần" else "Ngày thường"}).
            - Thời tiết: ${contextData.weatherCondition}, ${contextData.temperature}°C ($weatherType).
            - Ngữ cảnh: $weekendContext
            - Vận động: ${contextData.steps} bước chân hôm nay.

            Hãy sinh ra 3 đến 4 thẻ gợi ý lifestyle/mindfulness phong phú thuộc các nhóm:
            1. "Vận động & Du lịch" (Tag: "Ngoài trời", "Cuối tuần", "Khám phá"): Dựa vào thời tiết và ngày nghỉ.
            2. "Ẩm thực & Thưởng thức" (Tag: "Thưởng thức", "Dinh dưỡng", "Giải nhiệt"): Dựa vào buổi và nhiệt độ.
            3. "Thư giãn & Phục hồi" (Tag: "Phục hồi", "Mindfulness"): Dựa vào số bước và nhịp sinh học.
            4. "Mẹo sức khỏe" (Tag: "Sức khỏe", "Bảo vệ"): Nhắc uống nước, bôi kem chống nắng, giãn cơ.

            Trả về DUY NHẤT định dạng JSON mảng các object với các trường:
            [
              {
                "category": "Ẩm thực",
                "tag": "Thưởng thức",
                "title": "Cà phê sương sớm & Phở nóng",
                "description": "Bắt đầu ngày mới với phở bò thơm lừng kèm tách cà phê phin đậm đà.",
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
                val category = obj.optString("category", "Lối sống")
                val tag = obj.optString("tag", "Gợi ý")
                val title = obj.optString("title", "Lối sống lành mạnh")
                val description = obj.optString("description", "Dành thời gian chăm sóc bản thân mỗi ngày.")
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
