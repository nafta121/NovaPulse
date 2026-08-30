package com.example.core.notification

import android.content.Context
import android.util.Log
import com.example.core.ai.ExtractedCategory
import com.example.core.ai.LocalLlmManager
import com.example.core.database.ExpenseEntity
import com.example.core.database.NotificationLogEntity
import com.example.core.database.NovaPulseRepository
import com.example.core.database.TaskEntity
import com.example.feature.widget.NovaPulseGlanceWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationParserAgent(
    private val context: Context,
    private val repository: NovaPulseRepository,
    private val llmManager: LocalLlmManager
) {
    companion object {
        private const val TAG = "NotificationParserAgent"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    fun processNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String
    ) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val filter = RegexPreFilter.evaluate(title, content)

            if (!filter.isTriggered) {
                // Zero-cost bypass: Log as skipped without waking up the local SLM
                repository.insertLog(
                    NotificationLogEntity(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        content = content,
                        matchedRegex = false,
                        regexTrigger = "",
                        extractedType = "SKIPPED",
                        summary = "Bypassed (Zero CPU Cost)",
                        processingTimeMs = System.currentTimeMillis() - startTime
                    )
                )
                return@launch
            }

            // Wake up on-device SLM with INT4 inference
            Log.d(TAG, "Regex woke SLM for: [$appName] matched '${filter.matchedPattern}'")
            val aiResult = llmManager.parseNotification(
                sourceApp = appName,
                title = title,
                content = content
            )

            when (aiResult.category) {
                ExtractedCategory.TASK -> {
                    aiResult.task?.let { task ->
                        repository.insertTask(
                            TaskEntity(
                                title = task.title,
                                datetime = task.timestamp,
                                priority = task.priority,
                                sourceApp = appName,
                                rawSnippet = content.take(150)
                            )
                        )
                    }
                }
                ExtractedCategory.EXPENSE -> {
                    aiResult.expense?.let { expense ->
                        repository.insertExpense(
                            ExpenseEntity(
                                amount = expense.amount,
                                currency = expense.currency,
                                category = expense.category,
                                note = expense.note,
                                sourceApp = appName,
                                rawSnippet = content.take(150)
                            )
                        )
                    }
                }
                else -> { /* Digest or Irrelevant */ }
            }

            // Record full audit log
            repository.insertLog(
                NotificationLogEntity(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    content = content,
                    matchedRegex = true,
                    regexTrigger = filter.matchedPattern,
                    extractedType = aiResult.category.name,
                    summary = when (aiResult.category) {
                        ExtractedCategory.TASK -> "Task created: ${aiResult.task?.title}"
                        ExtractedCategory.EXPENSE -> "Expense tracked: ${aiResult.expense?.amount} ${aiResult.expense?.currency}"
                        ExtractedCategory.DIGEST -> "Digest: ${aiResult.digest?.summary}"
                        ExtractedCategory.IRRELEVANT -> "Classified Irrelevant"
                    },
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
            )

            // Notify Glance Widget to update its living card
            NovaPulseGlanceWidgetReceiver.triggerUpdate(context)
        }
    }
}
