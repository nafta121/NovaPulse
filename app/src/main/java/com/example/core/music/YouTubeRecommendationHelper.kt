package com.example.core.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class YouTubeVideoRecommendation(
    val id: String,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val durationText: String,
    val aiReason: String,
    val categoryTag: String,
    val categoryType: YouTubeCategoryType = YouTubeCategoryType.VPOP_ARTISTS,
    val searchFallbackQuery: String
) {
    val thumbnailUrl: String
        get() = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
    val watchUrl: String
        get() = "https://www.youtube.com/watch?v=$videoId"
}

enum class YouTubeCategoryType(val label: String) {
    ALL("All"),
    VPOP_ARTISTS("🎵 Music & Artists"),
    GENRE_MIX("🎸 Pop • Indie • Hits"),
    PODCAST_KNOWLEDGE("🎙️ Podcasts & Tech"),
    LOFI_CHILL("☕ Acoustic & Lofi")
}

data class YouTubeFeedSource(
    val url: String,
    val defaultChannelName: String,
    val categoryType: YouTubeCategoryType,
    val categoryTag: String,
    val aiReason: String
)

object YouTubeRecommendationHelper {

    private const val TAG = "YouTubeRecHelper"

    private val httpClient: OkHttpClient
        get() = com.example.NovaPulseApp.sharedHttpClient

    // Curated YouTube RSS sources: Music Labels, Tech/Knowledge Podcasts & Acoustic/Lofi
    private val LIVE_SOURCES = listOf(
        // 1. Musica Studios (Indonesian Pop & Band Hits - Noah, D'Masiv, Nidji, etc.)
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCU_fuBEJq12D37WcE7Gn2Tw",
            defaultChannelName = "Musica Studios",
            categoryType = YouTubeCategoryType.VPOP_ARTISTS,
            categoryTag = "Pop & Band Hits",
            aiReason = "Trending music releases and timeless hits for your daily rhythm."
        ),
        // 2. Aquarius Musikindo (Top Indonesian Pop & Indie Releases)
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCk7v_7F6e5G1j_7fG6W_z9Q",
            defaultChannelName = "Aquarius Musikindo",
            categoryType = YouTubeCategoryType.GENRE_MIX,
            categoryTag = "Top Pop & Ballad",
            aiReason = "Popular tracks curated for your current mood."
        ),
        // 3. GadgetIn (Tech Reviews & Digital Lifestyle)
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC1dI4tO13ApuSX0QeX8pHng",
            defaultChannelName = "GadgetIn",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Tech & Gadgets",
            aiReason = "Latest technology insights and smartphone reviews."
        ),
        // 4. TED-Ed (Knowledge & Productivity)
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsooa4yRKGN_zEE8iknghZA",
            defaultChannelName = "TED-Ed",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Knowledge & Skills",
            aiReason = "Short lessons worth sharing to sharpen your mind."
        ),
        // 5. Cafe Music BGM Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCJhjE7wbdYAae1G25m0tHAA",
            defaultChannelName = "Cafe Music BGM",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Acoustic Cafe",
            aiReason = "Gentle jazz and acoustic music for resting and recharging your energy."
        ),
        // 6. Lofi Girl Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCSJ4gkVC6NrvII8umztf0Ow",
            defaultChannelName = "Lofi Girl",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Focus Lofi",
            aiReason = "Soothing instrumental beats to help you stay focused."
        )
    )

    private val _liveVideosState = MutableStateFlow(getFallbackVideos())
    val liveVideosState: StateFlow<List<YouTubeVideoRecommendation>> = _liveVideosState.asStateFlow()

    suspend fun fetchLiveYouTubeVideos(context: Context? = null): List<YouTubeVideoRecommendation> = withContext(Dispatchers.IO) {
        if (context != null) {
            RecentMediaTracker.refreshActiveSessions(context)
        }
        val liveResults = mutableListOf<YouTubeVideoRecommendation>()

        // 1. Prioritize real tracks/artists currently or recently played on the user's device (YouTube Music / Spotify / YouTube)
        val deviceHistory = RecentMediaTracker.recentHistoryState.value.take(1)
        for (item in deviceHistory) {
            val query = if (item.artist.isNotBlank()) "${item.title} ${item.artist}" else item.title
            val resolved = searchYouTubeVideoForQuery(
                query = query,
                channelFallback = item.artist.ifBlank { item.appName },
                categoryTag = "From Your ${item.appName}",
                aiReason = "Based on your recent playback on ${item.appName}.",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS
            )
            if (resolved != null) {
                liveResults.add(resolved)
            }
        }

        // 2. If device history has fewer than 2 items, fetch live trending music for the user's country
        if (liveResults.size < 2) {
            val country = com.example.core.location.LocationHelper.getDetectedCountryCode(context)
            val regionalQuery = if (country == "ID") {
                "Lagu Pop Indonesia Terpopuler Official MV"
            } else {
                "Top Pop Music Hits Official Video"
            }
            val trendingHit = searchYouTubeVideoForQuery(
                query = regionalQuery,
                channelFallback = "YouTube Music",
                categoryTag = "Trending Music",
                aiReason = "Trending music in your region right now.",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS
            )
            if (trendingHit != null) {
                liveResults.add(trendingHit)
            }
        }

        // 3. Fetch from curated YouTube RSS feeds
        for (source in LIVE_SOURCES) {
            try {
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xmlString = response.body?.string()
                        if (!xmlString.isNullOrBlank()) {
                            val parsed = parseYouTubeAtomXml(xmlString, source)
                            liveResults.addAll(parsed.take(2))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching YouTube RSS for ${source.defaultChannelName}: ${e.message}")
            }
        }

        val finalResults = if (liveResults.isNotEmpty()) {
            liveResults + getFallbackVideos()
        } else {
            getFallbackVideos()
        }

        // Sort videos by priority score learned from the ML model, keeping device-history items on top
        val sortedResults = if (context != null) {
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            finalResults.distinctBy { it.videoId.ifBlank { it.title } }.sortedByDescending { rec ->
                val baseScore = mlEngine.mediaLearner.calculateCategoryScore(rec.categoryType.name)
                if (rec.categoryTag.startsWith("From Your")) baseScore + 100.0f else baseScore
            }
        } else {
            finalResults.distinctBy { it.videoId.ifBlank { it.title } }
        }

        _liveVideosState.value = sortedResults
        return@withContext _liveVideosState.value
    }

    private fun searchYouTubeVideoForQuery(
        query: String,
        channelFallback: String,
        categoryTag: String,
        aiReason: String,
        categoryType: YouTubeCategoryType
    ): YouTubeVideoRecommendation? {
        return try {
            val url = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string() ?: return null
                val marker = "\"videoRenderer\":{\"videoId\":\""
                val startIdx = html.indexOf(marker)
                if (startIdx < 0) return null
                val snippet = html.substring(startIdx, minOf(html.length, startIdx + 700))
                val videoId = snippet.substringAfter(marker, "").take(11)
                if (videoId.length != 11) return null
                val rawTitle = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"")
                    .find(snippet)?.groupValues?.getOrNull(1)
                    ?.replace("\\u0026", "&")
                    ?.replace("&amp;", "&")
                    ?.replace("\\\"", "\"")
                    ?: query
                YouTubeVideoRecommendation(
                    id = "yt_search_$videoId",
                    videoId = videoId,
                    title = rawTitle,
                    channelTitle = channelFallback,
                    durationText = "YouTube Music",
                    aiReason = aiReason,
                    categoryTag = categoryTag,
                    categoryType = categoryType,
                    searchFallbackQuery = query
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseYouTubeAtomXml(
        xml: String,
        source: YouTubeFeedSource
    ): List<YouTubeVideoRecommendation> {
        val list = mutableListOf<YouTubeVideoRecommendation>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inEntry = false
            var curVideoId = ""
            var curTitle = ""
            var curAuthorName = ""
            var curPublished = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("entry", ignoreCase = true)) {
                            inEntry = true
                            curVideoId = ""
                            curTitle = ""
                            curAuthorName = ""
                            curPublished = ""
                        } else if (inEntry) {
                            when {
                                tagName.equals("videoId", ignoreCase = true) -> {
                                    curVideoId = parser.nextText().trim()
                                }
                                tagName.equals("title", ignoreCase = true) && curTitle.isBlank() -> {
                                    curTitle = parser.nextText().trim()
                                }
                                tagName.equals("name", ignoreCase = true) && curAuthorName.isBlank() -> {
                                    curAuthorName = parser.nextText().trim()
                                }
                                tagName.equals("published", ignoreCase = true) -> {
                                    curPublished = parser.nextText().trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("entry", ignoreCase = true) && inEntry) {
                            inEntry = false
                            if (curVideoId.isNotBlank() && curTitle.isNotBlank()) {
                                val channel = if (curAuthorName.isNotBlank()) curAuthorName else source.defaultChannelName
                                val formattedDate = formatRelativeTime(curPublished)

                                list.add(
                                    YouTubeVideoRecommendation(
                                        id = "yt_live_$curVideoId",
                                        videoId = curVideoId,
                                        title = curTitle,
                                        channelTitle = channel,
                                        durationText = formattedDate,
                                        aiReason = source.aiReason,
                                        categoryTag = source.categoryTag,
                                        categoryType = source.categoryType,
                                        searchFallbackQuery = "$curTitle $channel"
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML: ${e.message}")
        }
        return list
    }

    private fun formatRelativeTime(isoDate: String): String {
        return try {
            if (isoDate.isBlank()) return "New release"
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(isoDate.substring(0, minOf(19, isoDate.length))) ?: return "New"
            val diffMs = System.currentTimeMillis() - date.time
            val diffDays = (diffMs / (24 * 3600 * 1000L)).toInt()
            when {
                diffDays <= 0 -> "Today"
                diffDays == 1 -> "Yesterday"
                diffDays < 7 -> "$diffDays days ago"
                else -> "${diffDays / 7} weeks ago"
            }
        } catch (e: Exception) {
            "Recently updated"
        }
    }

    fun getFallbackVideos(): List<YouTubeVideoRecommendation> {
        return listOf(
            YouTubeVideoRecommendation(
                id = "yt_pop_1",
                videoId = "m57X6f12hNs",
                title = "TULUS - Hati-Hati di Jalan (Official Music Video)",
                channelTitle = "Tulus",
                durationText = "04:22",
                aiReason = "Warm Indonesian pop hit for a calm and uplifting mood.",
                categoryTag = "Top Pop Hit",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Tulus Hati Hati di Jalan"
            ),
            YouTubeVideoRecommendation(
                id = "yt_pop_2",
                videoId = "e2f1y1W4uG8",
                title = "Hindia - Evaluasi (Official Music Video)",
                channelTitle = "Hindia",
                durationText = "03:45",
                aiReason = "Reflective and energizing indie anthem to recharge your spirit.",
                categoryTag = "Indie Favorite",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Hindia Evaluasi Official Music Video"
            ),
            YouTubeVideoRecommendation(
                id = "yt_genre_1",
                videoId = "5qap5aO4i9A",
                title = "Best Indonesian Acoustic & Indie Chill Mix",
                channelTitle = "YouTube Music Mix",
                durationText = "58:20",
                aiReason = "A curated collection of acoustic and indie tracks.",
                categoryTag = "Curated Mix",
                categoryType = YouTubeCategoryType.GENRE_MIX,
                searchFallbackQuery = "Best Indonesian Acoustic Indie Songs"
            ),
            YouTubeVideoRecommendation(
                id = "yt_fallback_1",
                videoId = "jfKfPfyJRdk",
                title = "Lofi Hip Hop Radio - Beats to Relax/Study/Deep Work",
                channelTitle = "Lofi Girl",
                durationText = "LIVE",
                aiReason = "Peak focus hours (Deep Work) aligned with your circadian rhythm.",
                categoryTag = "Focus Lofi",
                categoryType = YouTubeCategoryType.LOFI_CHILL,
                searchFallbackQuery = "Lofi girl radio beats to relax study"
            ),
            YouTubeVideoRecommendation(
                id = "yt_fallback_2",
                videoId = "Dx5qFachd3A",
                title = "Relaxing Jazz Piano & Bossa Nova Cafe Music",
                channelTitle = "Cafe Music BGM",
                durationText = "01:30:00",
                aiReason = "Soothing background music to unwind after a busy day.",
                categoryTag = "Acoustic Cafe",
                categoryType = YouTubeCategoryType.LOFI_CHILL,
                searchFallbackQuery = "Relaxing Jazz Piano Cafe Music BGM"
            )
        )
    }

    /**
     * Search for an artist or song on YouTube Music (if installed/active) or YouTube
     */
    fun openSearchQuery(context: Context, query: String) {
        val recentPkg = RecentMediaTracker.currentPlaybackState.value?.packageName ?: ""
        if (recentPkg.contains("youtube.music")) {
            MusicRecommendationHelper.openYouTubeMusicApp(context, query)
            return
        }

        try {
            val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(searchIntent)
        } catch (e: Exception) {
            if (MusicRecommendationHelper.isPackageInstalled(context, MusicRecommendationHelper.YT_MUSIC_PACKAGE)) {
                MusicRecommendationHelper.openYouTubeMusicApp(context, query)
                return
            }
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed searching YouTube: ${e2.message}")
            }
        }
    }

    /**
     * Open the official YouTube or YouTube Music app
     */
    fun openYouTubeApp(context: Context) {
        val recentPkg = RecentMediaTracker.currentPlaybackState.value?.packageName ?: ""
        if (recentPkg.contains("youtube.music")) {
            MusicRecommendationHelper.openYouTubeMusicApp(context)
            return
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: context.packageManager.getLaunchIntentForPackage(MusicRecommendationHelper.YT_MUSIC_PACKAGE)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch YouTube: ${e.message}")
        }
    }

    /**
     * Open the video directly in YouTube Music (if preferred for music) or the YouTube app
     */
    fun openYouTubeVideo(context: Context, rec: YouTubeVideoRecommendation) {
        try {
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            mlEngine.recordMediaClick(rec.categoryType.name, "YOUTUBE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed recording ML interaction: ${e.message}")
        }

        val recentPkg = RecentMediaTracker.currentPlaybackState.value?.packageName ?: ""
        val isMusicCategory = rec.categoryType == YouTubeCategoryType.VPOP_ARTISTS ||
            rec.categoryType == YouTubeCategoryType.GENRE_MIX

        if (recentPkg.contains("youtube.music") && isMusicCategory && rec.videoId.isNotBlank()) {
            try {
                val ytMusicIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://music.youtube.com/watch?v=${rec.videoId}")
                ).apply {
                    setPackage(MusicRecommendationHelper.YT_MUSIC_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(ytMusicIntent)
                return
            } catch (_: Exception) {}
        }

        try {
            if (rec.videoId.isNotBlank()) {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${rec.videoId}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(appIntent)
                Log.d(TAG, "Opened YouTube App with videoId: ${rec.videoId}")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cannot launch vnd.youtube, trying web/app browser fallback")
        }

        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.watchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            openSearchQuery(context, rec.searchFallbackQuery)
        }
    }
}
