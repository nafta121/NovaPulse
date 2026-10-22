package com.example.feature.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Native Samsung One UI 7+ Now Bar & Android 16 Promoted Live Update Service.
 *
 * Injects an ongoing live activity into the Samsung Now Bar (status bar chip + lock screen pill)
 * using Samsung's ongoingActivityNoti Bundle extras and Notification.semFlags (bit 32768),
 * with an automatic Android 16 Live Update fallback (setRequestPromotedOngoing + setShortCriticalText).
 */
class NowBarLiveService : Service() {

    companion object {
        private const val TAG = "NowBarLiveService"
        const val NOTIFICATION_ID = 2001
        const val ACTION_REFRESH = "com.example.feature.island.ACTION_REFRESH_NOWBAR"

        private const val SAMSUNG_CHANNEL_ID = "nowbrief_samsung_nowbar"
        private const val LIVE_CHANNEL_ID = "nowbrief_live"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun createNotificationChannels(context: Context) {
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (manager.getNotificationChannel(SAMSUNG_CHANNEL_ID) == null) {
                NotificationChannel(
                    SAMSUNG_CHANNEL_ID,
                    "NowBrief Now Bar",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Powers the Samsung Now Bar pill — invisible in notification drawer"
                    setShowBadge(false)
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                }.also { manager.createNotificationChannel(it) }
            }

            if (manager.getNotificationChannel(LIVE_CHANNEL_ID) == null) {
                NotificationChannel(
                    LIVE_CHANNEL_ID,
                    "NowBrief Live",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "NowBrief live brief — weather, schedule, health and AI summary"
                    setShowBadge(false)
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
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
    private var cachedPool: List<String> = emptyList()

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
        val initialPrimary = liveGreeting()
        val initialSecondary = cachedPool.firstOrNull() ?: "Memuat ringkasan harian Anda…"
        val initial = buildNotification(initialPrimary, initialSecondary)

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

        if (tickJob?.isActive != true || intent?.action == ACTION_REFRESH) {
            startTickingMessages()
        }
        return START_STICKY
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

    private fun startTickingMessages() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var tickCount = 0
            while (isActive) {
                if (tickCount % 6 == 0 || cachedPool.isEmpty()) {
                    refreshRealDevicePool()
                }
                val pool = cachedPool.ifEmpty { listOf("Ketuk untuk membuka NowBrief") }
                val secondary = pool[chipIndex % pool.size]
                chipIndex++

                val notif = buildNotification(liveGreeting(), secondary)
                notificationManager.notify(NOTIFICATION_ID, notif)

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

            val pool = mutableListOf<String>()

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
                pool.add("\uD83C\uDF24\uFE0F $loc: ${temp}°C, ${weather.conditionDescription}")
            }

            // 2. AI Briefing & Energy Score
            val aiBrief = runCatching { prefs.widgetAiBriefing.first() }.getOrDefault("")
            val energyScore = runCatching { prefs.widgetAiEnergyScore.first() }.getOrDefault(88)
            if (aiBrief.isNotBlank()) {
                pool.add("✨ $aiBrief")
            }

            // 3. Steps & Calories (Sensor + Health Connect)
            val sensorData = stepCounterManager?.stepDataState?.value
            val hcData = runCatching { healthRepo.fetchHealthData() }.getOrNull()
            val steps = maxOf(sensorData?.steps ?: 0, hcData?.currentSteps ?: 0)
            val calories = maxOf(sensorData?.calories ?: 0, hcData?.currentCalories ?: 0)
            if (steps > 0) {
                pool.add("\uD83D\uDC5F $steps langkah · $calories kkal · Energi $energyScore%")
            } else {
                pool.add("⚡ Skor Energi $energyScore% · Siap beraktivitas")
            }

            // 4. Calendar & Tasks
            val events = runCatching { calendarRepo.getTodayEvents() }.getOrDefault(emptyList())
            events.firstOrNull()?.let { ev ->
                val timeStr = if (ev.isAllDay) {
                    "Sepanjang hari"
                } else {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(ev.startTimeMillis))
                }
                pool.add("\uD83D\uDCC5 $timeStr: ${ev.title}")
            }
            val tasks = runCatching { database.taskDao().getUpcomingTasksSync(2) }.getOrDefault(emptyList())
            tasks.firstOrNull()?.let { task ->
                pool.add("✅ Tugas: ${task.title}")
            }

            // 5. Screen Time
            val screenTime = runCatching { screenTimeRepo.getScreenTimeStats() }.getOrNull()
            if (screenTime != null && screenTime.totalScreenTimeTodayMillis > 0L) {
                pool.add("\uD83D\uDCF1 Layar aktif: ${screenTime.formattedDuration}")
            }

            cachedPool = pool
        } catch (e: Exception) {
            Log.w(TAG, "refreshRealDevicePool error: ${e.message}")
        }
    }

    private fun buildNotification(primaryText: String, secondaryText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_NOW_BRIEF", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return if (isSamsung) {
            buildSamsungNotification(primaryText, secondaryText, pendingIntent)
        } else {
            buildLiveUpdateNotification(primaryText, secondaryText, pendingIntent)
        }
    }

    private fun buildSamsungNotification(
        primaryText: String,
        secondaryText: String,
        pendingIntent: PendingIntent
    ): Notification {
        val extras = Bundle().apply {
            putInt("android.ongoingActivityNoti.style", 1)
            putString("android.ongoingActivityNoti.primaryInfo", primaryText)
            putString("android.ongoingActivityNoti.secondaryInfo", secondaryText)
            putString("android.ongoingActivityNoti.nowbarPrimaryInfo", "NowBrief")
            putString("android.ongoingActivityNoti.nowbarSecondaryInfo", primaryText)
            putString("android.ongoingActivityNoti.chipExpandedText", primaryText)
            putInt("android.ongoingActivityNoti.chipBgColor", 0xFF6750A4.toInt())
        }

        val notif = NotificationCompat.Builder(this, SAMSUNG_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_capsule_horizon)
            .setContentTitle(primaryText)
            .setContentText(secondaryText)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addExtras(extras)
            .build()

        // semFlags bit 32768 — instructs Samsung One UI SystemUI to show this in the Now Bar
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
        secondaryText: String,
        pendingIntent: PendingIntent
    ): Notification {
        val chipText = cachedTempStr.ifBlank {
            listOf("Brief", "Now", "Live", "Pulse")[chipIndex % 4]
        }

        return NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.ic_capsule_horizon)
            .setContentTitle(primaryText)
            .setContentText(secondaryText)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShortCriticalText(chipText)
            .setRequestPromotedOngoing(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(secondaryText)
                    .setBigContentTitle(primaryText)
            )
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
