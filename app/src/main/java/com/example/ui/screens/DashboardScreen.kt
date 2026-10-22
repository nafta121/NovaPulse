package com.example.ui.screens

import android.content.Intent
import com.example.ui.components.WeatherStickerImage
import com.example.ui.components.DynamicWeatherAdviceCard
import com.example.ui.components.NowNudgeCard
import com.example.core.engine.NowNudgeEngine
import com.example.core.engine.NowNudgeActionType
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.RealHealthData
import com.example.ui.components.ActivityEnergyCard
import com.example.ui.components.SleepRestStatusCard
import com.example.ui.components.SmartMusicCard
import com.example.ui.components.WaterHydrationCard
import com.example.ui.components.SmartCommuteCard
import com.example.ui.components.FocusSessionCard
import com.example.ui.components.OneUIGlassCard
import com.example.ui.components.StaggeredAnimatedItem
import com.example.ui.components.BreathingBottomSheet
import com.example.ui.components.MoodTrackerBottomSheet
import com.example.ui.components.MeditationTimerBottomSheet
import com.example.ui.components.textAlphaMaskReveal
import com.example.ui.components.OneUICalendarCard
import com.example.ui.components.NewsMarketBriefCard
import com.example.ui.components.WeatherNowBriefCard
import com.example.ui.components.SmartSuggestionWidgetsSection
import com.example.ui.components.BehaviorPredictionInsightCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.database.TaskEntity
import com.example.core.repository.RealCalendarEvent
import com.example.core.repository.RealScreenTimeData
import com.example.core.repository.RealWeatherData
import com.example.ui.MainViewModel
import com.example.ui.PermissionsState
import com.example.ui.theme.DiurnalPhase
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenContentVisibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val enabledContentCards by viewModel.enabledContentCards.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioBriefPlaying.collectAsStateWithLifecycle()

    // Real Repositories Data
    val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.todayCalendarEvents.collectAsStateWithLifecycle()
    val realWeatherData by viewModel.realWeatherData.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val realHealthData by viewModel.realHealthData.collectAsStateWithLifecycle()
    val isHealthLoading by viewModel.isHealthLoading.collectAsStateWithLifecycle()
    val nowBrief by viewModel.nowBrief.collectAsStateWithLifecycle()
    val isGeneratingNowBrief by viewModel.isGeneratingNowBrief.collectAsStateWithLifecycle()
    val lifestyleTips by viewModel.lifestyleTips.collectAsStateWithLifecycle()
    val hasPlayedEntranceAnimation by viewModel.hasPlayedEntranceAnimation.collectAsStateWithLifecycle()

    val briefingAiSummaryText by viewModel.briefingAiSummaryText.collectAsStateWithLifecycle()
    val isAiInferring by viewModel.isAiInferring.collectAsStateWithLifecycle()
    val selectedAiModelMode by viewModel.selectedAiModelMode.collectAsStateWithLifecycle()

    // Cross-Context Engine Evaluation State
    val crossContextEngine = remember(context) { com.example.core.engine.CrossContextEngine(context) }
    var contextCards by remember { mutableStateOf<List<com.example.core.engine.ContextActionCard>>(emptyList()) }
    var activeMindfulnessSheet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(calendarEvents, realWeatherData, allTasks) {
        contextCards = crossContextEngine.evaluateContextCards()
    }

    // Permission launchers
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkAndRefreshPermissions()
        if (granted) viewModel.refreshAllRealData()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.checkAndRefreshPermissions()
        val granted = permissions.values.any { it }
        if (granted) viewModel.fetchRealWeather()
    }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.checkAndRefreshPermissions()
        if (grantedPermissions.isNotEmpty()) {
            viewModel.fetchRealHealthData()
        }
    }

    val activityRecognitionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkAndRefreshPermissions()
        viewModel.fetchRealHealthData()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAllRealData()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !permissionsState.hasActivityRecognitionPermission) {
            activityRecognitionPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Automatically refresh calendar data & Glance widget immediately on app resume (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAllRealData()
                com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── Live Clock Ticker: automatically updates Session & Phase every minute ──
    var liveCal by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            liveCal = now
            // Calculate remaining seconds until the next minute for accurate synchronization
            val secsToNextMinute = 60 - now.get(Calendar.SECOND)
            kotlinx.coroutines.delay(secsToNextMinute * 1000L)
        }
    }

    val currentHour = liveCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = liveCal.get(Calendar.MINUTE)

    val diurnalPhase = remember(currentHour, currentMinute) {
        DiurnalPhase.fromHourAndMinute(currentHour, currentMinute)
    }

    val isDark = diurnalPhase.isDark

    val backgroundGradient = remember(diurnalPhase) {
        diurnalPhase.getBackgroundBrush()
    }

    val hour = currentHour
    val minute = currentMinute

    val briefingSession = remember(hour, minute) {
        com.example.feature.widget.BriefingSession.fromHourAndMinute(hour, minute)
    }
    val headlineGreeting = remember(briefingSession) {
        briefingSession.getRandomGreeting()
    }
    val headlineTitle = headlineGreeting.title
    val subtitle = headlineGreeting.subtitle
    val phaseTextVi = briefingSession.sessionTag
    val iconGradientColors = briefingSession.iconGradient
    val aiWeatherSnippet = remember(realWeatherData, diurnalPhase, currentHour) {
        val weather = realWeatherData
        if (weather != null) {
            val rawDesc = weather.conditionDescription
            val cleanDesc = when {
                rawDesc.contains("partly cloudy", ignoreCase = true) || rawDesc.contains("few clouds", ignoreCase = true) -> "Partly cloudy"
                rawDesc.contains("cloud", ignoreCase = true) -> "Cloudy"
                rawDesc.contains("rain", ignoreCase = true) || rawDesc.contains("drizzle", ignoreCase = true) || rawDesc.contains("shower", ignoreCase = true) -> "Rainy"
                rawDesc.contains("sun", ignoreCase = true) || rawDesc.contains("clear", ignoreCase = true) -> "Clear & sunny"
                rawDesc.isBlank() -> "Mostly cloudy"
                else -> rawDesc
            }
            val temp = Math.round(weather.currentTempC).toInt()
            val location = weather.locationName.ifBlank { "Ho Chi Minh City" }
            "$cleanDesc, $temp°C in $location"
        } else {
            when (diurnalPhase) {
                DiurnalPhase.NIGHT -> "Clear night, 23°C in Ho Chi Minh City"
                DiurnalPhase.EVENING -> "Sunset, 26°C in Ho Chi Minh City"
                DiurnalPhase.AFTERNOON -> "Sunny, 33°C in Ho Chi Minh City"
                DiurnalPhase.MORNING -> "Morning sun, 31°C in Ho Chi Minh City"
            }
        }
    }

    val formattedSpent = DecimalFormat("#,###").format(metrics.totalExpensesVnd)

    val audioBriefScript =
        remember(metrics, headlineTitle, aiWeatherSnippet, allTasks, calendarEvents, realHealthData) {
            val taskCount = allTasks.filter { !it.isCompleted }.size + calendarEvents.size
            val healthBrief = if (realHealthData != null && realHealthData?.isDataAvailable == true) {
                "Last night's sleep reached ${realHealthData?.sleepDurationFormatted}, with an average heart rate of ${realHealthData?.averageHeartRateBpm} bpm."
            } else {
                "Health data will be ready once connected to Samsung Health."
            }
            "$headlineTitle, Alex. $subtitle $aiWeatherSnippet $healthBrief You have $taskCount scheduled events and tasks today."
        }

    val dateFormatted = remember(liveCal) {
        SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH).format(liveCal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
    ) {
        // -------------------------------------------------------------
        // AMBIENT GLOW: 2 Radial Gradient Blur Orbs behind the cards
        // -------------------------------------------------------------
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDark) {
                // Orb 1: Top-Right Cyan / Sky Blue Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.32f),
                            Color(0xFF0284C7).copy(alpha = 0.16f),
                            Color(0xFF0369A1).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.15f),
                        radius = width * 0.75f
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.75f
                )

                // Orb 2: Middle-Left Indigo / Purple Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF818CF8).copy(alpha = 0.28f),
                            Color(0xFF6366F1).copy(alpha = 0.14f),
                            Color(0xFF4338CA).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.55f),
                        radius = width * 0.85f
                    ),
                    center = Offset(width * 0.15f, height * 0.55f),
                    radius = width * 0.85f
                )
            } else {
                // Day light ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            diurnalPhase.primaryColor.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.15f),
                        radius = width * 0.75f
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.75f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            diurnalPhase.secondaryColor.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.55f),
                        radius = width * 0.85f
                    ),
                    center = Offset(width * 0.15f, height * 0.55f),
                    radius = width * 0.85f
                )
            }
        }

        val urgentContextCard = remember(contextCards) {
            contextCards.firstOrNull {
                it.id.startsWith("weather_sched_") ||
                        it.id.startsWith("weather_heat_") ||
                        it.id.startsWith("batt_meeting_") ||
                        it.id.startsWith("meet_shortcut_") ||
                        it.id.startsWith("alarm_check_") ||
                        it.id.startsWith("travel_buffer_") ||
                        it.id.startsWith("energy_task_") ||
                        it.id.startsWith("procrastinate_micro_")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // INDEX 0: DYNAMIC SESSION HEADER (PHASE 1: Header Focus & Fade)
            item {
                StaggeredAnimatedItem(index = 0, isHeader = true, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dateFormatted • $phaseTextVi",
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VoiceWaveIconButton(
                                    isPlaying = isAudioPlaying,
                                    isDark = isDark,
                                    onClick = { viewModel.toggleAudioBrief(audioBriefScript) }
                                )

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.White.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .clickable { onOpenContentVisibility() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DashboardCustomize,
                                        contentDescription = "Content to include",
                                        tint = if (isDark) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.White.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .clickable { onOpenSettings() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Settings & Configuration",
                                        tint = if (isDark) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Floating AI Sparkle Icon (Gradient dynamically matching 7 sessions)
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .size(24.dp)
                                .graphicsLayer { alpha = 0.95f }
                                .drawWithCache {
                                    val brush = Brush.linearGradient(colors = iconGradientColors)
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                                    }
                                }
                        )

                        // Header Text Group with Text Alpha Mask Reveal / Wipe effect (45°, 5000ms, feather 180px, one-shot on app launch)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .textAlphaMaskReveal(
                                    durationMillis = 5000,
                                    featherPx = 180f,
                                    repeatMode = false
                                )
                        ) {
                            Text(
                                text = headlineTitle,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = (-0.8).sp
                                )
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            if (isAiInferring) {
                                // Skeleton / Shimmer loading while On-Device Model is inferring (200-400ms)
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth(0.85f)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF0F172A).copy(
                                                alpha = 0.08f
                                            )
                                        )
                                )
                            } else {
                                val rawDisplay = briefingAiSummaryText.ifBlank { subtitle }
                                val displayText = remember(rawDisplay) {
                                    rawDisplay
                                        .replace(Regex("Temperature \\d+°C,", RegexOption.IGNORE_CASE), "")
                                        .replace(Regex("sunny \\d+°C,", RegexOption.IGNORE_CASE), "")
                                        .replace(Regex("rainy \\d+°C,", RegexOption.IGNORE_CASE), "")
                                        .replace(Regex("\\d+°C,"), "")
                                        .replace(Regex("\\d+°C"), "")
                                        .trim()
                                }
                                Text(
                                    text = displayText,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = aiWeatherSnippet,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // INDEX 1: WEATHER CARD
            if ("weather" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 1, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        WeatherNowBriefCard(
                            weatherData = realWeatherData,
                            isLoading = isWeatherLoading,
                            hasLocationPermission = permissionsState.hasLocationPermission,
                            currentHour = currentHour,
                            diurnalPhase = diurnalPhase,
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            onRefresh = { viewModel.fetchRealWeather() },
                            onCardClick = { viewModel.onCardClicked("weather_card") }
                        )
                    }
                }

                // INDEX 1.5: DYNAMIC WEATHER ADVICE CARD (STICKER + SUN/RAIN/NIGHT GEAR TIPS)
                item {
                    StaggeredAnimatedItem(index = 2, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        val isNightMode = diurnalPhase.isDark || currentHour >= 19 || currentHour < 5
                        DynamicWeatherAdviceCard(
                            weather = realWeatherData,
                            isNight = isNightMode,
                            isDark = isDark
                        )
                    }
                }
            }

            // INDEX 1.6: CROSS-CONTEXT ALERT ASSISTANT (SCHEDULE + HOURLY WEATHER / BATTERY / MEET)
            if (urgentContextCard != null && "now_nudge" in enabledContentCards) {
                item(key = urgentContextCard.id) {
                    StaggeredAnimatedItem(index = 2, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        com.example.ui.components.CrossContextAlertCard(
                            card = urgentContextCard,
                            isDark = isDark,
                            onDismiss = {
                                contextCards = contextCards.filter { it.id != urgentContextCard.id }
                            }
                        )
                    }
                }
            }

            // INDEX 1.8: NOW NUDGE (SAMSUNG ONE UI 9 PROACTIVE 1-TAP ACTION PROMPT)
            if ("now_nudge" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 2, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        val nudgeItem = remember(currentHour, allTasks.size, realHealthData) {
                            NowNudgeEngine.generateNudge(
                                currentHour = currentHour,
                                pendingTasksCount = allTasks.count { !it.isCompleted },
                                currentSteps = realHealthData?.currentSteps ?: 0,
                                waterDrunkMl = 500
                            )
                        }
                        NowNudgeCard(
                            nudgeItem = nudgeItem,
                            isDark = isDark,
                            onActionClick = {
                                when (nudgeItem.actionType) {
                                    NowNudgeActionType.OPEN_SPOTIFY -> {
                                        val rec =
                                            com.example.core.music.MusicRecommendationHelper.getRecommendationForSession(
                                                currentHour
                                            )
                                        com.example.core.music.MusicRecommendationHelper.openMusicApp(
                                            context,
                                            rec.query,
                                            rec.spotifyUri
                                        )
                                    }

                                    NowNudgeActionType.START_FOCUS -> {
                                        onNavigateToTasks()
                                    }

                                    NowNudgeActionType.DRINK_WATER -> {
                                        activeMindfulnessSheet = "breath"
                                    }

                                    NowNudgeActionType.START_BREATHING -> {
                                        activeMindfulnessSheet = "breath"
                                    }

                                    NowNudgeActionType.VIEW_SCHEDULE -> {
                                        com.example.core.repository.CalendarRepository.openCalendarApp(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // INDEX 2: ENERGY SCORE & HEALTH CARD (DAILY ACTIVITY)
            if ("health_energy" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 2, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        val health = realHealthData
                        ActivityEnergyCard(
                            currentSteps = health?.currentSteps ?: 0,
                            stepGoal = health?.stepGoal ?: 6000,
                            currentActiveMinutes = health?.currentActiveMinutes ?: 0,
                            activeGoalMinutes = health?.activeGoalMinutes ?: 60,
                            currentCalories = health?.currentCalories ?: 0,
                            calorieGoal = health?.calorieGoal ?: 500,
                            historyList = if (health != null && health.weeklyHistory.isNotEmpty()) health.weeklyHistory else com.example.ui.components.defaultHistoryList(),
                            isNight = diurnalPhase.isDark,
                            onSyncClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !permissionsState.hasActivityRecognitionPermission) {
                                    activityRecognitionPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                } else {
                                    val sdkStatus = try {
                                        HealthConnectClient.getSdkStatus(context)
                                    } catch (e: Throwable) {
                                        HealthConnectClient.SDK_UNAVAILABLE
                                    }
                                    if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                                        if (permissionsState.hasHealthPermission) {
                                            viewModel.fetchRealHealthData()
                                        } else {
                                            try {
                                                healthConnectPermissionLauncher.launch(HealthConnectRepository.REQUIRED_PERMISSIONS)
                                            } catch (e: Exception) {
                                                viewModel.openHealthConnectSettings(context)
                                            }
                                        }
                                    } else {
                                        viewModel.openHealthConnectSettings(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ONE UI 9 SCHEDULE CARD (Real Calendar Data & Room DB)
            if ("calendar_schedule" in enabledContentCards) {
                item {
                    val calendarRepo = remember { com.example.core.repository.CalendarRepository(context) }
                    val displayEvents = remember(calendarEvents) {
                        val now = System.currentTimeMillis()
                        val remaining = calendarEvents.filter { it.endTimeMillis > now || it.startTimeMillis >= now }
                        if (remaining.isNotEmpty()) {
                            remaining
                        } else {
                            try {
                                calendarRepo.getTomorrowEvents()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    val upcomingScheduleEvents = remember(allTasks, displayEvents) {
                        val now = System.currentTimeMillis()
                        val list = mutableListOf<Pair<Long, com.example.ui.components.OneUICalendarEventItem>>()
                        for (e in displayEvents) {
                            if (e.endTimeMillis > now || e.startTimeMillis >= now - 5 * 60 * 1000L) {
                                val timeStr =
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(e.startTimeMillis))
                                list.add(
                                    e.startTimeMillis to com.example.ui.components.OneUICalendarEventItem(
                                        title = e.title,
                                        timeSubtitle = timeStr,
                                        source = e.displaySourceLabel,
                                        eventId = e.id,
                                        location = e.location
                                    )
                                )
                            }
                        }
                        for (t in allTasks.filter { !it.isCompleted }) {
                            if (t.datetime >= now - 15 * 60 * 1000L) {
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t.datetime))
                                list.add(
                                    t.datetime to com.example.ui.components.OneUICalendarEventItem(
                                        title = t.title,
                                        timeSubtitle = timeStr,
                                        source = t.sourceApp.ifBlank { "Google Calendar" }
                                    )
                                )
                            }
                        }
                        list.sortBy { it.first }
                        list.map { it.second }
                    }

                    StaggeredAnimatedItem(index = 3, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        OneUICalendarCard(
                            events = upcomingScheduleEvents,
                            isDark = isDark,
                            onClick = {
                                val firstEvent = upcomingScheduleEvents.firstOrNull()
                                if (firstEvent?.eventId != null) {
                                    com.example.core.repository.CalendarRepository.openEventDetail(
                                        context,
                                        firstEvent.eventId
                                    )
                                } else {
                                    com.example.core.repository.CalendarRepository.openCalendarApp(context)
                                }
                            },
                            onItemClick = { item ->
                                if (item.eventId != null) {
                                    com.example.core.repository.CalendarRepository.openEventDetail(
                                        context,
                                        item.eventId
                                    )
                                } else {
                                    com.example.core.repository.CalendarRepository.openCalendarApp(context)
                                }
                            },
                            onMoreClick = {
                                com.example.core.repository.CalendarRepository.openCalendarApp(context)
                            }
                        )
                    }
                }
            }

            // WEEKLY HABIT INSIGHTS & ENERGY SCORE CARD (ONE UI GLASSMORPHISM)
            if ("habit_insights" in enabledContentCards) {
                item {
                    val habitAnalyticsEngine =
                        remember(context) { com.example.core.engine.HabitAnalyticsEngine(context) }
                    val firstUpcomingItem = remember(allTasks, calendarEvents) {
                        val now = System.currentTimeMillis()
                        // 1. Unfinished task occurring from now onwards (or today)
                        val nextTask = allTasks.firstOrNull { !it.isCompleted && it.datetime >= now - 30 * 60 * 1000L }
                        if (nextTask != null) {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTask.datetime))
                            nextTask.title to timeStr
                        } else {
                            // 2. Upcoming calendar event (excluding all-day events like holidays)
                            val nextEvent = calendarEvents.firstOrNull { event ->
                                !event.isAllDay && event.startTimeMillis >= now - 15 * 60 * 1000L
                            }
                            if (nextEvent != null) {
                                val timeStr =
                                    SimpleDateFormat(
                                        "HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(nextEvent.startTimeMillis))
                                nextEvent.title to timeStr
                            } else {
                                "" to ""
                            }
                        }
                    }

                    val weeklyInsightsData = remember(realHealthData, contextCards, allTasks, firstUpcomingItem) {
                        habitAnalyticsEngine.calculateWeeklyInsights(
                            healthData = realHealthData,
                            taskCount = allTasks.size,
                            completedTaskCount = allTasks.count { it.isCompleted },
                            upcomingEventTitle = firstUpcomingItem.first,
                            upcomingEventTime = firstUpcomingItem.second
                        )
                    }

                    StaggeredAnimatedItem(index = 4, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        com.example.ui.components.WeeklyHabitInsightsCard(
                            data = weeklyInsightsData,
                            isDark = isDark,
                            onDetailClick = {
                                val healthRepo = com.example.core.repository.HealthConnectRepository(context)
                                healthRepo.openHealthConnectSettings(context)
                            }
                        )
                    }
                }

                // AI LIFESTYLE PERSONA CARD (ON-DEVICE PERSONA CLUSTERING K-MEANS)
                item {
                    val mlEngine = remember(context) { com.example.core.ml.InternalMlEngine.getInstance(context) }
                    var personaProfile by remember {
                        mutableStateOf(
                            mlEngine.getCachedPersonaProfile()
                                ?: com.example.core.ml.UserPersonaProfile()
                        )
                    }

                    LaunchedEffect(realHealthData, allTasks.size) {
                        personaProfile = mlEngine.classifyUserPersona(
                            healthData = realHealthData,
                            taskCount = allTasks.size,
                            completedTaskCount = allTasks.count { it.isCompleted }
                        )
                    }

                    StaggeredAnimatedItem(index = 4, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        com.example.ui.components.UserPersonaBadgeCard(
                            profile = personaProfile,
                            isDark = isDark
                        )
                    }
                }
            }

            // INDEX 3: SLEEP & RECOVERY STATUS CARD
            if ("sleep_recovery" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 3, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        SleepRestStatusCard(
                            healthData = realHealthData,
                            isDark = isDark
                        )
                    }
                }
            }

            // ROUTINE & BEHAVIOR PREDICTION (MARKOV CHAIN) RIGHT BELOW SLEEP & RECOVERY
            item {
                StaggeredAnimatedItem(
                    index = 3,
                    hasPlayedAnimation = hasPlayedEntranceAnimation
                ) {
                    BehaviorPredictionInsightCard(
                        isDark = isDark,
                        realHealthData = realHealthData
                    )
                }
            }

            // INDEX 4: SMART MUSIC RECOMMENDATION CARD (SINGLE SPOTIFY / YOUTUBE DEEP LINK CARD)
            if ("smart_music" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 4, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        SmartMusicCard(
                            currentHour = currentHour,
                            isDark = isDark
                        )
                    }
                }
            }

            // INDEX 4.5: ONE UI 9 NEWS & MARKET BRIEF CARD (MARKET TICKER + NEWS HEADLINES)
            if ("news_market" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 4, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        NewsMarketBriefCard(
                            isDark = isDark
                        )
                    }
                }
            }

            // INDEX 5+: LIFESTYLE SUGGESTION CARDS & MINDFULNESS
            item {
                StaggeredAnimatedItem(
                    index = 5,
                    hasPlayedAnimation = hasPlayedEntranceAnimation,
                    onAnimationFinished = { viewModel.markEntranceAnimationPlayed() }
                ) {
                    SmartSuggestionWidgetsSection(
                        currentHour = currentHour,
                        weatherData = realWeatherData,
                        lifestyleTips = lifestyleTips,
                        isAiInferring = isAiInferring,
                        onToggleTipDone = { viewModel.toggleLifestyleTipDone(it) },
                        isDark = isDark
                    )
                }
            }

            // BOTTOM SPACER: AVOID OVERLAPPING NAVIGATION BARS
            item {
                Spacer(modifier = Modifier.navigationBarsPadding().height(28.dp))
            }
        }

        if (activeMindfulnessSheet != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            when (activeMindfulnessSheet) {
                "breath" -> com.example.ui.components.BreathingBottomSheet(
                    onDismiss = { activeMindfulnessSheet = null },
                    sheetState = sheetState,
                    isNight = isDark
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveIconButton(
    isPlaying: Boolean,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    val pulseAlpha = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "audio_wave_alpha"
        ).value
    } else 1.0f

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("voice_brief_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Listen to brief",
            tint = if (isPlaying) Color(0xFF3B82F6).copy(alpha = pulseAlpha) else if (isDark) Color(0xFF94A3B8) else Color(
                0xFF64748B
            ),
            modifier = Modifier.size(22.dp)
        )
        if (isPlaying) {
            Text(
                text = "Reading...",
                color = Color(0xFF3B82F6),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
