package com.example.feature.island

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.database.ExpenseEntity
import com.example.core.database.TaskEntity
import com.example.core.ml.TaskProcrastinationPredictor
import com.example.core.ml.TaskUrgency
import com.example.core.repository.PredictedSleepWindow
import com.example.core.repository.RealCalendarEvent
import com.example.core.repository.RealScreenTimeData
import com.example.feature.widget.BriefingSession
import com.example.feature.widget.WidgetThemeHelper
import com.example.feature.widget.cleanEmoji
import com.example.ui.components.ActivityRingsCanvas
import com.example.ui.components.WeatherStickerImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
private val cardLabels = listOf("AI Brief", "Cuaca", "Aktivitas", "Jadwal", "Layar", "Tidur")
private const val TOTAL_CARDS = 6

@Composable
fun DynamicIslandView(
    energyScore: Int = 88,
    weatherTemp: String = "--°C",
    weatherCode: Int = 0,
    weatherLocation: String = "Lokasi Anda",
    weatherCondition: String = "Cerah",
    weatherHighC: Int? = null,
    weatherLowC: Int? = null,
    isBottom: Boolean = true,
    aiBriefingText: String = "",
    upcomingTasks: List<TaskEntity> = emptyList(),
    calendarLabel: String = "Today's Schedule",
    calendarEvents: List<RealCalendarEvent> = emptyList(),
    currentSteps: Int = 0,
    stepGoal: Int = 6000,
    currentCalories: Int = 0,
    calorieGoal: Int = 500,
    currentActiveMinutes: Int = 0,
    activeGoalMinutes: Int = 60,
    screenTimeData: RealScreenTimeData? = null,
    healthSleepDurationFormatted: String = "",
    healthSleepSummary: String = "",
    predictedSleepWindow: PredictedSleepWindow = PredictedSleepWindow(),
    todayExpenseTotal: Double = 0.0,
    latestExpense: ExpenseEntity? = null,
    customWidthDp: Int = 320,
    customHeightDp: Int = 42,
    opacity: Float = 0.85f,
    colorPreset: String = "DYNAMIC",
    isFrostedGlass: Boolean = true,
    onExpandStateChanged: (Boolean) -> Unit = {},
    collapseTrigger: Int = 0,
    onCompleteTask: (Long) -> Unit = {},
    onOpenApp: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = System.currentTimeMillis()
            delay(15_000)
        }
    }

    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val isNight = hour >= 18 || hour < 6

    val tempInt = remember(weatherTemp) {
        weatherTemp.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 28
    }

    var activeCardIndex by remember {
        mutableIntStateOf(
            selectInitialCardIndex(
                hour = hour,
                tempInt = tempInt,
                weatherCode = weatherCode,
                scheduleCount = calendarEvents.size + upcomingTasks.size,
                hasAiBriefing = aiBriefingText.isNotBlank()
            )
        )
    }

    LaunchedEffect(collapseTrigger) {
        if (collapseTrigger > 0 && isExpanded) {
            isExpanded = false
            onExpandStateChanged(false)
        }
    }

    // Auto-collapse after 12 seconds of inactivity; resets whenever user interacts with the card
    LaunchedEffect(isExpanded, interactionTick) {
        if (isExpanded) {
            onExpandStateChanged(true)
            delay(12_000)
            isExpanded = false
            onExpandStateChanged(false)
        }
    }

    val timeTheme = remember(hour, minute) {
        WidgetThemeHelper.getThemeForCurrentTime(hour, minute)
    }
    val currentSession = remember(hour, minute) {
        BriefingSession.fromHourAndMinute(hour, minute)
    }
    val activeGreeting = remember(currentSession) {
        currentSession.getRandomGreeting()
    }

    val rawGradientColors = remember(timeTheme, colorPreset) {
        when (colorPreset) {
            "DARK_GLASS" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            "LIGHT_GLASS" -> listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
            "MATERIAL_YOU" -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF60A5FA))
            else -> timeTheme.gradientColors
        }
    }

    val effectiveOpacity = opacity.coerceIn(0.20f, 1.0f)
    val dynamicColors = remember(rawGradientColors, effectiveOpacity) {
        rawGradientColors.map { it.copy(alpha = effectiveOpacity) }
    }

    val isLightGlass = colorPreset == "LIGHT_GLASS"
    val titleColor = if (isLightGlass) Color(0xFF0F172A) else Color.White
    val subtitleColor = if (isLightGlass) Color(0xFF475569) else Color.White.copy(alpha = 0.88f)

    val gradientBrush = remember(rawGradientColors, dynamicColors, isLightGlass, isExpanded) {
        if (isExpanded) {
            val baseColor = if (isLightGlass) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            Brush.verticalGradient(
                listOf(
                    baseColor.copy(alpha = 0.96f),
                    rawGradientColors.first().copy(alpha = 0.95f),
                    rawGradientColors.last().copy(alpha = 0.97f)
                )
            )
        } else {
            Brush.horizontalGradient(dynamicColors)
        }
    }

    val borderBrush = remember(isLightGlass, effectiveOpacity, isExpanded) {
        if (isExpanded) {
            Brush.verticalGradient(
                if (isLightGlass) {
                    listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.15f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.32f)
                    )
                }
            )
        } else {
            Brush.horizontalGradient(
                if (isLightGlass) {
                    listOf(
                        Color.Black.copy(alpha = 0.15f * effectiveOpacity),
                        Color.Black.copy(alpha = 0.05f * effectiveOpacity),
                        Color.Black.copy(alpha = 0.12f * effectiveOpacity)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.35f * effectiveOpacity),
                        Color.White.copy(alpha = 0.12f * effectiveOpacity),
                        Color.White.copy(alpha = 0.30f * effectiveOpacity)
                    )
                }
            )
        }
    }

    val nextCalendarEvent = calendarEvents.firstOrNull()
    val topTask = upcomingTasks.firstOrNull()
    val totalScheduleCount = calendarEvents.size + upcomingTasks.size
    val screenTimeStr = screenTimeData?.formattedDuration?.takeIf { it.isNotBlank() } ?: "0h 0m"

    val synthesizedRealBriefing = remember(
        aiBriefingText,
        nextCalendarEvent,
        topTask,
        weatherLocation,
        weatherTemp,
        weatherCondition,
        currentSteps,
        screenTimeStr,
        energyScore
    ) {
        val cleanedAi = aiBriefingText.cleanEmoji().trim()
        val isLegacyCannedPhrase = cleanedAi.contains("Keep your afternoon", ignoreCase = true) ||
                cleanedAi.contains("grab some water", ignoreCase = true) ||
                cleanedAi.contains("Take a short break", ignoreCase = true) ||
                cleanedAi.contains("Stay focused on your key goals", ignoreCase = true)
        if (cleanedAi.isNotBlank() && !isLegacyCannedPhrase) {
            cleanedAi
        } else {
            buildString {
                append("$weatherTemp ($weatherCondition) di $weatherLocation")
                when {
                    nextCalendarEvent != null -> {
                        val t = if (nextCalendarEvent.isAllDay) "Seharian" else timeFormatter.format(Date(nextCalendarEvent.startTimeMillis))
                        append(" • Jadwal: ${nextCalendarEvent.title} ($t)")
                    }
                    topTask != null -> append(" • Tugas: ${topTask.title}")
                }
                if (currentSteps > 0) append(" • $currentSteps langkah")
                if (screenTimeStr != "0h 0m") append(" • Layar $screenTimeStr")
            }
        }
    }

    val formattedExpense = remember(todayExpenseTotal, latestExpense) {
        if (todayExpenseTotal <= 0.0) {
            "Rp 0"
        } else {
            val currency = latestExpense?.currency ?: "IDR"
            if (currency.equals("USD", ignoreCase = true)) {
                String.format(Locale.US, "$%.2f", todayExpenseTotal)
            } else {
                "Rp " + NumberFormat.getIntegerInstance(Locale("id", "ID")).format(todayExpenseTotal.toLong())
            }
        }
    }

    val (pillTitle, pillSubtitle) = remember(
        activeCardIndex,
        activeGreeting,
        weatherLocation,
        weatherTemp,
        weatherCondition,
        weatherHighC,
        weatherLowC,
        synthesizedRealBriefing,
        currentSteps,
        stepGoal,
        currentCalories,
        currentActiveMinutes,
        nextCalendarEvent,
        topTask,
        screenTimeStr,
        screenTimeData,
        energyScore,
        healthSleepDurationFormatted,
        predictedSleepWindow,
        todayExpenseTotal,
        formattedExpense
    ) {
        when (activeCardIndex) {
            0 -> {
                val title = "${activeGreeting.title} • $weatherTemp $weatherLocation"
                title to synthesizedRealBriefing
            }
            1 -> {
                val hl = if (weatherHighC != null && weatherLowC != null) " • H:${weatherHighC}° L:${weatherLowC}°" else ""
                "$weatherLocation • $weatherTemp" to "$weatherCondition$hl"
            }
            2 -> {
                "$currentSteps / $stepGoal langkah" to "$currentCalories kcal • $currentActiveMinutes mnt aktif"
            }
            3 -> {
                when {
                    nextCalendarEvent != null -> {
                        val timeStr = if (nextCalendarEvent.isAllDay) "Seharian" else timeFormatter.format(Date(nextCalendarEvent.startTimeMillis))
                        nextCalendarEvent.title to "$timeStr • ${nextCalendarEvent.displaySourceLabel}"
                    }
                    topTask != null -> {
                        val timeStr = if (topTask.datetime > 0L) timeFormatter.format(Date(topTask.datetime)) else "Prioritas"
                        topTask.title to "$timeStr • ${topTask.sourceApp.ifBlank { "Tugas" }}"
                    }
                    else -> "Jadwal Hari Ini Kosong" to "Ketuk untuk melihat atau menambah agenda"
                }
            }
            4 -> {
                val topApp = screenTimeData?.topAppName?.takeIf { it.isNotBlank() }
                "Layar: $screenTimeStr • ⚡$energyScore%" to (if (topApp != null) "Terbanyak: $topApp" else "Digital Wellbeing aktif")
            }
            else -> {
                val sleepTitle = if (healthSleepDurationFormatted.isNotBlank()) {
                    "Tidur: $healthSleepDurationFormatted"
                } else {
                    "Tidur: ${predictedSleepWindow.bedtimeFormatted}–${predictedSleepWindow.wakeTimeFormatted}"
                }
                val expSub = if (todayExpenseTotal > 0.0) {
                    "Pengeluaran hari ini: $formattedExpense"
                } else {
                    "Pengeluaran hari ini: Rp 0"
                }
                sleepTitle to expSub
            }
        }
    }

    val targetExpandedHeight = remember(activeCardIndex) {
        when (activeCardIndex) {
            0 -> 228.dp // AI Brief + stats chips + action button
            1 -> 224.dp // Weather + hydration + refresh
            2 -> 244.dp // Activity rings + steps/calories/mins
            3 -> 224.dp // Calendar & Tasks + Done button
            4 -> 236.dp // Screen Time + 7-day history + Energy
            else -> 224.dp // Sleep + Expenses
        }
    }

    val targetPillWidth = customWidthDp.coerceIn(180, 420).dp
    val targetPillHeight = customHeightDp.coerceIn(32, 64).dp

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val expandedTargetWidth = remember(screenWidthDp, targetPillWidth) {
        maxOf(screenWidthDp - 16.dp, targetPillWidth)
    }

    val springSpecDp = remember {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedTargetWidth else targetPillWidth,
        animationSpec = springSpecDp,
        label = "islandWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) targetExpandedHeight else targetPillHeight,
        animationSpec = springSpecDp,
        label = "islandHeight"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else (targetPillHeight / 2),
        animationSpec = springSpecDp,
        label = "islandCorner"
    )
    val compactAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        label = "compactAlpha"
    )
    val expandedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        label = "expandedAlpha"
    )

    val shape = RoundedCornerShape(animatedCornerRadius)

    // Persistent daily water hydration counter in SharedPreferences
    val hydrationPrefs = remember(context) {
        context.getSharedPreferences("island_hydration_prefs", Context.MODE_PRIVATE)
    }
    var waterDrunkMl by remember {
        val todayKey = LocalDate.now().toString()
        val savedDate = hydrationPrefs.getString("date", "")
        val initialMl = if (savedDate == todayKey) hydrationPrefs.getInt("ml", 0) else 0
        mutableIntStateOf(initialMl)
    }

    var horizontalDragAccum by remember { mutableFloatStateOf(0f) }
    var verticalDragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .graphicsLayer {
                this.shadowElevation = (if (isExpanded) 12.dp else 3.dp).toPx()
                this.shape = shape
                this.clip = true
            }
            .background(brush = gradientBrush)
            .then(
                if (isFrostedGlass) {
                    Modifier.border(
                        width = 1.dp,
                        brush = borderBrush,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .pointerInput(isExpanded, isBottom) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccum = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDragAccum += dragAmount
                    },
                    onDragEnd = {
                        if (horizontalDragAccum <= -45f) {
                            activeCardIndex = (activeCardIndex + 1) % TOTAL_CARDS
                            interactionTick++
                        } else if (horizontalDragAccum >= 45f) {
                            activeCardIndex = (activeCardIndex - 1 + TOTAL_CARDS) % TOTAL_CARDS
                            interactionTick++
                        }
                        horizontalDragAccum = 0f
                    }
                )
            }
            .pointerInput(isExpanded, isBottom) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccum = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDragAccum += dragAmount
                    },
                    onDragEnd = {
                        if (!isExpanded) {
                            // Swipe up on bottom pill or swipe down on top pill expands the Live Notification
                            val shouldExpand = if (isBottom) verticalDragAccum <= -25f else verticalDragAccum >= 25f
                            if (shouldExpand) {
                                interactionTick++
                                onExpandStateChanged(true)
                                isExpanded = true
                            }
                        } else {
                            // Swipe down on bottom card or swipe up on top card collapses the Live Notification
                            val shouldCollapse = if (isBottom) verticalDragAccum >= 35f else verticalDragAccum <= -35f
                            if (shouldCollapse) {
                                onExpandStateChanged(false)
                                isExpanded = false
                            }
                        }
                        verticalDragAccum = 0f
                    }
                )
            }
            .then(
                if (!isExpanded) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        interactionTick++
                        onExpandStateChanged(true)
                        isExpanded = true
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = if (isExpanded) Alignment.TopCenter else if (isBottom) Alignment.BottomCenter else Alignment.TopCenter
    ) {
        // 1. COMPACT PILL MODE (One UI Lock Screen Now Bar Pill)
        if (!isExpanded || compactAlpha > 0.01f) {
            val iconBoxSize = (customHeightDp * 0.76f).coerceIn(22f, 44f).dp
            val iconSize = (customHeightDp * 0.54f).coerceIn(15f, 30f).dp
            val titleFontSize = (customHeightDp * 0.29f).coerceIn(10.5f, 14.5f).sp
            val subtitleFontSize = (customHeightDp * 0.24f).coerceIn(9f, 12f).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetPillHeight)
                    .graphicsLayer { alpha = compactAlpha }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(timeTheme.iconGradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeCardIndex) {
                        1 -> WeatherStickerImage(
                            weatherCode = weatherCode,
                            isNight = isNight,
                            size = iconSize,
                            animate = false
                        )
                        2 -> Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Steps",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        3 -> Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Schedule",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        4 -> Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Screen Time",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        5 -> Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Sleep",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                        else -> Image(
                            painter = painterResource(id = R.drawable.ic_capsule_horizon),
                            contentDescription = "NowBrief Logo",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pillTitle,
                        color = titleColor,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pillSubtitle,
                        color = subtitleColor,
                        fontSize = subtitleFontSize,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Interactive One UI Live Notification Card Counter Badge (tap to cycle next card)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isLightGlass) 0.45f else 0.18f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            activeCardIndex = (activeCardIndex + 1) % TOTAL_CARDS
                        }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${activeCardIndex + 1}/$TOTAL_CARDS",
                        color = titleColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. EXPANDED LIVE NOTIFICATION MODE (One UI 7 / 8 Now Bar Expanded Deck)
        if (isExpanded || expandedAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer {
                        alpha = expandedAlpha
                        transformOrigin = TransformOrigin(0.5f, if (isBottom) 1.0f else 0.0f)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Squircle Logo + Live Location & Session + Weather Pill + Refresh + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                interactionTick++
                                onOpenApp()
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .border(
                                    0.8.dp,
                                    Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_capsule_horizon),
                                contentDescription = "NowBrief Logo",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "NowBrief • ${cardLabels[activeCardIndex]}",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$weatherLocation • ${activeGreeting.title}",
                                color = Color.White.copy(alpha = 0.76f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Weather Badge (tap to switch to Weather card)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                .clickable {
                                    interactionTick++
                                    activeCardIndex = 1
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WeatherStickerImage(
                                weatherCode = weatherCode,
                                isNight = isNight,
                                size = 18.dp,
                                animate = true
                            )
                            Text(
                                text = weatherTemp,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Refresh Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .clickable {
                                    interactionTick++
                                    onRefresh()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .clickable {
                                    onExpandStateChanged(false)
                                    isExpanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Active Real-Data Live Notification Card
                when (activeCardIndex) {
                    0 -> {
                        // CARD 0: AI Now Brief & Real Device Summary
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Ringkasan Cerdas Perangkat",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.22f))
                                        .border(0.6.dp, Color(0xFF10B981).copy(alpha = 0.45f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚡ Energi $energyScore%",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = synthesizedRealBriefing,
                                color = titleColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chipBg = Color.White.copy(alpha = if (isLightGlass) 0.5f else 0.14f)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipBg)
                                        .clickable {
                                            interactionTick++
                                            activeCardIndex = 3
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "📅 $totalScheduleCount agenda",
                                        color = titleColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipBg)
                                        .clickable {
                                            interactionTick++
                                            activeCardIndex = 2
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "👟 $currentSteps langkah",
                                        color = titleColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.24f))
                                        .border(0.7.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                                        .clickable {
                                            interactionTick++
                                            onOpenApp()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Buka App ↗",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // CARD 1: Real Weather & Location + Hydration Counter
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "$weatherLocation • $weatherCondition",
                                        color = titleColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val highLowStr = if (weatherHighC != null && weatherLowC != null) {
                                    "H:${weatherHighC}° L:${weatherLowC}°"
                                } else {
                                    weatherTemp
                                }
                                Text(
                                    text = highLowStr,
                                    color = subtitleColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (tempInt >= 30) {
                                        "Suhu saat ini $weatherTemp di $weatherLocation ($weatherCondition). Jaga hidrasi tubuh Anda."
                                    } else if (isNight) {
                                        "Suhu malam ini $weatherTemp di $weatherLocation ($weatherCondition). Udara nyaman untuk istirahat."
                                    } else {
                                        "Suhu $weatherTemp di $weatherLocation ($weatherCondition). Cuaca mendukung untuk beraktivitas."
                                    },
                                    color = subtitleColor,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.5.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                WeatherStickerImage(
                                    weatherCode = weatherCode,
                                    isNight = isNight,
                                    size = 36.dp,
                                    animate = true
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pillBg = Color.White.copy(alpha = if (isLightGlass) 0.48f else 0.16f)
                                val pillBorder = Color.White.copy(alpha = if (isLightGlass) 0.6f else 0.25f)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .border(0.7.dp, pillBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            interactionTick++
                                            val updated = (waterDrunkMl + 250).coerceAtMost(4000)
                                            waterDrunkMl = updated
                                            hydrationPrefs.edit()
                                                .putString("date", LocalDate.now().toString())
                                                .putInt("ml", updated)
                                                .apply()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "💧 +250ml ($waterDrunkMl ml)",
                                        color = titleColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .border(0.7.dp, pillBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            interactionTick++
                                            onRefresh()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "🔄 Perbarui Cuaca",
                                        color = titleColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // CARD 2: Real Physical Activity (Hardware Step Sensor + Health Connect)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Aktivitas Fisik Hari Ini",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val stepPct = ((currentSteps.toFloat() / stepGoal.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 999)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF10B981).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$stepPct% target",
                                        color = Color(0xFF86EFAC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ActivityRingsCanvas(
                                    stepsProgress = (currentSteps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
                                    activeMinutesProgress = (currentActiveMinutes.toFloat() / activeGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                                    caloriesProgress = (currentCalories.toFloat() / calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
                                    sizeDp = 50.dp
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentSteps / $stepGoal langkah",
                                            color = titleColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF22D3EE),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentActiveMinutes / $activeGoalMinutes menit aktif",
                                            color = titleColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentCalories / $calorieGoal kcal terbakar",
                                            color = titleColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // CARD 3: Real Calendar Events & Pending Tasks + Direct Lockscreen Completion
                        val sourceTitle = when {
                            nextCalendarEvent != null -> nextCalendarEvent.displaySourceLabel
                            topTask != null -> topTask.sourceApp.ifBlank { "Pengingat Tugas" }
                            else -> calendarLabel
                        }

                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = sourceTitle,
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$totalScheduleCount aktif",
                                        color = Color(0xFF7DD3FC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            when {
                                topTask != null -> {
                                    val taskPred = remember(topTask.title) {
                                        TaskProcrastinationPredictor.predict(topTask.title)
                                    }
                                    val taskTimeStr = remember(topTask.datetime) {
                                        if (topTask.datetime > 0L) timeFormatter.format(Date(topTask.datetime)) else "Hari ini"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (taskPred.urgency == TaskUrgency.CRITICAL) Color(0xFFEF4444)
                                                    else Color(taskPred.category.colorHex)
                                                )
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${taskPred.category.emoji} ${topTask.title}",
                                                color = titleColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$taskTimeStr • ${taskPred.microActionStep}",
                                                color = subtitleColor,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        // Interactive Lock Screen "Complete Task" button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                                .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    interactionTick++
                                                    onCompleteTask(topTask.id)
                                                }
                                                .padding(horizontal = 9.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selesai",
                                                    tint = Color(0xFF6EE7B7),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Selesai",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                nextCalendarEvent != null -> {
                                    val eventTimeStr = remember(nextCalendarEvent.startTimeMillis, nextCalendarEvent.endTimeMillis, nextCalendarEvent.isAllDay) {
                                        if (nextCalendarEvent.isAllDay) {
                                            "Seharian"
                                        } else {
                                            "${timeFormatter.format(Date(nextCalendarEvent.startTimeMillis))} - ${timeFormatter.format(Date(nextCalendarEvent.endTimeMillis))}"
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF38BDF8))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = nextCalendarEvent.title,
                                                color = titleColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val locSuffix = if (nextCalendarEvent.location.isNotBlank()) " • ${nextCalendarEvent.location}" else ""
                                            Text(
                                                text = "$eventTimeStr$locSuffix",
                                                color = subtitleColor,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Tidak ada jadwal atau tugas tersisa hari ini.",
                                            color = subtitleColor,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.16f))
                                                .clickable {
                                                    interactionTick++
                                                    onOpenApp()
                                                }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "+ Tambah",
                                                color = titleColor,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // CARD 4: Real Digital Wellbeing & Screen Time
                        val todayMinutes = ((screenTimeData?.totalScreenTimeTodayMillis ?: 0L) / 60_000L).toInt()
                        val screenGoalMinutes = 360 // 6h daily reference
                        val screenProgress = (todayMinutes.toFloat() / screenGoalMinutes).coerceIn(0f, 1f)
                        val topApp = screenTimeData?.topAppName?.takeIf { it.isNotBlank() } ?: "Belum ada"

                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Waktu Layar & Energi",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Top: $topApp",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "📱 Durasi Layar Hari Ini",
                                            color = subtitleColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = screenTimeStr,
                                            color = titleColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { screenProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.5.dp)
                                            .clip(CircleShape),
                                        color = if (todayMinutes > 300) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                        trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "⚡ Skor Kesiapan Energi AI",
                                            color = subtitleColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$energyScore%",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (energyScore / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.5.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF10B981),
                                        trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // CARD 5: Real Sleep Window & Today's Expenses
                        val sleepDisplay = if (healthSleepDurationFormatted.isNotBlank()) {
                            "Durasi tidur: $healthSleepDurationFormatted"
                        } else {
                            "Estimasi tidur: ${predictedSleepWindow.bedtimeFormatted} → ${predictedSleepWindow.wakeTimeFormatted}"
                        }
                        val sleepBadge = if (healthSleepSummary.isNotBlank()) {
                            healthSleepSummary
                        } else if (predictedSleepWindow.sampleCount > 0) {
                            "${predictedSleepWindow.sampleCount} log rutinitas"
                        } else {
                            "Pola Rutinitas"
                        }

                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NightlightRound,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tidur & Pengeluaran",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF7C3AED).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sleepBadge,
                                        color = Color(0xFFC084FC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "🌙 $sleepDisplay",
                                color = titleColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val latestMerchant = latestExpense?.merchant?.takeIf { it.isNotBlank() }
                                Text(
                                    text = if (latestMerchant != null) {
                                        "💳 Hari ini: $formattedExpense (Terakhir: $latestMerchant)"
                                    } else {
                                        "💳 Pengeluaran hari ini: $formattedExpense"
                                    },
                                    color = subtitleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Bottom One UI Live Notification Card Switcher Pills (Tap or Swipe to switch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cardLabels.forEachIndexed { idx, label ->
                        val isSelected = idx == activeCardIndex
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = if (isLightGlass) 0.75f else 0.28f)
                                    else Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.08f)
                                )
                                .border(
                                    width = if (isSelected) 0.9.dp else 0.5.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    interactionTick++
                                    activeCardIndex = idx
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) titleColor else subtitleColor,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun selectInitialCardIndex(
    hour: Int,
    tempInt: Int,
    weatherCode: Int,
    scheduleCount: Int,
    hasAiBriefing: Boolean
): Int {
    if (hasAiBriefing) return 0
    return when {
        scheduleCount > 0 && hour in 7..20 -> 3
        tempInt >= 31 || weatherCode in 51..99 -> 1
        hour in 6..10 -> 0
        hour in 11..17 -> 2
        hour in 18..21 -> 4
        else -> 5
    }
}

@Composable
private fun IslandInnerCardContainer(
    isLightGlass: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val cardBg = if (isLightGlass) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f)
    val cardBorder = if (isLightGlass) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.18f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(0.8.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}
