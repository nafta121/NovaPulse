package com.example.core.ml

import android.content.Context
import android.util.Log
import com.example.core.ai.LifestyleTip
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class LifestyleContext(
    val hourOfDay: Int,
    val isWeekend: Boolean,
    val temperatureC: Int,
    val isRainy: Boolean,
    val dayOfWeek: Int,
    val streakDays: Int,
    val personaType: UserPersonaType = UserPersonaType.BALANCED_DEFAULT
) {
    fun toFeatureVector(): FloatArray {
        val angle = 2.0 * Math.PI * (hourOfDay.toDouble() / 24.0)
        val hourSin = ((sin(angle) + 1.0) / 2.0).toFloat()
        val hourCos = ((cos(angle) + 1.0) / 2.0).toFloat()
        val weekendVal = if (isWeekend) 1.0f else 0.0f
        val tempNorm = ((temperatureC - 15).coerceIn(0, 25) / 25.0f)
        val rainVal = if (isRainy) 1.0f else 0.0f
        val streakNorm = (streakDays.coerceIn(0, 14) / 14.0f)

        val personaVal = when (personaType) {
            UserPersonaType.WELLNESS_ATHLETE -> 1.0f
            UserPersonaType.CORPORATE_PRO -> 0.8f
            UserPersonaType.FLEX_FREELANCER -> 0.6f
            UserPersonaType.ACTIVE_STUDENT -> 0.4f
            UserPersonaType.NIGHT_OWL -> 0.2f
            UserPersonaType.BALANCED_DEFAULT -> 0.5f
        }

        return floatArrayOf(
            1.0f,
            hourSin,
            hourCos,
            weekendVal,
            tempNorm,
            rainVal,
            streakNorm,
            personaVal
        )
    }

    companion object {
        const val FEATURE_DIM = 8

        fun fromCurrentState(
            hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            temperatureC: Int = 27,
            isRainy: Boolean = false,
            streakDays: Int = 0,
            personaType: UserPersonaType = UserPersonaType.BALANCED_DEFAULT
        ): LifestyleContext {
            val cal = Calendar.getInstance()
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
            return LifestyleContext(
                hourOfDay = hourOfDay,
                isWeekend = isWeekend,
                temperatureC = temperatureC,
                isRainy = isRainy,
                dayOfWeek = dow,
                streakDays = streakDays,
                personaType = personaType
            )
        }
    }
}

class ContextualLifestyleBandit(context: Context) {

    companion object {
        private const val TAG = "ContextualBandit"
        private const val PREFS_NAME = "contextual_lifestyle_bandit_v2"
        private const val D = LifestyleContext.FEATURE_DIM
        private const val ALPHA = 0.6f
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyLearner = LifestylePreferenceLearner(context)

    fun recordInteraction(
        itemTitle: String,
        category: String,
        context: LifestyleContext,
        reward: Float = 1.0f
    ) {
        legacyLearner.recordInteraction(itemTitle, category, context.hourOfDay, reward)

        val x = context.toFeatureVector()
        val armKey = normalizeCategoryKey(category)
        val itemHash = itemTitle.trim().lowercase().hashCode()

        val editor = prefs.edit()
        for (i in 0 until D) {
            val aKey = "arm_${armKey}_A_$i"
            val bKey = "arm_${armKey}_b_$i"

            val oldA = prefs.getFloat(aKey, 1.0f)
            val oldB = prefs.getFloat(bKey, 0.0f)

            val xi = x[i]
            editor.putFloat(aKey, oldA + (xi * xi))
            editor.putFloat(bKey, oldB + (reward * xi))
        }

        val itemCountKey = "ctx_item_${itemHash}_count"
        val itemRewardKey = "ctx_item_${itemHash}_reward"
        val totalKey = "ctx_total_pulls"

        val itemCount = prefs.getInt(itemCountKey, 0) + 1
        val itemReward = prefs.getFloat(itemRewardKey, 0f) + reward
        val totalPulls = prefs.getInt(totalKey, 0) + 1

        editor.putInt(itemCountKey, itemCount)
        editor.putFloat(itemRewardKey, itemReward)
        editor.putInt(totalKey, totalPulls)
        editor.apply()

        Log.d(TAG, "LinUCB updated arm='$armKey' for '$itemTitle' (totalPulls=$totalPulls)")
    }

    fun calculateScore(
        itemTitle: String,
        category: String,
        context: LifestyleContext,
        baseOrderScore: Float = 0.5f
    ): Float {
        val totalPulls = prefs.getInt("ctx_total_pulls", 0)
        val x = context.toFeatureVector()
        val armKey = normalizeCategoryKey(category)

        var expectedReward = 0f
        var varianceSum = 0f

        for (i in 0 until D) {
            val aDiag = prefs.getFloat("arm_${armKey}_A_$i", 1.0f).coerceAtLeast(1.0f)
            val bVal = prefs.getFloat("arm_${armKey}_b_$i", 0.0f)
            val thetaI = bVal / aDiag
            val xi = x[i]

            expectedReward += thetaI * xi
            varianceSum += (xi * xi) / aDiag
        }

        val uncertaintyBonus = ALPHA * sqrt(varianceSum.coerceAtLeast(0f))
        val linUcbCategoryScore = expectedReward + uncertaintyBonus

        val itemHash = itemTitle.trim().lowercase().hashCode()
        val itemCount = prefs.getInt("ctx_item_${itemHash}_count", 0)
        val itemReward = prefs.getFloat("ctx_item_${itemHash}_reward", 0f)

        val itemBonus = if (itemCount > 0 && totalPulls > 1) {
            (itemReward / itemCount) * 0.45f + 0.25f * sqrt(ln(totalPulls.toDouble()) / itemCount.toDouble()).toFloat()
        } else {
            0.15f
        }

        val domainPrior = computeDomainPrior(category, itemTitle, context)

        return (linUcbCategoryScore * 0.45f) +
                (itemBonus * 0.25f) +
                (domainPrior * 0.20f) +
                (baseOrderScore * 0.10f)
    }

    fun rankLifestyleTips(
        tips: List<LifestyleTip>,
        context: LifestyleContext
    ): List<LifestyleTip> {
        if (tips.size <= 1) return tips

        return tips.mapIndexed { index, tip ->
            val baseOrder = 1.0f - (index * 0.06f)
            tip to calculateScore(tip.title, tip.category, context, baseOrder)
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun generateSessionLeadText(
        context: LifestyleContext,
        weatherDesc: String = "cool"
    ): String {
        val totalPulls = prefs.getInt("ctx_total_pulls", 0)
        if (totalPulls < 2) {
            return legacyLearner.generateSessionLeadText(
                currentHour = context.hourOfDay,
                tempC = context.temperatureC,
                weatherDesc = weatherDesc
            )
        }

        val categories = listOf("Food & Dining", "Fitness", "Travel", "Relaxation", "Lifestyle")
        val topCategory = categories.maxByOrNull { cat ->
            calculateScore(itemTitle = "", category = cat, context = context, baseOrderScore = 0.5f)
        } ?: "Lifestyle"

        val weekendTag = if (context.isWeekend) "Weekend" else "Today"
        val tempStr = "${context.temperatureC}°C ($weatherDesc)"

        return when (topCategory) {
            "Fitness" -> "⚡ $weekendTag $tempStr — AI prioritizes activity & energy suggestions tailored to your rhythm:"
            "Food & Dining" -> "🍲 $weekendTag $tempStr — Suggested dining & nutrition spots suited to your taste:"
            "Relaxation" -> "🌿 $weekendTag $tempStr — Prioritizing relaxation & mental recovery activities:"
            "Travel" -> "✨ $weekendTag $tempStr — Ideal weather for an outing or a change of scenery:"
            else -> "🎯 $weekendTag $tempStr — Personalized suggestions based on your habits & weather:"
        }
    }

    private fun computeDomainPrior(
        category: String,
        title: String,
        ctx: LifestyleContext
    ): Float {
        var bonus = 0f
        val normTitle = title.lowercase()

        if (ctx.isRainy) {
            if (category == "Relaxation" || category == "Food & Dining") bonus += 0.35f
            if (normTitle.contains("run") || normTitle.contains("park") || normTitle.contains("outdoor")) {
                bonus -= 0.45f
            }
        }

        if (ctx.temperatureC >= 33) {
            if (normTitle.contains("swim") || normTitle.contains("smoothie") || normTitle.contains("cool") || category == "Relaxation") {
                bonus += 0.25f
            }
        }

        if (ctx.isWeekend && (category == "Travel" || category == "Food & Dining")) {
            bonus += 0.25f
        }

        when (ctx.personaType) {
            UserPersonaType.WELLNESS_ATHLETE -> if (category == "Fitness") bonus += 0.35f
            UserPersonaType.CORPORATE_PRO -> if (ctx.hourOfDay in 7..9 && category == "Food & Dining") bonus += 0.25f
            UserPersonaType.NIGHT_OWL -> if (ctx.hourOfDay >= 20 && category == "Relaxation") bonus += 0.30f
            else -> {}
        }

        return bonus
    }

    private fun normalizeCategoryKey(category: String): String {
        return when {
            category.contains("Food", ignoreCase = true) || category.contains("Dining", ignoreCase = true) -> "FOOD"
            category.contains("Fitness", ignoreCase = true) || category.contains("Workout", ignoreCase = true) -> "FITNESS"
            category.contains("Travel", ignoreCase = true) -> "TRAVEL"
            category.contains("Relax", ignoreCase = true) -> "RELAX"
            else -> "LIFESTYLE"
        }
    }

    fun resetData() {
        prefs.edit().clear().apply()
        legacyLearner.resetData()
    }
}
