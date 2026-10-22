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

// -------------------------------------------------------------
// CARD 1: REAL WEATHER (Open-Meteo Real Data Source)
// -------------------------------------------------------------
@Composable
private fun WeatherNowBriefCard(
    weatherData: RealWeatherData?,
    isLoading: Boolean,
    hasLocationPermission: Boolean,
    currentHour: Int,
    diurnalPhase: DiurnalPhase,
    onRequestLocationPermission: () -> Unit,
    onRefresh: () -> Unit,
    onCardClick: () -> Unit = {}
) {
    val isNight = diurnalPhase.isDark || currentHour >= 19 || currentHour < 5
    val cardBg = Color(0xFF2563EB).copy(alpha = 0.4f)
    val cardBorder = Color.White.copy(alpha = 0.2f)

    val currentTempStr =
        if (weatherData != null) "${Math.round(weatherData.currentTempC).toInt()}°" else if (isNight) "29°" else "32°"
    val feelsLikeStr =
        if (weatherData != null) "Feels like ${Math.round(weatherData.apparentTempC).toInt()}°" else ""
    val conditionText = weatherData?.conditionDescription ?: if (isNight) "Cool night" else "Fresh morning"
    val locationLabel = weatherData?.locationName ?: "Current location"

    // "Saved at HH:mm" badge when displaying cached data
    val cacheLabel = if (weatherData?.isFromCache == true && weatherData.cacheTimestampMs > 0L) {
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = weatherData.cacheTimestampMs }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        "Saved at $h:$m"
    } else null

    val weatherIcon = when (weatherData?.weatherCode) {
        0 -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
        1, 2, 3 -> if (isNight) Icons.Default.CloudQueue else Icons.Default.WbCloudy
        51, 53, 55, 61, 63, 65, 80, 81 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("weather_now_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Current Weather Main Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = currentTempStr,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    if (feelsLikeStr.isNotEmpty()) {
                        Text(
                            text = feelsLikeStr,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = conditionText,
                            tint = if (isNight) Color(0xFF93C5FD) else Color(0xFFFEF08A),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = conditionText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Badge cache stale
                    if (cacheLabel != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cacheLabel,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hourly Forecast Horizontal Row (Real Data from Open-Meteo)
            if (weatherData != null && weatherData.hourlyForecast.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(weatherData.hourlyForecast) { item ->
                        val hourNum = try {
                            if (item.timeLabel.contains(":")) {
                                item.timeLabel.split(":")[0].trim().toInt()
                            } else {
                                currentHour
                            }
                        } catch (e: Exception) {
                            currentHour
                        }
                        val isSlotNight = hourNum >= 19 || hourNum < 5

                        val itemIcon = when (item.weatherCode) {
                            0 -> if (isSlotNight) Icons.Default.NightsStay else Icons.Default.WbSunny
                            1, 2, 3 -> if (isSlotNight) Icons.Default.CloudQueue else Icons.Default.WbCloudy
                            51, 53, 55, 61, 63, 80 -> Icons.Default.WaterDrop
                            95, 96 -> Icons.Default.Thunderstorm
                            else -> if (isSlotNight) Icons.Default.NightsStay else Icons.Default.CloudQueue
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.timeLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = itemIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${Math.round(item.temperatureC).toInt()}°",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.apparentTemperatureC > item.temperatureC + 1.5) {
                                Text(
                                    text = "${Math.round(item.apparentTemperatureC).toInt()}°",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state / Request Location Permission
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasLocationPermission) "Grant location permission for live weather" else "Updating weather from Open-Meteo...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!hasLocationPermission) {
                        Button(
                            onClick = onRequestLocationPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                        }
                    }
                }
            }

            // Weekly Forecast Row (7-day - One UI 9 Style)
            if (weatherData != null && weatherData.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                // Thin divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "7-day forecast",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(weatherData.dailyForecast) { day ->
                        val dayIcon = when (day.weatherCode) {
                            0 -> Icons.Default.WbSunny
                            1, 2, 3 -> Icons.Default.WbCloudy
                            51, 53, 55, 61, 63, 65, 80, 81 -> Icons.Default.WaterDrop
                            95, 96, 99 -> Icons.Default.Thunderstorm
                            else -> Icons.Default.CloudQueue
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = day.dayLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = if (day.dayLabel == "Today") FontWeight.Bold else FontWeight.Normal
                            )
                            Icon(
                                imageVector = dayIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${Math.round(day.tempMaxC).toInt()}°",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${Math.round(day.tempMinC).toInt()}°",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Weather Advice Banner (Dynamic from Real Weather Data)
            val tempC = weatherData?.currentTempC ?: 30.0
            val feelsC = weatherData?.apparentTempC ?: tempC
            val wCode = weatherData?.weatherCode ?: 0

            val adviceText = when {
                // Thunderstorm, lightning
                wCode in listOf(95, 96, 99) ->
                    "Thunderstorms expected! Limit outdoor activities and stay away from open areas."

                // Rain (light to heavy)
                wCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ->
                    "It's raining. Remember to bring an umbrella or raincoat when heading out."

                // Fog
                wCode in listOf(45, 48) ->
                    "Dense fog with low visibility. Drive carefully."

                // Extreme heat (feels like >= 40°C or temp >= 38°C)
                feelsC >= 40.0 || tempC >= 38.0 ->
                    "Extreme heat, feels like ${Math.round(feelsC).toInt()}°C! Limit time outdoors and stay hydrated."

                // Hot / Muggy (feels like >= 34°C or temp >= 31°C)
                feelsC >= 34.0 || tempC >= 31.0 ->
                    "Hot and muggy, feels like ${
                        Math.round(feelsC).toInt()
                    }°C. Seek shade and drink water regularly."

                // Warm & clear
                tempC >= 28.0 && wCode in listOf(0, 1) ->
                    "Warm and sunny ${Math.round(tempC).toInt()}°C, clear skies. Remember sunscreen if going outside."

                // Mostly cloudy & mild
                wCode in listOf(2, 3) ->
                    "Mostly cloudy ${Math.round(tempC).toInt()}°C, mild and pleasant weather."

                // Cool / Chilly (< 20°C)
                tempC < 20.0 ->
                    "Chilly ${Math.round(tempC).toInt()}°C. Remember to wear a warm jacket when going out."

                // Cool night
                isNight ->
                    "Tonight is ${Math.round(tempC).toInt()}°C with pleasant weather. Have a restful night!"

                // Default
                else ->
                    "Current temperature is ${Math.round(tempC).toInt()}°C. Great conditions for commuting and activities."
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val adviceIcon = when {
                        wCode in listOf(95, 96, 99) -> Icons.Default.Thunderstorm
                        wCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> Icons.Default.Umbrella
                        wCode in listOf(45, 48) -> Icons.Default.CloudQueue
                        feelsC >= 35.0 -> Icons.Default.WbSunny
                        tempC < 20.0 -> Icons.Default.Air
                        isNight -> Icons.Default.CheckCircle
                        else -> Icons.Default.WbSunny
                    }
                    Icon(
                        imageVector = adviceIcon,
                        contentDescription = "Advice",
                        tint = Color(0xFFFEF08A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = adviceText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CARD 2: REAL SCHEDULE (Calendar Contract & Room Database)
// -------------------------------------------------------------
@Composable
private fun ScheduleNowBriefCard(
    tasks: List<TaskEntity>,
    calendarEvents: List<RealCalendarEvent>,
    hasCalendarPermission: Boolean,
    currentHour: Int,
    diurnalPhase: DiurnalPhase,
    onRequestCalendarPermission: () -> Unit,
    onNavigateToTasks: () -> Unit
) {
    val context = LocalContext.current
    val isNight = diurnalPhase.isDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToTasks() }
            .testTag("schedule_now_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNight) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isNight) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            val calendarRepo = remember { com.example.core.repository.CalendarRepository(context) }
            val (scheduleTitle, displayEvents) = remember(calendarEvents) {
                val now = System.currentTimeMillis()
                val remaining = calendarEvents.filter { it.endTimeMillis > now || it.startTimeMillis >= now }
                if (remaining.isNotEmpty()) {
                    Pair("Today's Schedule", remaining)
                } else {
                    val tomorrow = try {
                        calendarRepo.getTomorrowEvents()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    if (tomorrow.isNotEmpty()) {
                        Pair("Tomorrow's Schedule", tomorrow)
                    } else {
                        Pair("Today's Schedule", emptyList())
                    }
                }
            }

            // Combined Real Schedule Items (Filtered for UPCOMING items only)
            val realItems = remember(tasks, displayEvents) {
                val list = mutableListOf<ScheduleItemData>()
                val now = System.currentTimeMillis()

                // Add real calendar events that have not ended yet
                for (e in displayEvents) {
                    if (e.endTimeMillis > now || e.startTimeMillis >= now - 5 * 60 * 1000L) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(e.startTimeMillis))
                        val locStr = if (e.location.isNotBlank()) " • ${e.location}" else ""
                        list.add(
                            ScheduleItemData(
                                time = timeStr,
                                title = e.title,
                                subtitle = "${e.displaySourceLabel}$locStr",
                                eventId = e.id,
                                rawTime = e.startTimeMillis
                            )
                        )
                    }
                }

                // Add real tasks from Room that are in the future or ongoing
                for (t in tasks.filter { !it.isCompleted }) {
                    if (t.datetime >= now - 15 * 60 * 1000L) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t.datetime))
                        list.add(
                            ScheduleItemData(
                                time = timeStr,
                                title = t.title,
                                subtitle = "Source: ${t.sourceApp}",
                                rawTime = t.datetime
                            )
                        )
                    }
                }

                list.sortBy { it.rawTime }
                list
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheduleTitle,
                    color = if (isNight) Color.White else Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )

                IconButton(
                    onClick = { com.example.core.repository.CalendarRepository.openCalendarApp(context) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NorthEast,
                        contentDescription = "View calendar",
                        tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Automatically syncs events from Samsung Calendar, Google Calendar, and linked accounts on your device.",
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            // Smart summary banner for upcoming events (AI Event Summary Banner)
            if (realItems.isNotEmpty()) {
                val nextEvent = realItems.first()
                val totalCount = realItems.size
                val summaryText = "Up next ${nextEvent.time}: ${nextEvent.title}" +
                        if (totalCount > 1) " ($totalCount upcoming events & tasks in total)" else ""

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNight) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.10f))
                        .border(
                            1.dp,
                            if (isNight) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text("✨ ", fontSize = 13.sp)
                            Text(
                                text = summaryText,
                                color = if (isNight) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Gold Hour Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNight) Color(0xFFF59E0B).copy(alpha = 0.20f) else Color(0xFFFEF3C7))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚡ Peak hours: 09:00 - 11:30",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNight) Color(0xFFFCD34D) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            if (!hasCalendarPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNight) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.8f))
                        .border(
                            1.dp,
                            if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sync Device Calendar",
                                color = if (isNight) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Automatically syncs events from Samsung Calendar, Google Calendar, and linked accounts on your device.",
                                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onRequestCalendarPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (realItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNight) Color.White.copy(alpha = 0.04f) else Color(0xFF0F172A).copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No upcoming events or tasks",
                            color = if (isNight) Color.White else Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "When calendar events or task reminders arrive, AI will automatically sync them here.",
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                realItems.take(4).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (item.eventId != null) {
                                    com.example.core.repository.CalendarRepository.openEventDetail(
                                        context,
                                        item.eventId
                                    )
                                } else {
                                    com.example.core.repository.CalendarRepository.openCalendarApp(context)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isNight) Color.White.copy(alpha = 0.10f) else Color(0xFF0F172A).copy(
                                        alpha = 0.06f
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isNight) Color.White.copy(alpha = 0.10f) else Color(0xFF0F172A).copy(alpha = 0.10f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.time,
                                color = if (isNight) Color.White else Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val taskPred = remember(item.title) {
                            com.example.core.ml.TaskProcrastinationPredictor.predict(
                                title = item.title,
                                scheduledTimestampMs = item.rawTime
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    color = if (isNight) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(taskPred.category.colorHex).copy(alpha = if (isNight) 0.15f else 0.10f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${taskPred.category.emoji} ${taskPred.category.label}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(taskPred.category.colorHex)
                                        )
                                    }

                                    // Duration Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isNight) Color.White.copy(alpha = 0.08f) else Color(
                                                    0xFF2563EB
                                                ).copy(alpha = 0.08f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = taskPred.durationLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNight) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                        )
                                    }

                                    // Risk / Urgency Tag
                                    if (taskPred.urgency == com.example.core.ml.TaskUrgency.CRITICAL) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = taskPred.urgency.tagLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(taskPred.risk.colorHex).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = taskPred.risk.tagLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(taskPred.risk.colorHex)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = item.subtitle,
                                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                            // AI Micro-Action Advice
                            if (taskPred.risk != com.example.core.ml.ProcrastinationRisk.LOW || taskPred.isDeepWork) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isNight) Color.White.copy(alpha = 0.04f) else Color(0xFF0F172A).copy(
                                                alpha = 0.03f
                                            )
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💡 ", fontSize = 10.sp)
                                    Text(
                                        text = taskPred.microActionStep,
                                        fontSize = 10.5.sp,
                                        color = if (isNight) Color(0xFFFCD34D) else Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (index < minOf(realItems.size, 4) - 1) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(
                                        alpha = 0.06f
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (One UI 9 Style Quick Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Add new task
                OutlinedButton(
                    onClick = onNavigateToTasks,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isNight) Color.White.copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.3f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isNight) Color.White.copy(alpha = 0.06f) else Color(0xFF2563EB).copy(alpha = 0.08f),
                        contentColor = if (isNight) Color.White else Color(0xFF2563EB)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Button 2: Open Device Calendar
                OutlinedButton(
                    onClick = { com.example.core.repository.CalendarRepository.openCalendarApp(context) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isNight) Color.White.copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.3f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isNight) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.6f),
                        contentColor = if (isNight) Color.White else Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Calendar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private data class ScheduleItemData(
    val time: String,
    val title: String,
    val subtitle: String,
    val eventId: Long? = null,
    val rawTime: Long = 0L
)

// -------------------------------------------------------------
// 3D CLAY MASCOT (3D Yellow Clay with Breathing & Bouncing Animation)
// -------------------------------------------------------------
@Composable
private fun ClayMascot3D(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clay_mascot_transition")

    // Breathing Effect (Breathing Scale: 0.95f -> 1.05f)
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clay_scale_y"
    )

    val scaleX by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clay_scale_x"
    )

    // Floating up and down (Floating Y: -6f -> 6f)
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clay_bounce_y"
    )

    Canvas(
        modifier = modifier
            .size(92.dp)
            .testTag("clay_mascot_3d")
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = (height / 2f) + bounceY

        // 1. Soft Clay Drop Shadow
        drawOval(
            color = Color(0x2E000000),
            topLeft = Offset(centerX - 30f, height - 12f),
            size = androidx.compose.ui.geometry.Size(60f, 12f)
        )

        // 52dp round yellow mascot
        val bodyRx = 35f * scaleX
        val bodyRy = 34f * scaleY

        // 2. 3D round clay body with warm yellow gradient & rim lighting
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A), // Top highlight
                    Color(0xFFFBBF24), // Bright yellow clay body
                    Color(0xFFF59E0B), // Amber gold
                    Color(0xFFD97706)  // Bottom clay shadow
                ),
                center = Offset(centerX - 9f, centerY - 10f),
                radius = 48f
            ),
            topLeft = Offset(centerX - bodyRx, centerY - bodyRy),
            size = androidx.compose.ui.geometry.Size(bodyRx * 2, bodyRy * 2)
        )

        // 3. Cute Clay Ears on top
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                center = Offset(centerX - 20f, centerY - bodyRy + 2f),
                radius = 14f
            ),
            radius = 9f,
            center = Offset(centerX - 19f, centerY - bodyRy + 5f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                center = Offset(centerX + 20f, centerY - bodyRy + 2f),
                radius = 14f
            ),
            radius = 9f,
            center = Offset(centerX + 19f, centerY - bodyRy + 5f)
        )

        // 4. Pastel Rosy Blush Cheeks
        drawCircle(
            color = Color(0xFFFB7185).copy(alpha = 0.62f),
            radius = 6f,
            center = Offset(centerX - 17f, centerY + 4f)
        )
        drawCircle(
            color = Color(0xFFFB7185).copy(alpha = 0.62f),
            radius = 6f,
            center = Offset(centerX + 17f, centerY + 4f)
        )

        // 5. Glossy Black Clay Eyes with white catchlight
        // Left eye
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 4f,
            center = Offset(centerX - 10f, centerY - 2f)
        )
        drawCircle(
            color = Color.White,
            radius = 1.6f,
            center = Offset(centerX - 11.2f, centerY - 3.6f)
        )
        // Right eye
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 4f,
            center = Offset(centerX + 10f, centerY - 2f)
        )
        drawCircle(
            color = Color.White,
            radius = 1.6f,
            center = Offset(centerX + 8.8f, centerY - 3.6f)
        )

        // 6. Cute Clay Happy Smile
        val mouthPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 6f, centerY + 6f)
            quadraticTo(centerX, centerY + 11.5f, centerX + 6f, centerY + 6f)
        }
        drawPath(
            path = mouthPath,
            color = Color(0xFF78350F),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 7. Sparkle highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 3f,
            center = Offset(centerX - 13f, centerY - 18f)
        )
    }
}

// -------------------------------------------------------------
// CARD 3: ENERGY SCORE (Energy Score Card - 61 • -9)
// -------------------------------------------------------------
@Composable
private fun EnergyScoreCard(
    healthData: RealHealthData?,
    isLoading: Boolean,
    hasHealthPermission: Boolean,
    diurnalPhase: DiurnalPhase,
    onSyncHealthConnect: () -> Unit
) {
    val isNight = diurnalPhase.isDark

    val energyScore = if (healthData != null && healthData.isDataAvailable) {
        val sleepScore = (healthData.sleepDurationMinutes / 480f * 50).coerceIn(20f, 50f)
        val hrScore = if (healthData.averageHeartRateBpm in 55..80) 40f else 30f
        (sleepScore + hrScore).toInt().coerceIn(45, 98)
    } else {
        61
    }
    val scoreDelta = if (energyScore >= 75) "+6" else "-9"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSyncHealthConnect() }
            .testTag("energy_score_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNight) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isNight) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title + Score + 3D Clay Mascot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.20f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Energy score",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Energy score",
                            color = if (isNight) Color.White else Color(0xFF0F172A),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Score 61 • -9
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$energyScore",
                            color = if (isNight) Color.White else Color(0xFF0F172A),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )

                        Text(
                            text = "• $scoreDelta",
                            color = if (scoreDelta.startsWith("-")) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(
                                        alpha = 0.05f
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (scoreDelta.startsWith("-")) "Down from yesterday" else "Up from yesterday",
                                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3D Yellow Clay Mascot Canvas with breathing & bouncing animation
                ClayMascot3D(
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (energyScore < 70) "Your energy level is moderate today. Pace yourself and avoid overworking." else "Abundant energy—ready to tackle your key goals today!",
                color = if (isNight) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Compact Metric Pills (Sleep • Heart Rate • Recovery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Sleep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNight) Color.White.copy(alpha = 0.06f) else Color(0xFFF8FAFC))
                        .border(
                            1.dp,
                            if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "🌙 Sleep",
                            fontSize = 10.sp,
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = healthData?.sleepDurationFormatted ?: "7h 20m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                // Metric 2: Heart Rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNight) Color.White.copy(alpha = 0.06f) else Color(0xFFF8FAFC))
                        .border(
                            1.dp,
                            if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "❤️ Heart rate",
                            fontSize = 10.sp,
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${healthData?.averageHeartRateBpm ?: 72} bpm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                // Metric 3: Recovery
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNight) Color.White.copy(alpha = 0.06f) else Color(0xFFF8FAFC))
                        .border(
                            1.dp,
                            if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "⚡ Recovery",
                            fontSize = 10.sp,
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "82% (Good)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Health Connect sync footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hasHealthPermission) Color(0xFF10B981) else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasHealthPermission) "Samsung Health • Connected" else "Tap to sync Health Connect",
                        color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LOCAL AI SUGGESTION ENGINE (Multi-context: Sports, Food & Dining, Travel)
// -------------------------------------------------------------
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

    // 1. SPORTS: Running / Cycling (Early morning 5:00-7:00 or Late afternoon 17:00-18:30 when cool < 30°C and not raining)
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

    // 2. FOOD & DINING: Time-of-day culinary picks
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

    // 3. TRAVEL & WEEKEND TRIP (Triggered Fri - Sun or in nice weather)
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

    // 4. If no Sports card yet (noon/evening), add indoor stretching suggestion
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

// -------------------------------------------------------------
// 3D CLAYMORPHISM STICKER VISUALS (52dp Prominent on Dark Glass)
// -------------------------------------------------------------

@Composable
fun ClayCoffeeCupSticker(
    isTea: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(52.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 4f

        // 1. Soft Drop Shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 20f, h - 8f),
            size = androidx.compose.ui.geometry.Size(40f, 8f)
        )

        // 2. Cup Handle (3D Clay Cup Handle)
        val handleBrush = Brush.radialGradient(
            colors = if (isTea) listOf(Color(0xFFFDE047), Color(0xFFD97706)) else listOf(
                Color(0xFFFFB74D),
                Color(0xFFE65100)
            ),
            center = androidx.compose.ui.geometry.Offset(cx + 18f, cy - 2f),
            radius = 16f
        )
        drawArc(
            brush = handleBrush,
            startAngle = -70f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx + 4f, cy - 14f),
            size = androidx.compose.ui.geometry.Size(22f, 24f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 3. Cup Body (Rounded 3D Clay Cup Body)
        val cupPath = androidx.compose.ui.graphics.Path().apply {
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
            center = androidx.compose.ui.geometry.Offset(cx - 6f, cy - 6f),
            radius = 28f
        )
        drawPath(path = cupPath, brush = cupBrush)

        // Specular highlight on cup body
        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 160f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - 16f, cy - 8f),
            size = androidx.compose.ui.geometry.Size(32f, 22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 4. Liquid Surface (Coffee / Chamomile Tea surface)
        drawOval(
            brush = Brush.radialGradient(
                colors = if (isTea) listOf(Color(0xFFFDE68A), Color(0xFFD97706)) else listOf(
                    Color(0xFFD7CCC8),
                    Color(0xFF4E342E)
                ),
                center = androidx.compose.ui.geometry.Offset(cx, cy - 12f),
                radius = 16f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 18f, cy - 18f),
            size = androidx.compose.ui.geometry.Size(36f, 12f)
        )

        // Latte Art / Chamomile Petal
        if (isTea) {
            drawCircle(
                color = Color(0xFFFEF08A),
                radius = 3.5f,
                center = androidx.compose.ui.geometry.Offset(cx, cy - 12f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2f,
                center = androidx.compose.ui.geometry.Offset(cx - 3.5f, cy - 12f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2f,
                center = androidx.compose.ui.geometry.Offset(cx + 3.5f, cy - 12f)
            )
        } else {
            drawCircle(
                color = Color(0xFFFFF8E1).copy(alpha = 0.80f),
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(cx - 2f, cy - 12f)
            )
        }

        // 5. Static Steam Puffs
        drawCircle(
            color = Color.White.copy(alpha = 0.60f),
            radius = 3f,
            center = androidx.compose.ui.geometry.Offset(cx - 4f, cy - 24f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = 3.8f,
            center = androidx.compose.ui.geometry.Offset(cx + 4f, cy - 30f)
        )
    }
}

@Composable
fun ClayBicycleSticker(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(52.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        // 1. Drop Shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 22f, h - 8f),
            size = androidx.compose.ui.geometry.Size(44f, 8f)
        )

        val mintLight = Color(0xFF6EE7B7)
        val mintMid = Color(0xFF10B981)
        val mintDark = Color(0xFF047857)

        // 2. Rear Wheel
        val rearWheelCenter = androidx.compose.ui.geometry.Offset(cx - 14f, cy + 6f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                center = rearWheelCenter,
                radius = 10f
            ),
            radius = 9.5f,
            center = rearWheelCenter
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(mintLight, mintMid), center = rearWheelCenter, radius = 6f),
            radius = 5.5f,
            center = rearWheelCenter
        )
        drawCircle(color = Color.White, radius = 1.8f, center = rearWheelCenter)

        // 3. Front Wheel
        val frontWheelCenter = androidx.compose.ui.geometry.Offset(cx + 14f, cy + 6f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                center = frontWheelCenter,
                radius = 10f
            ),
            radius = 9.5f,
            center = frontWheelCenter
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(mintLight, mintMid), center = frontWheelCenter, radius = 6f),
            radius = 5.5f,
            center = frontWheelCenter
        )
        drawCircle(color = Color.White, radius = 1.8f, center = frontWheelCenter)

        // 4. Bike Frame (3D Mint Claymorphism)
        val framePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(rearWheelCenter.x, rearWheelCenter.y)
            lineTo(cx - 2f, cy + 6f) // Pedal crank
            lineTo(cx + 8f, cy - 6f) // Handlebar base
            lineTo(frontWheelCenter.x, frontWheelCenter.y)
            moveTo(cx - 2f, cy + 6f)
            lineTo(cx - 6f, cy - 6f) // Saddle post
            lineTo(rearWheelCenter.x, rearWheelCenter.y)
            moveTo(cx - 6f, cy - 6f)
            lineTo(cx + 8f, cy - 6f) // Top tube
        }
        drawPath(
            path = framePath,
            brush = Brush.linearGradient(listOf(mintLight, mintDark)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // 5. Saddle
        drawOval(
            color = Color(0xFF1E293B),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 10f, cy - 10f),
            size = androidx.compose.ui.geometry.Size(9f, 4.5f)
        )

        // 6. Handlebar
        drawArc(
            color = Color(0xFF047857),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx + 6f, cy - 12f),
            size = androidx.compose.ui.geometry.Size(8f, 7f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Composable
fun ClayTentSticker(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(52.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 4f

        // 1. Soft Drop Shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 22f, h - 8f),
            size = androidx.compose.ui.geometry.Size(44f, 8f)
        )

        // 2. Main Tent Canopy (3D Coral/Salmon)
        val tentPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - 18f)
            lineTo(cx - 20f, cy + 14f)
            quadraticTo(cx, cy + 17f, cx + 20f, cy + 14f)
            close()
        }
        val tentBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFDA4AF), Color(0xFFFB7185), Color(0xFFE11D48)),
            center = androidx.compose.ui.geometry.Offset(cx - 6f, cy - 4f),
            radius = 26f
        )
        drawPath(path = tentPath, brush = tentBrush)

        // 3. Side Fabric Flap (Yellow clay accent)
        val flapPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - 18f)
            lineTo(cx + 20f, cy + 14f)
            lineTo(cx + 5f, cy + 15f)
            close()
        }
        drawPath(
            path = flapPath,
            brush = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B)))
        )

        // 4. Open Door with Warm Glowing Light
        val doorPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - 2f, cy - 4f)
            lineTo(cx - 10f, cy + 15f)
            lineTo(cx + 6f, cy + 15f)
            close()
        }
        drawPath(
            path = doorPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFF78350F)),
                center = androidx.compose.ui.geometry.Offset(cx - 2f, cy + 10f),
                radius = 12f
            )
        )

        // 5. Mini Flagpole & Pennant on Top
        drawLine(
            color = Color(0xFFE2E8F0),
            start = androidx.compose.ui.geometry.Offset(cx, cy - 18f),
            end = androidx.compose.ui.geometry.Offset(cx, cy - 26f),
            strokeWidth = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        val flagPath = androidx.compose.ui.graphics.Path().apply {
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

        // Drop shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 20f, h - 8f),
            size = androidx.compose.ui.geometry.Size(40f, 8f)
        )

        // Pot Ear Handles
        drawCircle(
            color = Color(0xFFEA580C),
            radius = 4.5f,
            center = androidx.compose.ui.geometry.Offset(cx - 18f, cy - 2f)
        )
        drawCircle(
            color = Color(0xFFEA580C),
            radius = 4.5f,
            center = androidx.compose.ui.geometry.Offset(cx + 18f, cy - 2f)
        )

        // Pot Bowl
        val potPath = androidx.compose.ui.graphics.Path().apply {
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
                center = androidx.compose.ui.geometry.Offset(cx - 6f, cy),
                radius = 24f
            )
        )

        // Broth
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFEA580C)),
                center = androidx.compose.ui.geometry.Offset(cx, cy - 6f),
                radius = 16f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 18f, cy - 12f),
            size = androidx.compose.ui.geometry.Size(36f, 12f)
        )

        // Static Steam
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = 3.5f,
            center = androidx.compose.ui.geometry.Offset(cx - 4f, cy - 22f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.40f),
            radius = 4.2f,
            center = androidx.compose.ui.geometry.Offset(cx + 5f, cy - 28f)
        )
    }
}

@Composable
fun ClayYogaSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        // Drop shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 18f, h - 8f),
            size = androidx.compose.ui.geometry.Size(36f, 8f)
        )

        // Glowing Aura Ring
        drawCircle(
            color = Color(0xFFC084FC).copy(alpha = 0.25f),
            radius = 18f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Center Lotus Petal (Pastel Purple Clay)
        val lotusPath = androidx.compose.ui.graphics.Path().apply {
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
                center = androidx.compose.ui.geometry.Offset(cx, cy - 2f),
                radius = 18f
            )
        )

        // Side petals
        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFD8B4FE), Color(0xFF9333EA))),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 16f, cy + 2f),
            size = androidx.compose.ui.geometry.Size(12f, 9f)
        )
        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFD8B4FE), Color(0xFF9333EA))),
            topLeft = androidx.compose.ui.geometry.Offset(cx + 4f, cy + 2f),
            size = androidx.compose.ui.geometry.Size(12f, 9f)
        )

        // Sparkle
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 2.5f,
            center = androidx.compose.ui.geometry.Offset(cx, cy - 4f)
        )
    }
}

@Composable
fun ClayRooftopSticker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(52.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 2f

        // Drop shadow
        drawOval(
            color = Color(0x38000000),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 16f, h - 8f),
            size = androidx.compose.ui.geometry.Size(32f, 8f)
        )

        // Cocktail Glass Body (Sunset Cocktail)
        val glassPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - 14f, cy - 10f)
            lineTo(cx + 14f, cy - 10f)
            lineTo(cx, cy + 8f)
            close()
        }
        drawPath(
            path = glassPath,
            brush = Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFF59E0B), Color(0xFF06B6D4)))
        )

        // Glass Stem & Base
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(cx, cy + 8f),
            end = androidx.compose.ui.geometry.Offset(cx, cy + 16f),
            strokeWidth = 2.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(cx - 7f, cy + 16f),
            end = androidx.compose.ui.geometry.Offset(cx + 7f, cy + 16f),
            strokeWidth = 2.5f
        )

        // Citrus Slice
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = 5.5f,
            center = androidx.compose.ui.geometry.Offset(cx + 12f, cy - 12f)
        )
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = 4f,
            center = androidx.compose.ui.geometry.Offset(cx + 12f, cy - 12f)
        )

        // Straw
        drawLine(
            color = Color(0xFF38BDF8),
            start = androidx.compose.ui.geometry.Offset(cx - 8f, cy - 20f),
            end = androidx.compose.ui.geometry.Offset(cx - 1f, cy),
            strokeWidth = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
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

// -------------------------------------------------------------
// ONE UI VERTICAL SMART LIFESTYLE SUGGESTION CARDS
// -------------------------------------------------------------
@Composable
private fun VerticalLifestyleSuggestionStack(
    lifestyleTips: List<com.example.core.ai.LifestyleTip>,
    suggestions: List<LocalAiSuggestion>,
    isAiInferring: Boolean,
    isDark: Boolean,
    onRefreshTips: () -> Unit,
    onToggleDone: (String) -> Unit,
    onSuggestionPositive: (String) -> Unit = {},
    onSuggestionDismiss: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row: Title + Shuffle Button
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

            // Shuffle Suggestions Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f)
                    )
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
            // Shimmer loading on vertical cards
            ShimmerLifestyleTipCard(isDark = isDark)
            ShimmerLifestyleTipCard(isDark = isDark)
        } else if (lifestyleTips.isNotEmpty()) {
            lifestyleTips.take(3).forEach { tip ->
                LifestyleTipCard(
                    tip = tip,
                    isDark = isDark,
                    onToggleDone = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
            animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
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
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
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
    tip: com.example.core.ai.LifestyleTip,
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
                    textDecoration = if (tip.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
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

            // 3D frosted glass sphere containing Icon/Emoji
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
            // Left: Category Badge + Tag + Title + Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Row: Category Badge + Tag + Small close X button
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

                    // Small X button to dismiss suggestion
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

                // Suggestion title
                Text(
                    text = suggestion.title,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Concise suggestion description
                Text(
                    text = suggestion.description,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: 3D CLAYMORPHISM STICKER / LARGE VISUAL (52dp)
            ClaymorphismSuggestionVisual(
                visualType = suggestion.visualType,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// CARD 4: SMART SUGGESTION WIDGETS (Editorial & Adaptive Widgets)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartSuggestionWidgetsSection(
    currentHour: Int,
    weatherData: RealWeatherData?,
    lifestyleTips: List<com.example.core.ai.LifestyleTip>,
    isAiInferring: Boolean,
    onToggleTipDone: (String) -> Unit,
    isDark: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mlEngine = remember { com.example.core.ml.InternalMlEngine.getInstance(context) }
    val feedbackTracker = remember { com.example.core.engine.UserFeedbackTracker(context) }
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

    // Build multidimensional LifestyleContext for LinUCB Contextual Bandit combined with Persona
    val cachedPersona = remember {
        mlEngine.getCachedPersonaProfile()?.personaType ?: com.example.core.ml.UserPersonaType.BALANCED_DEFAULT
    }
    val lifestyleContext = remember(currentHour, tempInt, isRainy, leadTextRefreshKey, cachedPersona) {
        com.example.core.ml.LifestyleContext.fromCurrentState(
            hourOfDay = currentHour,
            temperatureC = tempInt,
            isRainy = isRainy,
            streakDays = mlEngine.streakTracker.getStreakCount(),
            personaType = cachedPersona
        )
    }

    // Lead text generated by Contextual Bandit (context-aware: weekend, rain, streak, temperature)
    val aiLeadText = remember(lifestyleContext, weatherDesc, leadTextRefreshKey) {
        mlEngine.generateContextualLeadText(lifestyleContext, weatherDesc)
    }

    // Rank tips using LinUCB (8-dimensional context vector)
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
        // Editorial Lead Text auto-generated from LinUCB Contextual Bandit (Clean Typography, strictly no emoji)
        Text(
            text = aiLeadText,
            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Vertical Lifestyle Suggestion Stack: Vertical card list displayed directly in One UI style
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
                    // Record interaction via LinUCB Contextual Bandit (dual-write legacy + LinUCB)
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
            // Nighttime: Tomorrow Morning Alarm Widget + 3 Mindfulness Mini Cards Row
            MorningAlarmWidget(isDark = isDark)

            MindfulnessMiniCardsRow(
                isDark = isDark,
                onSelect = { selectedMindfulnessType = it }
            )
        }
    }

    // Modal BottomSheet when tapping the 3 Mindfulness Mini Cards
    if (selectedMindfulnessType != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        when (selectedMindfulnessType) {
            "mood" -> MoodTrackerBottomSheet(
                onDismiss = { selectedMindfulnessType = null },
                onSaveMood = { selectedMood, selectedTags ->
                    // Saved mood state
                },
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

// -------------------------------------------------------------
// SPOTIFY / MEDIA CARD WIDGET (Morning / Afternoon)
// -------------------------------------------------------------
@Composable
private fun SpotifyMediaSuggestionCard(
    currentHour: Int,
    isDark: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }

    val (playlistTitle, playlistDesc, genreTag) = if (currentHour in 6..11) {
        Triple(
            "Morning Acoustic - Fresh Start",
            "Gentle acoustic melodies to kick off an inspiring new day",
            "Morning boost"
        )
    } else {
        Triple(
            "Lofi Beats & Deep Focus",
            "Soothing beats to maintain peak focus without fatigue",
            "Deep focus"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spotify_wave_anim")
    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spotify_media_card"),
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artistic 3D Vinyl Record Squircle Album Cover
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Grooves & 3D Lighting
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // Vinyl record grooves
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = 24f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 19f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = 14f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )

                    // Emerald green Spotify center label
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1ED760), Color(0xFF1DB954), Color(0xFF047857)),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            radius = 11f
                        ),
                        radius = 10f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )

                    // Center spindle hole
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 2.5f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                }

                // Subtle Music Note Icon in Center
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track/playlist info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Spotify",
                        color = Color(0xFF1DB954),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• $genreTag",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = playlistTitle,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = playlistDesc,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Pause Button & Wave Visualizer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPlaying) {
                    Row(
                        modifier = Modifier.height(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(3.dp).height(waveHeight1.dp).clip(CircleShape)
                                .background(Color(0xFF1DB954))
                        )
                        Box(
                            modifier = Modifier.width(3.dp).height(waveHeight2.dp).clip(CircleShape)
                                .background(Color(0xFF1DB954))
                        )
                        Box(
                            modifier = Modifier.width(3.dp).height(waveHeight3.dp).clip(CircleShape)
                                .background(Color(0xFF1DB954))
                        )
                    }
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF1DB954) else Color(0xFF1DB954).copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color.White else Color(0xFF1DB954),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TOMORROW MORNING ALARM WIDGET (Evening / Night)
// -------------------------------------------------------------
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

// -------------------------------------------------------------
// 3 MINDFULNESS MINI CARDS ROW (Mood • Breathing • Meditation)
// -------------------------------------------------------------
@Composable
private fun MindfulnessMiniCardsRow(
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mini Card 1: [Mood check-in]
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

        // Mini Card 2: [Breathing exercise]
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

        // Mini Card 3: [Meditation]
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

    // Subtle motion animation tailored to each type (gentle breathing / pulse / glow)
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
            // Icon Badge inside One UI Frosted Glass Gradient Circle
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

// -------------------------------------------------------------
// INTERACTIVE MINDFULNESS DIALOG (Interactive Mindfulness Dialog)
// -------------------------------------------------------------
@Composable
private fun MindfulnessInteractiveDialog(
    type: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val (title, desc, icon, iconTint) = when (type) {
        "mood" -> Tuple4(
            "Mood Check-in",
            "How are you feeling today? Logging your mood helps AI balance tomorrow's routine.",
            Icons.Default.SentimentSatisfiedAlt,
            Color(0xFFEC4899)
        )

        "breath" -> Tuple4(
            "4-7-8 Breathing Exercise",
            "Inhale for 4s • Hold for 7s • Exhale slowly for 8s to relax your heart rate.",
            Icons.Default.Air,
            Color(0xFF06B6D4)
        )

        else -> Tuple4(
            "5-Minute Calm Meditation",
            "Close your eyes, relax your shoulders, and focus on each natural breath.",
            Icons.Default.SelfImprovement,
            Color(0xFF8B5CF6)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    lineHeight = 19.sp
                )

                if (type == "mood") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("😄 Great", "🙂 Okay", "😴 Tired", "🧘 Calm").forEach { mood ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                    .clickable { onDismiss() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(text = mood, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = iconTint),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            }
        },
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// -------------------------------------------------------------
// TOMORROW BEHAVIOR PREDICTION CARD — ONE UI STYLE (SIMILAR TO ONE UI CALENDAR CARD)
// -------------------------------------------------------------
@Composable
private fun BehaviorPredictionInsightCard(
    isDark: Boolean,
    realHealthData: com.example.core.repository.RealHealthData? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mlEngine = remember { com.example.core.ml.InternalMlEngine.getInstance(context) }

    // Calculate today's activity rate
    val todayActivityRate = if (realHealthData != null && realHealthData.stepGoal > 0) {
        ((realHealthData.currentSteps.toFloat() / realHealthData.stepGoal) * 0.5f +
                (realHealthData.currentActiveMinutes.toFloat() / realHealthData.activeGoalMinutes.coerceAtLeast(1)) * 0.3f +
                (realHealthData.currentCalories.toFloat() / realHealthData.calorieGoal.coerceAtLeast(1)) * 0.2f).coerceIn(
            0f,
            2f
        )
    } else 0f
    val todaySteps = realHealthData?.currentSteps ?: 0

    // Read cache first (synchronous), then launch coroutine refresh
    var prediction by remember {
        mutableStateOf(
            mlEngine.getCachedBehaviorPrediction()
                ?: com.example.core.ml.BehaviorPrediction()
        )
    }

    LaunchedEffect(realHealthData?.currentSteps, realHealthData?.currentCalories) {
        prediction = mlEngine.predictTomorrowBehavior(todayActivityRate, todaySteps)
    }

    // Only display when >= 3 days of data are available
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

    val sleepAccentColor = Color(0xFF818CF8) // Indigo Purple
    val wakeAccentColor = Color(0xFF38BDF8)  // Cyan Blue

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
            // -------------------------------------------------------------
            // [HEADER]: Squircle Icon + App/Source Title + Badge Trend
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // App Icon inside Squircle Container (36dp, RoundedCornerShape 12dp)
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

                // Trend Badge (Pill Container matching OneUICalendarCard, clean without emoji)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when (prediction.trendDirection) {
                                com.example.core.ml.TrendDirection.IMPROVING -> Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.12f)
                                com.example.core.ml.TrendDirection.DECLINING -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.12f)
                                com.example.core.ml.TrendDirection.STABLE -> if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(
                                    alpha = 0.05f
                                )
                            }
                        )
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val badgeColor = when (prediction.trendDirection) {
                            com.example.core.ml.TrendDirection.IMPROVING -> Color(0xFF10B981)
                            com.example.core.ml.TrendDirection.DECLINING -> Color(0xFFEF4444)
                            com.example.core.ml.TrendDirection.STABLE -> if (isDark) Color(0xFFCBD5E1) else Color(
                                0xFF475569
                            )
                        }

                        Icon(
                            imageVector = when (prediction.trendDirection) {
                                com.example.core.ml.TrendDirection.IMPROVING -> Icons.Default.ArrowDropUp
                                com.example.core.ml.TrendDirection.DECLINING -> Icons.Default.ArrowDropDown
                                com.example.core.ml.TrendDirection.STABLE -> Icons.Default.AccessTime
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

            // -------------------------------------------------------------
            // [CONTENT SECTION]: 2 Prediction Items (Tonight + Tomorrow morning)
            // -------------------------------------------------------------
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Item 1: Tonight's sleep
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Colored vertical accent bar
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(sleepAccentColor)
                    )

                    // Sleep prediction details
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

                // Thin divider between the 2 items
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                )

                // Item 2: Tomorrow morning wake-up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Colored vertical accent bar
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(wakeAccentColor)
                    )

                    // Wake-up prediction details
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

                // -------------------------------------------------------------
                // [ACTIVITY IMPACT BANNER]: Activity impact on sleep & Recovery
                // -------------------------------------------------------------
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

                // -------------------------------------------------------------
                // [BOTTOM BANNER]: Action Recommendation or Insight
                // -------------------------------------------------------------
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
