package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.music.MusicRecommendationHelper
import com.example.core.music.YouTubeCategoryType
import com.example.core.music.YouTubeRecommendationHelper
import com.example.core.music.YouTubeVideoRecommendation
import kotlinx.coroutines.launch

enum class MediaPlatform {
    YOUTUBE,
    SPOTIFY
}

/**
 * Thẻ Đề xuất Đa phương tiện Thông minh (YouTube & Spotify)
 * Được kết nối trực tiếp với YouTube Channel Feeds, Thanh Capsule Mini Player & Mô hình Máy học On-Device.
 */
@Composable
fun SmartMusicCard(
    currentHour: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPlatform by rememberSaveable {
        mutableStateOf(
            if (com.example.core.ml.InternalMlEngine.getInstance(context).mediaLearner.getPreferredPlatform() == "SPOTIFY") {
                MediaPlatform.SPOTIFY
            } else {
                MediaPlatform.YOUTUBE
            }
        )
    }
    var selectedCategory by rememberSaveable { mutableStateOf(YouTubeCategoryType.ALL) }
    var isYtRefreshing by remember { mutableStateOf(false) }

    // Media Player Capsule States
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var activeTrackTitle by rememberSaveable { mutableStateOf("") }
    var activeTrackSubtitle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        YouTubeRecommendationHelper.fetchLiveYouTubeVideos(context)
    }

    val spotifyRec = remember(currentHour) {
        MusicRecommendationHelper.getRecommendationForSession(currentHour)
    }

    val allLiveYtVideos by YouTubeRecommendationHelper.liveVideosState.collectAsStateWithLifecycle()

    val filteredVideos = remember(selectedCategory, allLiveYtVideos) {
        if (selectedCategory == YouTubeCategoryType.ALL) {
            allLiveYtVideos
        } else {
            allLiveYtVideos.filter { it.categoryType == selectedCategory }
        }
    }

    val currentDisplayTitle = remember(selectedPlatform, spotifyRec, filteredVideos, activeTrackTitle) {
        if (activeTrackTitle.isNotBlank()) {
            activeTrackTitle
        } else if (selectedPlatform == MediaPlatform.SPOTIFY) {
            spotifyRec.title
        } else {
            filteredVideos.firstOrNull()?.title ?: "YouTube Media Feed"
        }
    }

    val currentDisplaySubtitle = remember(selectedPlatform, spotifyRec, filteredVideos, activeTrackSubtitle) {
        if (activeTrackSubtitle.isNotBlank()) {
            activeTrackSubtitle
        } else if (selectedPlatform == MediaPlatform.SPOTIFY) {
            spotifyRec.genreTag
        } else {
            filteredVideos.firstOrNull()?.channelTitle ?: "Phát trực tiếp"
        }
    }

    val youtubeRed = Color(0xFFFF0000)
    val spotifyGreen = Color(0xFF1DB954)

    val activeAccentColor = if (selectedPlatform == MediaPlatform.YOUTUBE) youtubeRed else spotifyGreen

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

    Card(
        modifier = modifier
            .fillMaxWidth()
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
            // [TOP SEGMENTED SWITCHER]: YouTube vs Spotify Toggle + Refresh
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Switcher Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // YouTube Tab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedPlatform == MediaPlatform.YOUTUBE) youtubeRed else Color.Transparent
                            )
                            .clickable {
                                selectedPlatform = MediaPlatform.YOUTUBE
                                activeTrackTitle = ""
                                activeTrackSubtitle = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartDisplay,
                                contentDescription = null,
                                tint = if (selectedPlatform == MediaPlatform.YOUTUBE) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "YouTube Live",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedPlatform == MediaPlatform.YOUTUBE) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedPlatform == MediaPlatform.YOUTUBE) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }

                    // Spotify Tab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedPlatform == MediaPlatform.SPOTIFY) spotifyGreen else Color.Transparent
                            )
                            .clickable {
                                selectedPlatform = MediaPlatform.SPOTIFY
                                activeTrackTitle = ""
                                activeTrackSubtitle = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (selectedPlatform == MediaPlatform.SPOTIFY) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Spotify",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedPlatform == MediaPlatform.SPOTIFY) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedPlatform == MediaPlatform.SPOTIFY) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                        }
                    }
                }

                // AI Engine Tag & Refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedPlatform == MediaPlatform.YOUTUBE) {
                        IconButton(
                            onClick = {
                                if (!isYtRefreshing) {
                                    isYtRefreshing = true
                                    coroutineScope.launch {
                                        YouTubeRecommendationHelper.fetchLiveYouTubeVideos()
                                        isYtRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới YouTube",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Live Feed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // [CAPSULE MINI PLAYER]: Thanh Mini Player 1 chạm + Sóng nhạc động
            // -------------------------------------------------------------
            MediaCapsuleMiniPlayer(
                title = currentDisplayTitle,
                subtitle = currentDisplaySubtitle,
                platform = selectedPlatform,
                isPlaying = isPlaying,
                isDark = isDark,
                onPlayPauseClick = {
                    isPlaying = !isPlaying
                    if (isPlaying) {
                        try {
                            com.example.core.ml.InternalMlEngine.getInstance(context)
                                .recordMediaClick(
                                    if (selectedPlatform == MediaPlatform.SPOTIFY) "SPOTIFY_PLAYLIST" else "YOUTUBE_VIDEO",
                                    selectedPlatform.name
                                )
                        } catch (e: Exception) {}
                        if (selectedPlatform == MediaPlatform.SPOTIFY) {
                            MusicRecommendationHelper.openMusicApp(context, spotifyRec.query, spotifyRec.spotifyUri)
                        } else {
                            val firstVideo = filteredVideos.firstOrNull()
                            if (firstVideo != null) {
                                YouTubeRecommendationHelper.openYouTubeVideo(context, firstVideo)
                            }
                        }
                    }
                },
                onOpenAppClick = {
                    if (selectedPlatform == MediaPlatform.SPOTIFY) {
                        MusicRecommendationHelper.openMusicApp(context, spotifyRec.query, spotifyRec.spotifyUri)
                    } else {
                        val firstVideo = filteredVideos.firstOrNull()
                        if (firstVideo != null) {
                            YouTubeRecommendationHelper.openYouTubeVideo(context, firstVideo)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // [MAIN CONTENT]: YouTube List vs Spotify View
            // -------------------------------------------------------------
            AnimatedContent(
                targetState = selectedPlatform,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "platform_content_anim"
            ) { platform ->
                when (platform) {
                    MediaPlatform.YOUTUBE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Category Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                YouTubeCategoryType.values().forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) {
                                                    if (isDark) youtubeRed.copy(alpha = 0.25f) else Color(0xFFFEE2E2)
                                                } else {
                                                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) youtubeRed.copy(alpha = 0.6f) else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedCategory = cat }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = cat.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) youtubeRed else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // List of Curated YouTube Videos
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredVideos.take(4).forEachIndexed { index, video ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                activeTrackTitle = video.title
                                                activeTrackSubtitle = "${video.categoryTag} • ${video.channelTitle}"
                                                isPlaying = true
                                                YouTubeRecommendationHelper.openYouTubeVideo(context, video)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Left Squircle Icon with Duration
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isDark) Color(0xFF330000) else Color(0xFFFEE2E2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = youtubeRed,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Center Video Details
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = video.categoryTag,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = youtubeRed
                                                )
                                                Text(
                                                    text = "• ${video.durationText}",
                                                    fontSize = 11.sp,
                                                    color = timeTextColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Text(
                                                text = video.title,
                                                color = titleColor,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = video.channelTitle,
                                                    color = timeTextColor,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }

                                        // Right Direct Play Icon
                                        Icon(
                                            imageVector = Icons.Default.NorthEast,
                                            contentDescription = "Mở video",
                                            tint = timeTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (index < filteredVideos.take(4).size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.6.dp)
                                                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    MediaPlatform.SPOTIFY -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeTrackTitle = spotifyRec.title
                                    activeTrackSubtitle = spotifyRec.genreTag
                                    isPlaying = true
                                    try {
                                        com.example.core.ml.InternalMlEngine.getInstance(context).recordMediaClick("SPOTIFY_PLAYLIST", "SPOTIFY")
                                    } catch (e: Exception) {}
                                    MusicRecommendationHelper.openMusicApp(context, spotifyRec.query, spotifyRec.spotifyUri)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF0A2614) else Color(0xFFE8F8EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Spotify Icon",
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = spotifyRec.genreTag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = spotifyGreen
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = spotifyRec.title,
                                    color = titleColor,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = spotifyRec.subtitle,
                                    color = timeTextColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) spotifyGreen else Color(0xFFE8F8EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Nghe nhạc Spotify",
                                    tint = if (isDark) Color.White else spotifyGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thanh Mini Player dạng Capsule chuẩn One UI với thanh sóng nhạc động (Audio Waveform)
 * và nút bấm Play/Pause 1 chạm tương tác trực tiếp.
 */
@Composable
fun MediaCapsuleMiniPlayer(
    title: String,
    subtitle: String,
    platform: MediaPlatform,
    isPlaying: Boolean,
    isDark: Boolean,
    onPlayPauseClick: () -> Unit,
    onOpenAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val youtubeRed = Color(0xFFFF0000)
    val spotifyGreen = Color(0xFF1DB954)
    val accentColor = if (platform == MediaPlatform.YOUTUBE) youtubeRed else spotifyGreen

    // Dynamic wave animation for audio waveform only when playing
    val (wave1, wave2, wave3, wave4, vinylRotation) = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "capsule_wave_anim")
        val w1 by infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = 18f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w1"
        )
        val w2 by infiniteTransition.animateFloat(
            initialValue = 16f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w2"
        )
        val w3 by infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w3"
        )
        val w4 by infiniteTransition.animateFloat(
            initialValue = 14f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w4"
        )
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
            label = "vinyl_rot"
        )
        arrayOf(w1, w2, w3, w4, rot)
    } else {
        arrayOf(4f, 4f, 4f, 4f, 0f)
    }

    val playBtnInteractionSource = remember { MutableInteractionSource() }
    val isPlayBtnPressed by playBtnInteractionSource.collectIsPressedAsState()
    val playBtnScale by animateFloatAsState(
        targetValue = if (isPlayBtnPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "play_btn_scale"
    )

    // Capsule Glass Container
    val capsuleBg = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                accentColor.copy(alpha = if (isPlaying) 0.22f else 0.12f),
                Color(0xFF1E293B).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                accentColor.copy(alpha = if (isPlaying) 0.14f else 0.07f),
                Color.White.copy(alpha = 0.85f)
            )
        )
    }

    val capsuleBorder = if (isPlaying) {
        accentColor.copy(alpha = if (isDark) 0.45f else 0.35f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE2E8F0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(capsuleBg)
            .border(1.dp, capsuleBorder, RoundedCornerShape(22.dp))
            .clickable { onOpenAppClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // [LEFT]: Rotating Vinyl / Squircle App Icon
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            if (isPlaying) rotationZ = vinylRotation
                        }
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .border(1.dp, accentColor.copy(alpha = if (isPlaying) 0.8f else 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (platform == MediaPlatform.YOUTUBE) Icons.Default.SmartDisplay else Icons.Default.MusicNote,
                        contentDescription = "Platform Icon",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // [CENTER]: Track Title + Dynamic Waveform Bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title.ifBlank { if (platform == MediaPlatform.YOUTUBE) "YouTube Live Media" else "Spotify Daily Mix" },
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = subtitle.ifBlank { if (platform == MediaPlatform.YOUTUBE) "Đang phát • YouTube" else "Đang phát • Spotify" },
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // 4-Bar Animated Equalizer / Soundwave
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            modifier = Modifier.height(18.dp)
                        ) {
                            val h1 = if (isPlaying) wave1.dp else 4.dp
                            val h2 = if (isPlaying) wave2.dp else 4.dp
                            val h3 = if (isPlaying) wave3.dp else 4.dp
                            val h4 = if (isPlaying) wave4.dp else 4.dp

                            Box(modifier = Modifier.width(3.dp).height(h1).clip(CircleShape).background(accentColor))
                            Box(modifier = Modifier.width(3.dp).height(h2).clip(CircleShape).background(accentColor))
                            Box(modifier = Modifier.width(3.dp).height(h3).clip(CircleShape).background(accentColor))
                            Box(modifier = Modifier.width(3.dp).height(h4).clip(CircleShape).background(accentColor))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // [RIGHT]: Quick Play/Pause Action Button (1-Tap Operation)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = playBtnScale
                        scaleY = playBtnScale
                    }
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) accentColor else accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                    )
                    .clickable(
                        interactionSource = playBtnInteractionSource,
                        indication = ripple(bounded = true)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPauseClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Tạm dừng" else "Phát nhanh",
                    tint = if (isPlaying) Color.White else accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

