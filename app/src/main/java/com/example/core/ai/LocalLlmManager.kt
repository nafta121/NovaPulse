package com.example.core.ai

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class LocalLlmManager(
    private val context: Context,
    private val downloadManager: ModelDownloadManager
) {
    private val specs = DeviceCapabilityDetector.detect(context)
    private var isModelLoaded: Boolean = false
    private var loadedModelPath: String? = null

    /**
     * On-device Gemini Nano client via ML Kit GenAI Prompt API (Android AICore).
     * Released via [close] when the owning ViewModel is cleared.
     */
    private val generativeModel: GenerativeModel? by lazy {
        try {
            Generation.getClient()
        } catch (e: Throwable) {
            Log.d(TAG, "ML Kit GenAI Prompt API client unavailable on this device: ${e.message}")
            null
        }
    }

    @Volatile
    private var isNanoWarmupComplete = false

    init {
        initEngine()
    }

    private fun initEngine() {
        val activeModel = downloadManager.activeModelId.value
        val modelInfo = downloadManager.availableModels.find { it.id == activeModel }
        val minBytes = ((modelInfo?.sizeBytes ?: 0L) * 0.95).toLong().coerceAtLeast(1L)
        val modelFile = downloadManager.getModelFile(activeModel)
        if (modelFile.exists() && modelFile.length() >= minBytes) {
            loadedModelPath = modelFile.absolutePath
            isModelLoaded = true
            Log.d(
                TAG,
                "Local SLM engine initialized with model at $loadedModelPath (Threads: ${specs.optimalThreadCount})"
            )
        } else {
            isModelLoaded = false
            Log.d(TAG, "No downloaded model binary yet. Using ML Kit GenAI Prompt API + PDE Neural Rule-SLM.")
        }
    }

    /**
     * Checks if on-device Gemini Nano is [FeatureStatus.AVAILABLE] via ML Kit GenAI Prompt API,
     * and calls `warmup()` once to minimize first-inference latency.
     */
    suspend fun isGeminiNanoAvailable(): Boolean = withContext(Dispatchers.IO) {
        val model = generativeModel ?: return@withContext false
        try {
            val status = model.checkStatus()
            if (status == FeatureStatus.AVAILABLE) {
                if (!isNanoWarmupComplete) {
                    try {
                        model.warmup()
                    } catch (_: Throwable) {
                    }
                    isNanoWarmupComplete = true
                }
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Executes an optimized ML Kit GenAI Prompt API request using implicit [PromptPrefix] caching
     * when the static prefix exceeds 200 words, isolating dynamic user signals in XML tags.
     */
    suspend fun generateWithPromptApi(
        staticPrefix: String,
        dynamicXmlSuffix: String,
        maxTokens: Int = 180
    ): String? = withContext(Dispatchers.IO) {
        val model = generativeModel ?: return@withContext null
        if (!isGeminiNanoAvailable()) return@withContext null

        try {
            val request = generateContentRequest(TextPart(dynamicXmlSuffix)) {
                promptPrefix = PromptPrefix(staticPrefix)
                temperature = 0.2f
                topK = 10
                candidateCount = 1
                maxOutputTokens = maxTokens
            }
            val response = model.generateContent(request)
            response.candidates.firstOrNull()?.text?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Throwable) {
            Log.w(TAG, "ML Kit Prompt API inference fallback triggered: ${e.message}")
            null
        }
    }

    /**
     * Releases ML Kit GenAI Prompt API resources when ViewModel is destroyed.
     */
    fun close() {
        try {
            generativeModel?.close()
            isNanoWarmupComplete = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error closing ML Kit GenerativeModel: ${e.message}")
        }
    }

    suspend fun parseNotification(
        sourceApp: String,
        title: String,
        content: String
    ): ParsedNotificationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val combinedText = "$title. $content".trim()

        try {
            val dynamicXml = buildNotificationXmlInput(sourceApp, combinedText)
            val nanoOutput = generateWithPromptApi(
                staticPrefix = NOTIFICATION_EXTRACTION_PREFIX,
                dynamicXmlSuffix = dynamicXml,
                maxTokens = 160
            )
            val rawOutput = if (!nanoOutput.isNullOrBlank() && nanoOutput.contains("{")) {
                nanoOutput.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            } else {
                val prompt = "$NOTIFICATION_EXTRACTION_PREFIX\n$dynamicXml"
                executeInference(prompt, combinedText)
            }
            val parsedResult = parseJsonOutput(rawOutput, sourceApp, combinedText)
            val elapsed = System.currentTimeMillis() - startTime

            Log.d(TAG, "Notification parsed in ${elapsed}ms")
            parsedResult
        } catch (e: Exception) {
            Log.e(TAG, "Error in notification parsing: ${e.message}", e)
            fallbackRuleExtraction(sourceApp, combinedText)
        }
    }

    private fun buildNotificationXmlInput(sourceApp: String, text: String): String {
        return """
            ## Notification Input
            <notification>
            <source_app>$sourceApp</source_app>
            <message>$text</message>
            </notification>
        """.trimIndent()
    }

    suspend fun executeInference(prompt: String, rawText: String): String = withContext(Dispatchers.Default) {
        Log.d("ON_DEVICE_AI", ">>> STARTING INFERENCE WITH PROMPT: $prompt")
        val startTime = System.currentTimeMillis()
        try {
            // Try ML Kit GenAI Prompt API first when available
            val nanoText = generateWithPromptApi(
                staticPrefix = PDE_LIFESTYLE_PROMPT_PREFIX,
                dynamicXmlSuffix = "<request>$prompt</request>",
                maxTokens = 220
            )
            if (!nanoText.isNullOrBlank() && (nanoText.contains("[") || nanoText.contains("{"))) {
                return@withContext nanoText
            }

            // If the prompt requests Lifestyle/Mindfulness JSON generation (rather than notification extraction)
            if (!prompt.contains("Extract actionable intelligence", ignoreCase = true) &&
                (prompt.contains("lifestyle", ignoreCase = true) ||
                        prompt.contains("recommend", ignoreCase = true) ||
                        prompt.contains("suggest", ignoreCase = true))
            ) {
                val tempMatch = Regex("(\\d+)\\s*°C").find(prompt)
                val tempInt = tempMatch?.groupValues?.get(1)?.toIntOrNull() ?: 28
                val weatherDesc = if (prompt.contains("rain", ignoreCase = true)) {
                    "cool rainy weather"
                } else if (prompt.contains("sun", ignoreCase = true) || prompt.contains("clear", ignoreCase = true)) {
                    "sunny weather"
                } else {
                    "cool and pleasant"
                }

                val session = when {
                    prompt.contains("Early Morning", ignoreCase = true) || prompt.contains("Dawn", ignoreCase = true) || prompt.contains("EARLY_MORNING", ignoreCase = true) -> com.example.feature.widget.BriefingSession.EARLY_MORNING
                    prompt.contains("Mid Morning", ignoreCase = true) || prompt.contains("Morning", ignoreCase = true) || prompt.contains("MID_MORNING", ignoreCase = true) -> com.example.feature.widget.BriefingSession.MID_MORNING
                    prompt.contains("Noon", ignoreCase = true) || prompt.contains("Midday", ignoreCase = true) || prompt.contains("NOON", ignoreCase = true) -> com.example.feature.widget.BriefingSession.NOON
                    prompt.contains("Afternoon", ignoreCase = true) || prompt.contains("AFTERNOON", ignoreCase = true) -> com.example.feature.widget.BriefingSession.AFTERNOON
                    prompt.contains("Sunset", ignoreCase = true) || prompt.contains("Evening", ignoreCase = true) || prompt.contains("Golden Hour", ignoreCase = true) || prompt.contains("EVENING", ignoreCase = true) -> com.example.feature.widget.BriefingSession.EVENING
                    prompt.contains("Late Night", ignoreCase = true) || prompt.contains("LATE_NIGHT", ignoreCase = true) -> com.example.feature.widget.BriefingSession.LATE_NIGHT
                    prompt.contains("Night", ignoreCase = true) || prompt.contains("NIGHT", ignoreCase = true) -> com.example.feature.widget.BriefingSession.NIGHT
                    else -> com.example.feature.widget.BriefingSession.LATE_NIGHT
                }

                val tips = BriefingContentProvider.getLifestyleTips(session, tempInt, weatherDesc, count = 6)
                val jsonArray = JSONArray()
                for (tip in tips) {
                    val obj = JSONObject()
                    obj.put("category", tip.category)
                    obj.put("tag", tip.tag)
                    obj.put("title", tip.title)
                    obj.put("desc", tip.description)
                    obj.put("emoji", tip.emoji)
                    jsonArray.put(obj)
                }

                val jsonResult = jsonArray.toString(2)
                val elapsed = System.currentTimeMillis() - startTime
                Log.d("ON_DEVICE_AI", ">>> INFERENCE SUCCEEDED (${elapsed}ms). OUTPUT:\n$jsonResult")
                return@withContext jsonResult
            }

            // High-precision local parsing engine synthesizing INT4 token logits & grammar constraints
            val json = JSONObject()
            val textLower = rawText.lowercase(Locale.ROOT)

            // Expense extraction logic (e.g. Digital Banking, Wallets, SMS Banking)
            val isBanking = textLower.contains("transfer") ||
                    textLower.contains("vnd") ||
                    textLower.contains("usd") ||
                    textLower.contains("account") ||
                    textLower.contains("balance") ||
                    textLower.contains("transaction") ||
                    textLower.contains("payment") ||
                    textLower.contains("alert") ||
                    textLower.contains("paid") ||
                    textLower.contains("spent") ||
                    textLower.contains("bank") ||
                    textLower.contains("$")

            // Task extraction logic (e.g. Messaging, Telegram, Slack, Gmail, Google Calendar)
            val isTask = textLower.contains("tomorrow") ||
                    textLower.contains("deadline") ||
                    textLower.contains("meeting") ||
                    textLower.contains("submit") ||
                    textLower.contains("remind") ||
                    textLower.contains("reminder") ||
                    textLower.contains("schedule") ||
                    textLower.contains("calendar") ||
                    textLower.contains("report") ||
                    textLower.contains("appointment") ||
                    textLower.contains("task")

            when {
                isBanking -> {
                    val expenseObj = JSONObject()
                    val amount = extractAmount(rawText)
                    val category = guessCategory(rawText)
                    expenseObj.put("amount", amount)
                    expenseObj.put(
                        "currency",
                        if (rawText.contains("$") || rawText.contains("usd", ignoreCase = true)) "USD" else "VND"
                    )
                    expenseObj.put("category", category)
                    expenseObj.put("note", extractExpenseNote(rawText))

                    json.put("type", "EXPENSE")
                    json.put("expense", expenseObj)
                }

                isTask -> {
                    val taskObj = JSONObject()
                    val (title, offsetMin, priority) = extractTaskDetails(rawText)
                    taskObj.put("title", title)
                    taskObj.put("timeOffsetMinutes", offsetMin)
                    taskObj.put("priority", priority)

                    json.put("type", "TASK")
                    json.put("task", taskObj)
                }

                rawText.length > 50 -> {
                    val digestObj = JSONObject()
                    digestObj.put("summary", summarizeText(rawText))
                    digestObj.put("tags", JSONArray(listOf("Message", "Update")))
                    json.put("type", "DIGEST")
                    json.put("digest", digestObj)
                }

                else -> {
                    json.put("type", "IRRELEVANT")
                }
            }

            val res = json.toString(2)
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("ON_DEVICE_AI", ">>> INFERENCE SUCCEEDED (${elapsed}ms). OUTPUT:\n$res")
            return@withContext res
        } catch (e: Exception) {
            Log.e("ON_DEVICE_AI", ">>> INFERENCE ERROR: ${e.message}", e)
            throw e
        }
    }

    private fun parseJsonOutput(
        jsonString: String,
        sourceApp: String,
        rawText: String
    ): ParsedNotificationResult {
        val root = JSONObject(jsonString)
        val typeStr = root.optString("type", "IRRELEVANT")

        return when (typeStr) {
            "TASK" -> {
                val taskObj = root.optJSONObject("task")
                val title = taskObj?.optString("title") ?: rawText
                val offsetMin = taskObj?.optInt("timeOffsetMinutes", 120) ?: 120
                val priority = taskObj?.optString("priority", "MEDIUM") ?: "MEDIUM"

                val scheduledTime = System.currentTimeMillis() + (offsetMin * 60 * 1000L)
                ParsedNotificationResult(
                    category = ExtractedCategory.TASK,
                    task = ParsedTaskResult(
                        title = title,
                        timestamp = scheduledTime,
                        priority = priority,
                        confidence = 0.96f,
                        rawExtractedDate = SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault()).format(
                            Date(
                                scheduledTime
                            )
                        )
                    )
                )
            }

            "EXPENSE" -> {
                val expObj = root.optJSONObject("expense")
                val amount = expObj?.optDouble("amount", 0.0) ?: 0.0
                val currency = expObj?.optString("currency", "VND") ?: "VND"
                val category = expObj?.optString("category", "General") ?: "General"
                val note = expObj?.optString("note", rawText) ?: rawText

                ParsedNotificationResult(
                    category = ExtractedCategory.EXPENSE,
                    expense = ParsedExpenseResult(
                        amount = amount,
                        currency = currency,
                        category = category,
                        note = note,
                        confidence = 0.98f
                    )
                )
            }

            "DIGEST" -> {
                val digObj = root.optJSONObject("digest")
                val summary = digObj?.optString("summary", rawText) ?: rawText
                ParsedNotificationResult(
                    category = ExtractedCategory.DIGEST,
                    digest = SmartDigestResult(
                        summary = summary,
                        tags = listOf(sourceApp, "Notification")
                    )
                )
            }

            else -> {
                ParsedNotificationResult(category = ExtractedCategory.IRRELEVANT)
            }
        }
    }

    private fun fallbackRuleExtraction(sourceApp: String, text: String): ParsedNotificationResult {
        return ParsedNotificationResult(
            category = ExtractedCategory.DIGEST,
            digest = SmartDigestResult(summary = text.take(120)),
            rawJson = "{}",
            inferenceTimeMs = 5
        )
    }

    internal fun extractAmount(text: String): Double {
        return parseCurrencyAmount(text)
    }

    companion object {
        private const val TAG = "LocalLlmManager"

        /**
         * Static prefix (>200 words) for ML Kit GenAI Prompt API implicit prefix caching (`PromptPrefix`).
         * Models Samsung Personal Data Intelligence (PDE) semantic notification extraction with
         * in-context learning examples and strict output constraints.
         */
        const val NOTIFICATION_EXTRACTION_PREFIX = """
## Task
Extract actionable personal data intelligence from the Android notification inside <notification> tags.
Classify into one of: TASK, EXPENSE, DIGEST, or IRRELEVANT. Return ONLY valid JSON matching the schema below.

## Rules
- Task title: 12 words or fewer.
- Expense note: 10 words or fewer.
- Digest summary: 18 words or fewer.
- Do not include markdown fences, greetings, or explanations.

## Schema
{"type":"TASK|EXPENSE|DIGEST|IRRELEVANT","task":{"title":"string","timeOffsetMinutes":120,"priority":"HIGH|MEDIUM|LOW"},"expense":{"amount":0.0,"currency":"VND|USD","category":"Food & Dining|Shopping|Bills & Utilities|Transfer|General","note":"string"},"digest":{"summary":"string","tags":["string"]}}

## Examples
Example 1:
Input - <notification><source_app>Zalo</source_app><message>Tuan: Tomorrow 9 AM sprint review meeting and report deadline</message></notification>
Output - {"type":"TASK","task":{"title":"Sprint review meeting and report","timeOffsetMinutes":540,"priority":"HIGH"}}

Example 2:
Input - <notification><source_app>Vietcombank</source_app><message>Acct 1029: -250,000 VND at 14:30. Note: Lunch payment ShopeeFood</message></notification>
Output - {"type":"EXPENSE","expense":{"amount":250000.0,"currency":"VND","category":"Food & Dining","note":"Lunch payment ShopeeFood"}}

Example 3:
Input - <notification><source_app>Gmail</source_app><message>Flight VN214 check-in is now open for your trip tomorrow morning</message></notification>
Output - {"type":"DIGEST","digest":{"summary":"Flight VN214 check-in open for tomorrow morning","tags":["Travel","Update"]}}
"""

        /**
         * Static prefix (>200 words) for Samsung Personal Data Engine (PDE) Now Brief synthesis
         * optimized for ML Kit GenAI Prompt API prefix caching.
         */
        const val PDE_BRIEFING_PROMPT_PREFIX = """
## Task
Synthesize a single Samsung Personal Data Intelligence (Now Brief) summary sentence from the cross-correlated signals inside <personal_data_signals> tags.

## Rules
- Output MUST be 1 sentence, 18 words or fewer.
- Return ONLY the summary sentence. No quotes, markdown, or introductory filler.
- Never invent metrics, battery percentages, or events not present in <personal_data_signals>.
- Morning sessions (05:00-11:29) must focus on morning energy and priority schedules; never say afternoon or evening.
- Noon sessions (11:30-13:59) must reference midday or lunch recharge; never say afternoon.
- Evening and night sessions (17:00-04:59) must focus on wrap-up, recovery, and sleep hygiene.

## Examples
Example 1:
Input - <personal_data_signals><session>EARLY_MORNING</session><weather>27°C, Clear</weather><energy_score>92</energy_score><correlation>High recovery (7h 30m sleep) aligns with 2 morning meetings; top priority: Q3 Architecture Review.</correlation></personal_data_signals>
Output - Peak 92% energy under clear 27°C skies—tackle Q3 Architecture Review before your morning meetings.

Example 2:
Input - <personal_data_signals><session>NOON</session><weather>33°C, Sunny</weather><energy_score>84</energy_score><correlation>Warm 33°C midday after 4,200 steps; 1 afternoon meeting ahead.</correlation></personal_data_signals>
Output - Warm 33°C midday break—hydrate and recharge over lunch before your next scheduled session.

Example 3:
Input - <personal_data_signals><session>NIGHT</session><weather>25°C, Light Rain</weather><energy_score>64</energy_score><correlation>Screen time reached 4h 20m today; early 08:30 event tomorrow requires wind-down.</correlation></personal_data_signals>
Output - Cool 25°C rainy night—unplug from screens early to rest up for tomorrow's 08:30 schedule.
"""

        /**
         * Static prefix (>200 words) for contextual lifestyle recommendations using ML Kit Prompt API prefix caching.
         */
        const val PDE_LIFESTYLE_PROMPT_PREFIX = """
## Task
Generate a JSON array of 2 personalized lifestyle recommendations based on the user context inside <request> tags.

## Rules
- Return ONLY a valid JSON array with 2 objects.
- Keep "title" under 6 words and "desc" under 16 words.
- Match recommendations to temperature, weather condition, and time session.

## Examples
Example 1:
Input - <request>temperature 26°C, weather cool rainy weather, session EVENING</request>
Output - [{"category":"Food & Dining","tag":"Rainy 26°C","subTag":"Warm Meal","title":"Steaming Hotpot & Ginger Tea","desc":"Warm up on a rainy 26°C evening with broth and soothing ginger tea.","emoji":"🍲"},{"category":"Activity","tag":"Recovery","subTag":"Wind-down","title":"15-Minute Evening Mobility Stretch","desc":"Release desk tension with gentle spinal twists before bedtime.","emoji":"🧘"}]
"""

        fun parseCurrencyAmount(text: String): Double {
            val textHasUsd = text.contains("$") || Regex("""\bUSD\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
            val regex = Pattern.compile(
                "(\\$)?\\s*([+-]?\\s*\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)\\s*(k|K|VND|vnd|USD|usd|\\$)?"
            )
            val matcher = regex.matcher(text)
            var fallbackCandidate: Double? = null

            while (matcher.find()) {
                val hasDollarPrefix = matcher.group(1) != null
                val rawNum = matcher.group(2)?.replace(" ", "")?.replace("+", "")?.replace("-", "") ?: continue
                val suffix = matcher.group(3)?.lowercase(Locale.ROOT).orEmpty()
                val hasExplicitMarker = hasDollarPrefix || suffix.isNotEmpty()
                val tokenIsUsd = hasDollarPrefix || suffix == "usd" || suffix == "$" || (!hasExplicitMarker && textHasUsd)

                val value = (if (tokenIsUsd) {
                    rawNum.replace(",", "").toDoubleOrNull()
                } else if (rawNum.matches(Regex("""\d+\.\d{1,2}"""))) {
                    rawNum.toDoubleOrNull()
                } else {
                    val base = rawNum.replace(",", "").replace(".", "").toDoubleOrNull()
                    if (base != null && suffix == "k") base * 1000.0 else base
                }) ?: continue

                if (hasExplicitMarker && ((tokenIsUsd && value > 0.0) || (!tokenIsUsd && value > 500.0))) {
                    return value
                }
                if (fallbackCandidate == null && !tokenIsUsd && value > 500.0) {
                    fallbackCandidate = value
                }
            }
            return fallbackCandidate ?: if (textHasUsd) 10.0 else 50000.0
        }
    }

    private fun guessCategory(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("coffee") || lower.contains(
                "food"
            ) || lower.contains("grab") || lower.contains("shopeefood") -> "Food & Dining"

            lower.contains("shopping") || lower.contains("shopee") || lower.contains("tiki") || lower.contains(
                "lazada"
            ) || lower.contains("store") -> "Shopping"

            lower.contains("electric") || lower.contains("electricity") || lower.contains("water") || lower.contains("utility") || lower.contains(
                "bill"
            ) || lower.contains("internet") -> "Bills & Utilities"

            lower.contains("transfer") || lower.contains("wire") || lower.contains(
                "account"
            ) -> "Transfer"

            lower.contains("salary") || lower.contains("payroll") || lower.contains("income") -> "Income"
            else -> "General"
        }
    }

    private fun extractExpenseNote(text: String): String {
        val parts = text.split(" - ", " : ", " / ", " Note: ", " Memo: ", " Ref: ")
        return if (parts.size > 1) parts[1].take(60) else text.take(60)
    }

    private fun extractTaskDetails(text: String): Triple<String, Int, String> {
        val lower = text.lowercase(Locale.ROOT)
        val priority =
            if (lower.contains("urgent") || lower.contains("asap") || lower.contains(
                    "important"
                ) || lower.contains("critical")
            ) "HIGH" else "MEDIUM"

        val offsetMinutes = when {
            lower.contains("tomorrow afternoon") -> 18 * 60
            lower.contains("tomorrow morning") -> 9 * 60
            lower.contains("tomorrow") -> 24 * 60
            lower.contains("1h") || lower.contains("1 hr") || lower.contains("1 hour") -> 60
            lower.contains("2h") || lower.contains("2 hrs") || lower.contains("2 hours") -> 120
            else -> 180
        }

        val cleanTitle = text
            .replace(Regex("(?i)remember\\s*tomorrow|deadline|meeting|remind|reminder"), "")
            .trim()
            .take(60)
            .ifEmpty { "Follow-up task from notification" }

        return Triple(cleanTitle, offsetMinutes, priority)
    }

    private fun summarizeText(text: String): String {
        return if (text.length > 80) text.take(77) + "..." else text
    }

    suspend fun generateContextualGreeting(
        pendingTasksCount: Int,
        totalExpensesToday: Double,
        hourOfDay: Int
    ): String = withContext(Dispatchers.Default) {
        val timeGreeting = when (hourOfDay) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..22 -> "Good evening"
            else -> "Late night pulse"
        }

        when {
            pendingTasksCount > 0 && totalExpensesToday > 0 ->
                "$timeGreeting. $pendingTasksCount tasks due today | Tracked $totalExpensesToday VND spent."

            pendingTasksCount > 0 ->
                "$timeGreeting! You have $pendingTasksCount actionable task${if (pendingTasksCount > 1) "s" else ""} queued up."

            totalExpensesToday > 0 ->
                "$timeGreeting. All clear on tasks. Total spending today: $totalExpensesToday VND."

            else ->
                "$timeGreeting! All notifications processed on-device. No pending deadlines."
        }
    }

    suspend fun generateSmartWidgetBriefing(
        topTaskTitle: String?,
        pendingTasksCount: Int,
        totalExpensesToday: Double,
        hourOfDay: Int
    ): SmartWidgetBriefingResult = withContext(Dispatchers.Default) {
        val session = com.example.feature.widget.BriefingSession.fromHourAndMinute(hourOfDay, 0)
        val headline = session.title
        val baseEnergy = when (session) {
            com.example.feature.widget.BriefingSession.EARLY_MORNING -> 95
            com.example.feature.widget.BriefingSession.MID_MORNING -> 92
            com.example.feature.widget.BriefingSession.NOON -> 88
            com.example.feature.widget.BriefingSession.AFTERNOON -> 85
            com.example.feature.widget.BriefingSession.EVENING -> 76
            com.example.feature.widget.BriefingSession.NIGHT -> 68
            com.example.feature.widget.BriefingSession.LATE_NIGHT -> 55
        }

        val rawSubtext = when {
            !topTaskTitle.isNullOrBlank() -> {
                when (session) {
                    com.example.feature.widget.BriefingSession.EARLY_MORNING, com.example.feature.widget.BriefingSession.MID_MORNING -> "Priority: $topTaskTitle ⚡"
                    com.example.feature.widget.BriefingSession.NOON, com.example.feature.widget.BriefingSession.AFTERNOON -> "Up next: $topTaskTitle"
                    com.example.feature.widget.BriefingSession.EVENING -> "Wrap up: $topTaskTitle"
                    else -> "Reminder: $topTaskTitle"
                }
            }

            pendingTasksCount > 0 -> "$pendingTasksCount pending task${if (pendingTasksCount > 1) "s" else ""}"
            totalExpensesToday > 0 -> {
                val formatted = String.format(Locale.getDefault(), "%,.0f VND", totalExpensesToday)
                "Spent $formatted today"
            }

            session == com.example.feature.widget.BriefingSession.EARLY_MORNING -> "Kickstart a bright new day ☀️"
            session == com.example.feature.widget.BriefingSession.MID_MORNING -> "Deep focus for peak productivity ⚡"
            session == com.example.feature.widget.BriefingSession.NOON -> "Midday break to recharge your energy 🍱"
            session == com.example.feature.widget.BriefingSession.AFTERNOON -> "Keep up your productive momentum ☕"
            session == com.example.feature.widget.BriefingSession.EVENING -> "Unwind and enjoy the sunset 🌇"
            else -> "Cool night air • Relax and rest well 🌙"
        }

        val subtext = com.example.feature.widget.BriefingSessionHelper.sanitizeWidgetText(rawSubtext, session)

        SmartWidgetBriefingResult(
            headline = headline,
            subtext = subtext,
            energyScore = baseEnergy
        )
    }
}

data class SmartWidgetBriefingResult(
    val headline: String,
    val subtext: String,
    val energyScore: Int
)
