package com.example.core.ai

import android.util.Log
import com.example.feature.widget.BriefingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

/**
 * Engine chịu trách nhiệm sinh dữ liệu động cho cụm Carousel "Đề xuất có thể bạn sẽ thích"
 * sử dụng mô hình On-Device AI (Local LLM Inference)
 */
object LifestyleRecommendationEngine {

    private const val TAG = "LifestyleEngine"
    private val lifestyleCache = ConcurrentHashMap<String, List<LifestyleTip>>()

    fun clearCache() {
        lifestyleCache.clear()
    }

    /**
     * Hàm gọi trực tiếp Engine suy luận AI Local thật trên thiết bị (On-Device LLM Inference)
     */
    suspend fun generateLifestyleRecommendations(
        temp: Int,
        weather: String,
        session: String,
        runner: OnDeviceModelRunner? = null
    ): List<LifestyleTip> = withContext(Dispatchers.IO) {
        val cacheKey = "lifestyle_${session}_${temp}_${weather}"

        // Kiểm tra Memory Cache
        lifestyleCache[cacheKey]?.let { cachedList ->
            Log.d(TAG, "Cache Hit for Lifestyle Tips key $cacheKey")
            return@withContext cachedList
        }

        try {
            val modelRunner = runner ?: throw IllegalStateException("Model runner chưa được khởi tạo")

            val prompt = """
                <|im_start|>system
                Bạn là Galaxy AI on-device. Trả về đúng 1 mảng JSON gồm 2 gợi ý phong cách sống theo ngữ cảnh: nhiệt độ ${temp}°C, thời tiết ${weather}, buổi ${session}.
                Không viết markdown, không giải thích.
                Format:
                [
                  {"category": "Ẩm thực", "tag": "Món ngon", "subTag": "Hấp dẫn", "title": "...", "desc": "...", "emoji": "🍜"},
                  {"category": "Hoạt động", "tag": "Sức khỏe", "subTag": "Thư giãn", "title": "...", "desc": "...", "emoji": "🌿"}
                ]
                <|im_end|>
                <|im_start|>user
                Gợi ý ngay<|im_end|>
                <|im_start|>assistant
            """.trimIndent()

            // GỌI MODEL RUNNER LOCAL THẬT
            val rawOutput = modelRunner.generateResponse(prompt)

            // Parse kết quả trả về từ token của model
            val jsonString = rawOutput.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
            val tips = parseTipsFromJson(jsonString)

            if (tips.isNotEmpty()) {
                lifestyleCache[cacheKey] = tips
            }
            val resolvedSession = BriefingSession.values().firstOrNull { it.sessionTag == session } ?: BriefingSession.NOON
            return@withContext if (tips.isNotEmpty()) tips else BriefingContentProvider.getLifestyleTips(resolvedSession, temp, weather, 6)
        } catch (e: Exception) {
            Log.e("LOCAL_AI", "Lỗi sinh text: ${e.message}")
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
                val category = obj.optString("category", "Lối sống")
                val tag = if (obj.has("subTag") && obj.optString("subTag").isNotBlank()) {
                    obj.optString("subTag")
                } else {
                    obj.optString("tag", "Gợi ý")
                }
                val title = obj.optString("title", "Gợi ý lối sống")
                val desc = if (obj.has("desc") && obj.optString("desc").isNotBlank()) {
                    obj.optString("desc")
                } else {
                    obj.optString("description", "Dành thời gian chăm sóc sức khỏe mỗi ngày.")
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
            Log.e("LOCAL_AI", "Lỗi parse JSON tips: ${e.message}")
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
