package com.example.core.ml

import android.content.Context
import android.util.Log
import kotlin.math.ln
import kotlin.math.sqrt

class CardPreferenceLearner(context: Context) {

    companion object {
        private const val TAG = "CardPreferenceLearner"
        private const val PREFS_NAME = "card_preference_ml_stats"
        private const val UCB_EXPLORATION_FACTOR = 1.2
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordInteraction(cardId: String, reward: Float) {
        val countKey = "${cardId}_count"
        val rewardKey = "${cardId}_reward_sum"
        val totalInteractionsKey = "total_interactions"

        val currentCount = prefs.getInt(countKey, 0) + 1
        val currentRewardSum = prefs.getFloat(rewardKey, 0f) + reward
        val totalInteractions = prefs.getInt(totalInteractionsKey, 0) + 1

        prefs.edit()
            .putInt(countKey, currentCount)
            .putFloat(rewardKey, currentRewardSum)
            .putInt(totalInteractionsKey, totalInteractions)
            .apply()

        Log.d(TAG, "Recorded interaction for '$cardId': count=$currentCount, rewardSum=$currentRewardSum")
    }

    fun calculateUcbScore(cardId: String, defaultPriority: Float = 1.0f): Float {
        val count = prefs.getInt("${cardId}_count", 0)
        val totalInteractions = prefs.getInt("total_interactions", 0)

        if (count == 0 || totalInteractions <= 1) {
            return defaultPriority
        }

        val rewardSum = prefs.getFloat("${cardId}_reward_sum", 0f)
        val avgReward = rewardSum / count.toFloat()
        val explorationBonus = UCB_EXPLORATION_FACTOR * sqrt(ln(totalInteractions.toDouble()) / count.toDouble())

        return (avgReward + explorationBonus.toFloat() + defaultPriority * 0.5f)
    }

    fun rankCards(cardIds: List<String>): List<String> {
        return cardIds.mapIndexed { index, cardId ->
            val defaultPriority = 1.0f - (index * 0.05f)
            cardId to calculateUcbScore(cardId, defaultPriority)
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
