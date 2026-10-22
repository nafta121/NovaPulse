package com.example.core.ml

import android.content.Context
import android.util.Log
import java.util.Calendar
import kotlin.math.abs

class HabitStreakTracker(context: Context) {

    companion object {
        private const val TAG = "HabitStreakTracker"
        private const val PREFS_NAME = "habit_streak_ml_stats"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_LAST_CHECK_DAY = "last_checked_day"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordBedtimeEvent(
        actualBedHour: Int,
        actualBedMinute: Int,
        targetBedHour: Int,
        targetBedMinute: Int
    ): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastCheckedDay = prefs.getInt(KEY_LAST_CHECK_DAY, -1)
        val currentStreak = prefs.getInt(KEY_STREAK, 0)

        if (today == lastCheckedDay) {
            return currentStreak
        }

        val actualNormalized = if (actualBedHour < 12) actualBedHour + 24 else actualBedHour
        val targetNormalized = if (targetBedHour < 12) targetBedHour + 24 else targetBedHour

        val actualTotalMins = actualNormalized * 60 + actualBedMinute
        val targetTotalMins = targetNormalized * 60 + targetBedMinute

        val isOnTime = abs(actualTotalMins - targetTotalMins) <= 30

        val newStreak = if (isOnTime) {
            currentStreak + 1
        } else {
            maxOf(0, currentStreak - 1)
        }

        prefs.edit()
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_LAST_CHECK_DAY, today)
            .apply()

        Log.d(TAG, "Bedtime check: onTime=$isOnTime -> Streak = $newStreak")
        return newStreak
    }

    fun getStreakCount(): Int {
        return prefs.getInt(KEY_STREAK, 0)
    }

    fun getStreakBadgeText(): String? {
        val streak = getStreakCount()
        return if (streak > 0) {
            "🔥 $streak-day on-time sleep streak"
        } else {
            null
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}
