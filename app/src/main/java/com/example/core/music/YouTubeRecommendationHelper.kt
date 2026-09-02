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
    ALL("Tất cả"),
    VPOP_ARTISTS("🌟 V-Pop & Nghệ sĩ"),
    GENRE_MIX("🎸 Pop • Ballad • Rap"),
    PODCAST_KNOWLEDGE("🎙️ Podcast & Kỹ năng"),
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // Danh sách các nghệ sĩ thịnh hành & gợi ý tìm kiếm nhanh
    val POPULAR_ARTISTS = listOf(
        "Sơn Tùng M-TP",
        "Đen Vâu",
        "Vũ.",
        "HIEUTHUHAI",
        "Soobin Hoàng Sơn",
        "Vũ Cát Tường",
        "GREY D",
        "Trung Quân Idol",
        "Taylor Swift",
        "Rap Việt Tuyển Chọn"
    )

    // Nguồn YouTube RSS đa dạng: V-Pop, Nghệ sĩ hàng đầu, Podcast & Acoustic
    private val LIVE_SOURCES = listOf(
        // 1. Kênh Sơn Tùng M-TP Official
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCKaTC4hX2dE5g_pU5N7K08Q",
            defaultChannelName = "Sơn Tùng M-TP Official",
            categoryType = YouTubeCategoryType.VPOP_ARTISTS,
            categoryTag = "V-Pop Thịnh hành",
            aiReason = "Âm nhạc sôi động từ nghệ sĩ hàng đầu Việt Nam."
        ),
        // 2. Kênh Đen Vâu Official
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCa4g2qQc7b1qF5_e2W5Qxkg",
            defaultChannelName = "Đen Vâu Official",
            categoryType = YouTubeCategoryType.VPOP_ARTISTS,
            categoryTag = "Rap Triết lý",
            aiReason = "Lời ca mộc mạc và suy ngẫm giúp thư giãn tâm trí."
        ),
        // 3. Kênh Vietcetera Podcast
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCEm_b62Y4IisWj6S0gq4dvw",
            defaultChannelName = "Vietcetera",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Podcast trò chuyện",
            aiReason = "Podcast sâu sắc về cuộc sống, sự nghiệp và phong cách sống."
        ),
        // 4. Kênh Web5Ngay
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC20h6p4f0UjU8Xo1jA8Jc2w",
            defaultChannelName = "Web5Ngay",
            categoryType = YouTubeCategoryType.PODCAST_KNOWLEDGE,
            categoryTag = "Kỹ năng sống",
            aiReason = "Bài học quản lý năng lượng và tư duy làm việc hiệu quả."
        ),
        // 5. Kênh Cafe Music BGM
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCH4g5C_yYv_7F_c1kQeA1aA",
            defaultChannelName = "Cafe Music BGM",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Acoustic Cafe",
            aiReason = "Âm nhạc nhẹ nhàng hỗ trợ nghỉ ngơi và nạp lại năng lượng."
        ),
        // 6. Kênh Lofi Girl
        YouTubeFeedSource(
            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCSJ4gkVC6NrvII8umztf0Ow",
            defaultChannelName = "Lofi Girl",
            categoryType = YouTubeCategoryType.LOFI_CHILL,
            categoryTag = "Lofi Tập trung",
            aiReason = "Giai điệu không lời êm dịu hỗ trợ tập trung."
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

        // Sắp xếp video theo điểm ưu tiên học được từ mô hình máy học
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
            if (isoDate.isBlank()) return "Mới phát hành"
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(isoDate.substring(0, minOf(19, isoDate.length))) ?: return "Mới"
            val diffMs = System.currentTimeMillis() - date.time
            val diffDays = (diffMs / (24 * 3600 * 1000L)).toInt()
            when {
                diffDays <= 0 -> "Hôm nay"
                diffDays == 1 -> "Hôm qua"
                diffDays < 7 -> "$diffDays ngày trước"
                else -> "${diffDays / 7} tuần trước"
            }
        } catch (e: Exception) {
            "Mới cập nhật"
        }
    }

    fun getFallbackVideos(): List<YouTubeVideoRecommendation> {
        return listOf(
            YouTubeVideoRecommendation(
                id = "yt_vpop_1",
                videoId = "abPmZCZZrFA",
                title = "Đừng Làm Trái Tim Anh Đau - Sơn Tùng M-TP | Official Music Video",
                channelTitle = "Sơn Tùng M-TP Official",
                durationText = "05:18",
                aiReason = "Bản hit V-Pop năng lượng cho ngày mới tươi vui.",
                categoryTag = "V-Pop Thịnh hành",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Đừng Làm Trái Tim Anh Đau Sơn Tùng M-TP"
            ),
            YouTubeVideoRecommendation(
                id = "yt_vpop_2",
                videoId = "Z3q1j3rM3cE",
                title = "Nấu Ăn Cho Em - Đen ft. PiaLinh | Official Music Video",
                channelTitle = "Đen Vâu Official",
                durationText = "04:45",
                aiReason = "Âm nhạc truyền cảm hứng tích cực và yêu thương.",
                categoryTag = "Rap Triết lý",
                categoryType = YouTubeCategoryType.VPOP_ARTISTS,
                searchFallbackQuery = "Nấu Ăn Cho Em Đen Vâu"
            ),
            YouTubeVideoRecommendation(
                id = "yt_genre_1",
                videoId = "K1B_442_gP4",
                title = "Những Bản Ballad & Indie Hay Nhất Việt Nam 2026",
                channelTitle = "V-Pop Ballad Station",
                durationText = "01:15:30",
                aiReason = "Tổng hợp các bản Ballad cảm xúc sâu lắng.",
                categoryTag = "Ballad Tuyển chọn",
                categoryType = YouTubeCategoryType.GENRE_MIX,
                searchFallbackQuery = "Nhạc Ballad Việt Nam hay nhất"
            ),
            YouTubeVideoRecommendation(
                id = "yt_fallback_1",
                videoId = "jfKfPfyJRdk",
                title = "Lofi Hip Hop Radio - Beats to Relax/Study/Deep Work",
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
     * Tìm kiếm nghệ sĩ / ca khúc trực tiếp trên YouTube
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
     * Mở ứng dụng YouTube chính thức
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
     * Mở trực tiếp video trong YouTube App hoặc Deep Link
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
