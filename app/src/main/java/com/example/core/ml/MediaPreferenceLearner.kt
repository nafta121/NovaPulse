package com.example.core.ml

import android.content.Context
import android.util.Log
import kotlin.math.ln
import kotlin.math.sqrt

data class MediaPreferenceStats(
    val topCategory: String = "Lofi & Deep Work",
    val topCategoryPercentage: Int = 65,
    val preferredPlatform: String = "YouTube",
    val totalInteractions: Int = 0
)

class MediaPreferenceLearner(context: Context) {

    companion object {
        private const val TAG = "MediaPreferenceLearner"
        private const val PREFS_NAME = "media_preference_ml_stats"
        private const val EXPLORATION_FACTOR = 0.8
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordInteraction(categoryKey: String, platform: String, reward: Float = 1.0f) {
        val catCountKey = "cat_${categoryKey}_count"
        val catRewardKey = "cat_${categoryKey}_reward"
        val platCountKey = "plat_${platform}_count"
        val totalKey = "total_media_interactions"

        val catCount = prefs.getInt(catCountKey, 0) + 1
        val catReward = prefs.getFloat(catRewardKey, 0f) + reward
        val platCount = prefs.getInt(platCountKey, 0) + 1
        val total = prefs.getInt(totalKey, 0) + 1

        prefs.edit()
            .putInt(catCountKey, catCount)
            .putFloat(catRewardKey, catReward)
            .putInt(platCountKey, platCount)
            .putInt(totalKey, total)
            .apply()

        Log.d(TAG, "Recorded media interaction: category='$categoryKey' ($catCount), platform='$platform' ($platCount)")
    }

    fun calculateCategoryScore(categoryKey: String, defaultWeight: Float = 1.0f): Float {
        val count = prefs.getInt("cat_${categoryKey}_count", 0)
        val total = prefs.getInt("total_media_interactions", 0)

        if (count == 0 || total <= 1) {
            return defaultWeight
        }

        val rewardSum = prefs.getFloat("cat_${categoryKey}_reward", 0f)
        val avgReward = rewardSum / count.toFloat()
        val bonus = EXPLORATION_FACTOR * sqrt(ln(total.toDouble()) / count.toDouble())

        return avgReward + bonus.toFloat() + (defaultWeight * 0.4f)
    }

    fun getPreferredPlatform(): String {
        val ytCount = prefs.getInt("plat_YouTube_count", 0)
        val spCount = prefs.getInt("plat_Spotify_count", 0)
        return if (spCount > ytCount) "Spotify" else "YouTube"
    }

    fun getPreferenceStats(): MediaPreferenceStats {
        val total = prefs.getInt("total_media_interactions", 0)
        if (total == 0) {
            return MediaPreferenceStats()
        }

        val categories = listOf(
            "ENERGETIC" to "Upbeat & Energetic",
            "FOCUS" to "Lofi & Deep Work",
            "CHILL" to "Chill & Acoustic",
            "AMBIENT" to "Sleep & Ambient"
        )

        var bestLabel = "Lofi & Deep Work"
        var maxCount = 0

        for ((key, label) in categories) {
            val c = prefs.getInt("cat_${key}_count", 0)
            if (c > maxCount) {
                maxCount = c
                bestLabel = label
            }
        }

        val percentage = if (total > 0 && maxCount > 0) {
            ((maxCount.toFloat() / total.toFloat()) * 100).toInt().coerceIn(30, 98)
        } else {
            65
        }

        return MediaPreferenceStats(
            topCategory = bestLabel,
            topCategoryPercentage = percentage,
            preferredPlatform = getPreferredPlatform(),
            totalInteractions = total
        )
    }

    fun resetData() {
        prefs.edit().clear().apply()
    }
}
