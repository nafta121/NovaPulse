package com.example.core.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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

    val POPULAR_SPOTIFY_CHIPS = listOf(
        "Trending V-Pop",
        "Son Tung M-TP",
        "Den Vau",
        "Vu.",
        "HIEUTHUHAI",
        "Indie Vietnam Picks",
        "Top Hits Vietnam",
        "Acoustic Chill",
        "Rap Viet"
    )

    fun getRecommendationForSession(hour: Int, minute: Int = 0): MusicRecommendation {
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return when (session) {
            BriefingSession.EARLY_MORNING -> MusicRecommendation(
                title = "Morning V-Pop & Acoustic",
                subtitle = "Bright, gentle melodies to welcome the new day",
                genreTag = "Spotify • Morning Start",
                query = "V-Pop Morning Acoustic",
                spotifyUri = "spotify:search:V-Pop%20Acoustic"
            )
            BriefingSession.MID_MORNING -> MusicRecommendation(
                title = "Deep Focus - High Concentration",
                subtitle = "Instrumental & piano music to help you reach a flow state",
                genreTag = "Spotify • Focus Hours",
                query = "Deep Focus Piano Work",
                spotifyUri = "spotify:search:Deep%20Focus%20Work"
            )
            BriefingSession.NOON -> MusicRecommendation(
                title = "Relaxing Noon Cafe & Acoustic",
                subtitle = "Soft music for midday relaxation and a peaceful lunch break",
                genreTag = "Spotify • Lunch Break & Chill",
                query = "Acoustic Cafe Lunch Chill",
                spotifyUri = "spotify:search:Acoustic%20Cafe"
            )
            BriefingSession.AFTERNOON -> MusicRecommendation(
                title = "V-Pop Hits & Afternoon Energy",
                subtitle = "Keep your energy up with trending hits",
                genreTag = "Spotify • Afternoon Boost",
                query = "V-Pop Hits Afternoon",
                spotifyUri = "spotify:search:V-Pop%20Hits"
            )
            BriefingSession.EVENING -> MusicRecommendation(
                title = "Sunset Indie & Chillout",
                subtitle = "Enjoy unwinding after work as the day transitions",
                genreTag = "Spotify • Sunset Relaxation",
                query = "Indie Viet Sunset Chill",
                spotifyUri = "spotify:search:Indie%20Viet"
            )
            BriefingSession.NIGHT -> MusicRecommendation(
                title = "Night Unwind - Evening Relaxation",
                subtitle = "Ballads & lofi to relax your mind after work",
                genreTag = "Spotify • Evening Unwind",
                query = "Night Ballad Chillout",
                spotifyUri = "spotify:search:Night%20Ballad"
            )
            BriefingSession.LATE_NIGHT -> MusicRecommendation(
                title = "Late Night - Deep Sleep Piano",
                subtitle = "Soothing melodies to guide you into restorative sleep",
                genreTag = "Spotify • Deep Sleep & Recovery",
                query = "Night Relaxing Piano Sleep Music",
                spotifyUri = "spotify:search:Night%20Relaxing%20Piano"
            )
        }
    }

    /**
     * Open the Spotify app directly
     */
    fun openSpotifyApp(context: Context) {
        try {
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:home")).apply {
                setPackage("com.spotify.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
        } catch (e: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Cannot launch Spotify: ${e2.message}")
            }
        }
    }

    /**
     * Open music app via Deep Link Intent (Spotify -> YouTube App -> Web YouTube)
     */
    fun openMusicApp(context: Context, query: String, spotifyUri: String? = null) {
        try {
            // Priority 1: Open Spotify app
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri ?: "spotify:search:$query")).apply {
                setPackage("com.spotify.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(spotifyIntent)
            Log.d(TAG, "Launched Spotify via Deep Link")
        } catch (e: Exception) {
            try {
                // Priority 2: Open YouTube app
                val youtubeIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(youtubeIntent)
                Log.d(TAG, "Launched YouTube App via Intent")
            } catch (e2: Exception) {
                try {
                    // Fallback 3: Open web browser for YouTube search
                    val webUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                } catch (e3: Exception) {
                    Log.e(TAG, "Cannot launch music player: ${e3.message}")
                }
            }
        }
    }
}
