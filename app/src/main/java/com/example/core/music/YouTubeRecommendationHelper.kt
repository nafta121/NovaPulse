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
    val categoryType: YouTubeCategoryType = YouTubeCategoryType.MUSIC_HIT,
    val searchFallbackQuery: String
) {
    val thumbnailUrl: String
        get() = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
    val watchUrl: String
        get() = "https://www.youtube.com/watch?v=$videoId"
}

enum class YouTubeCategoryType(val label: String) {
    ALL("Tất cả"),
    MUSIC_HIT("🎵 MV & Nhạc Mới"),
    LOFI_CHILL("☕ Lofi & Chill"),
    PODCAST_KNOWLEDGE("🎙️ Podcast & Học tập")
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Danh sách nguồn YouTube RSS trực tiếp (Playlist thịnh hành + Kênh âm nhạc / podcast hàng đầu)
    private val LIVE_SOURCES = listOf(
        // 1. Kênh Nhạc Lofi Live
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCSJ4gkVC6NrvII8umztf0Ow",
            defaultChannelName = "Lofi Girl",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Lofi Tập trung",
            aiReason = "Nhạc Lofi không lời hỗ trợ duy trì trạng thái tập trung sâu."
        ),
        // 2. Kênh Podcast Đời sống & Phỏng vấn
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCEm_b62Y4IisWj6S0gq4dvw",
            defaultChannelName = "Vietcetera",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Podcast trò chuyện",
            aiReason = "Podcast bàn về cuộc sống & công việc giúp thả lỏng tâm trí."
        ),
        // 3. Kênh Kỹ năng sống & Làm việc
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC20h6p4f0UjU8Xo1jA8Jc2w",
            defaultChannelName = "Web5Ngay",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Kỹ năng sống",
            aiReason = "Bài học quản lý năng lượng và tư duy làm việc hiệu quả."
        ),
        // 4. Kênh Cafe BGM Nhạc Chill
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCH4g5C_yYv_7F_c1kQeA1aA",
            defaultChannelName = "Cafe Music BGM",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Acoustic Cafe",
            aiReason = "Âm nhạc nhẹ nhàng hỗ trợ nghỉ ngơi và nạp lại năng lượng."
        )
    )

    private val _liveVideosState = MutableStateFlow(getFallbackVideos())
    val liveVideosState: StateFlow<List<YouTubeVideoRecommendation>> = _liveVideosState.asStateFlow()

    /**
     * Tải và phân tích cú pháp 100% trực tiếp từ YouTube RSS feeds qua mạng
     */
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
                        // Lấy 2 video mới nhất từ mỗi nguồn
                        liveResults.addAll(parsed.take(2))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching YouTube RSS for ${source.defaultChannelName}: ${e.message}")
            }
        }

        // Sắp xếp video theo điểm ưu tiên học được từ mô hình máy học (ML UCB1)
        val sortedResults = if (context != null) {
            val mlEngine = com.example.core.ml.InternalMlEngine.getInstance(context)
            liveResults.sortedByDescending {
                mlEngine.mediaLearner.calculateCategoryScore(it.categoryType.name)
            }
        } else {
            liveResults
        }

        // Cập nhật StateFlow với dữ liệu live lấy từ mạng
        if (sortedResults.isNotEmpty()) {
            _liveVideosState.value = sortedResults
        }
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
                        if (tagName.equals("entry", ignoreCase = true)) {
                            inEntry = false
                            if (curVideoId.isNotBlank() && curTitle.isNotBlank()) {
                                val channel = if (curAuthorName.isNotBlank()) curAuthorName else source.defaultChannelName
                                val timeText = formatIsoPublished(curPublished)

                                list.add(
                                    YouTubeVideoRecommendation(
                                        id = "yt_live_${curVideoId}",
                                        videoId = curVideoId,
                                        title = curTitle,
                                        channelTitle = channel,
                                        durationText = timeText,
                                        aiReason = source.aiReason,
                                        categoryTag = source.categoryTag,
                                        categoryType = source.categoryType,
                                        searchFallbackQuery = curTitle
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing YouTube XML: ${e.message}")
        }
        return list
    }

    private fun formatIsoPublished(isoDate: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val date = sdf.parse(isoDate) ?: return "Mới tải lên"
            val diffMinutes = (System.currentTimeMillis() - date.time) / (1000 * 60)
            when {
                diffMinutes < 60 -> "$diffMinutes phút trước"
                diffMinutes < 1440 -> "${diffMinutes / 60} giờ trước"
                diffMinutes < 10080 -> "${diffMinutes / 1440} ngày trước"
                else -> "Gần đây"
            }
        } catch (e: Exception) {
            "Mới tải lên"
        }
    }

    private fun getFallbackVideos(): List<YouTubeVideoRecommendation> {
        return listOf(
            YouTubeVideoRecommendation(
                id = "yt_fallback_1",
                videoId = "jfKfPfyJRdk",
                title = "Lofi Hip Hop Radio - Beats to Relax/Study/Deep Work to",
                channelTitle = "Lofi Girl",
                durationText = "TRỰC TIẾP",
                aiReason = "Khung giờ vàng tập trung (Deep Work) theo nhịp sinh học.",
                categoryTag = "Lofi Tập trung",
                categoryType = YouTubeCategoryType.LOFI_CHILL,
                searchFallbackQuery = "Lofi girl radio beats to relax study"
            ),
            YouTubeVideoRecommendation(
                id = "yt_fallback_2",
                videoId = "1ZYbU82GVz4",
                title = "Have A Sip - Trò chuyện về cuộc sống & Hành trình trưởng thành",
                channelTitle = "Vietcetera",
                durationText = "45:10",
                aiReason = "Nội dung sâu sắc giúp thả lỏng tâm trí sau ngày bận rộn.",
                categoryTag = "Podcast trò chuyện",
                categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
                searchFallbackQuery = "Have A Sip Vietcetera podcast"
            )
        )
    }

    /**
     * Mở trực tiếp video trong YouTube App hoặc Deep Link
     */
    fun openYouTubeVideo(context: Context, rec: YouTubeVideoRecommendation) {
        // Ghi nhận tương tác vào mô hình máy học (ML UCB1)
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
            try {
                val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", rec.searchFallbackQuery)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(searchIntent)
            } catch (e3: Exception) {
                Log.e(TAG, "Failed launching YouTube: ${e3.message}")
            }
        }
    }
}
