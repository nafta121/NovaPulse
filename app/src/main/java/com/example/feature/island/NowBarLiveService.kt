package com.example.feature.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.NovaPulseApp
import com.example.R
import com.example.core.database.AppDatabase
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.ScreenTimeRepository
import com.example.core.repository.WeatherCacheManager
import com.example.core.repository.WeatherRepository
import com.example.core.sensor.StepCounterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class NowBarCardItem(
    val text: String,
    val nowbarTitle: String,
    val chipLabel: String,
    val iconRes: Int,
    val chipBgColor: Int,
    val progress: Int,
    val progressColor: Int
)

/**
 * Native Samsung One UI 7/8 Live Notification & Now Bar Service.
 *
 * Implements the full Samsung One UI ongoingActivityNoti specification documented by Akexorcist:
 *  - Standard Style (primaryInfo, secondaryInfo, secondaryInfoIcon)
 *  - Status Bar Chip & Drawer Gradient (chipBgColor, chipIcon, chipExpandedText)
 *  - Multi-Segment Progress Style (progress, progressMax, progressSegments, progressSegments.icon)
 *  - Interactive Action Buttons (actionType = 1, actionPrimarySet = 0, Next Card + Open Brief actions)
 *  - Lock Screen Now Bar (nowbarPrimaryInfo, nowbarSecondaryInfo)
 *  - Android 16 / One UI 8 Live Updates (setRequestPromotedOngoing + setShortCriticalText)
 */
class NowBarLiveService : Service() {

    companion object {
        private const val TAG = "NowBarLiveService"
        const val NOTIFICATION_ID = 2001
        const val ACTION_REFRESH = "com.example.feature.island.ACTION_REFRESH_NOWBAR"
        const val ACTION_NEXT_CARD = "com.example.feature.island.ACTION_NEXT_NOWBAR_CARD"

        private const val LEGACY_SAMSUNG_CHANNEL_ID = "nowbrief_samsung_nowbar"
        private const val SAMSUNG_CHANNEL_ID = "nowbrief_samsung_nowbar_v2"
        private const val LIVE_CHANNEL_ID = "nowbrief_live"

        // Samsung One UI ongoingActivityNoti Bundle keys (Akexorcist specification)
        const val EXTRA_STYLE = "android.ongoingActivityNoti.style"
        const val EXTRA_PRIMARY_INFO = "android.ongoingActivityNoti.primaryInfo"
        const val EXTRA_SECONDARY_INFO = "android.ongoingActivityNoti.secondaryInfo"
        const val EXTRA_SECONDARY_INFO_ICON = "android.ongoingActivityNoti.secondaryInfoIcon"
        const val EXTRA_CHIP_BG_COLOR = "android.ongoingActivityNoti.chipBgColor"
        const val EXTRA_CHIP_ICON = "android.ongoingActivityNoti.chipIcon"
        const val EXTRA_CHIP_EXPANDED_TEXT = "android.ongoingActivityNoti.chipExpandedText"
        const val EXTRA_ACTION_TYPE = "android.ongoingActivityNoti.actionType"
        const val EXTRA_ACTION_PRIMARY_SET = "android.ongoingActivityNoti.actionPrimarySet"
        const val EXTRA_PROGRESS = "android.ongoingActivityNoti.progress"
        const val EXTRA_PROGRESS_MAX = "android.ongoingActivityNoti.progressMax"
        const val EXTRA_PROGRESS_ICON = "android.ongoingActivityNoti.progressSegments.icon"
        const val EXTRA_PROGRESS_COLOR = "android.ongoingActivityNoti.progressSegments.progressColor"
        const val EXTRA_PROGRESS_SEGMENTS = "android.ongoingActivityNoti.progressSegments"
        const val EXTRA_SEGMENT_START = "android.ongoingActivityNoti.progressSegments.segmentStart"
        const val EXTRA_SEGMENT_COLOR = "android.ongoingActivityNoti.progressSegments.segmentColor"
        const val EXTRA_NOWBAR_PRIMARY_INFO = "android.ongoingActivityNoti.nowbarPrimaryInfo"
        const val EXTRA_NOWBAR_SECONDARY_INFO = "android.ongoingActivityNoti.nowbarSecondaryInfo"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun createNotificationChannels(context: Context) {
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Remove old IMPORTANCE_MIN channel so One UI renders both the Drawer Gradient Card & Lock Screen Now Bar
            runCatching { manager.deleteNotificationChannel(LEGACY_SAMSUNG_CHANNEL_ID) }

            if (manager.getNotificationChannel(SAMSUNG_CHANNEL_ID) == null) {
                NotificationChannel(
                    SAMSUNG_CHANNEL_ID,
                    "NowBrief Live Notification & Now Bar",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Powers Samsung One UI Live Notifications, Status Bar Chip, and Lock Screen Now Bar"
                    setShowBadge(false)
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }.also { manager.createNotificationChannel(it) }
            }

            if (manager.getNotificationChannel(LIVE_CHANNEL_ID) == null) {
                NotificationChannel(
                    LIVE_CHANNEL_ID,
                    "NowBrief Live Updates",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Android 16 Promoted Live Updates — weather, schedule, health and AI summary"
                    setShowBadge(false)
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }.also { manager.createNotificationChannel(it) }
            }
        }

        fun startOrRefresh(context: Context) {
            try {
                val intent = Intent(context, NowBarLiveService::class.java).apply {
                    action = ACTION_REFRESH
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start/refresh NowBarLiveService: ${e.message}")
            }
        }

        fun nextCard(context: Context) {
            try {
                val intent = Intent(context, NowBarLiveService::class.java).apply {
                    action = ACTION_NEXT_CARD
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to advance NowBarLiveService card: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, NowBarLiveService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Unable to stop NowBarLiveService: ${e.message}")
            }
        }
    }

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickJob: Job? = null
    private var stepCounterManager: StepCounterManager? = null
    private var chipIndex = 0

    @Volatile
    private var cachedItems: List<NowBarCardItem> = emptyList()

    @Volatile
    private var cachedTempStr: String = ""

    private val isSamsung: Boolean by lazy {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannels(this)
        stepCounterManager = StepCounterManager(this).also { it.startListening() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentItem = getCurrentCardItem()
        val initial = buildNotification(liveGreeting(), currentItem)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_NEXT_CARD -> {
                chipIndex++
                val nextItem = getCurrentCardItem()
                notificationManager.notify(NOTIFICATION_ID, buildNotification(liveGreeting(), nextItem))
                startTickingMessages(skipImmediateIncrement = true)
            }
            ACTION_REFRESH -> {
                startTickingMessages(forceRefreshPool = true)
            }
            else -> {
                if (tickJob?.isActive != true) {
                    startTickingMessages(forceRefreshPool = true)
                }
            }
        }
        return START_STICKY
    }

    private fun getCurrentCardItem(): NowBarCardItem {
        val items = cachedItems
        if (items.isEmpty()) {
            return NowBarCardItem(
                text = "Memuat ringkasan cerdas dari perangkat Anda…",
                nowbarTitle = "NowBrief",
                chipLabel = cachedTempStr.ifBlank { "Brief" },
                iconRes = R.drawable.ic_capsule_horizon,
                chipBgColor = 0xFF2563EB.toInt(),
                progress = 85,
                progressColor = 0xFF60A5FA.toInt()
            )
        }
        return items[chipIndex.coerceAtLeast(0) % items.size]
    }

    private fun liveGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val base = when {
            hour < 5 -> "Good night \uD83C\uDF19"
            hour < 12 -> "Good morning \u2600\uFE0F"
            hour < 17 -> "Good afternoon \uD83C\uDF24\uFE0F"
            hour < 21 -> "Good evening \uD83C\uDF05"
            else -> "Good night \uD83C\uDF19"
        }
        return if (cachedTempStr.isNotBlank()) "$base · $cachedTempStr" else base
    }

    private fun startTickingMessages(
        forceRefreshPool: Boolean = false,
        skipImmediateIncrement: Boolean = false
    ) {
        tickJob?.cancel()
        tickJob = scope.launch {
            var tickCount = 0
            var skipFirstAdvance = skipImmediateIncrement
            while (isActive) {
                if ((forceRefreshPool && tickCount == 0) || tickCount % 6 == 0 || cachedItems.isEmpty()) {
                    refreshRealDevicePool()
                }
                if (!skipFirstAdvance) {
                    val item = getCurrentCardItem()
                    chipIndex++
                    val notif = buildNotification(liveGreeting(), item)
                    notificationManager.notify(NOTIFICATION_ID, notif)
                } else {
                    skipFirstAdvance = false
                }

                tickCount++
                delay(30_000L)
            }
        }
    }

    private suspend fun refreshRealDevicePool() {
        try {
            val app = application as? NovaPulseApp
            val prefs = app?.preferencesManager ?: PreferencesManager(this)
            val database = app?.database ?: AppDatabase.getInstance(this)
            val weatherRepo = app?.weatherRepository ?: WeatherRepository(this)
            val calendarRepo = app?.calendarRepository ?: CalendarRepository(this)
            val healthRepo = app?.healthConnectRepository ?: HealthConnectRepository(this)
            val screenTimeRepo = app?.screenTimeRepository ?: ScreenTimeRepository(this)

            val aiBrief = runCatching { prefs.widgetAiBriefing.first() }.getOrDefault("")
            val energyScore = runCatching { prefs.widgetAiEnergyScore.first() }.getOrDefault(88).coerceIn(0, 100)

            val items = mutableListOf<NowBarCardItem>()

            // 1. Weather (cache + fallback network if stale)
            var weather = WeatherCacheManager.load(this)
            val now = System.currentTimeMillis()
            if (weather == null || (now - weather.cacheTimestampMs) > 30 * 60_000L) {
                val fresh = runCatching { weatherRepo.fetchWeather() }.getOrNull()
                if (fresh != null) weather = fresh
            }
            if (weather != null) {
                val temp = weather.currentTempC.roundToInt()
                cachedTempStr = "${temp}°C"
                val loc = weather.locationName.takeIf { it.isNotBlank() } ?: "Lokasi Anda"
                items.add(
                    NowBarCardItem(
                        text = "\uD83C\uDF24\uFE0F $loc: ${temp}°C, ${weather.conditionDescription}",
                        nowbarTitle = "$loc · ${temp}°C",
                        chipLabel = "${temp}°C",
                        iconRes = R.drawable.ic_widget_weather_sun,
                        chipBgColor = 0xFF0284C7.toInt(),
                        progress = energyScore,
                        progressColor = 0xFF38BDF8.toInt()
                    )
                )
            }

            // 2. AI Briefing & Energy Score
            if (aiBrief.isNotBlank()) {
                items.add(
                    NowBarCardItem(
                        text = "✨ $aiBrief",
                        nowbarTitle = "NowBrief AI · Energi $energyScore%",
                        chipLabel = "$energyScore%",
                        iconRes = R.drawable.ic_widget_energy,
                        chipBgColor = 0xFF6750A4.toInt(),
                        progress = energyScore,
                        progressColor = 0xFFA855F7.toInt()
                    )
                )
            }

            // 3. Steps & Calories (Sensor + Health Connect)
            val sensorData = stepCounterManager?.stepDataState?.value
            val hcData = runCatching { healthRepo.fetchHealthData() }.getOrNull()
            val steps = maxOf(sensorData?.steps ?: 0, hcData?.currentSteps ?: 0)
            val stepGoal = (hcData?.stepGoal ?: 6000).coerceAtLeast(1)
            val calories = maxOf(sensorData?.calories ?: 0, hcData?.currentCalories ?: 0)
            val stepPct = ((steps.toFloat() / stepGoal) * 100f).roundToInt().coerceIn(0, 100)
            if (steps > 0) {
                val shortSteps = if (steps >= 1000) {
                    String.format(Locale.US, "%.1fk", steps / 1000f)
                } else {
                    "$steps"
                }
                items.add(
                    NowBarCardItem(
                        text = "\uD83D\uDC5F $steps / $stepGoal langkah · $calories kkal · Energi $energyScore%",
                        nowbarTitle = "$steps Langkah ($stepPct%)",
                        chipLabel = shortSteps,
                        iconRes = R.drawable.ic_widget_mindful,
                        chipBgColor = 0xFF059669.toInt(),
                        progress = stepPct,
                        progressColor = 0xFF34D399.toInt()
                    )
                )
            } else {
                items.add(
                    NowBarCardItem(
                        text = "⚡ Skor Energi $energyScore% · Siap beraktivitas hari ini",
                        nowbarTitle = "Skor Energi $energyScore%",
                        chipLabel = "$energyScore%",
                        iconRes = R.drawable.ic_widget_energy,
                        chipBgColor = 0xFF059669.toInt(),
                        progress = energyScore,
                        progressColor = 0xFF34D399.toInt()
                    )
                )
            }

            // 4. Calendar & Tasks
            val events = runCatching { calendarRepo.getTodayEvents() }.getOrDefault(emptyList())
            events.firstOrNull()?.let { ev ->
                val timeStr = if (ev.isAllDay) {
                    "Hari ini"
                } else {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ev.startTimeMillis))
                }
                items.add(
                    NowBarCardItem(
                        text = "\uD83D\uDCC5 $timeStr: ${ev.title}",
                        nowbarTitle = "Agenda · $timeStr",
                        chipLabel = timeStr,
                        iconRes = R.drawable.ic_capsule_horizon,
                        chipBgColor = 0xFF2563EB.toInt(),
                        progress = energyScore,
                        progressColor = 0xFF60A5FA.toInt()
                    )
                )
            }
            val tasks = runCatching { database.taskDao().getUpcomingTasksSync(2) }.getOrDefault(emptyList())
            tasks.firstOrNull()?.let { task ->
                items.add(
                    NowBarCardItem(
                        text = "✅ Tugas: ${task.title}",
                        nowbarTitle = "Tugas Prioritas",
                        chipLabel = "Tugas",
                        iconRes = R.drawable.ic_widget_task,
                        chipBgColor = 0xFFD97706.toInt(),
                        progress = energyScore,
                        progressColor = 0xFFFBBF24.toInt()
                    )
                )
            }

            // 5. Screen Time
            val screenTime = runCatching { screenTimeRepo.getScreenTimeStats() }.getOrNull()
            if (screenTime != null && screenTime.totalScreenTimeTodayMillis > 0L) {
                val screenPct = ((screenTime.totalScreenTimeTodayMillis / (4 * 3600_000f)) * 100f)
                    .roundToInt()
                    .coerceIn(5, 100)
                items.add(
                    NowBarCardItem(
                        text = "\uD83D\uDCF1 Layar aktif: ${screenTime.formattedDuration}",
                        nowbarTitle = "Layar · ${screenTime.formattedDuration}",
                        chipLabel = screenTime.formattedDuration.replace(" ", "").take(7),
                        iconRes = R.drawable.ic_capsule_horizon,
                        chipBgColor = 0xFF4F46E5.toInt(),
                        progress = screenPct,
                        progressColor = 0xFF818CF8.toInt()
                    )
                )
            }

            cachedItems = items
        } catch (e: Exception) {
            Log.w(TAG, "refreshRealDevicePool error: ${e.message}")
        }
    }

    private fun buildNotification(primaryText: String, item: NowBarCardItem): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_NOW_BRIEF", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(this, NowBarLiveService::class.java).apply {
            action = ACTION_NEXT_CARD
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            101,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return if (isSamsung) {
            buildSamsungNotification(primaryText, item, openPendingIntent, nextPendingIntent)
        } else {
            buildLiveUpdateNotification(primaryText, item, openPendingIntent, nextPendingIntent)
        }
    }

    private fun buildSamsungNotification(
        primaryText: String,
        item: NowBarCardItem,
        openPendingIntent: PendingIntent,
        nextPendingIntent: PendingIntent
    ): Notification {
        val segment1 = Bundle().apply {
            putFloat(EXTRA_SEGMENT_START, 0.0f)
            putInt(EXTRA_SEGMENT_COLOR, item.progressColor)
        }
        val segment2 = Bundle().apply {
            putFloat(EXTRA_SEGMENT_START, 0.5f)
            putInt(EXTRA_SEGMENT_COLOR, item.chipBgColor)
        }

        val extras = Bundle().apply {
            // 1. Standard Style (Akexorcist Live Notifications spec)
            putInt(EXTRA_STYLE, 1)
            putString(EXTRA_PRIMARY_INFO, primaryText)
            putString(EXTRA_SECONDARY_INFO, item.text)
            putParcelable(EXTRA_SECONDARY_INFO_ICON, Icon.createWithResource(this@NowBarLiveService, item.iconRes))

            // 2. Status Bar Chip & Notification Drawer Gradient Color
            putInt(EXTRA_CHIP_BG_COLOR, item.chipBgColor)
            putParcelable(EXTRA_CHIP_ICON, Icon.createWithResource(this@NowBarLiveService, item.iconRes))
            putString(EXTRA_CHIP_EXPANDED_TEXT, item.chipLabel.take(12))

            // 3. Action Buttons Configuration on Live Notification / Now Bar
            putInt(EXTRA_ACTION_TYPE, 1)
            putInt(EXTRA_ACTION_PRIMARY_SET, 0)

            // 4. Multi-Segment Progress Bar Style (Energy / Steps / Screen Time)
            putInt(EXTRA_PROGRESS, item.progress.coerceIn(0, 100))
            putInt(EXTRA_PROGRESS_MAX, 100)
            putParcelable(EXTRA_PROGRESS_ICON, Icon.createWithResource(this@NowBarLiveService, R.drawable.ic_widget_energy))
            putInt(EXTRA_PROGRESS_COLOR, item.progressColor)
            putParcelableArray(EXTRA_PROGRESS_SEGMENTS, arrayOf(segment1, segment2))

            // 5. Lock Screen Now Bar Specific Text
            putString(EXTRA_NOWBAR_PRIMARY_INFO, item.nowbarTitle)
            putString(EXTRA_NOWBAR_SECONDARY_INFO, item.text)
        }

        val notif = NotificationCompat.Builder(this, SAMSUNG_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setSmallIcon(item.iconRes)
            .setContentTitle(primaryText)
            .setContentText(item.text)
            .setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShortCriticalText(item.chipLabel.take(7))
            .setRequestPromotedOngoing(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(item.text)
                    .setBigContentTitle(primaryText)
            )
            .addAction(R.drawable.ic_capsule_horizon, "Berikutnya", nextPendingIntent)
            .addAction(R.drawable.ic_widget_energy, "Buka Brief", openPendingIntent)
            .addExtras(extras)
            .build()

        // semFlags bit 32768 — instructs Samsung One UI SystemUI to promote into Now Bar & Live Notification
        try {
            val field = Notification::class.java.getDeclaredField("semFlags")
            field.isAccessible = true
            field.setInt(notif, field.getInt(notif) or 32768)
        } catch (_: Exception) {
        }

        return notif
    }

    private fun buildLiveUpdateNotification(
        primaryText: String,
        item: NowBarCardItem,
        openPendingIntent: PendingIntent,
        nextPendingIntent: PendingIntent
    ): Notification {
        val chipText = item.chipLabel.take(7).ifBlank {
            listOf("Brief", "Now", "Live", "Pulse")[chipIndex % 4]
        }

        return NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setSmallIcon(item.iconRes)
            .setContentTitle(primaryText)
            .setContentText(item.text)
            .setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setProgress(100, item.progress.coerceIn(0, 100), false)
            .setShortCriticalText(chipText)
            .setRequestPromotedOngoing(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(item.text)
                    .setBigContentTitle(primaryText)
            )
            .addAction(R.drawable.ic_capsule_horizon, "Berikutnya", nextPendingIntent)
            .addAction(R.drawable.ic_widget_energy, "Buka Brief", openPendingIntent)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        tickJob?.cancel()
        stepCounterManager?.stopListening()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
