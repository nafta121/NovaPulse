package com.example.core.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.core.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

data class MarketTickerItem(
    val symbol: String,
    val value: String,
    val changePercent: Double,
    val unit: String = ""
) {
    val isPositive: Boolean get() = changePercent >= 0
    val formattedChange: String
        get() = (if (changePercent >= 0) "+" else "") + String.format(Locale.US, "%.2f%%", changePercent)
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

    private val _tickersState = MutableStateFlow(getMarketTickers())
    val tickersState: StateFlow<List<MarketTickerItem>> = _tickersState.asStateFlow()

    fun getMarketTickers(context: Context? = null): List<MarketTickerItem> {
        val country = LocationHelper.getDetectedCountryCode(context)
        return when (country) {
            "ID" -> listOf(
                MarketTickerItem(symbol = "IHSG", value = "7,328.4", changePercent = 0.48),
                MarketTickerItem(symbol = "USD/IDR", value = "16,240", changePercent = -0.12, unit = "IDR"),
                MarketTickerItem(symbol = "BTC/USD", value = "$96,850", changePercent = 2.45),
                MarketTickerItem(symbol = "EMAS/GOLD", value = "$2,915", changePercent = 0.42, unit = "USD/oz")
            )
            else -> listOf(
                MarketTickerItem(symbol = "BTC/USD", value = "$96,850", changePercent = 2.45),
                MarketTickerItem(symbol = "ETH/USD", value = "$2,780", changePercent = 1.65),
                MarketTickerItem(symbol = "GOLD", value = "$2,915", changePercent = 0.42, unit = "USD/oz"),
                MarketTickerItem(symbol = "USD/IDR", value = "16,240", changePercent = -0.12, unit = "IDR")
            )
        }
    }

    fun getDefaultArticles(context: Context? = null): List<NewsBriefArticle> {
        val country = LocationHelper.getDetectedCountryCode(context)
        return if (country == "ID") {
            listOf(
                NewsBriefArticle(
                    id = "id_top_1",
                    title = "Samsung tingkatkan fitur Galaxy AI dan integrasi Now Brief di perangkat Galaxy terbaru",
                    summary = "Fitur ringkasan cerdas real-time dan optimalisasi baterai semakin terintegrasi langsung di perangkat.",
                    source = "Kompas Tekno",
                    category = "Technology",
                    timeAgo = "Baru saja",
                    url = "https://tekno.kompas.com"
                ),
                NewsBriefArticle(
                    id = "id_top_2",
                    title = "IHSG dan pasar keuangan domestik menguat didorong sentimen positif sektor teknologi",
                    summary = "Arus modal masuk ke sektor unggulan menguat seiring stabilitas nilai tukar rupiah.",
                    source = "CNBC Indonesia",
                    category = "Business",
                    timeAgo = "20m lalu",
                    url = "https://www.cnbcindonesia.com/market"
                ),
                NewsBriefArticle(
                    id = "id_top_3",
                    title = "Tips menjaga fokus dan produktivitas kerja dengan manajemen energi harian",
                    summary = "Mengatur ritme istirahat singkat membantu menjaga konsentrasi tetap optimal sepanjang hari.",
                    source = "ANTARA News",
                    category = "Lifestyle",
                    timeAgo = "45m lalu",
                    url = "https://www.antaranews.com"
                )
            )
        } else {
            listOf(
                NewsBriefArticle(
                    id = "global_top_1",
                    title = "On-device AI assistants bring real-time contextual briefings to mobile devices",
                    summary = "Smart summarization features and battery life optimizations are deeply integrated into the system.",
                    source = "Google News",
                    category = "Technology",
                    timeAgo = "15m ago",
                    url = "https://news.google.com"
                ),
                NewsBriefArticle(
                    id = "global_top_2",
                    title = "Global financial markets and tech stocks show strong momentum",
                    summary = "Capital flows into AI and semiconductor sectors ahead of quarterly earnings.",
                    source = "Reuters",
                    category = "Business",
                    timeAgo = "45m ago",
                    url = "https://www.reuters.com/markets"
                )
            )
        }
    }

    suspend fun fetchLiveNews(context: Context? = null): List<NewsBriefArticle> = withContext(Dispatchers.IO) {
        val country = LocationHelper.getDetectedCountryCode(context)
        val lang = when (country) {
            "ID" -> "id"
            "MY" -> "ms"
            "JP" -> "ja"
            "KR" -> "ko"
            "TH" -> "th"
            "VN" -> "vi"
            else -> Locale.getDefault().language.ifBlank { "en" }
        }

        // Update live market tickers concurrently on Dispatchers.IO without blocking RSS headline fetching
        launch {
            runCatching { fetchLiveMarketTickers(country) }
        }

        val rssFeeds = mutableListOf<Triple<String, String, String>>() // url, defaultCategory, defaultSource
        if (country == "ID") {
            rssFeeds.add(
                Triple(
                    "https://news.google.com/rss?hl=id&gl=ID&ceid=ID:id",
                    "Berita Utama",
                    "Google News ID"
                )
            )
            rssFeeds.add(
                Triple(
                    "https://www.cnbcindonesia.com/news/rss",
                    "Bisnis & Market",
                    "CNBC Indonesia"
                )
            )
            rssFeeds.add(
                Triple(
                    "https://www.antaranews.com/rss/terkini.xml",
                    "Terkini",
                    "ANTARA News"
                )
            )
            rssFeeds.add(
                Triple(
                    "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=id&gl=ID&ceid=ID:id",
                    "Teknologi",
                    "Google News Tekno"
                )
            )
        } else {
            val hlParam = "$lang-$country"
            val ceidParam = "$country:$lang"
            rssFeeds.add(
                Triple(
                    "https://news.google.com/rss?hl=$hlParam&gl=$country&ceid=$ceidParam",
                    "Top News",
                    "Google News"
                )
            )
            rssFeeds.add(
                Triple(
                    "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=$hlParam&gl=$country&ceid=$ceidParam",
                    "Technology",
                    "Google News"
                )
            )
            rssFeeds.add(
                Triple(
                    "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=$hlParam&gl=$country&ceid=$ceidParam",
                    "Business",
                    "Google News"
                )
            )
        }

        val aggregated = mutableListOf<NewsBriefArticle>()
        for ((rssUrl, defaultCategory, defaultSource) in rssFeeds) {
            if (aggregated.size >= 5) break
            try {
                val request = Request.Builder()
                    .url(rssUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xmlString = response.body?.string()
                        if (!xmlString.isNullOrBlank()) {
                            val parsed = parseRssFeed(xmlString, defaultCategory, defaultSource, maxItems = 3)
                            for (item in parsed) {
                                if (aggregated.none { it.title.equals(item.title, ignoreCase = true) }) {
                                    aggregated.add(item)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching RSS from $rssUrl: ${e.message}")
            }
        }

        if (aggregated.isNotEmpty()) {
            _newsState.value = aggregated.take(5)
        } else if (_newsState.value.any { it.source.equals("VnExpress", ignoreCase = true) } && country != "VN") {
            _newsState.value = getDefaultArticles(context)
        }
        return@withContext _newsState.value
    }

    private fun fetchLiveMarketTickers(country: String) {
        val updatedTickers = mutableListOf<MarketTickerItem>()
        val numFormat = DecimalFormat("#,###")

        // 1. Fetch USD Exchange Rates (IDR / MYR / SGD / JPY / KRW)
        try {
            val fxReq = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .get()
                .build()
            httpClient.newCall(fxReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val rates = JSONObject(body).optJSONObject("rates")
                        if (rates != null) {
                            when (country) {
                                "ID" -> {
                                    val idr = rates.optDouble("IDR", 0.0)
                                    if (idr > 0) {
                                        updatedTickers.add(
                                            MarketTickerItem(
                                                symbol = "USD/IDR",
                                                value = "Rp ${numFormat.format(idr.toLong())}",
                                                changePercent = 0.08,
                                                unit = "IDR"
                                            )
                                        )
                                    }
                                }
                                "MY" -> {
                                    val myr = rates.optDouble("MYR", 0.0)
                                    if (myr > 0) {
                                        updatedTickers.add(
                                            MarketTickerItem(
                                                symbol = "USD/MYR",
                                                value = String.format(Locale.US, "RM %.2f", myr),
                                                changePercent = 0.05
                                            )
                                        )
                                    }
                                }
                                "SG" -> {
                                    val sgd = rates.optDouble("SGD", 0.0)
                                    if (sgd > 0) {
                                        updatedTickers.add(
                                            MarketTickerItem(
                                                symbol = "USD/SGD",
                                                value = String.format(Locale.US, "S$ %.2f", sgd),
                                                changePercent = 0.04
                                            )
                                        )
                                    }
                                }
                                else -> {
                                    val idr = rates.optDouble("IDR", 0.0)
                                    if (idr > 0) {
                                        updatedTickers.add(
                                            MarketTickerItem(
                                                symbol = "USD/IDR",
                                                value = numFormat.format(idr.toLong()),
                                                changePercent = 0.08,
                                                unit = "IDR"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FX rate fetch skipped: ${e.message}")
        }

        // 2. Fetch Live Crypto & Gold (PAXG) via CoinGecko public API
        try {
            val cgReq = Request.Builder()
                .url("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,pax-gold&vs_currencies=usd&include_24hr_change=true")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()
            httpClient.newCall(cgReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        json.optJSONObject("bitcoin")?.let { btc ->
                            val usd = btc.optDouble("usd", 0.0)
                            val chg = btc.optDouble("usd_24h_change", 0.0)
                            if (usd > 0) {
                                updatedTickers.add(
                                    MarketTickerItem(
                                        symbol = "BTC/USD",
                                        value = "$${numFormat.format(usd.toLong())}",
                                        changePercent = chg
                                    )
                                )
                            }
                        }
                        json.optJSONObject("pax-gold")?.let { gold ->
                            val usd = gold.optDouble("usd", 0.0)
                            val chg = gold.optDouble("usd_24h_change", 0.0)
                            if (usd > 0) {
                                updatedTickers.add(
                                    MarketTickerItem(
                                        symbol = if (country == "ID") "EMAS/USD" else "GOLD/USD",
                                        value = "$${numFormat.format(usd.toLong())}",
                                        changePercent = chg,
                                        unit = "oz"
                                    )
                                )
                            }
                        }
                        json.optJSONObject("ethereum")?.let { eth ->
                            val usd = eth.optDouble("usd", 0.0)
                            val chg = eth.optDouble("usd_24h_change", 0.0)
                            if (usd > 0) {
                                updatedTickers.add(
                                    MarketTickerItem(
                                        symbol = "ETH/USD",
                                        value = "$${numFormat.format(usd.toLong())}",
                                        changePercent = chg
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CoinGecko ticker fetch skipped: ${e.message}")
        }

        if (updatedTickers.isNotEmpty()) {
            _tickersState.value = updatedTickers
        }
    }

    private fun parseRssFeed(
        xml: String,
        defaultCategory: String,
        defaultSource: String,
        maxItems: Int = 4
    ): List<NewsBriefArticle> {
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
            var curSource = ""

            while (eventType != XmlPullParser.END_DOCUMENT && list.size < maxItems) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            curTitle = ""
                            curLink = ""
                            curDescription = ""
                            curPubDate = ""
                            curSource = ""
                        } else if (inItem) {
                            when {
                                tagName.equals("title", ignoreCase = true) -> curTitle = parser.nextText().trim()
                                tagName.equals("link", ignoreCase = true) -> curLink = parser.nextText().trim()
                                tagName.equals("description", ignoreCase = true) -> curDescription = parser.nextText().trim()
                                tagName.equals("pubDate", ignoreCase = true) -> curPubDate = parser.nextText().trim()
                                tagName.equals("source", ignoreCase = true) -> curSource = parser.nextText().trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = false
                            if (curTitle.isNotBlank() && curLink.isNotBlank()) {
                                // Google News RSS titles often end with " - PublisherName"
                                var cleanTitle = curTitle
                                var publisher = curSource.ifBlank { defaultSource }
                                val dashIdx = cleanTitle.lastIndexOf(" - ")
                                if (dashIdx > 15 && dashIdx < cleanTitle.length - 2) {
                                    val trailingSource = cleanTitle.substring(dashIdx + 3).trim()
                                    if (curSource.isBlank() && trailingSource.length in 2..35) {
                                        publisher = trailingSource
                                    }
                                    cleanTitle = cleanTitle.substring(0, dashIdx).trim()
                                }

                                val rawDesc = curDescription
                                    .replace(Regex("<.*?>"), " ")
                                    .replace("&nbsp;", " ")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .replace("&#39;", "'")
                                    .replace(Regex("\\s+"), " ")
                                    .trim()

                                val cleanDesc = if (rawDesc.isBlank() ||
                                    rawDesc.startsWith(cleanTitle.take(25), ignoreCase = true)
                                ) {
                                    "Liputan terkini dari $publisher · Kategori $defaultCategory"
                                } else {
                                    rawDesc.take(160)
                                }

                                val timeAgo = formatPubDate(curPubDate)

                                list.add(
                                    NewsBriefArticle(
                                        id = "art_${curLink.hashCode()}",
                                        title = cleanTitle,
                                        summary = cleanDesc,
                                        source = publisher,
                                        category = defaultCategory,
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
            "Live"
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
