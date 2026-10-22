package com.example.core.ml

import android.content.Context
import com.example.core.ai.LifestyleTip
import com.example.core.repository.RealHealthData
import com.example.feature.widget.BriefingSession
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt

data class OnDeviceSummaryContext(
    val session: BriefingSession,
    val tempC: Double,
    val weatherCondition: String = "Cool and pleasant",
    val routineInsight: RoutineInsight = RoutineInsight(),
    val currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val currentMinute: Int = Calendar.getInstance().get(Calendar.MINUTE)
)

class InternalMlEngine(private val context: Context) {

    val routineLearner = RoutinePatternLearner(context)
    val cardLearner = CardPreferenceLearner(context)
    val streakTracker = HabitStreakTracker(context)
    val mediaLearner = MediaPreferenceLearner(context)
    val lifestyleLearner = LifestylePreferenceLearner(context)
    val contextualBandit = ContextualLifestyleBandit(context)
    val behaviorPredictor = BehaviorSequencePredictor(context)
    val personaClassifier = UserPersonaClassifier(context)

    companion object {
        @Volatile
        private var INSTANCE: InternalMlEngine? = null

        fun getInstance(context: Context): InternalMlEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InternalMlEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun getRoutineInsight(): RoutineInsight {
        return routineLearner.analyzeRoutinePatterns()
    }

    fun rankCards(cardIds: List<String>): List<String> {
        return cardLearner.rankCards(cardIds)
    }

    fun recordCardClick(cardId: String) {
        cardLearner.recordInteraction(cardId, reward = 1.0f)
    }

    fun recordCardDismiss(cardId: String) {
        cardLearner.recordInteraction(cardId, reward = -0.3f)
    }

    fun recordMediaClick(categoryKey: String, platform: String) {
        mediaLearner.recordInteraction(categoryKey, platform, reward = 1.0f)
    }

    fun getMediaPreferenceStats(): MediaPreferenceStats {
        return mediaLearner.getPreferenceStats()
    }

    fun recordLifestyleClick(itemTitle: String, category: String, currentHour: Int) {
        val ctx = LifestyleContext.fromCurrentState(
            hourOfDay = currentHour,
            streakDays = streakTracker.getStreakCount(),
            personaType = personaClassifier.getCachedPersonaProfile()?.personaType ?: UserPersonaType.BALANCED_DEFAULT
        )
        contextualBandit.recordInteraction(itemTitle, category, ctx, reward = 1.0f)
    }

    fun recordLifestyleClickContextual(
        itemTitle: String,
        category: String,
        context: LifestyleContext
    ) {
        contextualBandit.recordInteraction(itemTitle, category, context, reward = 1.0f)
    }

    fun rankLifestyleTipsContextual(
        tips: List<LifestyleTip>,
        context: LifestyleContext
    ): List<LifestyleTip> {
        return contextualBandit.rankLifestyleTips(tips, context)
    }

    fun generateContextualLeadText(
        context: LifestyleContext,
        weatherDesc: String = "cool"
    ): String {
        return contextualBandit.generateSessionLeadText(context, weatherDesc)
    }

    suspend fun predictTomorrowBehavior(
        todayActivityRate: Float = 0f,
        todayStepsCount: Int = 0
    ): BehaviorPrediction {
        return behaviorPredictor.predictTomorrow(todayActivityRate, todayStepsCount)
    }

    fun getCachedBehaviorPrediction(): BehaviorPrediction? {
        return behaviorPredictor.getCachedPrediction()
    }

    suspend fun classifyUserPersona(
        healthData: RealHealthData? = null,
        taskCount: Int = 0,
        completedTaskCount: Int = 0
    ): UserPersonaProfile {
        return personaClassifier.classifyUserPersona(healthData, taskCount, completedTaskCount)
    }

    fun getCachedPersonaProfile(): UserPersonaProfile? {
        return personaClassifier.getCachedPersonaProfile()
    }

    fun predictTask(
        title: String,
        scheduledTimestampMs: Long = System.currentTimeMillis(),
        createdAtMs: Long = System.currentTimeMillis()
    ): TaskPredictionResult {
        return TaskProcrastinationPredictor.predict(
            title = title,
            scheduledTimestampMs = scheduledTimestampMs,
            createdAtMs = createdAtMs
        )
    }

    fun calculateDailyReadiness(
        sleepDurationMinutes: Int?,
        tempC: Double? = 28.0,
        humidity: Int? = 65,
        pendingTasksCount: Int = 0
    ): DailyReadinessResult {
        return SmartReadinessEngine.calculateReadiness(
            sleepDurationMinutes = sleepDurationMinutes,
            streakDays = streakTracker.getStreakCount(),
            temperatureC = tempC,
            humidityPercent = humidity,
            pendingTasksCount = pendingTasksCount
        )
    }

    fun calculateDynamicEnergyScore(
        sleepHours: Double = 7.2,
        currentHour: Int,
        tempC: Double = 28.0
    ): Int {
        val sleepRatio = (sleepHours / 7.5).coerceIn(0.5, 1.15)
        val baseScore = sleepRatio * 82.0

        val circadianWave = when (currentHour) {
            in 6..11 -> 12.0 * cos((currentHour - 9) * Math.PI / 8.0)
            in 12..14 -> -6.0
            in 15..18 -> 8.0
            in 19..22 -> -5.0
            else -> -15.0
        }

        val heatPenalty = if (tempC > 33.0) (tempC - 33.0) * 1.5 else 0.0
        val streakBonus = (streakTracker.getStreakCount().coerceAtMost(5)) * 1.5

        val finalScore = baseScore + circadianWave - heatPenalty + streakBonus
        return finalScore.roundToInt().coerceIn(45, 98)
    }

    fun generatePersonalizedSummary(ctx: OnDeviceSummaryContext): String {
        val tempInt = ctx.tempC.roundToInt()
        val insight = ctx.routineInsight
        val anomaly = RoutineAnomalyDetector.detectWakeAnomaly(
            currentHour = ctx.currentHour,
            currentMinute = ctx.currentMinute,
            predictedHour = insight.predictedWakeHour,
            predictedMinute = insight.predictedWakeMinute
        )

        val streakText = streakTracker.getStreakBadgeText()
        val weekendNote = if (insight.isTodayWeekend) {
            "Weekend schedule (~${insight.weekend.wakeTimeFormatted})"
        } else {
            "Weekday habit (~${insight.weekday.wakeTimeFormatted})"
        }

        return when (ctx.session) {
            BriefingSession.EARLY_MORNING -> {
                val base = "Start your morning at ${tempInt}°C (${ctx.weatherCondition}). "
                val routinePart = if (anomaly.recoveryAdvice != null) {
                    anomaly.recoveryAdvice
                } else {
                    "You woke up right on your $weekendNote. Drink some warm water and get ready for the new day!"
                }
                base + routinePart
            }
            BriefingSession.MID_MORNING -> {
                val energy = calculateDynamicEnergyScore(currentHour = ctx.currentHour, tempC = ctx.tempC)
                val streakSuffix = if (streakText != null) " ($streakText)" else ""
                "Peak morning focus with $energy/100 energy$streakSuffix. Weather is ${tempInt}°C, ${ctx.weatherCondition.lowercase()} — prioritize your most important work."
            }
            BriefingSession.NOON -> {
                val hydrationTip = if (tempInt >= 31) "Remember to drink extra water as outdoor temperature is ${tempInt}°C." else "Give your eyes a 15-minute rest."
                "Relax at midday (${tempInt}°C, ${ctx.weatherCondition.lowercase()}). $hydrationTip"
            }
            BriefingSession.AFTERNOON -> {
                val energy = calculateDynamicEnergyScore(currentHour = ctx.currentHour, tempC = ctx.tempC)
                "Keep up your afternoon momentum (${tempInt}°C, ${ctx.weatherCondition.lowercase()}). Current energy level is ~$energy%, great for wrapping up remaining tasks."
            }
            BriefingSession.EVENING -> {
                "Unwind this evening at ${tempInt}°C (${ctx.weatherCondition.lowercase()}). Set work aside, do some light exercise, and enjoy dinner."
            }
            BriefingSession.NIGHT -> {
                val targetBed = insight.predictedBedtimeFormatted
                "Enjoy a peaceful night (${tempInt}°C). Based on your personal rhythm, start winding down before $targetBed for a deep sleep."
            }
            BriefingSession.LATE_NIGHT -> {
                val targetBed = insight.predictedBedtimeFormatted
                val targetWake = insight.predictedWakeTimeFormatted
                "It's late (${tempInt}°C). Put your phone away and rest (usual bedtime ~$targetBed) so you can wake up refreshed at $targetWake tomorrow."
            }
        }
    }

    fun resetAllMlData() {
        streakTracker.reset()
        mediaLearner.resetData()
        lifestyleLearner.resetData()
        contextualBandit.resetData()
        behaviorPredictor.resetData()
        personaClassifier.resetData()
    }
}
