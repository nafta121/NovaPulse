package com.example.core.ai

enum class ExtractedCategory {
    TASK,
    EXPENSE,
    DIGEST,
    IRRELEVANT
}

data class ParsedTaskResult(
    val title: String,
    val timestamp: Long,
    val priority: String = "MEDIUM",
    val confidence: Float = 0.95f,
    val rawExtractedDate: String = ""
)

data class ParsedExpenseResult(
    val amount: Double,
    val currency: String = "VND",
    val category: String = "General",
    val note: String = "",
    val confidence: Float = 0.95f
)

data class SmartDigestResult(
    val summary: String,
    val tags: List<String> = emptyList(),
    val sentiment: String = "NEUTRAL"
)

data class ParsedNotificationResult(
    val category: ExtractedCategory,
    val task: ParsedTaskResult? = null,
    val expense: ParsedExpenseResult? = null,
    val digest: SmartDigestResult? = null,
    val rawJson: String = "",
    val inferenceTimeMs: Long = 0,
    val engineUsed: String = "Local SLM (INT4)"
)
