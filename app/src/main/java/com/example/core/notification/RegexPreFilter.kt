package com.example.core.notification

import com.example.core.ai.ExtractedCategory
import java.util.Locale
import java.util.regex.Pattern

data class FilterEvaluation(
    val isTriggered: Boolean,
    val matchedPattern: String,
    val predictedCategory: ExtractedCategory
)

object RegexPreFilter {

    private val TASK_PATTERN = Pattern.compile(
        "(?i)\\b(tomorrow|meeting|conference|deadline|submit|submission|remind|reminder|schedule|calendar|task|due|report|appointment|agenda|todo)\\b"
    )

    private val EXPENSE_PATTERN = Pattern.compile(
        "(?i)\\b(transfer|vnd|usd|\\bstk\\b|account|balance|transaction|payment|charged|debited|credited|paid|spent|momo|zalopay|vcb|tcb|mbbank|bidv|vietinbank|acb|tpbank|vpbank|sacombank|invoice|bill|receipt)\\b"
    )

    private val DIGEST_PATTERN = Pattern.compile(
        "(?i)\\b(notification|alert|notice|urgent|critical|important|priority|security alert|verification code|otp code|otp)\\b"
    )

    fun evaluate(title: String, content: String): FilterEvaluation {
        val combined = "$title $content"
        val lower = combined.lowercase(Locale.ROOT)

        // 1. Check Task Triggers
        val taskMatcher = TASK_PATTERN.matcher(lower)
        if (taskMatcher.find()) {
            return FilterEvaluation(
                isTriggered = true,
                matchedPattern = taskMatcher.group(),
                predictedCategory = ExtractedCategory.TASK
            )
        }

        // 2. Check Banking & Expense Triggers
        val expenseMatcher = EXPENSE_PATTERN.matcher(lower)
        if (expenseMatcher.find()) {
            return FilterEvaluation(
                isTriggered = true,
                matchedPattern = expenseMatcher.group(),
                predictedCategory = ExtractedCategory.EXPENSE
            )
        }

        // 3. Check General Digest
        val digestMatcher = DIGEST_PATTERN.matcher(lower)
        if (digestMatcher.find()) {
            return FilterEvaluation(
                isTriggered = true,
                matchedPattern = digestMatcher.group(),
                predictedCategory = ExtractedCategory.DIGEST
            )
        }

        return FilterEvaluation(
            isTriggered = false,
            matchedPattern = "",
            predictedCategory = ExtractedCategory.IRRELEVANT
        )
    }
}
