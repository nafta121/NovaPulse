package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.LifestyleTip
import com.example.core.engine.UserFeedbackTracker
import com.example.core.ml.BehaviorPrediction
import com.example.core.ml.InternalMlEngine
import com.example.core.ml.LifestyleContext
import com.example.core.ml.TrendDirection
import com.example.core.ml.UserPersonaType
import com.example.core.repository.RealHealthData
import com.example.core.repository.RealWeatherData
import java.util.Calendar

enum class SuggestionVisualType {
    COFFEE_CUP,      // 3D Peach / Amber Clay Coffee Cup with rising steam
    TEA_POT,         // 3D Warm Honey Chamomile Teapot / Cup
    BICYCLE,         // 3D Mint Green / Emerald Clay Bicycle
    TENT_CAMPING,    // 3D Coral / Pastel Yellow Camping Tent with warm light
    HOTPOT_BOWL,     // 3D Hotpot / Ramen bowl with steaming broth
    YOGA_MEDITATION, // 3D Lotus / Yoga stretch with soothing aura ring
    ROOFTOP_CHILL    // 3D Sunset Cocktail overlooking the city skyline
}

data class LocalAiSuggestion(
    val id: String,
    val category: String, // "Sports", "Food & Dining", "Travel"
    val categoryBadgeColor: Color,
    val categoryTextColor: Color,
    val title: String,
    val description: String,
    val tag: String,
    val visualType: SuggestionVisualType
)

fun generateLocalAiSuggestions(
    currentHour: Int,
    weatherData: RealWeatherData?,
    calendar: Calendar = Calendar.getInstance()
): List<LocalAiSuggestion> {
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val isWeekend = dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    val temp = weatherData?.currentTempC?.let { Math.round(it).toInt() } ?: if (currentHour in 6..17) 29 else 24
    val condition = weatherData?.conditionDescription?.lowercase() ?: ""
    val isRaining = weatherData?.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99) ||
            condition.contains("rain", ignoreCase = true) || condition.contains("storm", ignoreCase = true) || condition.contains("thunder", ignoreCase = true)

    val suggestions = mutableListOf<LocalAiSuggestion>()

    val isCoolSportTime = (currentHour in 5..7 || currentHour in 17..18) && !isRaining && temp <= 29
    if (isCoolSportTime) {
        suggestions.add(
            LocalAiSuggestion(
                id = "sport_run_bike",
                category = "Sports",
                categoryBadgeColor = Color(0xFF10B981).copy(alpha = 0.20f),
                categoryTextColor = Color(0xFF10B981),
                title = if (currentHour in 5..7) "Early Morning Run & Breathwork" else "Evening Park Cycling",
                description = "Cool $temp°C weather is ideal for a 5km run or a refreshing bike ride outdoors.",
                tag = "Cool $temp°C",
                visualType = SuggestionVisualType.BICYCLE
            )
        )
    }

    val foodSuggestion = when {
        isRaining -> LocalAiSuggestion(
            id = "food_rain_hotpot",
            category = "Food & Dining",
            categoryBadgeColor = Color(0xFFEA580C).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFEA580C),
            title = "Warm Hotpot & Grilled Dishes",
            description = "Cool rainy weather ($temp°C)—gather around a steaming hotpot, BBQ, or a warm bowl of rib porridge.",
            tag = "Rainy $temp°C",
            visualType = SuggestionVisualType.HOTPOT_BOWL
        )

        currentHour in 5..10 -> LocalAiSuggestion(
            id = "food_morning_coffee",
            category = "Food & Dining",
            categoryBadgeColor = Color(0xFFD97706).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFD97706),
            title = "Iced Milk Coffee & Crispy Banh Mi",
            description = "Kickstart your morning with rich roasted coffee and a warm crispy baguette or fragrant beef pho.",
            tag = "Breakfast",
            visualType = SuggestionVisualType.COFFEE_CUP
        )

        currentHour in 11..14 && temp >= 30 -> LocalAiSuggestion(
            id = "food_noon_refresh",
            category = "Food & Dining",
            categoryBadgeColor = Color(0xFF06B6D4).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF06B6D4),
            title = "Fresh Coconut Water & Light Lunch",
            description = "Sunny midday heat ($temp°C)—recharge with fresh coconut water, fruit tea, and a light cooling lunch.",
            tag = "Cooling",
            visualType = SuggestionVisualType.ROOFTOP_CHILL
        )

        currentHour in 11..14 -> LocalAiSuggestion(
            id = "food_noon_lunch",
            category = "Food & Dining",
            categoryBadgeColor = Color(0xFF059669).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF059669),
            title = "Nutritious Lunch & Fresh Salad",
            description = "Sustain your afternoon energy with lean protein and plenty of fresh greens.",
            tag = "Lunch",
            visualType = SuggestionVisualType.HOTPOT_BOWL
        )

        else -> LocalAiSuggestion(
            id = "food_evening_dinner",
            category = "Food & Dining",
            categoryBadgeColor = Color(0xFF8B5CF6).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF8B5CF6),
            title = "Light Dinner & Chamomile Tea",
            description = "Avoid heavy meals after 19:30 and sip warm chamomile tea to ease into deep sleep.",
            tag = "Evening Wind-down",
            visualType = SuggestionVisualType.TEA_POT
        )
    }
    suggestions.add(foodSuggestion)

    val travelSuggestion = if (isWeekend) {
        LocalAiSuggestion(
            id = "travel_weekend_picnic",
            category = "Travel",
            categoryBadgeColor = Color(0xFF3B82F6).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF3B82F6),
            title = "Park Picnic & Weekend Camping",
            description = "Great weekend weather—plan a park picnic, suburban camping trip, or a chill cafe outing with friends.",
            tag = "Weekend",
            visualType = SuggestionVisualType.TENT_CAMPING
        )
    } else {
        LocalAiSuggestion(
            id = "travel_chill_spot",
            category = "Travel",
            categoryBadgeColor = Color(0xFFEC4899).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFEC4899),
            title = "Sunset Rooftop Lounge",
            description = "Unwind after work in an open-air rooftop space and watch the city lights come alive.",
            tag = "Explore",
            visualType = SuggestionVisualType.ROOFTOP_CHILL
        )
    }
    suggestions.add(travelSuggestion)

    if (suggestions.none { it.category == "Sports" }) {
        suggestions.add(
            LocalAiSuggestion(
                id = "sport_indoor_stretch",
                category = "Sports",
                categoryBadgeColor = Color(0xFF10B981).copy(alpha = 0.20f),
                categoryTextColor = Color(0xFF10B981),
                title = "15-Min Stretching & Light Yoga",
                description = "Do joint rotations and spinal stretches to relieve tension after sitting at your desk.",
                tag = "Recovery",
                visualType = SuggestionVisualType.YOGA_MEDITATION
            )
        )
    }

    return suggestions
}

@Composable
fun ClayCoffeeCupSticker(
    isTea: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 4f

        drawOval(
            color = Color(0x38000000),
            topLeft = Offset(cx - 20f, h - 8f),
            size = Size(40f, 8f)
        )

        val handleBrush = Brush.radialGradient(
            colors = if (isTea) listOf(Color(0xFFFDE047), Color(0xFFD97706)) else listOf(
                Color(0xFFFFB74D),
                Color(0xFFE65100)
            ),
            center = Offset(cx + 18f, cy - 2f),
            radius = 16f
        )
        drawArc(
            brush = handleBrush,
            startAngle = -70f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + 4f, cy - 14f),
            size = Size(22f, 24f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        val cupPath = Path().apply {
            moveTo(cx - 18f, cy - 12f)
            lineTo(cx + 18f, cy - 12f)
            quadraticTo(cx + 18f, cy + 16f, cx + 10f, cy + 18f)
            lineTo(cx - 10f, cy + 18f)
            quadraticTo(cx - 18f, cy + 16f, cx - 18f, cy - 12f)
            close()
        }
        val cupBrush = Brush.radialGradient(
            colors = if (isTea) {
                listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
            } else {
                listOf(Color(0xFFFFCC80), Color(0xFFFF9800), Color(0xFFE65100))
            },
            center = Offset(cx - 6f, cy - 6f),
            radius = 28f
        )
        drawPath(path = cupPath, brush = cupBrush)

        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 160f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(cx - 16f, cy - 8f),
            size = Size(32f, 22f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = if (isTea) listOf(Color(0xFFFDE68A), Color(0xFFD97706)) else listOf(
                    Color(0xFFD7CCC8),
                    Color(0xFF4E342E)
                ),
                center = Offset(cx, cy - 12f),
                radius = 16f
            ),
            topLeft = Offset(cx - 18f, cy - 18f),
            size = Size(36f, 12f)
        )

        if (isTea) {
            drawCircle(color = Color(0xFFFEF08A), radius = 3.5f, center = Offset(cx, cy - 12f))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2f, center = Offset(cx - 3.5f, cy - 12f))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2f, center = Offset(cx + 3.5f, cy - 12f))
        } else {
            drawCircle(color = Color(0xFFFFF8E1).copy(alpha = 0.80f), radius = 4f, center = Offset(cx - 2f, cy - 12f))
        }

        drawCircle(color = Color.White.copy(alpha = 0.60f), radius = 3f, center = Offset(cx - 4f, cy - 24f))
        drawCircle(color = Color.White.copy(alpha = 0.45f), radius = 3.8f, center = Offset(cx + 4f, cy - 30f))
    }
}

@Composable
fun ClayBicycleSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        drawOval(color = Color(0x38000000), topLeft = Offset(cx - 22f, h - 8f), size = Size(44f, 8f))

        val mintLight = Color(0xFF6EE7B7)
        val mintMid = Color(0xFF10B981)
        val mintDark = Color(0xFF047857)

        val rearWheelCenter = Offset(cx - 14f, cy + 6f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)), center = rearWheelCenter, radius = 10f),
            radius = 9.5f,
            center = rearWheelCenter
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(mintLight, mintMid), center = rearWheelCenter, radius = 6f),
            radius = 5.5f,
            center = rearWheelCenter
        )
        drawCircle(color = Color.White, radius = 1.8f, center = rearWheelCenter)

        val frontWheelCenter = Offset(cx + 14f, cy + 6f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)), center = frontWheelCenter, radius = 10f),
            radius = 9.5f,
            center = frontWheelCenter
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(mintLight, mintMid), center = frontWheelCenter, radius = 6f),
            radius = 5.5f,
            center = frontWheelCenter
        )
        drawCircle(color = Color.White, radius = 1.8f, center = frontWheelCenter)

        val framePath = Path().apply {
            moveTo(rearWheelCenter.x, rearWheelCenter.y)
            lineTo(cx - 2f, cy + 6f)
            lineTo(cx + 8f, cy - 6f)
            lineTo(frontWheelCenter.x, frontWheelCenter.y)
            moveTo(cx - 2f, cy + 6f)
            lineTo(cx - 6f, cy - 6f)
            lineTo(rearWheelCenter.x, rearWheelCenter.y)
            moveTo(cx - 6f, cy - 6f)
            lineTo(cx + 8f, cy - 6f)
        }
        drawPath(
            path = framePath,
            brush = Brush.linearGradient(listOf(mintLight, mintDark)),
            style = Stroke(width = 3.6f, cap = StrokeCap.Round)
        )

        drawOval(color = Color(0xFF1E293B), topLeft = Offset(cx - 10f, cy - 10f), size = Size(9f, 4.5f))

        drawArc(
            color = Color(0xFF047857),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx + 6f, cy - 12f),
            size = Size(8f, 7f),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ClayTentSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 4f

        drawOval(color = Color(0x38000000), topLeft = Offset(cx - 22f, h - 8f), size = Size(44f, 8f))

        val tentPath = Path().apply {
            moveTo(cx, cy - 18f)
            lineTo(cx - 20f, cy + 14f)
            quadraticTo(cx, cy + 17f, cx + 20f, cy + 14f)
            close()
        }
        val tentBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFDA4AF), Color(0xFFFB7185), Color(0xFFE11D48)),
            center = Offset(cx - 6f, cy - 4f),
            radius = 26f
        )
        drawPath(path = tentPath, brush = tentBrush)

        val flapPath = Path().apply {
            moveTo(cx, cy - 18f)
            lineTo(cx + 20f, cy + 14f)
            lineTo(cx + 5f, cy + 15f)
            close()
        }
        drawPath(path = flapPath, brush = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B))))

        val doorPath = Path().apply {
            moveTo(cx - 2f, cy - 4f)
            lineTo(cx - 10f, cy + 15f)
            lineTo(cx + 6f, cy + 15f)
            close()
        }
        drawPath(
            path = doorPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFF78350F)),
                center = Offset(cx - 2f, cy + 10f),
                radius = 12f
            )
        )

        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(cx, cy - 18f),
            end = Offset(cx, cy - 26f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        val flagPath = Path().apply {
            moveTo(cx, cy - 26f)
            lineTo(cx + 9f, cy - 23f)
            lineTo(cx, cy - 20f)
            close()
        }
        drawPath(path = flagPath, color = Color(0xFF38BDF8))
    }
}

@Composable
fun ClayHotpotSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 4f

        drawOval(color = Color(0x38000000), topLeft = Offset(cx - 20f, h - 8f), size = Size(40f, 8f))
        drawCircle(color = Color(0xFFEA580C), radius = 4.5f, center = Offset(cx - 18f, cy - 2f))
        drawCircle(color = Color(0xFFEA580C), radius = 4.5f, center = Offset(cx + 18f, cy - 2f))

        val potPath = Path().apply {
            moveTo(cx - 18f, cy - 6f)
            lineTo(cx + 18f, cy - 6f)
            quadraticTo(cx + 16f, cy + 16f, cx, cy + 17f)
            quadraticTo(cx - 16f, cy + 16f, cx - 18f, cy - 6f)
            close()
        }
        drawPath(
            path = potPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF9A3412)),
                center = Offset(cx - 6f, cy),
                radius = 24f
            )
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFEA580C)),
                center = Offset(cx, cy - 6f),
                radius = 16f
            ),
            topLeft = Offset(cx - 18f, cy - 12f),
            size = Size(36f, 12f)
        )

        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 3.5f, center = Offset(cx - 4f, cy - 22f))
        drawCircle(color = Color.White.copy(alpha = 0.40f), radius = 4.2f, center = Offset(cx + 5f, cy - 28f))
    }
}

@Composable
fun ClayYogaSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        drawOval(color = Color(0x38000000), topLeft = Offset(cx - 18f, h - 8f), size = Size(36f, 8f))
        drawCircle(color = Color(0xFFC084FC).copy(alpha = 0.25f), radius = 18f, center = Offset(cx, cy))

        val lotusPath = Path().apply {
            moveTo(cx, cy - 16f)
            quadraticTo(cx + 12f, cy - 2f, cx + 8f, cy + 12f)
            quadraticTo(cx, cy + 14f, cx - 8f, cy + 12f)
            quadraticTo(cx - 12f, cy - 2f, cx, cy - 16f)
            close()
        }
        drawPath(
            path = lotusPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE9D5FF), Color(0xFFA855F7), Color(0xFF6B21A8)),
                center = Offset(cx, cy - 2f),
                radius = 18f
            )
        )

        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFD8B4FE), Color(0xFF9333EA))),
            topLeft = Offset(cx - 16f, cy + 2f),
            size = Size(12f, 9f)
        )
        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFD8B4FE), Color(0xFF9333EA))),
            topLeft = Offset(cx + 4f, cy + 2f),
            size = Size(12f, 9f)
        )

        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 2.5f, center = Offset(cx, cy - 4f))
    }
}

@Composable
fun ClayRooftopSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        drawOval(color = Color(0x38000000), topLeft = Offset(cx - 16f, h - 8f), size = Size(32f, 8f))

        val glassPath = Path().apply {
            moveTo(cx - 14f, cy - 10f)
            lineTo(cx + 14f, cy - 10f)
            lineTo(cx, cy + 8f)
            close()
        }
        drawPath(
            path = glassPath,
            brush = Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFF59E0B), Color(0xFF06B6D4)))
        )

        drawLine(color = Color.White.copy(alpha = 0.8f), start = Offset(cx, cy + 8f), end = Offset(cx, cy + 16f), strokeWidth = 2.5f)
        drawLine(color = Color.White.copy(alpha = 0.8f), start = Offset(cx - 7f, cy + 16f), end = Offset(cx + 7f, cy + 16f), strokeWidth = 2.5f)

        drawCircle(color = Color(0xFFFEF08A), radius = 5.5f, center = Offset(cx + 12f, cy - 12f))
        drawCircle(color = Color(0xFFF59E0B), radius = 4f, center = Offset(cx + 12f, cy - 12f))

        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(cx - 8f, cy - 20f),
            end = Offset(cx - 1f, cy),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ClaymorphismSuggestionVisual(
    visualType: SuggestionVisualType,
    modifier: Modifier = Modifier
) {
    when (visualType) {
        SuggestionVisualType.COFFEE_CUP -> ClayCoffeeCupSticker(isTea = false, modifier = modifier)
        SuggestionVisualType.TEA_POT -> ClayCoffeeCupSticker(isTea = true, modifier = modifier)
        SuggestionVisualType.BICYCLE -> ClayBicycleSticker(modifier = modifier)
        SuggestionVisualType.TENT_CAMPING -> ClayTentSticker(modifier = modifier)
        SuggestionVisualType.HOTPOT_BOWL -> ClayHotpotSticker(modifier = modifier)
        SuggestionVisualType.YOGA_MEDITATION -> ClayYogaSticker(modifier = modifier)
        SuggestionVisualType.ROOFTOP_CHILL -> ClayRooftopSticker(modifier = modifier)
    }
}

@Composable
private fun VerticalLifestyleSuggestionStack(
    lifestyleTips: List<LifestyleTip>,
    suggestions: List<LocalAiSuggestion>,
    isAiInferring: Boolean,
    isDark: Boolean,
    onRefreshTips: () -> Unit,
    onToggleDone: (String) -> Unit,
    onSuggestionPositive: (String) -> Unit = {},
    onSuggestionDismiss: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "Lifestyle suggestions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 14.sp
                    )
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRefreshTips()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Shuffle suggestions",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Shuffle",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isAiInferring) {
            ShimmerLifestyleTipCard(isDark = isDark)
            ShimmerLifestyleTipCard(isDark = isDark)
        } else if (lifestyleTips.isNotEmpty()) {
            lifestyleTips.take(3).forEach { tip ->
                LifestyleTipCard(
                    tip = tip,
                    isDark = isDark,
                    onToggleDone = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleDone(tip.id)
                    }
                )
            }
        } else {
            suggestions.take(3).forEach { suggestion ->
                SuggestionCarouselCard(
                    suggestion = suggestion,
                    isDark = isDark,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSuggestionPositive(suggestion.category)
                    },
                    onDismiss = {
                        onSuggestionDismiss(suggestion.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShimmerLifestyleTipCard(isDark: Boolean) {
    val transition = rememberInfiniteTransition(label = "shimmer_card")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1E293B).copy(alpha = 0.6f),
            Color(0xFF334155).copy(alpha = 0.85f),
            Color(0xFF1E293B).copy(alpha = 0.6f)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0).copy(alpha = 0.7f),
            Color(0xFFF8FAFC).copy(alpha = 1.0f),
            Color(0xFFE2E8F0).copy(alpha = 0.7f)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.40f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(65.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
        }
    }
}

@Composable
private fun LifestyleTipCard(
    tip: LifestyleTip,
    isDark: Boolean,
    onToggleDone: () -> Unit
) {
    val badgeBg = when (tip.category) {
        "Food & Dining" -> Color(0xFFFEF3C7)
        "Beverages" -> Color(0xFFE0E7FF)
        "Sports", "Fitness" -> Color(0xFFDCFCE7)
        "Travel" -> Color(0xFFDBEAFE)
        "Relaxation" -> Color(0xFFF3E8FF)
        "Wellness" -> Color(0xFFCCFBF1)
        "Productivity" -> Color(0xFFFEE2E2)
        else -> Color(0xFFE0F2FE)
    }
    val badgeTxt = when (tip.category) {
        "Food & Dining" -> Color(0xFFD97706)
        "Beverages" -> Color(0xFF4F46E5)
        "Sports", "Fitness" -> Color(0xFF16A34A)
        "Travel" -> Color(0xFF2563EB)
        "Relaxation" -> Color(0xFF9333EA)
        "Wellness" -> Color(0xFF0F766E)
        "Productivity" -> Color(0xFFDC2626)
        else -> Color(0xFF0284C7)
    }

    OneUIGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleDone() }
            .testTag("lifestyle_card_${tip.id}"),
        shape = RoundedCornerShape(22.dp),
        isDark = isDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(badgeBg.copy(alpha = if (isDark) 0.35f else 0.85f))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = tip.category,
                            color = badgeTxt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = tip.tag,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = tip.title,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (tip.isDone) TextDecoration.LineThrough else null
                )

                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.25f else 0.85f),
                                badgeBg.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = if (isDark) 0.35f else 0.80f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (tip.isDone) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(text = tip.emoji, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun SuggestionCarouselCard(
    suggestion: LocalAiSuggestion,
    isDark: Boolean,
    onClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(284.dp)
            .clickable { onClick() }
            .testTag("suggestion_card_${suggestion.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.50f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(suggestion.categoryBadgeColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = suggestion.category,
                                color = suggestion.categoryTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(
                                        alpha = 0.05f
                                    )
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = suggestion.tag,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Text(
                    text = suggestion.title,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = suggestion.description,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ClaymorphismSuggestionVisual(
                visualType = suggestion.visualType,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSuggestionWidgetsSection(
    currentHour: Int,
    weatherData: RealWeatherData?,
    lifestyleTips: List<LifestyleTip>,
    isAiInferring: Boolean,
    onToggleTipDone: (String) -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val mlEngine = remember { InternalMlEngine.getInstance(context) }
    val feedbackTracker = remember { UserFeedbackTracker(context) }
    val isDaytime = currentHour in 6..17
    var selectedMindfulnessType by remember { mutableStateOf<String?>(null) }
    var dismissedSuggestionIds by remember { mutableStateOf(setOf<String>()) }
    var leadTextRefreshKey by remember { mutableStateOf(0) }
    var shuffleOffset by remember { mutableStateOf(0) }

    val tempInt = weatherData?.currentTempC?.toInt() ?: 29
    val weatherDesc = weatherData?.conditionDescription ?: "cool"
    val isRainy = weatherData?.conditionDescription?.let {
        it.contains("rain", ignoreCase = true) || it.contains("drizzle", ignoreCase = true) || it.contains("shower", ignoreCase = true)
    } ?: false

    val cachedPersona = remember {
        mlEngine.getCachedPersonaProfile()?.personaType ?: UserPersonaType.BALANCED_DEFAULT
    }
    val lifestyleContext = remember(currentHour, tempInt, isRainy, leadTextRefreshKey, cachedPersona) {
        LifestyleContext.fromCurrentState(
            hourOfDay = currentHour,
            temperatureC = tempInt,
            isRainy = isRainy,
            streakDays = mlEngine.streakTracker.getStreakCount(),
            personaType = cachedPersona
        )
    }

    val aiLeadText = remember(lifestyleContext, weatherDesc, leadTextRefreshKey) {
        mlEngine.generateContextualLeadText(lifestyleContext, weatherDesc)
    }

    val rankedLifestyleTips = remember(lifestyleTips, lifestyleContext, leadTextRefreshKey, shuffleOffset) {
        val ranked = mlEngine.rankLifestyleTipsContextual(lifestyleTips, lifestyleContext)
        val offset = shuffleOffset
        if (ranked.size > 3 && offset > 0) {
            val shift = offset % ranked.size
            ranked.drop(shift) + ranked.take(shift)
        } else {
            ranked
        }
    }

    val localAiSuggestions = remember(currentHour, weatherData, dismissedSuggestionIds, shuffleOffset) {
        val base = generateLocalAiSuggestions(currentHour, weatherData)
        val filtered = base.filter { it.id !in dismissedSuggestionIds }
            .sortedByDescending { feedbackTracker.getSuggestionWeight(it.category) }
        val offset = shuffleOffset
        if (filtered.size > 3 && offset > 0) {
            val shift = offset % filtered.size
            filtered.drop(shift) + filtered.take(shift)
        } else {
            filtered
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = aiLeadText,
            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        VerticalLifestyleSuggestionStack(
            modifier = Modifier.fillMaxWidth(),
            lifestyleTips = rankedLifestyleTips,
            suggestions = localAiSuggestions,
            isAiInferring = isAiInferring,
            onRefreshTips = {
                shuffleOffset += 2
                leadTextRefreshKey++
            },
            onToggleDone = { tipId ->
                val clickedTip = rankedLifestyleTips.firstOrNull { it.id == tipId }
                if (clickedTip != null) {
                    mlEngine.recordLifestyleClickContextual(
                        clickedTip.title, clickedTip.category, lifestyleContext
                    )
                    leadTextRefreshKey++
                }
                onToggleTipDone(tipId)
            },
            onSuggestionPositive = { category ->
                feedbackTracker.recordSuggestionPositive(category)
                mlEngine.recordLifestyleClickContextual(category, category, lifestyleContext)
                leadTextRefreshKey++
            },
            onSuggestionDismiss = { id ->
                val dismissedItem = localAiSuggestions.firstOrNull { it.id == id }
                if (dismissedItem != null) {
                    feedbackTracker.recordSuggestionDismiss(dismissedItem.category)
                }
                dismissedSuggestionIds = dismissedSuggestionIds + id
            },
            isDark = isDark
        )

        if (isDaytime) {
            MindfulnessMiniCardsRow(
                isDark = isDark,
                onSelect = { selectedMindfulnessType = it }
            )
        } else {
            MorningAlarmWidget(isDark = isDark)
            MindfulnessMiniCardsRow(
                isDark = isDark,
                onSelect = { selectedMindfulnessType = it }
            )
        }
    }

    if (selectedMindfulnessType != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        when (selectedMindfulnessType) {
            "mood" -> MoodTrackerBottomSheet(
                onDismiss = { selectedMindfulnessType = null },
                onSaveMood = { _, _ -> },
                sheetState = sheetState,
                isNight = isDark
            )
            "breath" -> BreathingBottomSheet(
                onDismiss = { selectedMindfulnessType = null },
                sheetState = sheetState,
                isNight = isDark
            )
            "meditate" -> MeditationTimerBottomSheet(
                onDismiss = { selectedMindfulnessType = null },
                sheetState = sheetState,
                totalTimeSeconds = 300,
                isNight = isDark
            )
        }
    }
}

@Composable
private fun MorningAlarmWidget(isDark: Boolean) {
    var isAlarmEnabled by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("morning_alarm_widget"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Alarm",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "06:30",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tomorrow morning",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "First event at 08:30 • In 7h 45m",
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isAlarmEnabled,
                onCheckedChange = { isAlarmEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6366F1),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFFE2E8F0)
                )
            )
        }
    }
}

@Composable
private fun MindfulnessMiniCardsRow(
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MindfulnessItemCard(
            title = "Mood",
            subtitle = "Check-in",
            icon = Icons.Default.SentimentSatisfiedAlt,
            iconTint = Color(0xFFEC4899),
            iconBg = Color(0xFFFDF2F8),
            isDark = isDark,
            animationType = 1,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("mood") }
        )
        MindfulnessItemCard(
            title = "Breathe",
            subtitle = "4-7-8 Relax",
            icon = Icons.Default.Air,
            iconTint = Color(0xFF06B6D4),
            iconBg = Color(0xFFECFEFF),
            isDark = isDark,
            animationType = 2,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("breath") }
        )
        MindfulnessItemCard(
            title = "Meditate",
            subtitle = "5 min calm",
            icon = Icons.Default.SelfImprovement,
            iconTint = Color(0xFF8B5CF6),
            iconBg = Color(0xFFF5F3FF),
            isDark = isDark,
            animationType = 3,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("meditate") }
        )
    }
}

@Composable
private fun MindfulnessItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isDark: Boolean,
    animationType: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mindfulness_card_$title")

    val animScale by when (animationType) {
        2 -> infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breathe_scale"
        )
        1 -> infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "mood_pulse"
        )
        else -> infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "meditate_glow"
        )
    }

    val customGlassBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                iconTint.copy(alpha = 0.22f),
                Color(0xFF0F172A).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                iconBg.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.35f)
            )
        )
    }

    OneUIGlassCard(
        modifier = modifier
            .clickable { onClick() }
            .testTag("mindfulness_item_$title"),
        shape = RoundedCornerShape(22.dp),
        isDark = isDark,
        customBrush = customGlassBrush
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.20f else 0.85f),
                                iconTint.copy(alpha = if (isDark) 0.35f else 0.25f)
                            )
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = if (isDark) 0.35f else 0.80f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = animScale
                            scaleY = animScale
                        }
                )
            }

            Text(
                text = title,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = if (isDark) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BehaviorPredictionInsightCard(
    isDark: Boolean,
    realHealthData: RealHealthData? = null
) {
    val context = LocalContext.current
    val mlEngine = remember { InternalMlEngine.getInstance(context) }

    val todayActivityRate = if (realHealthData != null && realHealthData.stepGoal > 0) {
        ((realHealthData.currentSteps.toFloat() / realHealthData.stepGoal) * 0.5f +
                (realHealthData.currentActiveMinutes.toFloat() / realHealthData.activeGoalMinutes.coerceAtLeast(1)) * 0.3f +
                (realHealthData.currentCalories.toFloat() / realHealthData.calorieGoal.coerceAtLeast(1)) * 0.2f).coerceIn(
            0f,
            2f
        )
    } else 0f
    val todaySteps = realHealthData?.currentSteps ?: 0

    var prediction by remember {
        mutableStateOf(
            mlEngine.getCachedBehaviorPrediction() ?: BehaviorPrediction()
        )
    }

    LaunchedEffect(realHealthData?.currentSteps, realHealthData?.currentCalories) {
        prediction = mlEngine.predictTomorrowBehavior(todayActivityRate, todaySteps)
    }

    if (prediction.totalDaysSampled < 3) return

    val containerBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val strokeColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }

    val titleColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val appNameColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF5F6368)
    val timeTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF757575)

    val iconBgColor = if (isDark) Color(0xFF312E81) else Color(0xFFE8F0FE)
    val iconTintColor = if (isDark) Color(0xFF818CF8) else Color(0xFF1A73E8)

    val sleepAccentColor = Color(0xFF818CF8)
    val wakeAccentColor = Color(0xFF38BDF8)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, strokeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Routine Predictor Icon",
                            tint = iconTintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Routine prediction",
                            color = appNameColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when (prediction.trendDirection) {
                                TrendDirection.IMPROVING -> Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.12f)
                                TrendDirection.DECLINING -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.12f)
                                TrendDirection.STABLE -> if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                            }
                        )
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val badgeColor = when (prediction.trendDirection) {
                            TrendDirection.IMPROVING -> Color(0xFF10B981)
                            TrendDirection.DECLINING -> Color(0xFFEF4444)
                            TrendDirection.STABLE -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        }

                        Icon(
                            imageVector = when (prediction.trendDirection) {
                                TrendDirection.IMPROVING -> Icons.Default.ArrowDropUp
                                TrendDirection.DECLINING -> Icons.Default.ArrowDropDown
                                TrendDirection.STABLE -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            text = prediction.trendDirection.label,
                            color = badgeColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(sleepAccentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Tonight: ${prediction.predictedSleepState.label}",
                            color = titleColor,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = timeTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${(prediction.sleepProbability * 100).toInt()}% probability via Markov Chain",
                                color = timeTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(wakeAccentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Tomorrow morning: ${prediction.predictedWakeState.label}",
                            color = titleColor,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = timeTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${(prediction.wakeProbability * 100).toInt()}% probability from habit chain",
                                color = timeTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                if (prediction.activityImpactNote != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFECFDF5))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${prediction.activityImpactNote} (Est. recovery: ${prediction.estimatedRecoveryPercent}%)",
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val bottomText = prediction.recommendation.ifBlank { prediction.insight }
                if (bottomText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = bottomText,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
