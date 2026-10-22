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
            Log.e("LOCAL_AI", "Text generation error: ${e.message}")
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

        // 1. Read cache first unless manually refreshed (to save quota)
        if (!forceRefresh) {
            val cachedText = sharedPrefs.getString(cacheKey, null)
            if (!cachedText.isNullOrBlank()) {
                return@withContext cachedText
            }
        }

        // 2. Concise prompt for One UI 9 Briefing
        val prompt = """
            You are the One UI 9 Briefing virtual assistant. Create a very concise, warm, and inspiring daily recap sentence (maximum 20 words).
            Current context:
            - Session: $sessionName
            - Weather: $temp°C, $weatherDesc
            - Activity: Walked $steps steps.
            Return only a single summary sentence, without greetings, quotation marks, or markdown formatting.
        """.trimIndent()

        // 3. Call Gemini API via Firebase AI and cache the result
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
            Log.e("LOCAL_AI", "Text generation error: ${e.message}")
        }

        // 4. Smooth fallback when offline, on network error, or out of quota
        return@withContext BriefingContentProvider.getFallbackRecap(sessionName, temp, weatherDesc)
    }
}
