package com.example.core.ml

import android.content.Context
import android.util.Log
import com.example.core.ai.LifestyleTip
import kotlin.math.ln
import kotlin.math.sqrt

enum class LifestyleSessionKey {
    MORNING,
    NOON,
    EVENING_NIGHT;

    companion object {
        fun fromHour(hour: Int): LifestyleSessionKey {
            return when (hour) {
                in 5..10 -> MORNING
                in 11..16 -> NOON
                else -> EVENING_NIGHT
            }
        }
    }
}

class LifestylePreferenceLearner(context: Context) {

    companion object {
        private const val TAG = "LifestylePrefLearner"
        private const val PREFS_NAME = "lifestyle_preference_ml_stats"
        private const val EXPLORATION_FACTOR = 0.85
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordInteraction(itemTitle: String, category: String, currentHour: Int, reward: Float = 1.0f) {
        val sessionKey = LifestyleSessionKey.fromHour(currentHour).name
        val itemHash = itemTitle.trim().lowercase().hashCode()

        val itemCountKey = "item_${itemHash}_count"
        val itemRewardKey = "item_${itemHash}_reward"
        val catSessionCountKey = "cat_${sessionKey}_${category}_count"
        val catSessionRewardKey = "cat_${sessionKey}_${category}_reward"
        val totalKey = "total_lifestyle_interactions"

        val itemCount = prefs.getInt(itemCountKey, 0) + 1
        val itemReward = prefs.getFloat(itemRewardKey, 0f) + reward
        val catCount = prefs.getInt(catSessionCountKey, 0) + 1
        val catReward = prefs.getFloat(catSessionRewardKey, 0f) + reward
        val total = prefs.getInt(totalKey, 0) + 1

        prefs.edit()
            .putInt(itemCountKey, itemCount)
            .putFloat(itemRewardKey, itemReward)
            .putInt(catSessionCountKey, catCount)
            .putFloat(catSessionRewardKey, catReward)
            .putInt(totalKey, total)
            .apply()

        Log.d(TAG, "Recorded lifestyle click: '$itemTitle' [$category] in $sessionKey (total=$total)")
    }

    fun calculateScore(itemTitle: String, category: String, currentHour: Int, baseOrderScore: Float = 0.5f): Float {
        val total = prefs.getInt("total_lifestyle_interactions", 0)
        if (total <= 1) return baseOrderScore

        val sessionKey = LifestyleSessionKey.fromHour(currentHour).name
        val itemHash = itemTitle.trim().lowercase().hashCode()

        val itemCount = prefs.getInt("item_${itemHash}_count", 0)
        val itemReward = prefs.getFloat("item_${itemHash}_reward", 0f)
        val catCount = prefs.getInt("cat_${sessionKey}_${category}_count", 0)
        val catReward = prefs.getFloat("cat_${sessionKey}_${category}_reward", 0f)

        val itemScore = if (itemCount > 0) {
            (itemReward / itemCount) + EXPLORATION_FACTOR.toFloat() * sqrt(ln(total.toDouble()) / itemCount.toDouble()).toFloat()
        } else {
            0.3f
        }

        val catScore = if (catCount > 0) {
            (catReward / catCount) * 0.6f
        } else {
            0.2f
        }

        return (itemScore * 0.6f) + (catScore * 0.4f) + (baseOrderScore * 0.3f)
    }

    fun rankLifestyleTips(tips: List<LifestyleTip>, currentHour: Int): List<LifestyleTip> {
        if (tips.size <= 1) return tips

        return tips.mapIndexed { index, tip ->
            val baseOrder = 1.0f - (index * 0.08f)
            tip to calculateScore(tip.title, tip.category, currentHour, baseOrder)
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun generateSessionLeadText(currentHour: Int, tempC: Int, weatherDesc: String = "cool"): String {
        val session = LifestyleSessionKey.fromHour(currentHour)
        val total = prefs.getInt("total_lifestyle_interactions", 0)

        val topCat = if (total >= 3) {
            val categories = listOf("Food & Dining", "Fitness", "Travel", "Relaxation", "Lifestyle")
            categories.maxByOrNull { cat ->
                prefs.getInt("cat_${session.name}_${cat}_count", 0)
            }
        } else null

        return when (session) {
            LifestyleSessionKey.MORNING -> {
                if (topCat == "Fitness") {
                    "Morning is ${tempC}°C ($weatherDesc) — prioritizing workout & morning energy suggestions per your habits:"
                } else {
                    "Good morning! Current weather is ${tempC}°C ($weatherDesc), here are suggestions to start your day:"
                }
            }
            LifestyleSessionKey.NOON -> {
                if (topCat == "Food & Dining") {
                    "Midday is ${tempC}°C — here are nutritious lunch & break options tailored to your taste:"
                } else {
                    "Midday break (${tempC}°C, $weatherDesc) — recharge with these curated activities:"
                }
            }
            LifestyleSessionKey.EVENING_NIGHT -> {
                if (topCat == "Relaxation") {
                    "Pleasant evening (${tempC}°C) — prioritizing mind relaxation & recovery suggestions:"
                } else {
                    "Unwind tonight (${tempC}°C, $weatherDesc) — lifestyle & relaxation suggestions for you:"
                }
            }
        }
    }

    fun resetData() {
        prefs.edit().clear().apply()
    }
}
