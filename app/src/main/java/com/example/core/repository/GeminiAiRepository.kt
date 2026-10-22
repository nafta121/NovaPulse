package com.example.core.repository

import android.content.Context
import android.util.Log
import com.example.NovaPulseApp
import com.example.core.ai.BriefingContentProvider
import com.example.core.ai.LocalLlmManager
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
        val stepBucket = (steps / 500) * 500
        val cacheKey = "recap_${today}_${sessionName}_${temp}_$stepBucket"

        // 1. Read quantized signal cache first unless manually refreshed (saves AICore & cloud quota)
        if (!forceRefresh) {
            val cachedText = sharedPrefs.getString(cacheKey, null)
            if (!cachedText.isNullOrBlank()) {
                return@withContext cachedText
            }
        }

        val dynamicXml = """
            <personal_data_signals>
            <session>$sessionName</session>
            <weather>${temp}°C, $weatherDesc</weather>
            <steps>$steps</steps>
            <correlation>Session $sessionName at ${temp}°C ($weatherDesc) with $steps steps walked today.</correlation>
            </personal_data_signals>
        """.trimIndent()

        // 2. Tier 1: On-Device Gemini Nano via ML Kit GenAI Prompt API (with Prefix Caching)
        try {
            val llmManager = (context.applicationContext as? NovaPulseApp)?.llmManager
            val onDeviceResult = llmManager?.generateWithPromptApi(
                staticPrefix = LocalLlmManager.PDE_BRIEFING_PROMPT_PREFIX,
                dynamicXmlSuffix = dynamicXml,
                maxTokens = 80
            )?.replace("\"", "")?.replace("*", "")?.lineSequence()?.firstOrNull()?.trim()

            if (!onDeviceResult.isNullOrBlank()) {
                sharedPrefs.edit().putString(cacheKey, onDeviceResult).apply()
                return@withContext onDeviceResult
            }
        } catch (e: Exception) {
            Log.d("LOCAL_AI", "ML Kit Prompt API unavailable, trying cloud/fallback: ${e.message}")
        }

        // 3. Tier 2: Concise XML-delimited prompt for Firebase AI Gemini Flash
        val prompt = "${LocalLlmManager.PDE_BRIEFING_PROMPT_PREFIX}\n$dynamicXml"
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

        // 4. Tier 3: Deterministic Samsung PDE fallback when offline or out of quota
        return@withContext BriefingContentProvider.getFallbackRecap(sessionName, temp, weatherDesc)
    }
}
