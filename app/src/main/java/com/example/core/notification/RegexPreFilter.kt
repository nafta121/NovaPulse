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
        "(?i)\\b(mai|họp|hop|deadline|nộp|nop|meeting|remind|nhắc|lịch|schedule|tomorrow|task|due|báo cáo|report|hạn chót)\\b"
    )

    private val EXPENSE_PATTERN = Pattern.compile(
        "(?i)\\b(chuyển khoản|chuyen khoan|vnd|vnđ|\\bstk\\b|số dư|so du|giao dịch|giao dich|thanh toán|thanh toan|biến động|bien dong|paid|spent|momo|zalopay|vcb|tcb|mbbank|bidv|vietinbank|acb|tpbank|vpbank|sacombank|invoice|hóa đơn|hoa don)\\b"
    )

    private val DIGEST_PATTERN = Pattern.compile(
        "(?i)\\b(thông báo|thong bao|urgent|khẩn|quan trọng|important|security alert|mã otp|otp code)\\b"
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
