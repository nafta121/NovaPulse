package com.example.core.repository

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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MarketTickerItem(
    val symbol: String,
    val value: String,
    val changePercent: Double,
    val unit: String = ""
) {
    val isPositive: Boolean get() = changePercent >= 0
    val formattedChange: String
        get() = (if (changePercent >= 0) "+" else "") + String.format("%.2f%%", changePercent)
}

data class NewsBriefArticle(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val category: String,
    val timeAgo: String,
    val url: String
)

object NewsMarketRepository {
    private const val TAG = "NewsMarketRepository"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _newsState = MutableStateFlow(getDefaultArticles())
    val newsState: StateFlow<List<NewsBriefArticle>> = _newsState.asStateFlow()

    fun getMarketTickers(): List<MarketTickerItem> {
        return listOf(
            MarketTickerItem(symbol = "VN-INDEX", value = "1,288.5", changePercent = 0.65),
            MarketTickerItem(symbol = "Vàng SJC", value = "89.5M", changePercent = 0.34, unit = "₫/lượng"),
            MarketTickerItem(symbol = "USD/VND", value = "25,460", changePercent = -0.08, unit = "₫"),
            MarketTickerItem(symbol = "BTC", value = "$96,850", changePercent = 2.45)
        )
    }

    fun getDefaultArticles(): List<NewsBriefArticle> {
        return listOf(
            NewsBriefArticle(
                id = "vnexpress_top_1",
                title = "Samsung nâng cấp trải nghiệm AI Galaxy trên nền tảng di động mới",
                summary = "Các tính năng tóm tắt thông minh và tối ưu hóa thời lượng pin được tích hợp sâu vào hệ thống.",
                source = "VnExpress",
                category = "Công nghệ",
                timeAgo = "15 phút trước",
                url = "https://vnexpress.net/so-hoa"
            ),
            NewsBriefArticle(
                id = "vnexpress_top_2",
                title = "Thị trường tài chính và cổ phiếu công nghệ bứt phá ấn tượng",
                summary = "Dòng tiền dịch chuyển mạnh mẽ vào các nhóm ngành chủ chốt đón sóng tăng trưởng.",
                source = "VnExpress",
                category = "Kinh doanh",
                timeAgo = "45 phút trước",
                url = "https://vnexpress.net/kinh-doanh"
            ),
            NewsBriefArticle(
                id = "vnexpress_top_3",
                title = "Bí quyết duy trì năng lượng và tập trung làm việc hiệu quả",
                summary = "Thực hiện các phiên làm việc ngắt quãng giúp não bộ luôn duy trì trạng thái tỉnh táo.",
                source = "VnExpress",
                category = "Đời sống",
                timeAgo = "1 giờ trước",
                url = "https://vnexpress.net/doi-song"
            )
        )
    }

    suspend fun fetchLiveNews(): List<NewsBriefArticle> = withContext(Dispatchers.IO) {
        val rssUrls = listOf(
            "https://vnexpress.net/rss/tin-moi-nhat.rss" to "Tin mới",
            "https://vnexpress.net/rss/so-hoa.rss" to "Công nghệ",
            "https://vnexpress.net/rss/kinh-doanh.rss" to "Kinh doanh"
        )

        for ((rssUrl, defaultCategory) in rssUrls) {
            try {
                val request = Request.Builder()
                    .url(rssUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val xmlString = response.body?.string()
                    if (!xmlString.isNullOrBlank()) {
                        val parsed = parseVnExpressRss(xmlString, defaultCategory)
                        if (parsed.isNotEmpty()) {
                            _newsState.value = parsed
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed fetching RSS from $rssUrl: ${e.message}")
            }
        }
        return@withContext _newsState.value
    }

    private fun parseVnExpressRss(xml: String, defaultCategory: String): List<NewsBriefArticle> {
        val list = mutableListOf<NewsBriefArticle>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var curTitle = ""
            var curLink = ""
            var curDescription = ""
            var curPubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT && list.size < 4) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            curTitle = ""
                            curLink = ""
                            curDescription = ""
                            curPubDate = ""
                        } else if (inItem) {
                            when {
                                tagName.equals("title", ignoreCase = true) -> curTitle = parser.nextText().trim()
                                tagName.equals("link", ignoreCase = true) -> curLink = parser.nextText().trim()
                                tagName.equals("description", ignoreCase = true) -> curDescription = parser.nextText().trim()
                                tagName.equals("pubDate", ignoreCase = true) -> curPubDate = parser.nextText().trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = false
                            if (curTitle.isNotBlank() && curLink.isNotBlank()) {
                                val cleanDesc = curDescription
                                    .replace(Regex("<.*?>"), "") // Strip HTML tags
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .replace("&#39;", "'")
                                    .trim()

                                val timeAgo = formatPubDate(curPubDate)
                                val category = when {
                                    curLink.contains("/so-hoa/") -> "Công nghệ"
                                    curLink.contains("/kinh-doanh/") -> "Kinh doanh"
                                    curLink.contains("/the-thao/") -> "Thể thao"
                                    curLink.contains("/giai-tri/") -> "Giải trí"
                                    curLink.contains("/suc-khoe/") -> "Sức khỏe"
                                    curLink.contains("/khoa-hoc/") -> "Khoa học"
                                    curLink.contains("/thoi-su/") -> "Thời sự"
                                    curLink.contains("/the-gioi/") -> "Thế giới"
                                    else -> defaultCategory
                                }

                                list.add(
                                    NewsBriefArticle(
                                        id = "art_${curLink.hashCode()}",
                                        title = curTitle,
                                        summary = cleanDesc,
                                        source = "VnExpress",
                                        category = category,
                                        timeAgo = timeAgo,
                                        url = curLink
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS: ${e.message}")
        }
        return list
    }

    private fun formatPubDate(pubDateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            val date = sdf.parse(pubDateStr) ?: return "Vừa xong"
            val diffMinutes = (System.currentTimeMillis() - date.time) / (1000 * 60)
            when {
                diffMinutes < 1 -> "Vừa xong"
                diffMinutes < 60 -> "$diffMinutes phút trước"
                diffMinutes < 1440 -> "${diffMinutes / 60} giờ trước"
                else -> "${diffMinutes / 1440} ngày trước"
            }
        } catch (e: Exception) {
            "Mới cập nhật"
        }
    }

    fun openArticle(context: Context, url: String) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open article url: $url, ${e.message}")
        }
    }
}
