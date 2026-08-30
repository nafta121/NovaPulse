package com.example.core.repository

import android.content.Context
import android.util.Log
import com.example.core.ai.BriefingContentProvider
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiAiRepository(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("briefing_ai_cache", Context.MODE_PRIVATE)

    private val generativeModel by lazy {
        try {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-1.5-flash")
        } catch (e: Exception) {
            Log.e("LOCAL_AI", "Lỗi sinh text: ${e.message}")
            null
        }
    }

    suspend fun getBriefingRecap(
        temp: Int,
        weatherDesc: String,
        steps: Int,
        sessionName: String,
        forceRefresh: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val cacheKey = "recap_${today}_$sessionName"

        // 1. Đọc Cache trước nếu không bấm Refresh thủ công (tiết kiệm quota)
        if (!forceRefresh) {
            val cachedText = sharedPrefs.getString(cacheKey, null)
            if (!cachedText.isNullOrBlank()) {
                return@withContext cachedText
            }
        }

        // 2. Prompt ngắn gọn cho One UI 9 Briefing
        val prompt = """
            Bạn là trợ lý ảo One UI 9 Briefing. Hãy tạo một câu tóm tắt ngày mới thật ngắn gọn, ấm áp và truyền cảm hứng (tối đa 20 từ).
            Ngữ cảnh hiện tại:
            - Phiên: $sessionName
            - Thời tiết: $temp°C, $weatherDesc
            - Vận động: Đã đi được $steps bước.
            Chỉ trả về đúng 1 câu text tóm tắt, không thêm lời chào, dấu ngoặc kép hay định dạng markdown rườm rà.
        """.trimIndent()

        // 3. Gọi Gemini API qua Firebase AI và lưu lại cache
        try {
            val model = generativeModel
            if (model != null) {
                val response = model.generateContent(prompt)
                val resultText = response.text?.trim()?.replace("\"", "")?.replace("*", "")
                if (!resultText.isNullOrBlank()) {
                    sharedPrefs.edit().putString(cacheKey, resultText).apply()
                    return@withContext resultText
                }
            }
        } catch (e: Exception) {
            Log.e("LOCAL_AI", "Lỗi sinh text: ${e.message}")
        }

        // 4. Fallback mượt mà nếu offline / lỗi mạng / hết quota
        return@withContext BriefingContentProvider.getFallbackRecap(sessionName, temp, weatherDesc)
    }
}
