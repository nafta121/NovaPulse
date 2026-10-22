package com.example.core.music

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.core.location.LocationHelper
import com.example.feature.widget.BriefingSession

data class MusicRecommendation(
    val title: String,
    val subtitle: String,
    val genreTag: String,
    val query: String,
    val spotifyUri: String
)

object MusicRecommendationHelper {

    private const val TAG = "MusicHelper"
    const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    const val SPOTIFY_PACKAGE = "com.spotify.music"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    val POPULAR_SPOTIFY_CHIPS = listOf(
        "Pop Indonesia Hits",
        "Tulus",
        "Hindia",
        "Sheila On 7",
        "Nadin Amizah",
        "Bernadya",
        "Acoustic Chill",
        "Lofi Deep Focus",
        "Global Top 50"
    )

    /**
     * Builds personalized quick-search chips combining the user's real device listening history
     * (from YouTube Music / Spotify / YouTube) with regional & mood discovery tags.
     */
    fun getPersonalizedChips(context: Context? = null): List<String> {
        val recentArtists = RecentMediaTracker.recentHistoryState.value
            .mapNotNull { item ->
                item.artist.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
                    ?: item.title.takeIf { it.length <= 28 }
            }
            .distinct()
            .take(4)

        val country = LocationHelper.getDetectedCountryCode(context)
        val regionalDefaults = if (country == "ID") {
            POPULAR_SPOTIFY_CHIPS
        } else {
            listOf(
                "Today's Top Hits",
                "Lofi Deep Focus",
                "Acoustic Chill",
                "Indie Pop Mix",
                "Late Night Vibes",
                "Workout Energy"
            )
        }
        return (recentArtists + regionalDefaults).distinct().take(10)
    }

    fun getRecommendationForSession(hour: Int, minute: Int = 0): MusicRecommendation {
        val recent = RecentMediaTracker.currentPlaybackState.value
        val appLabel = recent?.appName ?: "YouTube Music"
        val artistHint = recent?.artist?.takeIf { it.isNotBlank() }

        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return when (session) {
            BriefingSession.EARLY_MORNING -> MusicRecommendation(
                title = if (artistHint != null) "Morning Mix • $artistHint" else "Morning Acoustic & Chill",
                subtitle = "Bright, gentle melodies to welcome the new day",
                genreTag = "$appLabel • Morning Start",
                query = if (artistHint != null) "$artistHint Morning Acoustic Mix" else "Morning Acoustic Chill Pop",
                spotifyUri = "spotify:search:Morning%20Acoustic%20Chill"
            )
            BriefingSession.MID_MORNING -> MusicRecommendation(
                title = "Deep Focus - High Concentration",
                subtitle = "Instrumental & lofi beats to help you reach a flow state",
                genreTag = "$appLabel • Focus Hours",
                query = "Deep Focus Lofi Instrumental Work",
                spotifyUri = "spotify:search:Deep%20Focus%20Work"
            )
            BriefingSession.NOON -> MusicRecommendation(
                title = "Relaxing Noon Cafe & Acoustic",
                subtitle = "Soft music for midday relaxation and a peaceful lunch break",
                genreTag = "$appLabel • Lunch Break & Chill",
                query = "Acoustic Cafe Lunch Chill",
                spotifyUri = "spotify:search:Acoustic%20Cafe"
            )
            BriefingSession.AFTERNOON -> MusicRecommendation(
                title = if (artistHint != null) "Afternoon Boost • $artistHint" else "Top Hits & Afternoon Energy",
                subtitle = "Keep your afternoon energy up with your favorite tracks",
                genreTag = "$appLabel • Afternoon Boost",
                query = if (artistHint != null) "$artistHint Top Hits" else "Pop Hits Afternoon Energy",
                spotifyUri = "spotify:search:Top%20Hits%20Afternoon"
            )
            BriefingSession.EVENING -> MusicRecommendation(
                title = "Sunset Indie & Chillout",
                subtitle = "Enjoy unwinding after work as the day transitions",
                genreTag = "$appLabel • Sunset Relaxation",
                query = if (artistHint != null) "$artistHint Chill Mix" else "Sunset Indie Acoustic Chill",
                spotifyUri = "spotify:search:Sunset%20Indie%20Chill"
            )
            BriefingSession.NIGHT -> MusicRecommendation(
                title = "Night Unwind - Evening Relaxation",
                subtitle = "Soft pop & lofi to relax your mind in the evening",
                genreTag = "$appLabel • Evening Unwind",
                query = "Night Lofi Ballad Chillout",
                spotifyUri = "spotify:search:Night%20Chillout"
            )
            BriefingSession.LATE_NIGHT -> MusicRecommendation(
                title = "Late Night - Deep Sleep Piano",
                subtitle = "Soothing melodies to guide you into restorative sleep",
                genreTag = "$appLabel • Deep Sleep & Recovery",
                query = "Night Relaxing Piano Sleep Music",
                spotifyUri = "spotify:search:Night%20Relaxing%20Piano"
            )
        }
    }

    fun isPackageInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens YouTube Music directly (or searches inside YouTube Music).
     */
    fun openYouTubeMusicApp(context: Context, query: String? = null) {
        if (!query.isNullOrBlank()) {
            try {
                val playIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    setPackage(YT_MUSIC_PACKAGE)
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                    putExtra(SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(playIntent)
                return
            } catch (_: Exception) {}

            try {
                val searchUrl = "https://music.youtube.com/search?q=${Uri.encode(query)}"
                val ytMusicWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    setPackage(YT_MUSIC_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(ytMusicWebIntent)
                return
            } catch (_: Exception) {}
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(YT_MUSIC_PACKAGE)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return
            }
        } catch (_: Exception) {}

        // Web fallback for YouTube Music
        try {
            val url = if (!query.isNullOrBlank()) {
                "https://music.youtube.com/search?q=${Uri.encode(query)}"
            } else {
                "https://music.youtube.com"
            }
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch YouTube Music: ${e.message}")
        }
    }

    /**
     * Open the user's preferred music app (YouTube Music or Spotify) directly.
     */
    fun openSpotifyApp(context: Context) {
        val recentPkg = RecentMediaTracker.currentPlaybackState.value?.packageName ?: ""
        if (recentPkg.contains("youtube.music") ||
            (isPackageInstalled(context, YT_MUSIC_PACKAGE) && !isPackageInstalled(context, SPOTIFY_PACKAGE))
        ) {
            openYouTubeMusicApp(context)
            return
        }

        try {
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:home")).apply {
                setPackage(SPOTIFY_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
        } catch (e: Exception) {
            if (isPackageInstalled(context, YT_MUSIC_PACKAGE)) {
                openYouTubeMusicApp(context)
            } else {
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                    } else {
                        openYouTubeMusicApp(context)
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Cannot launch music app: ${e2.message}")
                }
            }
        }
    }

    /**
     * Open music app via Deep Link Intent (prioritizes active player / YouTube Music / Spotify / YouTube)
     */
    fun openMusicApp(context: Context, query: String, spotifyUri: String? = null) {
        val recentPkg = RecentMediaTracker.currentPlaybackState.value?.packageName ?: ""
        val preferYtMusic = recentPkg.contains("youtube.music") ||
            isPackageInstalled(context, YT_MUSIC_PACKAGE)

        if (preferYtMusic) {
            openYouTubeMusicApp(context, query)
            return
        }

        try {
            // Try Spotify if installed
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri ?: "spotify:search:$query")).apply {
                setPackage(SPOTIFY_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
            Log.d(TAG, "Launched Spotify via Deep Link")
        } catch (e: Exception) {
            try {
                // Fallback to YouTube app
                val youtubeIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(YOUTUBE_PACKAGE)
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(youtubeIntent)
                Log.d(TAG, "Launched YouTube App via Intent")
            } catch (e2: Exception) {
                openYouTubeMusicApp(context, query)
            }
        }
    }
}
