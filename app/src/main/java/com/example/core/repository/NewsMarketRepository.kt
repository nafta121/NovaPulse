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

    private val httpClient: OkHttpClient
        get() = com.example.NovaPulseApp.sharedHttpClient

    private val _newsState = MutableStateFlow(getDefaultArticles())
    val newsState: StateFlow<List<NewsBriefArticle>> = _newsState.asStateFlow()

    fun getMarketTickers(): List<MarketTickerItem> {
        return listOf(
            MarketTickerItem(symbol = "VN-INDEX", value = "1,288.5", changePercent = 0.65),
            MarketTickerItem(symbol = "SJC Gold", value = "89.5M", changePercent = 0.34, unit = "VND/tael"),
            MarketTickerItem(symbol = "USD/VND", value = "25,460", changePercent = -0.08, unit = "VND"),
            MarketTickerItem(symbol = "BTC", value = "$96,850", changePercent = 2.45)
        )
    }

    fun getDefaultArticles(): List<NewsBriefArticle> {
        return listOf(
            NewsBriefArticle(
                id = "vnexpress_top_1",
                title = "Samsung upgrades Galaxy AI experience on new mobile platform",
                summary = "Smart summarization features and battery life optimizations are deeply integrated into the system.",
                source = "VnExpress",
                category = "Technology",
                timeAgo = "15m ago",
                url = "https://vnexpress.net/so-hoa"
            ),
            NewsBriefArticle(
                id = "vnexpress_top_2",
                title = "Financial markets and tech stocks make an impressive breakout",
                summary = "Capital flows strongly into key sectors ahead of the new growth wave.",
                source = "VnExpress",
                category = "Business",
                timeAgo = "45m ago",
                url = "https://vnexpress.net/kinh-doanh"
            ),
            NewsBriefArticle(
                id = "vnexpress_top_3",
                title = "Secrets to maintaining energy and staying focused at work",
                summary = "Practicing interval work sessions helps the brain stay sharp and alert throughout the day.",
                source = "VnExpress",
                category = "Lifestyle",
                timeAgo = "1h ago",
                url = "https://vnexpress.net/doi-song"
            )
        )
    }

    suspend fun fetchLiveNews(): List<NewsBriefArticle> = withContext(Dispatchers.IO) {
        val rssUrls = listOf(
            "https://vnexpress.net/rss/tin-moi-nhat.rss" to "Latest News",
            "https://vnexpress.net/rss/so-hoa.rss" to "Technology",
            "https://vnexpress.net/rss/kinh-doanh.rss" to "Business"
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
                                    curLink.contains("/so-hoa/") -> "Technology"
                                    curLink.contains("/kinh-doanh/") -> "Business"
                                    curLink.contains("/the-thao/") -> "Sports"
                                    curLink.contains("/giai-tri/") -> "Entertainment"
                                    curLink.contains("/suc-khoe/") -> "Health"
                                    curLink.contains("/khoa-hoc/") -> "Science"
                                    curLink.contains("/thoi-su/") -> "News"
                                    curLink.contains("/the-gioi/") -> "World"
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
            val date = sdf.parse(pubDateStr) ?: return "Just now"
            val diffMinutes = (System.currentTimeMillis() - date.time) / (1000 * 60)
            when {
                diffMinutes < 1 -> "Just now"
                diffMinutes < 60 -> "${diffMinutes}m ago"
                diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
                diffMinutes < 2880 -> "Yesterday"
                else -> "${diffMinutes / 1440}d ago"
            }
        } catch (e: Exception) {
            "Recently updated"
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
