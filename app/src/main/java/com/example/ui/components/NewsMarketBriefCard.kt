package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.repository.MarketTickerItem
import com.example.core.repository.NewsBriefArticle
import com.example.core.repository.NewsMarketRepository
import kotlinx.coroutines.launch

/**
 * Thẻ Tin tức & Thị trường chuẩn ngôn ngữ thiết kế Samsung One UI 9.
 * Tích hợp chỉ số tài chính thời gian thực (Market Tickers) và tin tức mới nhất từ RSS báo chính thống.
 */
@Composable
fun NewsMarketBriefCard(
    modifier: Modifier = Modifier,
    tickers: List<MarketTickerItem> = remember { NewsMarketRepository.getMarketTickers() },
    isDark: Boolean = true,
    onArticleClick: ((NewsBriefArticle) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val liveArticles by NewsMarketRepository.newsState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        NewsMarketRepository.fetchLiveNews()
    }

    val containerBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val strokeColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }

    val titleColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val appNameColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF5F6368)
    val timeTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF757575)

    val iconBgColor = if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2)
    val iconTintColor = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, strokeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // -------------------------------------------------------------
            // [HEADER]: Squircle Icon 36dp + App Name + Live Indicator + Refresh
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = "News & Market",
                            tint = iconTintColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Tin tức & Thị trường",
                            color = appNameColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live Indicator Pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF065F46).copy(alpha = 0.4f) else Color(0xFFD1FAE5))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "Trực tiếp",
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isRefreshing) {
                                isRefreshing = true
                                coroutineScope.launch {
                                    NewsMarketRepository.fetchLiveNews()
                                    isRefreshing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới tin",
                            tint = appNameColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // [MARKET TICKERS]: Horizontal Scroll Chips
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tickers.forEach { ticker ->
                    val isGreen = ticker.isPositive
                    val badgeColor = if (isGreen) Color(0xFF10B981) else Color(0xFFEF4444)
                    val badgeBg = if (isDark) {
                        badgeColor.copy(alpha = 0.15f)
                    } else {
                        badgeColor.copy(alpha = 0.12f)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = ticker.symbol,
                                color = timeTextColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = ticker.value,
                                    color = titleColor,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isGreen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = ticker.formattedChange,
                                            color = badgeColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thin divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // [TOP ARTICLES LIST]: Live Curated RSS Articles
            // -------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                liveArticles.take(4).forEachIndexed { index, article ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (onArticleClick != null) {
                                    onArticleClick(article)
                                } else {
                                    NewsMarketRepository.openArticle(context, article.url)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Category & Metadata Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFDBEAFE))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = article.category,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "${article.source} • ${article.timeAgo}",
                                    color = timeTextColor,
                                    fontSize = 12.sp
                                )
                            }

                            // Article Title
                            Text(
                                text = article.title,
                                color = titleColor,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                        }

                        // Open External Icon
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = "Mở bài viết",
                            tint = timeTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (index < liveArticles.take(4).size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.6.dp)
                                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "News & Market Dark Preview")
@Composable
private fun NewsMarketBriefCardDarkPreview() {
    Surface(color = Color(0xFF0F172A)) {
        Box(modifier = Modifier.padding(16.dp)) {
            NewsMarketBriefCard(isDark = true)
        }
    }
}

@Preview(name = "News & Market Light Preview")
@Composable
private fun NewsMarketBriefCardLightPreview() {
    Surface(color = Color(0xFFF2F4F7)) {
        Box(modifier = Modifier.padding(16.dp)) {
            NewsMarketBriefCard(isDark = false)
        }
    }
}
