package com.example.core.music

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * Quản lý và lưu trữ thông tin bài hát / nghệ sĩ người dùng vừa nghe thực tế từ YouTube, Spotify, Zing MP3... trên máy.
 */
object RecentMediaTracker {

    private const val PREFS_NAME = "recent_device_media_prefs"
    private const val KEY_TITLE = "media_title"
    private const val KEY_ARTIST = "media_artist"
    private const val KEY_APP_NAME = "media_app_name"
    private const val KEY_PACKAGE = "media_package"
    private const val KEY_TIMESTAMP = "media_timestamp"

    private val _currentPlaybackState = MutableStateFlow<ActiveMediaPlayback?>(null)
    val currentPlaybackState: StateFlow<ActiveMediaPlayback?> = _currentPlaybackState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null)
        val artist = prefs.getString(KEY_ARTIST, "") ?: ""
        val appName = prefs.getString(KEY_APP_NAME, "YouTube") ?: "YouTube"
        val packageName = prefs.getString(KEY_PACKAGE, "com.google.android.youtube") ?: "com.google.android.youtube"
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)

        if (!title.isNullOrBlank()) {
            _currentPlaybackState.value = ActiveMediaPlayback(
                title = title,
                artist = artist,
                appName = appName,
                packageName = packageName,
                timestamp = timestamp,
                isPlaying = (System.currentTimeMillis() - timestamp) < 15 * 60 * 1000L
            )
        }
    }

    fun updatePlayback(
        context: Context,
        title: String,
        artist: String,
        packageName: String,
        isPlaying: Boolean = true
    ) {
        if (title.isBlank()) return

        val appName = when {
            packageName.contains("youtube.music") -> "YouTube Music"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("spotify") -> "Spotify"
            packageName.contains("zing") -> "Zing MP3"
            packageName.contains("apple") -> "Apple Music"
            packageName.contains("soundcloud") -> "SoundCloud"
            else -> "Trình phát nhạc"
        }

        val playback = ActiveMediaPlayback(
            title = title.trim(),
            artist = artist.trim(),
            appName = appName,
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            isPlaying = isPlaying
        )

        _currentPlaybackState.value = playback

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TITLE, playback.title)
            .putString(KEY_ARTIST, playback.artist)
            .putString(KEY_APP_NAME, playback.appName)
            .putString(KEY_PACKAGE, playback.packageName)
            .putLong(KEY_TIMESTAMP, playback.timestamp)
            .apply()
    }
}
