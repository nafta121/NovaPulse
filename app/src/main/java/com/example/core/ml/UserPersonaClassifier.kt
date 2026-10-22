package com.example.core.ml

import android.content.Context
import android.util.Log
import com.example.core.database.AppDatabase
import com.example.core.repository.RealHealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class UserPersonaType(
    val id: String,
    val title: String,
    val subtitle: String,
    val colorHex: Long,
    val iconType: String
) {
    CORPORATE_PRO(
        id = "corporate_pro",
        title = "Office Professional",
        subtitle = "Structured & Disciplined Schedule",
        colorHex = 0xFF3B82F6,
        iconType = "WORK"
    ),
    ACTIVE_STUDENT(
        id = "active_student",
        title = "Active Student / Youth",
        subtitle = "Dynamic & Flexible",
        colorHex = 0xFF8B5CF6,
        iconType = "STUDENT"
    ),
    FLEX_FREELANCER(
        id = "flex_freelancer",
        title = "Independent Freelancer",
        subtitle = "Creative & Self-Directed Pace",
        colorHex = 0xFFF59E0B,
        iconType = "FREELANCE"
    ),
    WELLNESS_ATHLETE(
        id = "wellness_athlete",
        title = "Wellness & Fitness Enthusiast",
        subtitle = "High Activity & Early Riser",
        colorHex = 0xFF10B981,
        iconType = "FITNESS"
    ),
    NIGHT_OWL(
        id = "night_owl",
        title = "Deep Night Owl",
        subtitle = "Peak Late-Night Productivity",
        colorHex = 0xFF6366F1,
        iconType = "MOON"
    ),
    BALANCED_DEFAULT(
        id = "balanced_default",
        title = "Balanced Bio-Rhythm",
        subtitle = "Adapting & Learning Habits",
        colorHex = 0xFF06B6D4,
        iconType = "SPARKLE"
    )
}

data class UserPersonaProfile(
    val personaType: UserPersonaType = UserPersonaType.BALANCED_DEFAULT,
    val similarityPercent: Int = 85,
    val keyTraits: List<String> = emptyList(),
    val tailoredAdvice: String = "",
    val optimalFocusWindow: String = "08:30 – 11:30",
    val recommendedSleepWindow: String = "22:30 – 06:30",
    val daysSampled: Int = 0
)

data class PersonaFeatureVector(
    val wakeHour: Float,
    val sleepHour: Float,
    val dailyTasks: Float,
    val activityRatio: Float,
    val weekendShiftHours: Float,
    val eveningScreenRatio: Float,
    val streakConsistency: Float
) {
    fun toArray(): FloatArray = floatArrayOf(
        wakeHour,
        sleepHour,
        dailyTasks,
        activityRatio,
        weekendShiftHours,
        eveningScreenRatio,
        streakConsistency
    )
}

class UserPersonaClassifier(private val context: Context) {

    internal data class PersonaMetadata(
        val traits: List<String>,
        val advice: String,
        val focusWindow: String,
        val sleepWindow: String
    )

    companion object {
        private const val TAG = "PersonaClassifier"
        private const val PREFS_NAME = "user_persona_ml_v1"

        private val CENTROIDS = mapOf(
            UserPersonaType.CORPORATE_PRO to floatArrayOf(0.25f, 0.42f, 0.65f, 0.50f, 0.35f, 0.35f, 0.80f),
            UserPersonaType.ACTIVE_STUDENT to floatArrayOf(0.45f, 0.65f, 0.50f, 0.60f, 0.75f, 0.70f, 0.45f),
            UserPersonaType.FLEX_FREELANCER to floatArrayOf(0.55f, 0.60f, 0.70f, 0.45f, 0.20f, 0.65f, 0.55f),
            UserPersonaType.WELLNESS_ATHLETE to floatArrayOf(0.12f, 0.25f, 0.55f, 0.90f, 0.20f, 0.20f, 0.90f),
            UserPersonaType.NIGHT_OWL to floatArrayOf(0.75f, 0.88f, 0.50f, 0.35f, 0.40f, 0.90f, 0.30f)
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val routineLearner = RoutinePatternLearner(context)
    private val streakTracker = HabitStreakTracker(context)
    private val database = AppDatabase.getInstance(context)

    suspend fun classifyUserPersona(
        healthData: RealHealthData? = null,
        taskCount: Int = 0,
        completedTaskCount: Int = 0
    ): UserPersonaProfile = withContext(Dispatchers.IO) {
        try {
            val routine = routineLearner.analyzeRoutinePatterns()
            val streak = streakTracker.getStreakCount()

            val wakeDecimal = routine.weekday.wakeHour + (routine.weekday.wakeMinute / 60f)
            val wakeNorm = ((wakeDecimal - 5f) / 6f).coerceIn(0f, 1f)

            val rawBedHour = if (routine.weekday.bedHour < 12) routine.weekday.bedHour + 24 else routine.weekday.bedHour
            val bedDecimal = rawBedHour + (routine.weekday.bedMinute / 60f)
            val sleepNorm = ((bedDecimal - 21f) / 6f).coerceIn(0f, 1f)

            val totalTasks = (taskCount + completedTaskCount).coerceAtLeast(1)
            val taskNorm = (totalTasks / 8f).coerceIn(0.1f, 1f)

            val activityRatio = if (healthData != null && healthData.isDataAvailable) {
                val stepProgress = (healthData.currentSteps.toFloat() / healthData.stepGoal.coerceAtLeast(1)).coerceIn(0f, 1.2f)
                val activeProgress = (healthData.currentActiveMinutes.toFloat() / healthData.activeGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1.2f)
                ((stepProgress + activeProgress) / 2f).coerceIn(0.1f, 1f)
            } else {
                0.5f
            }

            val weekendWakeDecimal = routine.weekend.wakeHour + (routine.weekend.wakeMinute / 60f)
            val weekendShift = abs(weekendWakeDecimal - wakeDecimal)
            val weekendShiftNorm = (weekendShift / 3.0f).coerceIn(0f, 1f)

            val eveningRatio = computeEveningActivityRatio()
            val streakNorm = (streak / 7f).coerceIn(0.1f, 1f)

            val userVector = PersonaFeatureVector(
                wakeHour = wakeNorm,
                sleepHour = sleepNorm,
                dailyTasks = taskNorm,
                activityRatio = activityRatio,
                weekendShiftHours = weekendShiftNorm,
                eveningScreenRatio = eveningRatio,
                streakConsistency = streakNorm
            ).toArray()

            var bestType = UserPersonaType.BALANCED_DEFAULT
            var minDistance = Float.MAX_VALUE

            for ((type, centroid) in CENTROIDS) {
                val dist = euclideanDistance(userVector, centroid)
                if (dist < minDistance) {
                    minDistance = dist
                    bestType = type
                }
            }

            val similarity = ((1f - (minDistance / 1.8f)).coerceIn(0.62f, 0.98f) * 100).roundToInt()

            val finalType = if (routine.totalSampleCount < 3 && taskCount == 0 && (healthData == null || !healthData.isDataAvailable)) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (currentHour in 0..3) UserPersonaType.NIGHT_OWL else UserPersonaType.CORPORATE_PRO
            } else {
                bestType
            }

            val metadata = buildPersonaMetadata(finalType, routine, activityRatio, streak)

            prefs.edit()
                .putString("cached_persona_id", finalType.name)
                .putInt("cached_similarity", similarity)
                .apply()

            UserPersonaProfile(
                personaType = finalType,
                similarityPercent = similarity,
                keyTraits = metadata.traits,
                tailoredAdvice = metadata.advice,
                optimalFocusWindow = metadata.focusWindow,
                recommendedSleepWindow = metadata.sleepWindow,
                daysSampled = (routine.totalSampleCount / 2).coerceAtLeast(1)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying user persona: ${e.message}")
            getCachedPersonaProfile() ?: UserPersonaProfile()
        }
    }

    fun getCachedPersonaProfile(): UserPersonaProfile? {
        val savedName = prefs.getString("cached_persona_id", null) ?: return null
        val type = try {
            UserPersonaType.valueOf(savedName)
        } catch (_: Exception) {
            UserPersonaType.BALANCED_DEFAULT
        }
        val sim = prefs.getInt("cached_similarity", 85)
        val metadata = buildPersonaMetadata(type, RoutineInsight(), 0.5f, streakTracker.getStreakCount())
        return UserPersonaProfile(
            personaType = type,
            similarityPercent = sim,
            keyTraits = metadata.traits,
            tailoredAdvice = metadata.advice,
            optimalFocusWindow = metadata.focusWindow,
            recommendedSleepWindow = metadata.sleepWindow,
            daysSampled = 3
        )
    }

    private suspend fun computeEveningActivityRatio(): Float {
        return try {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
            val logs = database.routineDao().getLogsSince(sevenDaysAgo)
            if (logs.isEmpty()) return 0.45f
            val lateCount = logs.count { it.hourOfDay >= 22 || it.hourOfDay <= 3 }
            (lateCount.toFloat() / logs.size.toFloat()).coerceIn(0.1f, 0.95f)
        } catch (_: Exception) {
            0.45f
        }
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun buildPersonaMetadata(
        type: UserPersonaType,
        routine: RoutineInsight,
        activityRatio: Float,
        streak: Int
    ): PersonaMetadata {
        return when (type) {
            UserPersonaType.CORPORATE_PRO -> PersonaMetadata(
                traits = listOf(
                    "Wake up at ${routine.weekday.wakeTimeFormatted}",
                    "Structured schedule",
                    if (streak > 0) "${streak}-day streak" else "High discipline"
                ),
                advice = "Prioritize Deep Work from 09:00–11:00 before afternoon meetings. Take a 5-minute walk after lunch to avoid afternoon drowsiness.",
                focusWindow = "08:30 – 11:30 & 14:00 – 16:00",
                sleepWindow = "22:45 – 06:45"
            )
            UserPersonaType.ACTIVE_STUDENT -> PersonaMetadata(
                traits = listOf(
                    "Flexible schedule",
                    "High energy",
                    "Weekend sleep-in"
                ),
                advice = "Apply the 25/5 Pomodoro method while studying. Avoid sleeping in more than 90 minutes late on weekends to prevent Monday morning fatigue.",
                focusWindow = "09:30 – 12:00 & 19:30 – 22:00",
                sleepWindow = "23:30 – 07:15"
            )
            UserPersonaType.FLEX_FREELANCER -> PersonaMetadata(
                traits = listOf(
                    "Self-directed time",
                    "Multi-session focus",
                    "Creative pace"
                ),
                advice = "Split your day into 2 clear Deep Work blocks and set a hard cutoff time in the evening to avoid burnout.",
                focusWindow = "10:00 – 12:30 & 15:00 – 18:00",
                sleepWindow = "23:30 – 07:30"
            )
            UserPersonaType.WELLNESS_ATHLETE -> PersonaMetadata(
                traits = listOf(
                    "Active (${(activityRatio * 100).toInt()}%)",
                    "Early riser",
                    "Balanced recovery"
                ),
                advice = "Your physical fitness is excellent! Add electrolytes on hot days and maintain 7.5 hours of sleep for optimal muscle recovery.",
                focusWindow = "07:30 – 11:00",
                sleepWindow = "22:00 – 06:00"
            )
            UserPersonaType.NIGHT_OWL -> PersonaMetadata(
                traits = listOf(
                    "Active late at night",
                    "Nighttime focus",
                    "Need morning recovery"
                ),
                advice = "You hit peak focus at night, but enable Blue Light Filter after 22:30 and shift your bedtime 20 minutes earlier each day.",
                focusWindow = "15:00 – 18:00 & 21:00 – 23:30",
                sleepWindow = "00:15 – 07:45"
            )
            UserPersonaType.BALANCED_DEFAULT -> PersonaMetadata(
                traits = listOf(
                    "Stable bio-rhythm",
                    "Balanced work & rest"
                ),
                advice = "Maintain a consistent wake-up time and complete your 3 most important tasks before noon.",
                focusWindow = "08:30 – 11:30",
                sleepWindow = "22:45 – 06:45"
            )
        }
    }

    fun resetData() {
        prefs.edit().clear().apply()
    }
}
