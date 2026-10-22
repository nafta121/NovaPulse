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
    VPOP_ARTISTS("🌟 V-Pop & Artists"),
    GENRE_MIX("🎸 Pop • Ballad • Rap"),
    PODCAST_KNOWLEDGE("🎙️ Podcasts & Skills"),
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

    // List of trending artists & quick search suggestions
    val POPULAR_ARTISTS = listOf(
        "Son Tung M-TP",
        "Den Vau",
        "Vu.",
        "HIEUTHUHAI",
        "Soobin Hoang Son",
        "Vu Cat Tuong",
        "GREY D",
        "Trung Quan Idol",
        "Taylor Swift",
        "Best of Rap Viet"
    )

    // Diverse YouTube RSS sources: V-Pop, top artists, podcasts & acoustic
    private val LIVE_SOURCES = listOf(
        // 1. Son Tung M-TP Official Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCKaTC4hX2dE5g_pU5N7K08Q",
            defaultChannelName = "Son Tung M-TP Official",
            categoryType = YouTubeCategoryType.VPOP_ARTISTS,
            categoryTag = "Trending V-Pop",
            aiReason = "Upbeat music from Vietnam's top artist."
        ),
        // 2. Den Vau Official Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCa4g2qQc7b1qF5_e2W5Qxkg",
            defaultChannelName = "Den Vau Official",
            categoryType = YouTubeCategoryType.VPOP_ARTISTS,
            categoryTag = "Conscious Rap",
            aiReason = "Thoughtful and authentic lyrics to relax your mind."
        ),
        // 3. Vietcetera Podcast Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCEm_b62Y4IisWj6S0gq4dvw",
            defaultChannelName = "Vietcetera",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Talk Podcast",
            aiReason = "Insightful podcasts on life, career, and lifestyle."
        ),
        // 4. Web5Ngay Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC20h6p4f0UjU8Xo1jA8Jc2w",
            defaultChannelName = "Web5Ngay",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Life Skills",
            aiReason = "Lessons on energy management and productive mindset."
        ),
        // 5. Cafe Music BGM Channel
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCH4g5C_yYv_7F_c1kQeA1aA",
            defaultChannelName = "Cafe Music BGM",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Acoustic Cafe",
            aiReason = "Gentle music for resting and recharging your energy."
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
        val liveResults = mutableListOf<YouTubeVideoRecommendation>()

        for (source in LIVE_SOURCES) {
            try {
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val xmlString = response.body?.string()
                    if (!xmlString.isNullOrBlank()) {
                        val parsed = parseYouTubeAtomXml(xmlString, source)
                        liveResults.addAll(parsed.take(2))
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

        // Sort videos by priority score learned from the ML model
        val sortedResults = if (context != null) {
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            finalResults.distinctBy { it.videoId.ifBlank { it.title } }.sortedByDescending {
                mlEngine.mediaLearner.calculateCategoryScore(it.categoryType.name)
            }
        } else {
            finalResults.distinctBy { it.videoId.ifBlank { it.title } }
        }

        _liveVideosState.value = sortedResults
        return@withContext _liveVideosState.value
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
                id = "yt_vpop_1",
                videoId = "abPmZCZZrFA",
                title = "Dung Lam Trai Tim Anh Dau - Son Tung M-TP | Official Music Video",
                channelTitle = "Son Tung M-TP Official",
                durationText = "05:18",
                aiReason = "An energetic V-Pop hit for a bright new day.",
                categoryTag = "Trending V-Pop",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Dung Lam Trai Tim Anh Dau Son Tung M-TP"
            ),
            YouTubeVideoRecommendation(
                id = "yt_vpop_2",
                videoId = "Z3q1j3rM3cE",
                title = "Cooking for You - Den ft. PiaLinh | Official Music Video",
                channelTitle = "Den Vau Official",
                durationText = "04:45",
                aiReason = "Inspiring music filled with positivity and warmth.",
                categoryTag = "Conscious Rap",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Nau An Cho Em Den Vau"
            ),
            YouTubeVideoRecommendation(
                id = "yt_genre_1",
                videoId = "K1B_442_gP4",
                title = "Best Vietnamese Ballad & Indie Songs 2026",
                channelTitle = "V-Pop Ballad Station",
                durationText = "01:15:30",
                aiReason = "A curated collection of deeply emotional ballads.",
                categoryTag = "Curated Ballads",
                categoryType = YouTubeCategoryType.GENRE_MIX,
                searchFallbackQuery = "Best Vietnamese Ballad Songs"
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
                videoId = "1ZYbU82GVz4",
                title = "Have A Sip - Conversations on Life & Personal Growth",
                channelTitle = "Vietcetera",
                durationText = "45:10",
                aiReason = "Thoughtful content to help unwind your mind after a busy day.",
                categoryTag = "Talk Podcast",
                categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
                searchFallbackQuery = "Have A Sip Vietcetera podcast"
            )
        )
    }

    /**
     * Search for an artist or song directly on YouTube
     */
    fun openSearchQuery(context: Context, query: String) {
        try {
            val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(searchIntent)
        } catch (e: Exception) {
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
     * Open the official YouTube app
     */
    fun openYouTubeApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
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
     * Open the video directly in the YouTube app or via Deep Link
     */
    fun openYouTubeVideo(context: Context, rec: YouTubeVideoRecommendation) {
        try {
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            mlEngine.recordMediaClick(rec.categoryType.name, "YOUTUBE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed recording ML interaction: ${e.message}")
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
