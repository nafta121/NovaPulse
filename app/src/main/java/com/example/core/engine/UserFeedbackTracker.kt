package com.example.core.engine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Locale

class UserFeedbackTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_context_feedback", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "UserFeedbackTracker"
        private const val DEFAULT_WEIGHT = 1.0f
        private const val REWARD_DELTA = 0.25f
        private const val PENALTY_DELTA = 0.2f
        private const val MIN_WEIGHT = 0.15f
        private const val MAX_WEIGHT = 2.5f
    }

    fun getCategoryWeight(category: ContextCardCategory): Float {
        return prefs.getFloat("weight_${category.name}", DEFAULT_WEIGHT)
    }

    fun recordPositiveFeedback(category: ContextCardCategory) {
        val current = getCategoryWeight(category)
        val updated = (current + REWARD_DELTA).coerceAtMost(MAX_WEIGHT)
        prefs.edit().putFloat("weight_${category.name}", updated).apply()
        Log.d(TAG, "Positive feedback recorded for $category: $current -> $updated")
    }

    fun recordNegativeFeedback(category: ContextCardCategory) {
        val current = getCategoryWeight(category)
        val updated = (current - PENALTY_DELTA).coerceAtLeast(MIN_WEIGHT)
        prefs.edit().putFloat("weight_${category.name}", updated).apply()
        Log.d(TAG, "Negative feedback recorded for $category: $current -> $updated")
    }

    fun getSuggestionWeight(categoryName: String): Float {
        val key = "sugg_weight_${categoryName.trim().lowercase(Locale.ROOT)}"
        return prefs.getFloat(key, DEFAULT_WEIGHT)
    }

    fun recordSuggestionPositive(categoryName: String) {
        val current = getSuggestionWeight(categoryName)
        val updated = (current + REWARD_DELTA).coerceAtMost(MAX_WEIGHT)
        val key = "sugg_weight_${categoryName.trim().lowercase(Locale.ROOT)}"
        prefs.edit().putFloat(key, updated).apply()
        Log.d(TAG, "Suggestion positive feedback for $categoryName: $current -> $updated")
    }

    fun recordSuggestionDismiss(categoryName: String) {
        val current = getSuggestionWeight(categoryName)
        val updated = (current - PENALTY_DELTA).coerceAtLeast(MIN_WEIGHT)
        val key = "sugg_weight_${categoryName.trim().lowercase(Locale.ROOT)}"
        prefs.edit().putFloat(key, updated).apply()
        Log.d(TAG, "Suggestion dismiss feedback for $categoryName: $current -> $updated")
    }

    fun resetWeights() {
        prefs.edit().clear().apply()
    }
}
