package com.example.core.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.example.core.notification.SmartNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ActiveMediaPlayback(
    val title: String,
    val artist: String,
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPlaying: Boolean = true
) {
    val displaySubtitle: String
        get() = if (artist.isNotBlank()) "$artist • $appName" else appName
}

/**
 * Manages and tracks real-time media playback and recent listening history from the user's device
 * (YouTube Music, YouTube, Spotify, Samsung Music, Apple Music, etc.) via MediaSessionManager
 * and NotificationListenerService.
 */
object RecentMediaTracker {

    private const val TAG = "RecentMediaTracker"
    private const val PREFS_NAME = "recent_device_media_prefs"
    private const val KEY_TITLE = "media_title"
    private const val KEY_ARTIST = "media_artist"
    private const val KEY_APP_NAME = "media_app_name"
    private const val KEY_PACKAGE = "media_package"
    private const val KEY_TIMESTAMP = "media_timestamp"
    private const val KEY_IS_PLAYING = "media_is_playing"
    private const val KEY_HISTORY_JSON = "media_history_json"

    private val _currentPlaybackState = MutableStateFlow<ActiveMediaPlayback?>(null)
    val currentPlaybackState: StateFlow<ActiveMediaPlayback?> = _currentPlaybackState.asStateFlow()

    private val _recentHistoryState = MutableStateFlow<List<ActiveMediaPlayback>>(emptyList())
    val recentHistoryState: StateFlow<List<ActiveMediaPlayback>> = _recentHistoryState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null)
        val artist = prefs.getString(KEY_ARTIST, "") ?: ""
        val appName = prefs.getString(KEY_APP_NAME, "YouTube Music") ?: "YouTube Music"
        val packageName = prefs.getString(KEY_PACKAGE, "com.google.android.apps.youtube.music")
            ?: "com.google.android.apps.youtube.music"
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val wasPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)

        if (!title.isNullOrBlank()) {
            _currentPlaybackState.value = ActiveMediaPlayback(
                title = title,
                artist = artist,
                appName = appName,
                packageName = packageName,
                timestamp = timestamp,
                isPlaying = wasPlaying && (System.currentTimeMillis() - timestamp) < 15 * 60 * 1000L
            )
        }
        _recentHistoryState.value = loadHistory(prefs)

        // Immediately query active MediaSessions on the device (YouTube Music / Spotify / YouTube)
        refreshActiveSessions(context)
    }

    /**
     * Queries Android's MediaSessionManager for active media controllers on the device.
     * Prioritizes currently playing sessions (e.g. YouTube Music, Spotify, YouTube).
     * Returns true if an active media session with valid metadata was found.
     */
    fun refreshActiveSessions(context: Context): Boolean {
        return try {
            if (!SmartNotificationListenerService.isPermissionGranted(context)) return false
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return false
            val component = ComponentName(context, SmartNotificationListenerService::class.java)
            val controllers = msm.getActiveSessions(component)
            if (controllers.isEmpty()) return false

            // Prefer actively playing controller, otherwise pick the first controller with metadata
            val activeController = controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING &&
                    !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
            } ?: controllers.firstOrNull {
                !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
            } ?: return false

            val meta = activeController.metadata ?: return false
            val title = (meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "").trim()
            if (title.isBlank()) return false

            val artist = (meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: "").trim()

            val isPlaying = activeController.playbackState?.state == PlaybackState.STATE_PLAYING
            updatePlayback(
                context = context,
                title = title,
                artist = artist,
                packageName = activeController.packageName,
                isPlaying = isPlaying
            )
            true
        } catch (e: Exception) {
            Log.d(TAG, "refreshActiveSessions skipped: ${e.message}")
            false
        }
    }

    fun resolveAppName(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("youtube.music") || lower.contains("youtubemusic") -> "YouTube Music"
            lower.contains("youtube") -> "YouTube"
            lower.contains("spotify") -> "Spotify"
            lower.contains("sec.android.app.music") || lower.contains("samsung.android.app.music") -> "Samsung Music"
            lower.contains("apple") -> "Apple Music"
            lower.contains("soundcloud") -> "SoundCloud"
            lower.contains("resso") || lower.contains("tiktok.music") -> "TikTok Music"
            lower.contains("joox") -> "JOOX"
            else -> "Music Player"
        }
    }

    fun updatePlayback(
        context: Context,
        title: String,
        artist: String,
        packageName: String,
        isPlaying: Boolean = true
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        val appName = resolveAppName(packageName)
        val playback = ActiveMediaPlayback(
            title = cleanTitle,
            artist = artist.trim(),
            appName = appName,
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            isPlaying = isPlaying
        )

        _currentPlaybackState.value = playback

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updatedHistory = (listOf(playback) + loadHistory(prefs))
            .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
            .take(10)
        _recentHistoryState.value = updatedHistory

        val historyArr = JSONArray()
        updatedHistory.forEach { item ->
            historyArr.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("artist", item.artist)
                    put("appName", item.appName)
                    put("packageName", item.packageName)
                    put("timestamp", item.timestamp)
                }
            )
        }

        prefs.edit()
            .putString(KEY_TITLE, playback.title)
            .putString(KEY_ARTIST, playback.artist)
            .putString(KEY_APP_NAME, playback.appName)
            .putString(KEY_PACKAGE, playback.packageName)
            .putLong(KEY_TIMESTAMP, playback.timestamp)
            .putBoolean(KEY_IS_PLAYING, playback.isPlaying)
            .putString(KEY_HISTORY_JSON, historyArr.toString())
            .apply()
    }

    private fun loadHistory(prefs: android.content.SharedPreferences): List<ActiveMediaPlayback> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ActiveMediaPlayback>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val t = obj.optString("title", "").trim()
                if (t.isNotBlank()) {
                    list.add(
                        ActiveMediaPlayback(
                            title = t,
                            artist = obj.optString("artist", ""),
                            appName = obj.optString("appName", "YouTube Music"),
                            packageName = obj.optString("packageName", "com.google.android.apps.youtube.music"),
                            timestamp = obj.optLong("timestamp", 0L),
                            isPlaying = false
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
