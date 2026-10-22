package com.example.core.ml

import android.content.Context
import android.util.Log
import com.example.core.database.AppDatabase
import com.example.core.database.UserRoutineLogEntity
import com.example.core.repository.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TreeMap
import kotlin.math.roundToInt

enum class TrendDirection(val label: String, val emoji: String) {
    IMPROVING("Improving", "📈"),
    STABLE("Stable", "➡️"),
    DECLINING("Declining", "📉")
}

enum class DailyBehaviorState(val label: String, val emoji: String) {
    SLEEP_EARLY("Early sleep", "🌙"),
    SLEEP_NORMAL("On-time sleep", "😴"),
    SLEEP_LATE("Late sleep", "🦉"),
    WAKE_EARLY("Early wake-up", "🌅"),
    WAKE_NORMAL("On-time wake-up", "☀️"),
    WAKE_LATE("Late wake-up", "⏰"),
    HIGH_ENERGY("High energy", "⚡"),
    LOW_ENERGY("Low energy", "🔋"),
    UNKNOWN("Insufficient data", "❓");

    companion object {
        fun fromSleepHour(hour: Int, minute: Int = 0): DailyBehaviorState {
            return when {
                hour in 20..21 || (hour == 22 && minute <= 30) -> SLEEP_EARLY
                hour == 22 || (hour == 23 && minute <= 30) -> SLEEP_NORMAL
                else -> SLEEP_LATE
            }
        }

        fun fromWakeHour(hour: Int, minute: Int = 0): DailyBehaviorState {
            return when {
                hour < 6 || (hour == 6 && minute <= 30) -> WAKE_EARLY
                hour == 6 || (hour == 7 && minute <= 30) -> WAKE_NORMAL
                else -> WAKE_LATE
            }
        }
    }
}

data class BehaviorPrediction(
    val predictedSleepState: DailyBehaviorState = DailyBehaviorState.SLEEP_NORMAL,
    val sleepProbability: Float = 0.5f,
    val predictedWakeState: DailyBehaviorState = DailyBehaviorState.WAKE_NORMAL,
    val wakeProbability: Float = 0.5f,
    val insight: String = "Collecting data for more accurate predictions.",
    val recommendation: String = "",
    val confidenceLevel: String = "Initializing",
    val totalDaysSampled: Int = 0,
    val streakInfo: String? = null,
    val trendDirection: TrendDirection = TrendDirection.STABLE,
    val activityImpactNote: String? = null,
    val estimatedRecoveryPercent: Int = 75
)

class BehaviorSequencePredictor(private val context: Context) {

    companion object {
        private const val TAG = "BehaviorPredictor"
        private const val PREFS_NAME = "behavior_sequence_ml_v1"
        private const val HISTORY_DAYS = 30
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(context)
    private val routineDao = database.routineDao()

    private val sleepStates = listOf(
        DailyBehaviorState.SLEEP_EARLY,
        DailyBehaviorState.SLEEP_NORMAL,
        DailyBehaviorState.SLEEP_LATE
    )

    private val wakeStates = listOf(
        DailyBehaviorState.WAKE_EARLY,
        DailyBehaviorState.WAKE_NORMAL,
        DailyBehaviorState.WAKE_LATE
    )

    suspend fun predictTomorrow(
        todayActivityRate: Float = 0f,
        todayStepsCount: Int = 0
    ): BehaviorPrediction = withContext(Dispatchers.IO) {
        try {
            val nowMs = System.currentTimeMillis()
            val sinceMs = nowMs - (HISTORY_DAYS * 24 * 3600 * 1000L)
            val logs = routineDao.getLogsSince(sinceMs)

            if (logs.size < 4) {
                return@withContext createDefaultPrediction(logs.size, todayActivityRate, todayStepsCount)
            }

            val dailyMap = groupLogsByDay(logs)
            val dailyStates = extractDailyStates(dailyMap)

            if (dailyStates.size < 2) {
                return@withContext createDefaultPrediction(logs.size, todayActivityRate, todayStepsCount)
            }

            val sleepTransition = Array(3) { FloatArray(3) { 1.0f } }
            val sleepToWakeMatrix = Array(3) { FloatArray(3) { 1.0f } }

            sleepTransition[0][0] = 2.0f; sleepTransition[0][1] = 1.5f
            sleepTransition[1][1] = 2.5f
            sleepTransition[2][2] = 2.0f; sleepTransition[2][1] = 1.5f

            sleepToWakeMatrix[0][0] = 2.5f; sleepToWakeMatrix[0][1] = 1.5f
            sleepToWakeMatrix[1][1] = 2.5f
            sleepToWakeMatrix[2][2] = 2.5f; sleepToWakeMatrix[2][1] = 1.2f

            val statesList = dailyStates.values.toList()
            for (i in statesList.indices) {
                val (sleepS, wakeS) = statesList[i]
                val sIdx = sleepStates.indexOf(sleepS)
                val wIdx = wakeStates.indexOf(wakeS)

                val recencyWeight = 1.0f + (i.toFloat() / statesList.size.toFloat())

                if (sIdx in 0..2 && wIdx in 0..2) {
                    sleepToWakeMatrix[sIdx][wIdx] += recencyWeight
                }

                if (i < statesList.size - 1) {
                    val (nextSleepS, _) = statesList[i + 1]
                    val nextSIdx = sleepStates.indexOf(nextSleepS)
                    if (sIdx in 0..2 && nextSIdx in 0..2) {
                        sleepTransition[sIdx][nextSIdx] += recencyWeight
                    }
                }
            }

            val lastSleepState = statesList.lastOrNull()?.first ?: DailyBehaviorState.SLEEP_NORMAL
            val currentSIdx = sleepStates.indexOf(lastSleepState).coerceIn(0, 2)

            val nextSleepProbs = normalizeRow(sleepTransition[currentSIdx])

            if (todayActivityRate >= 0.75f || todayStepsCount >= 6500) {
                nextSleepProbs[0] += 0.14f
                nextSleepProbs[1] += 0.08f
                nextSleepProbs[2] = (nextSleepProbs[2] - 0.18f).coerceAtLeast(0.05f)
            } else if (todayActivityRate in 0.01f..0.25f && todayStepsCount in 1..2000) {
                nextSleepProbs[2] += 0.10f
            }

            val adjustedSleepProbs = normalizeRow(nextSleepProbs)
            val bestSleepIdx = adjustedSleepProbs.indices.maxByOrNull { adjustedSleepProbs[it] } ?: 1
            val predictedSleep = sleepStates[bestSleepIdx]
            val sleepProb = adjustedSleepProbs[bestSleepIdx].coerceIn(0.38f, 0.94f)

            val wakeProbs = normalizeRow(sleepToWakeMatrix[bestSleepIdx])
            val bestWakeIdx = wakeProbs.indices.maxByOrNull { wakeProbs[it] } ?: 1
            val predictedWake = wakeStates[bestWakeIdx]
            val wakeProb = wakeProbs[bestWakeIdx].coerceIn(0.40f, 0.94f)

            val daysSampled = dailyStates.size
            val confidence = when {
                daysSampled >= 14 -> "High (${daysSampled} days)"
                daysSampled >= 5 -> "Learning (${daysSampled} days)"
                else -> "Initializing"
            }

            val streakInfo = detectPatternStreak(statesList)
            val trend = analyzeRoutineTrend(statesList)
            val activityNote = buildActivityImpactNote(todayActivityRate, todayStepsCount, predictedSleep)
            val estRecovery = calculateEstimatedRecovery(predictedSleep, predictedWake, todayActivityRate, trend)

            val (insight, recommendation) = generateHumanInsight(
                predictedSleep = predictedSleep,
                sleepProb = sleepProb,
                predictedWake = predictedWake,
                wakeProb = wakeProb,
                lastSleep = lastSleepState,
                trend = trend,
                activityNote = activityNote
            )

            cachePrediction(predictedSleep, sleepProb, predictedWake, wakeProb, insight, recommendation, daysSampled)

            BehaviorPrediction(
                predictedSleepState = predictedSleep,
                sleepProbability = sleepProb,
                predictedWakeState = predictedWake,
                wakeProbability = wakeProb,
                insight = insight,
                recommendation = recommendation,
                confidenceLevel = confidence,
                totalDaysSampled = daysSampled,
                streakInfo = streakInfo,
                trendDirection = trend,
                activityImpactNote = activityNote,
                estimatedRecoveryPercent = estRecovery
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting behavior sequence: ${e.message}")
            getCachedPrediction() ?: createDefaultPrediction(0, todayActivityRate, todayStepsCount)
        }
    }

    fun getCachedPrediction(): BehaviorPrediction? {
        val sleepName = prefs.getString("cached_sleep_state", null) ?: return null
        val wakeName = prefs.getString("cached_wake_state", null) ?: return null
        return try {
            BehaviorPrediction(
                predictedSleepState = DailyBehaviorState.valueOf(sleepName),
                sleepProbability = prefs.getFloat("cached_sleep_prob", 0.65f),
                predictedWakeState = DailyBehaviorState.valueOf(wakeName),
                wakeProbability = prefs.getFloat("cached_wake_prob", 0.68f),
                insight = prefs.getString("cached_insight", "") ?: "",
                recommendation = prefs.getString("cached_rec", "") ?: "",
                confidenceLevel = "Updated",
                totalDaysSampled = prefs.getInt("cached_days", 5)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun groupLogsByDay(logs: List<UserRoutineLogEntity>): Map<Int, List<UserRoutineLogEntity>> {
        val map = TreeMap<Int, MutableList<UserRoutineLogEntity>>()
        for (log in logs) {
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
            map.getOrPut(dayKey) { mutableListOf() }.add(log)
        }
        return map
    }

    private fun extractDailyStates(
        dailyMap: Map<Int, List<UserRoutineLogEntity>>
    ): Map<Int, Pair<DailyBehaviorState, DailyBehaviorState>> {
        val result = TreeMap<Int, Pair<DailyBehaviorState, DailyBehaviorState>>()

        for ((dayKey, dayLogs) in dailyMap) {
            var sleepState = DailyBehaviorState.SLEEP_NORMAL
            var wakeState = DailyBehaviorState.WAKE_NORMAL

            val bedLog = dayLogs.lastOrNull {
                it.eventType in listOf(
                    RoutineRepository.EVENT_SCREEN_OFF,
                    RoutineRepository.EVENT_POWER_CONNECTED,
                    RoutineRepository.EVENT_HEALTH_SLEEP
                ) && (it.hourOfDay >= 20 || it.hourOfDay <= 3)
            }
            if (bedLog != null) {
                sleepState = DailyBehaviorState.fromSleepHour(bedLog.hourOfDay, bedLog.minuteOfHour)
            }

            val wakeLog = dayLogs.firstOrNull {
                it.eventType in listOf(
                    RoutineRepository.EVENT_USER_PRESENT,
                    RoutineRepository.EVENT_POWER_DISCONNECTED
                ) && (it.hourOfDay in 5..10)
            }
            if (wakeLog != null) {
                wakeState = DailyBehaviorState.fromWakeHour(wakeLog.hourOfDay, wakeLog.minuteOfHour)
            }

            result[dayKey] = sleepState to wakeState
        }
        return result
    }

    private fun normalizeRow(row: FloatArray): FloatArray {
        val sum = row.sum()
        if (sum <= 0f) return floatArrayOf(0.33f, 0.34f, 0.33f)
        return FloatArray(row.size) { i -> row[i] / sum }
    }

    private fun detectPatternStreak(
        statesList: List<Pair<DailyBehaviorState, DailyBehaviorState>>
    ): String? {
        if (statesList.size < 2) return null
        val recent = statesList.takeLast(3)
        val lateCount = recent.count { it.first == DailyBehaviorState.SLEEP_LATE }
        val onTimeCount = recent.count {
            it.first == DailyBehaviorState.SLEEP_NORMAL || it.first == DailyBehaviorState.SLEEP_EARLY
        }

        return when {
            lateCount >= 2 -> "⚠️ Slept late $lateCount nights in a row"
            onTimeCount >= 2 -> "✨ Kept on-time sleep for $onTimeCount days"
            else -> null
        }
    }

    private fun analyzeRoutineTrend(
        statesList: List<Pair<DailyBehaviorState, DailyBehaviorState>>
    ): TrendDirection {
        if (statesList.size < 4) return TrendDirection.STABLE

        fun scorePair(pair: Pair<DailyBehaviorState, DailyBehaviorState>): Float {
            val sScore = when (pair.first) {
                DailyBehaviorState.SLEEP_EARLY -> 1.0f
                DailyBehaviorState.SLEEP_NORMAL -> 0.85f
                DailyBehaviorState.SLEEP_LATE -> 0.3f
                else -> 0.5f
            }
            val wScore = when (pair.second) {
                DailyBehaviorState.WAKE_EARLY -> 1.0f
                DailyBehaviorState.WAKE_NORMAL -> 0.85f
                DailyBehaviorState.WAKE_LATE -> 0.35f
                else -> 0.5f
            }
            return (sScore + wScore) / 2f
        }

        val recent = statesList.takeLast(3).map { scorePair(it) }.average()
        val previous = statesList.dropLast(3).takeLast(4).map { scorePair(it) }.average()

        return when {
            recent - previous >= 0.12 -> TrendDirection.IMPROVING
            previous - recent >= 0.12 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }

    private fun buildActivityImpactNote(
        activityRate: Float,
        stepsCount: Int,
        predictedSleep: DailyBehaviorState
    ): String? {
        return when {
            stepsCount >= 7000 || activityRate >= 0.8f ->
                "🏃 High activity today (${if (stepsCount > 0) "$stepsCount steps" else "${(activityRate * 100).toInt()}% goal"}) boosts deep sleep probability by +15% tonight."
            predictedSleep == DailyBehaviorState.SLEEP_LATE && activityRate in 0.01f..0.3f ->
                "💡 Low daytime movement may make it harder to fall asleep tonight — try 10 minutes of evening stretching."
            else -> null
        }
    }

    private fun calculateEstimatedRecovery(
        sleepState: DailyBehaviorState,
        wakeState: DailyBehaviorState,
        activityRate: Float,
        trend: TrendDirection
    ): Int {
        var base = when (sleepState) {
            DailyBehaviorState.SLEEP_EARLY -> 88
            DailyBehaviorState.SLEEP_NORMAL -> 82
            DailyBehaviorState.SLEEP_LATE -> 64
            else -> 75
        }
        if (wakeState == DailyBehaviorState.WAKE_LATE && sleepState == DailyBehaviorState.SLEEP_LATE) {
            base -= 6
        }
        if (activityRate >= 0.7f) base += 5
        if (trend == TrendDirection.IMPROVING) base += 3
        return base.coerceIn(50, 96)
    }

    private fun generateHumanInsight(
        predictedSleep: DailyBehaviorState,
        sleepProb: Float,
        predictedWake: DailyBehaviorState,
        wakeProb: Float,
        lastSleep: DailyBehaviorState,
        trend: TrendDirection,
        activityNote: String?
    ): Pair<String, String> {
        val sleepPct = (sleepProb * 100).roundToInt()
        val wakePct = (wakeProb * 100).roundToInt()

        val insight = when {
            predictedSleep == DailyBehaviorState.SLEEP_LATE && predictedWake == DailyBehaviorState.WAKE_LATE ->
                "Forecast: $sleepPct% chance of late sleep tonight leading to a late wake-up tomorrow ($wakePct%)."
            predictedSleep == DailyBehaviorState.SLEEP_LATE && predictedWake == DailyBehaviorState.WAKE_EARLY ->
                "Alert: Likely to sleep late tonight ($sleepPct%) with an early wake-up tomorrow — watch out for morning fatigue."
            predictedSleep == DailyBehaviorState.SLEEP_EARLY ->
                "Positive rhythm: $sleepPct% chance of early sleep tonight and waking up refreshed (${predictedWake.label.lowercase()}) tomorrow."
            else ->
                "Stable bio-rhythm: Forecasted ${predictedSleep.label.lowercase()} ($sleepPct%) tonight and ${predictedWake.label.lowercase()} ($wakePct%) tomorrow."
        }

        val baseRecommendation = when (predictedSleep) {
            DailyBehaviorState.SLEEP_LATE ->
                if (lastSleep == DailyBehaviorState.SLEEP_LATE) {
                    "Break the late-sleep cycle: Enable Wind Down at 22:30 and put your phone away 30 mins before bed."
                } else {
                    "Suggested: Wrap up remaining tasks before 22:00 to avoid staying up past midnight."
                }
            DailyBehaviorState.SLEEP_EARLY ->
                "Great time for deep recovery: Prepare your morning outfit and enjoy a restful night."
            else ->
                "Keep your regular schedule (~23:00) to sustain peak focus for tomorrow morning."
        }

        val finalRecommendation = if (activityNote != null) {
            "$activityNote $baseRecommendation"
        } else {
            baseRecommendation
        }

        return insight to finalRecommendation
    }

    private fun createDefaultPrediction(
        sampleCount: Int,
        todayActivityRate: Float = 0f,
        todayStepsCount: Int = 0
    ): BehaviorPrediction {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isLateNow = hour >= 23 || hour in 0..3
        val sleepState = if (isLateNow) DailyBehaviorState.SLEEP_LATE else DailyBehaviorState.SLEEP_NORMAL
        val wakeState = if (isLateNow) DailyBehaviorState.WAKE_LATE else DailyBehaviorState.WAKE_NORMAL
        val activityNote = buildActivityImpactNote(todayActivityRate, todayStepsCount, sleepState)

        return BehaviorPrediction(
            predictedSleepState = sleepState,
            sleepProbability = if (isLateNow) 0.72f else 0.68f,
            predictedWakeState = wakeState,
            wakeProbability = 0.65f,
            insight = if (isLateNow) {
                "Late-night activity detected (72% chance of late sleep) — may impact tomorrow's wake-up time."
            } else {
                "Maintaining a balanced bio-rhythm (68% chance of on-time sleep tonight and waking up at ~07:00 tomorrow)."
            },
            recommendation = activityNote
                ?: if (isLateNow) {
                    "Turn on Eye Comfort Shield and relax your mind to fall asleep soon."
                } else {
                    "Keep up your current routine so the AI model can refine your personal habits."
                },
            confidenceLevel = if (sampleCount > 0) "Learning" else "Default",
            totalDaysSampled = (sampleCount / 2).coerceAtLeast(1),
            trendDirection = TrendDirection.STABLE,
            activityImpactNote = activityNote,
            estimatedRecoveryPercent = if (isLateNow) 66 else 82
        )
    }

    private fun cachePrediction(
        sleep: DailyBehaviorState,
        sProb: Float,
        wake: DailyBehaviorState,
        wProb: Float,
        insight: String,
        rec: String,
        days: Int
    ) {
        prefs.edit()
            .putString("cached_sleep_state", sleep.name)
            .putFloat("cached_sleep_prob", sProb)
            .putString("cached_wake_state", wake.name)
            .putFloat("cached_wake_prob", wProb)
            .putString("cached_insight", insight)
            .putString("cached_rec", rec)
            .putInt("cached_days", days)
            .apply()
    }

    fun resetData() {
        prefs.edit().clear().apply()
    }
}
