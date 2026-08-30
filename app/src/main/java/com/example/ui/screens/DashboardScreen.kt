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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
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
        if (grantedPermissions.containsAll(HealthConnectRepository.REQUIRED_PERMISSIONS)) {
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

    // Tự động làm mới dữ liệu lịch & Glance widget ngay lập tức mỗi khi mở lại app (ON_RESUME)
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

    // ── Live Clock Ticker: cập nhật Session & Phase mỗi phút tự động ──
    var liveCal by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            liveCal = now
            // Tính số giây còn lại đến đầu phút tiếp theo để đồng bộ chính xác
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
                rawDesc.contains("ít mây", ignoreCase = true) -> "Trời có mây"
                rawDesc.contains("có mây", ignoreCase = true) -> "Trời có mây"
                rawDesc.contains("mưa", ignoreCase = true) -> "Trời có mưa"
                rawDesc.contains("nắng", ignoreCase = true) -> "Trời nắng ráo"
                rawDesc.isBlank() -> "Trời nhiều mây"
                else -> if (rawDesc.startsWith("Trời", ignoreCase = true)) rawDesc else "Trời $rawDesc"
            }
            val temp = Math.round(weather.currentTempC).toInt()
            val location = weather.locationName.ifBlank { "TP. Hồ Chí Minh" }
            "$cleanDesc, $temp°C tại $location"
        } else {
            when (diurnalPhase) {
                DiurnalPhase.NIGHT -> "Trời đêm, 23°C tại TP. Hồ Chí Minh"
                DiurnalPhase.EVENING -> "Hoàng hôn, 26°C tại TP. Hồ Chí Minh"
                DiurnalPhase.AFTERNOON -> "Nắng ráo, 33°C tại TP. Hồ Chí Minh"
                DiurnalPhase.MORNING -> "Nắng sớm, 31°C tại TP. Hồ Chí Minh"
            }
        }
    }

    val formattedSpent = DecimalFormat("#,###").format(metrics.totalExpensesVnd)

    val audioBriefScript =
        remember(metrics, headlineTitle, aiWeatherSnippet, allTasks, calendarEvents, realHealthData) {
            val taskCount = allTasks.filter { !it.isCompleted }.size + calendarEvents.size
            val healthBrief = if (realHealthData != null && realHealthData?.isDataAvailable == true) {
                "Giấc ngủ đêm qua đạt ${realHealthData?.sleepDurationFormatted}, nhịp tim trung bình ${realHealthData?.averageHeartRateBpm} bpm."
            } else {
                "Dữ liệu sức khỏe sẵn sàng khi kết nối Samsung Health."
            }
            "$headlineTitle, Alex. $subtitle $aiWeatherSnippet $healthBrief Bạn có $taskCount lịch trình và việc cần lưu ý hôm nay."
        }

    val dateFormatted = remember(liveCal) {
        SimpleDateFormat("EEEE, d 'thg' M", Locale("vi", "VN")).format(liveCal.time)
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
                it.id.startsWith("alarm_check_")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // INDEX 0: CỤM DYNAMIC HEADER THEO BUỔI (PHA 1: Header Focus & Fade)
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
                                        contentDescription = "Nội dung muốn đưa vào",
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
                                        contentDescription = "Cài đặt & Cấu hình",
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

                        // Cụm Văn bản Header được áp dụng hiệu ứng Text Alpha Mask Reveal / Wipe (45°, 5000ms, feather 180px, quét 1 lần khi mở app)
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
                                // Skeleton / Shimmer loading khi On-Device Model đang suy luận (200-400ms)
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
                                        .replace(Regex("Nhiệt độ \\d+°C,"), "")
                                        .replace(Regex("nắng \\d+°C,"), "")
                                        .replace(Regex("mưa \\d+°C,"), "")
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

            // INDEX 1: CARD THỜI TIẾT
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

                // INDEX 1.5: CARD NHẮC NHỞ THỜI TIẾT ĐỘNG (STICKER + GỢI Ý VẬT DỤNG NẮNG/MƯA/ĐÊM)
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

            // INDEX 1.6: TRỢ LÝ CẢNH BÁO NGỮ CẢNH (LỊCH TRÌNH + THỜI TIẾT THEO GIỜ / PIN / MEET)
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
                                        val rec = com.example.core.music.MusicRecommendationHelper.getRecommendationForSession(currentHour)
                                        com.example.core.music.MusicRecommendationHelper.openMusicApp(context, rec.query, rec.spotifyUri)
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

            // INDEX 2: CARD ĐIỂM NĂNG LƯỢNG & SỨC KHỎE (HOẠT ĐỘNG HẰNG NGÀY)
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

            // CARD LỊCH TRÌNH CHUẨN ONE UI 9 (Đọc dữ liệu Lịch thật & Room DB)
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
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(e.startTimeMillis))
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

            // CARD BÁO CÁO THÓI QUEN & ĐIỂM NĂNG LƯỢNG TUẦN (CHUẨN ONE UI GLASSMORPHISM)
            if ("habit_insights" in enabledContentCards) {
                item {
                    val habitAnalyticsEngine = remember(context) { com.example.core.engine.HabitAnalyticsEngine(context) }
                    val firstUpcomingItem = remember(allTasks, calendarEvents) {
                        val nextTask = allTasks.firstOrNull { !it.isCompleted }
                        if (nextTask != null) {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTask.datetime))
                            nextTask.title to timeStr
                        } else {
                            val nextEvent = calendarEvents.firstOrNull()
                            if (nextEvent != null) {
                                val timeStr =
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextEvent.startTimeMillis))
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
            }

            // INDEX 3: CARD TRẠNG THÁI GIẤC NGỦ & PHỤC HỒI
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

            // INDEX 4: CARD ĐỀ XUẤT NHẠC THÔNG MINH (DUY NHẤT 1 CARD SPOTIFY / YOUTUBE DEEP LINK)
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

            // INDEX 4.5: CARD TIN TỨC & THỊ TRƯỜNG CHUẨN ONE UI 9 (MARKET TICKER + ĐIỂM TIN BÁO CHÍ)
            if ("news_market" in enabledContentCards) {
                item {
                    StaggeredAnimatedItem(index = 4, hasPlayedAnimation = hasPlayedEntranceAnimation) {
                        NewsMarketBriefCard(
                            isDark = isDark
                        )
                    }
                }
            }

            // INDEX 5+: CÁC CARD GỢI Ý LỐI SỐNG "SAU ĐÂY LÀ ĐỀ XUẤT CÓ THỂ BẠN SẼ THÍCH" & MINDFULNESS
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

            // BOTTOM SPACER: TRÁNH CẤN THANH ĐIỀU HƯỚNG NAVIGATION BARS
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
            contentDescription = "Nghe tóm tắt",
            tint = if (isPlaying) Color(0xFF3B82F6).copy(alpha = pulseAlpha) else if (isDark) Color(0xFF94A3B8) else Color(
                0xFF64748B
            ),
            modifier = Modifier.size(22.dp)
        )
        if (isPlaying) {
            Text(
                text = "Đang đọc...",
                color = Color(0xFF3B82F6),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// -------------------------------------------------------------
// THẺ 1: THỜI TIẾT THẬT (Open-Meteo Real Data Source)
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
        if (weatherData != null) "Cảm giác ${Math.round(weatherData.apparentTempC).toInt()}°" else ""
    val conditionText = weatherData?.conditionDescription ?: if (isNight) "Đêm dịu mát" else "Bình minh trong lành"
    val locationLabel = weatherData?.locationName ?: "Vị trí hiện tại"

    // Badge "Lưu lúc HH:mm" khi hiển thị data từ cache
    val cacheLabel = if (weatherData?.isFromCache == true && weatherData.cacheTimestampMs > 0L) {
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = weatherData.cacheTimestampMs }
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        "Lưu lúc $h:$m"
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
                            text = if (!hasLocationPermission) "Cấp quyền vị trí để lấy thời tiết thực" else "Đang cập nhật thời tiết từ Open-Meteo...",
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
                            Text("Cấp quyền", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = Color.White)
                        }
                    }
                }
            }

            // Weekly Forecast Row (7 ngày - One UI 9 Style)
            if (weatherData != null && weatherData.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                // Divider mỏng
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Dự báo 7 ngày",
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
                                fontWeight = if (day.dayLabel == "Hôm nay") FontWeight.Bold else FontWeight.Normal
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
                // Mưa giông, sấm sét
                wCode in listOf(95, 96, 99) ->
                    "Có giông sấm sét! Hạn chế ra ngoài và tránh xa vùng trống."

                // Mưa (nhẹ đến nặng)
                wCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ->
                    "Trời đang mưa, nhớ mang ô hoặc áo mưa khi ra ngoài."

                // Sương mù
                wCode in listOf(45, 48) ->
                    "Sương mù dày, tầm nhìn hạn chế. Lái xe cẩn thận."

                // Nắng nóng gay gắt (cảm giác >= 40°C hoặc nhiệt độ >= 38°C)
                feelsC >= 40.0 || tempC >= 38.0 ->
                    "Nắng nóng gay gắt, cảm giác ${Math.round(feelsC).toInt()}°C! Hạn chế ra ngoài trời, uống đủ nước."

                // Nóng / Oi bức (cảm giác >= 34°C hoặc nhiệt độ >= 31°C)
                feelsC >= 34.0 || tempC >= 31.0 ->
                    "Trời nóng oi bức, cảm giác ${Math.round(feelsC).toInt()}°C. Hãy che nắng và bổ sung nước thường xuyên."

                // Nắng ấm quang đãng
                tempC >= 28.0 && wCode in listOf(0, 1) ->
                    "Nắng ấm ${Math.round(tempC).toInt()}°C, trời quang đãng. Nhớ bôi kem chống nắng nếu ra ngoài."

                // Mây nhiều dịu mát
                wCode in listOf(2, 3) ->
                    "Trời nhiều mây ${Math.round(tempC).toInt()}°C, thời tiết dịu mát dễ chịu."

                // Lạnh (< 20°C)
                tempC < 20.0 ->
                    "Trời se lạnh ${Math.round(tempC).toInt()}°C. Nhớ mặc áo ấm khi ra ngoài."

                // Đêm mát mẻ
                isNight ->
                    "Đêm nay ${Math.round(tempC).toInt()}°C, thời tiết dễ chịu. Hãy nghỉ ngơi trọn vẹn!"

                // Mặc định
                else ->
                    "Thời tiết hiện tại ${Math.round(tempC).toInt()}°C. Thuận tiện cho di chuyển và hoạt động."
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
                        contentDescription = "Lời khuyên",
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
// THẺ 2: LỊCH TRÌNH THẬT (Calendar Contract & Room Database)
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
                    Pair("Lịch trình hôm nay", remaining)
                } else {
                    val tomorrow = try {
                        calendarRepo.getTomorrowEvents()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    if (tomorrow.isNotEmpty()) {
                        Pair("Lịch trình ngày mai", tomorrow)
                    } else {
                        Pair("Lịch trình hôm nay", emptyList())
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
                                subtitle = "Nguồn: ${t.sourceApp}",
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
                        contentDescription = "Xem lịch",
                        tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Tự động đọc lịch trình từ Samsung Calendar, Google Calendar và các tài khoản đã liên kết trên máy.",
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            // Tóm tắt thông minh cho sự kiện sắp tới (AI Event Summary Banner)
            if (realItems.isNotEmpty()) {
                val nextEvent = realItems.first()
                val totalCount = realItems.size
                val summaryText = "Sắp tới ${nextEvent.time}: ${nextEvent.title}" +
                        if (totalCount > 1) " (tổng ${totalCount} sự kiện & đầu việc sắp diễn ra)" else ""

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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
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
                                text = "⚡ Giờ vàng: 09:00 - 11:30",
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
                                text = "Đồng bộ Lịch thiết bị",
                                color = if (isNight) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tự động đọc lịch trình từ Samsung Calendar, Google Calendar và các tài khoản đã liên kết trên máy.",
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
                            Text("Cấp quyền", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                            text = "Không có lịch trình hay đầu việc nào sắp tới",
                            color = if (isNight) Color.White else Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Khi có lịch sự kiện hoặc tin nhắn nhắc việc, AI sẽ tự động đồng bộ tại đây.",
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
                                            .background(if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFF2563EB).copy(alpha = 0.08f))
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
                                        .background(if (isNight) Color.White.copy(alpha = 0.04f) else Color(0xFF0F172A).copy(alpha = 0.03f))
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
                // Button 1: Thêm việc mới
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
                    Text("Thêm việc", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Button 2: Mở Lịch thiết bị
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
                    Text("Mở Lịch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
// 3D CLAY MASCOT (Đất sét 3D Vàng với Animation thở & nhún nhảy)
// -------------------------------------------------------------
@Composable
private fun ClayMascot3D(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clay_mascot_transition")

    // Hiệu ứng thở (Breathing Scale: 0.95f -> 1.05f)
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

    // Nhún nhảy lên xuống (Floating Y: -6f -> 6f)
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

        // 1. Bóng đổ đất sét mềm (Soft Clay Drop Shadow)
        drawOval(
            color = Color(0x2E000000),
            topLeft = Offset(centerX - 30f, height - 12f),
            size = androidx.compose.ui.geometry.Size(60f, 12f)
        )

        // Mascot vàng 52dp thân tròn
        val bodyRx = 35f * scaleX
        val bodyRy = 34f * scaleY

        // 2. Thân đất sét tròn 3D với Gradient vàng ấm & viền ánh sáng
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A), // Ánh sáng đỉnh
                    Color(0xFFFBBF24), // Thân vàng đất sét tươi
                    Color(0xFFF59E0B), // Vàng hổ phách
                    Color(0xFFD97706)  // Bóng tối đáy đất sét
                ),
                center = Offset(centerX - 9f, centerY - 10f),
                radius = 48f
            ),
            topLeft = Offset(centerX - bodyRx, centerY - bodyRy),
            size = androidx.compose.ui.geometry.Size(bodyRx * 2, bodyRy * 2)
        )

        // 3. Tai đất sét tròn nhỏ trên đỉnh (Cute Clay Ears)
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

        // 4. Má hồng đất sét Pastel (Rosy Blush Cheeks)
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

        // 5. Mắt đất sét đen bóng với đốm sáng trắng (Clay Eyes)
        // Mắt trái
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
        // Mắt phải
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

        // 6. Miệng cười đất sét đáng yêu (Happy Smile)
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

        // 7. Ngôi sao/Điểm sáng lấp lánh (Sparkle)
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 3f,
            center = Offset(centerX - 13f, centerY - 18f)
        )
    }
}

// -------------------------------------------------------------
// THẺ 3: ĐIỂM NĂNG LƯỢNG (Energy Score Card - 61 • -9)
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
            // Header Row: Tiêu đề + Điểm + Linh vật Đất sét 3D
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
                                contentDescription = "Điểm năng lượng",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Điểm năng lượng",
                            color = if (isNight) Color.White else Color(0xFF0F172A),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Điểm số 61 • -9
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
                                text = if (scoreDelta.startsWith("-")) "Giảm so với hôm qua" else "Tăng so với hôm qua",
                                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3D Yellow Clay Mascot Canvas với animation thở & nhún nhảy
                ClayMascot3D(
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (energyScore < 70) "Mức năng lượng hôm nay ở mức trung bình. Hãy nghỉ ngơi hợp lý và tránh làm việc quá sức." else "Năng lượng dồi dào, sẵn sàng cho các mục tiêu trọng tâm hôm nay!",
                color = if (isNight) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Hàng chỉ số gọn gàng (Giấc ngủ • Nhịp tim • Phục hồi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Giấc ngủ
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
                            text = "🌙 Giấc ngủ",
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

                // Metric 2: Nhịp tim
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
                            text = "❤️ Nhịp tim",
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

                // Metric 3: Phục hồi
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
                            text = "⚡ Phục hồi",
                            fontSize = 10.sp,
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "82% (Tốt)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer kết nối Health Connect
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
                        text = if (hasHealthPermission) "Samsung Health • Đã kết nối" else "Chạm để đồng bộ Health Connect",
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
// LOCAL AI SUGGESTION ENGINE (Đa ngữ cảnh: Thể thao, Ẩm thực, Du lịch)
// -------------------------------------------------------------
enum class SuggestionVisualType {
    COFFEE_CUP,      // 3D Peach / Amber Clay Coffee Cup với hơi nước bốc lên
    TEA_POT,         // 3D Chamomile Teapot / Cup vàng mật ong ấm cúng
    BICYCLE,         // 3D Mint Green / Emerald Clay Bicycle nổi khối
    TENT_CAMPING,    // 3D Coral / Pastel Yellow Camping Tent có ánh đèn ấm
    HOTPOT_BOWL,     // 3D Hotpot / Ramen bowl với nước dùng bốc khói
    YOGA_MEDITATION, // 3D Lotus / Giãn cơ Yoga với vòng hào quang êm dịu
    ROOFTOP_CHILL    // 3D Sunset Cocktail ngắm hoàng hôn thành phố
}

data class LocalAiSuggestion(
    val id: String,
    val category: String, // "Thể thao", "Ẩm thực", "Du lịch"
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
            condition.contains("mưa") || condition.contains("dông")

    val suggestions = mutableListOf<LocalAiSuggestion>()

    // 1. THỂ THAO: Chạy bộ / Đạp xe (Sáng sớm 5h-7h hoặc Chiều tà 17h-18h30 khi trời mát < 30°C không mưa)
    val isCoolSportTime = (currentHour in 5..7 || currentHour in 17..18) && !isRaining && temp <= 29
    if (isCoolSportTime) {
        suggestions.add(
            LocalAiSuggestion(
                id = "sport_run_bike",
                category = "Thể thao",
                categoryBadgeColor = Color(0xFF10B981).copy(alpha = 0.20f),
                categoryTextColor = Color(0xFF10B981),
                title = if (currentHour in 5..7) "Chạy bộ sáng sớm & Hít thở" else "Đạp xe dạo quanh công viên",
                description = "Thời tiết $temp°C mát mẻ lý tưởng cho buổi chạy bộ 5km hoặc đạp xe hít thở không khí trong lành.",
                tag = "Trời mát $temp°C",
                visualType = SuggestionVisualType.BICYCLE
            )
        )
    }

    // 2. ẨM THỰC: Món ngon theo buổi
    val foodSuggestion = when {
        isRaining -> LocalAiSuggestion(
            id = "food_rain_hotpot",
            category = "Ẩm thực",
            categoryBadgeColor = Color(0xFFEA580C).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFEA580C),
            title = "Lẩu ấm nồng & Món nướng",
            description = "Trời mưa lành lạnh ($temp°C), quây quần bên nồi lẩu nóng hổi, thịt nướng hoặc bát cháo sườn ấm bụng.",
            tag = "Chiều mưa $temp°C",
            visualType = SuggestionVisualType.HOTPOT_BOWL
        )

        currentHour in 5..10 -> LocalAiSuggestion(
            id = "food_morning_coffee",
            category = "Ẩm thực",
            categoryBadgeColor = Color(0xFFD97706).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFD97706),
            title = "Cà phê sữa đá & Bánh mì giòn",
            description = "Khởi đầu ngày mới tỉnh táo với ly cà phê rang xay đậm đà kèm bánh mì nóng giòn hoặc phở bò tái thơm lừng.",
            tag = "Bữa sáng",
            visualType = SuggestionVisualType.COFFEE_CUP
        )

        currentHour in 11..14 && temp >= 30 -> LocalAiSuggestion(
            id = "food_noon_refresh",
            category = "Ẩm thực",
            categoryBadgeColor = Color(0xFF06B6D4).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF06B6D4),
            title = "Nước dừa tươi & Bữa trưa thanh nhiệt",
            description = "Trưa nắng oi bức ($temp°C), bổ sung nước dừa xiêm, trà trái cây và cơm trưa thanh đạm giải nhiệt cơ thể.",
            tag = "Giải nhiệt",
            visualType = SuggestionVisualType.ROOFTOP_CHILL
        )

        currentHour in 11..14 -> LocalAiSuggestion(
            id = "food_noon_lunch",
            category = "Ẩm thực",
            categoryBadgeColor = Color(0xFF059669).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF059669),
            title = "Bữa trưa dinh dưỡng & Salad",
            description = "Duy trì năng lượng bền bỉ cho ca chiều với thực đơn giàu protein nạc và nhiều rau củ tươi.",
            tag = "Bữa trưa",
            visualType = SuggestionVisualType.HOTPOT_BOWL
        )

        else -> LocalAiSuggestion(
            id = "food_evening_dinner",
            category = "Ẩm thực",
            categoryBadgeColor = Color(0xFF8B5CF6).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF8B5CF6),
            title = "Bữa tối nhẹ bụng & Trà hoa cúc",
            description = "Tránh đồ nhiều dầu mỡ sau 19h30, nhâm nhi một tách trà hoa cúc ấm giúp dễ dàng đi vào giấc ngủ sâu.",
            tag = "Thư giãn tối",
            visualType = SuggestionVisualType.TEA_POT
        )
    }
    suggestions.add(foodSuggestion)

    // 3. DU LỊCH & WEEKEND TRIP (Kích hoạt T6 - Chủ Nhật hoặc khi trời đẹp)
    val travelSuggestion = if (isWeekend) {
        LocalAiSuggestion(
            id = "travel_weekend_picnic",
            category = "Du lịch",
            categoryBadgeColor = Color(0xFF3B82F6).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFF3B82F6),
            title = "Picnic công viên & Cắm trại cuối tuần",
            description = "Cuối tuần thời tiết đẹp, lên lịch dã ngoại công viên, cắm trại ngoại ô hoặc hẹn hò cà phê chill cùng bạn bè.",
            tag = "Cuối tuần",
            visualType = SuggestionVisualType.TENT_CAMPING
        )
    } else {
        LocalAiSuggestion(
            id = "travel_chill_spot",
            category = "Du lịch",
            categoryBadgeColor = Color(0xFFEC4899).copy(alpha = 0.20f),
            categoryTextColor = Color(0xFFEC4899),
            title = "Cà phê Rooftop ngắm hoàng hôn",
            description = "Thư giãn sau giờ làm việc với không gian mở thoáng đãng, ngắm nhìn nhịp sống thành phố lên đèn.",
            tag = "Khám phá",
            visualType = SuggestionVisualType.ROOFTOP_CHILL
        )
    }
    suggestions.add(travelSuggestion)

    // 4. Nếu chưa có thẻ Thể thao (trưa/tối), thêm gợi ý Thể thao giãn cơ trong nhà
    if (suggestions.none { it.category == "Thể thao" }) {
        suggestions.add(
            LocalAiSuggestion(
                id = "sport_indoor_stretch",
                category = "Thể thao",
                categoryBadgeColor = Color(0xFF10B981).copy(alpha = 0.20f),
                categoryTextColor = Color(0xFF10B981),
                title = "Giãn cơ 15 phút & Yoga nhẹ",
                description = "Thực hiện các động tác xoay khớp, duỗi cơ cột sống để giải tỏa nhức mỏi sau thời gian dài ngồi bàn máy tính.",
                tag = "Phục hồi",
                visualType = SuggestionVisualType.YOGA_MEDITATION
            )
        )
    }

    return suggestions
}

// -------------------------------------------------------------
// 3D CLAYMORPHISM STICKER VISUALS (52dp Nổi bật trên Dark Glass)
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

        // 2. Cup Handle (Quai ly đất sét nổi 3D)
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

        // 3. Cup Body (Thân ly đất sét tròn bầu bĩnh)
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

        // Specular highlight trên thân ly
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

        // 4. Liquid Surface (Mặt nước cà phê thơm / trà hoa cúc)
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

        // 5. Static Steam Puffs (Hơi nước tĩnh mềm mại)
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

        // 2. Rear Wheel (Bánh sau)
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

        // 3. Front Wheel (Bánh trước)
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

        // 4. Bike Frame (Khung xe đạp 3D Mint Claymorphism)
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

        // 5. Saddle (Yên xe)
        drawOval(
            color = Color(0xFF1E293B),
            topLeft = androidx.compose.ui.geometry.Offset(cx - 10f, cy - 10f),
            size = androidx.compose.ui.geometry.Size(9f, 4.5f)
        )

        // 6. Handlebar (Ghi đông)
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

        // 2. Main Tent Canopy (Thân lều cắm trại 3D Coral/Salmon)
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

        // 3. Side Fabric Flap (Viền phối màu vàng đất sét)
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

        // 4. Open Door with Warm Glowing Light (Cửa lều mở có ánh đèn ấm vàng)
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

        // 5. Mini Flagpole & Pennant on Top (Cờ cắm trại trên đỉnh)
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

        // Center Lotus Petal (Cánh sen đất sét tím Pastel)
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

        // Cocktail Glass Body (Ly cocktail Sunset)
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
// GIAO DIỆN CAROUSEL GỢI Ý CUỘN NGANG (Horizontal Suggestion Carousel)
// -------------------------------------------------------------
@Composable
private fun HorizontalSuggestionCarousel(
    lifestyleTips: List<com.example.core.ai.LifestyleTip>,
    suggestions: List<LocalAiSuggestion>,
    isAiInferring: Boolean,
    isDark: Boolean,
    onToggleDone: (String) -> Unit,
    onSuggestionPositive: (String) -> Unit = {},
    onSuggestionDismiss: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 28.dp, top = 2.dp, bottom = 6.dp)
    ) {
        if (isAiInferring) {
            // Shimmer loading trên Card trong lúc chờ mô hình On-Device AI xử lý inference
            items(2) {
                ShimmerLifestyleTipCard(isDark = isDark)
            }
        } else if (lifestyleTips.isNotEmpty()) {
            items(lifestyleTips, key = { it.id }) { tip ->
                LifestyleTipCard(
                    tip = tip,
                    isDark = isDark,
                    onToggleDone = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggleDone(tip.id)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.width(4.dp))
            }
        } else {
            items(suggestions, key = { it.id }) { suggestion ->
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
            item {
                Spacer(modifier = Modifier.width(4.dp))
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
            .width(260.dp)
            .height(145.dp),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(65.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
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
        "Ẩm thực" -> Color(0xFFFEF3C7)
        "Thể thao", "Vận động" -> Color(0xFFDCFCE7)
        "Du lịch" -> Color(0xFFDBEAFE)
        "Thư giãn" -> Color(0xFFF3E8FF)
        else -> Color(0xFFE0F2FE)
    }
    val badgeTxt = when (tip.category) {
        "Ẩm thực" -> Color(0xFFD97706)
        "Thể thao", "Vận động" -> Color(0xFF16A34A)
        "Du lịch" -> Color(0xFF2563EB)
        "Thư giãn" -> Color(0xFF9333EA)
        else -> Color(0xFF0284C7)
    }

    OneUIGlassCard(
        modifier = Modifier
            .width(280.dp)
            .clickable { onToggleDone() }
            .testTag("lifestyle_card_${tip.id}"),
        shape = RoundedCornerShape(26.dp),
        isDark = isDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg.copy(alpha = if (isDark) 0.35f else 0.85f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
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
                    maxLines = 2,
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

            // Quả cầu kính mờ 3D chứa Icon/Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                        contentDescription = "Đã làm",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(text = tip.emoji, fontSize = 24.sp)
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
            // Bên trái: Category Badge + Tag + Tiêu đề + Nội dung
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Row: Category Badge + Tag + Nút đóng X nhỏ
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
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f))
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

                    // Nút X nhỏ ẩn gợi ý
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
                            contentDescription = "Ẩn",
                            tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Tiêu đề gợi ý
                Text(
                    text = suggestion.title,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Câu gợi ý súc tích
                Text(
                    text = suggestion.description,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bên phải: 3D CLAYMORPHISM STICKER / VISUAL LỚN (52dp)
            ClaymorphismSuggestionVisual(
                visualType = suggestion.visualType,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// THẺ 4: GỢI Ý THÔNG MINH WIDGETS (Editorial & Adaptive Widgets)
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

    val tempInt = weatherData?.currentTempC?.toInt() ?: 29
    val weatherDesc = weatherData?.conditionDescription ?: "mát mẻ"

    val aiLeadText = remember(currentHour, tempInt, weatherDesc, leadTextRefreshKey) {
        mlEngine.lifestyleLearner.generateSessionLeadText(currentHour, tempInt, weatherDesc)
    }

    val rankedLifestyleTips = remember(lifestyleTips, currentHour, leadTextRefreshKey) {
        mlEngine.lifestyleLearner.rankLifestyleTips(lifestyleTips, currentHour)
    }

    val localAiSuggestions = remember(currentHour, weatherData, dismissedSuggestionIds) {
        val base = generateLocalAiSuggestions(currentHour, weatherData)
        base.filter { it.id !in dismissedSuggestionIds }
            .sortedByDescending { feedbackTracker.getSuggestionWeight(it.category) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial Lead Text sinh tự động từ Mô hình Máy học (Clean Typography, strictly no emoji)
        Text(
            text = aiLeadText,
            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Horizontal Suggestion Carousel: Danh sách thẻ cuộn ngang Thể thao, Ẩm thực, Du lịch
        HorizontalSuggestionCarousel(
            modifier = Modifier.fillMaxWidth(),
            lifestyleTips = rankedLifestyleTips,
            suggestions = localAiSuggestions,
            isAiInferring = isAiInferring,
            onToggleDone = { tipId ->
                val clickedTip = rankedLifestyleTips.firstOrNull { it.id == tipId }
                if (clickedTip != null) {
                    mlEngine.recordLifestyleClick(clickedTip.title, clickedTip.category, currentHour)
                    leadTextRefreshKey++
                }
                onToggleTipDone(tipId)
            },
            onSuggestionPositive = { category ->
                feedbackTracker.recordSuggestionPositive(category)
                mlEngine.recordLifestyleClick(category, category, currentHour)
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
            // Nighttime: Widget Báo thức sáng mai + Hàng 3 Mini Card Chánh niệm
            MorningAlarmWidget(isDark = isDark)

            MindfulnessMiniCardsRow(
                isDark = isDark,
                onSelect = { selectedMindfulnessType = it }
            )
        }
    }

    // Modal BottomSheet khi nhấn vào 3 Mini Card Chánh niệm
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
// WIDGET SPOTIFY / MEDIA CARD (Buổi sáng / Chiều)
// -------------------------------------------------------------
@Composable
private fun SpotifyMediaSuggestionCard(
    currentHour: Int,
    isDark: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }

    val (playlistTitle, playlistDesc, genreTag) = if (currentHour in 6..11) {
        Triple(
            "Acoustic Buổi Sáng - Tỉnh Táo",
            "Giai điệu mộc nhẹ nhàng khởi động ngày mới tràn đầy cảm hứng",
            "Khởi đầu ngày"
        )
    } else {
        Triple(
            "Lofi Beats & Deep Focus",
            "Nhịp điệu êm dịu giúp duy trì sự tập trung cao độ và không mệt mỏi",
            "Tập trung sâu"
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
            // 3D Vinyl Record Squircle Album Cover Nghệ thuật
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

                    // Rãnh đĩa than Vinyl
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

                    // Tâm nhãn đĩa Spotify xanh Emerald
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1ED760), Color(0xFF1DB954), Color(0xFF047857)),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            radius = 11f
                        ),
                        radius = 10f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )

                    // Lỗ đĩa tâm
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

            // Nội dung bài hát/playlist
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
                        contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                        tint = if (isPlaying) Color.White else Color(0xFF1DB954),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// WIDGET BÁO THỨC SÁNG MAI (Buổi tối / Đêm)
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
                        contentDescription = "Báo thức",
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
                            text = "Sáng mai",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Lịch trình đầu tiên lúc 08:30 • Còn 7h 45m",
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
// HÀNG 3 MINI CARD CHÁNH NIỆM (Tâm trạng • Bài tập thở • Thiền)
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
        // Mini Card 1: [Kiểm tra tâm trạng]
        MindfulnessItemCard(
            title = "Tâm trạng",
            subtitle = "Ghi nhận",
            icon = Icons.Default.SentimentSatisfiedAlt,
            iconTint = Color(0xFFEC4899),
            iconBg = Color(0xFFFDF2F8),
            isDark = isDark,
            animationType = 1,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("mood") }
        )

        // Mini Card 2: [Bài tập thở]
        MindfulnessItemCard(
            title = "Tập thở",
            subtitle = "4-7-8 Thư giãn",
            icon = Icons.Default.Air,
            iconTint = Color(0xFF06B6D4),
            iconBg = Color(0xFFECFEFF),
            isDark = isDark,
            animationType = 2,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("breath") }
        )

        // Mini Card 3: [Thiền]
        MindfulnessItemCard(
            title = "Thiền sâu",
            subtitle = "5 phút an tĩnh",
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

    // Animation chuyển động nhẹ phù hợp từng loại (thở nhẹ / nhún nhẹ / phát sáng nhẹ)
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
            // Icon Badge trong vòng tròn kính mờ Gradient chuẩn One UI
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
// HỘP THOẠI TƯƠNG TÁC CHÁNH NIỆM (Interactive Mindfulness Dialog)
// -------------------------------------------------------------
@Composable
private fun MindfulnessInteractiveDialog(
    type: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val (title, desc, icon, iconTint) = when (type) {
        "mood" -> Tuple4(
            "Kiểm tra tâm trạng",
            "Hôm nay bạn cảm thấy thế nào? Ghi nhận cảm xúc giúp AI cân đối nhịp sinh hoạt ngày mai.",
            Icons.Default.SentimentSatisfiedAlt,
            Color(0xFFEC4899)
        )

        "breath" -> Tuple4(
            "Bài tập thở 4-7-8",
            "Hít vào 4 giây • Giữ hơi 7 giây • Thở ra từ từ trong 8 giây để thả lỏng nhịp tim.",
            Icons.Default.Air,
            Color(0xFF06B6D4)
        )

        else -> Tuple4(
            "Thiền tĩnh lặng 5 phút",
            "Nhắm mắt, thả lỏng vai và tập trung vào từng hơi thở tự nhiên.",
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
                        listOf("😄 Tuyệt vời", "🙂 Bình thường", "😴 Hơi mệt", "🧘 Thư thái").forEach { mood ->
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
                Text("Bắt đầu ngay", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
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



