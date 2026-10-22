package com.example.core.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.NovaPulseApp
import com.example.core.music.RecentMediaTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class SmartNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "SmartNotifService"
        private const val DEBOUNCE_INTERVAL_MS = 60_000L // Deduplicate identical notifications for 60s

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        // Per-notification content hash debounce so unrelated apps never block bank/task alerts
        private val recentNotificationTimestamps = ConcurrentHashMap<Int, Long>()

        // List of system packages to completely ignore (e.g., charging, low battery, screenshots)
        private val SYSTEM_IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.systemui",
            "com.sec.android.app.systemui",
            "android",
            "com.google.android.gms",
            "com.android.vending",
            "com.android.providers.downloads",
            "com.google.android.googlequicksearchbox"
        )

        fun isPermissionGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, SmartNotificationListenerService::class.java)
            return flat.contains(cn.flattenToString())
        }
    }

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener {
        RecentMediaTracker.refreshActiveSessions(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        Log.d(TAG, "SmartNotificationListenerService connected and active.")

        // 1. Register real-time MediaSession listener for YouTube Music / Spotify / YouTube
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val cn = ComponentName(this, SmartNotificationListenerService::class.java)
            msm?.addOnActiveSessionsChangedListener(activeSessionsListener, cn)
            RecentMediaTracker.refreshActiveSessions(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession listener registration skipped: ${e.message}")
        }

        // 2. Scan existing active notifications for any playing/paused media session
        try {
            activeNotifications?.forEach { sbn ->
                extractMediaFromNotification(sbn)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Active notifications scan skipped: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            msm?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {}
        super.onListenerDisconnected()
        _isConnected.value = false
        Log.d(TAG, "SmartNotificationListenerService disconnected.")
    }

    private fun extractMediaFromNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName ?: return false
        val notification = sbn.notification ?: return false
        val extras = notification.extras ?: return false

        val lowerPkg = packageName.lowercase()
        val hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        val isMediaApp = hasMediaSession ||
            lowerPkg.contains("youtube.music") ||
            lowerPkg.contains("youtubemusic") ||
            lowerPkg.contains("youtube") ||
            lowerPkg.contains("spotify") ||
            lowerPkg.contains("sec.android.app.music") ||
            lowerPkg.contains("samsung.android.app.music") ||
            lowerPkg.contains("apple.android.music") ||
            lowerPkg.contains("soundcloud") ||
            lowerPkg.contains("resso") ||
            notification.category == Notification.CATEGORY_TRANSPORT

        if (!isMediaApp) return false

        // First query MediaSessionManager for rich metadata (title + artist + accurate PlaybackState)
        if (RecentMediaTracker.refreshActiveSessions(applicationContext)) {
            return true
        }

        val mediaTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val mediaArtist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: ""
        if (mediaTitle.isNotBlank()) {
            RecentMediaTracker.updatePlayback(
                context = applicationContext,
                title = mediaTitle,
                artist = mediaArtist,
                packageName = packageName,
                isPlaying = sbn.isOngoing
            )
            return true
        }
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. FILTER: Completely ignore internal app and system notifications (com.android.systemui, charging, etc.)
        if (packageName == applicationContext.packageName ||
            SYSTEM_IGNORED_PACKAGES.contains(packageName.lowercase()) ||
            packageName.lowercase().contains("systemui")
        ) {
            Log.d(TAG, "Filter: Ignored system/internal notification from $packageName")
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras

        // Capture real-time song/artist playback info from YouTube Music / YouTube / Spotify / Samsung Music
        extractMediaFromNotification(sbn)

        // Ignore ongoing or foreground service notifications (e.g., charging, background music, navigation)
        if (sbn.isOngoing ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        ) {
            Log.d(TAG, "Filter: Ignored ongoing/background service notification from $packageName")
            return
        }

        // Ignore notifications belonging to system or progress categories
        val category = notification.category
        if (category == Notification.CATEGORY_SYSTEM ||
            category == Notification.CATEGORY_SERVICE ||
            category == Notification.CATEGORY_PROGRESS
        ) {
            Log.d(TAG, "Filter: Ignored system category notification ($category)")
            return
        }

        if (extras == null) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val fullContent = if (subText.isNotBlank()) "$text ($subText)" else text

        // 2. PRE-FILTER & PER-NOTIFICATION DEBOUNCE:
        // Evaluate RegexPreFilter first and deduplicate per (packageName, title, fullContent)
        val filterEvaluation = RegexPreFilter.evaluate(title, fullContent)
        val dedupeKey = if (filterEvaluation.isTriggered) {
            "$packageName|$title|$fullContent".hashCode()
        } else {
            packageName.hashCode()
        }
        val now = System.currentTimeMillis()
        val lastTime = recentNotificationTimestamps[dedupeKey] ?: 0L
        if (now - lastTime < DEBOUNCE_INTERVAL_MS) {
            val remainingSec = (DEBOUNCE_INTERVAL_MS - (now - lastTime)) / 1000
            Log.d(TAG, "Debounce: Throttled duplicate notification from $packageName. Remaining window: ${remainingSec}s")
            return
        }
        recentNotificationTimestamps[dedupeKey] = now
        if (recentNotificationTimestamps.size > 128) {
            recentNotificationTimestamps.entries.removeIf { now - it.value >= DEBOUNCE_INTERVAL_MS }
        }

        val appName = try {
            val pm = applicationContext.packageManager
            val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull() ?: packageName
        }

        Log.d(TAG, "Processing valid debounced notification from $appName ($packageName)")
        val app = application as? NovaPulseApp
        app?.parserAgent?.processNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            content = fullContent
        )
    }
}
