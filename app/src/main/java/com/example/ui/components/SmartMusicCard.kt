package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.music.ActiveMediaPlayback
import com.example.core.music.MusicRecommendationHelper
import com.example.core.music.RecentMediaTracker
import com.example.core.music.YouTubeCategoryType
import com.example.core.music.YouTubeRecommendationHelper
import com.example.core.music.YouTubeVideoRecommendation
import kotlinx.coroutines.launch

enum class MediaPlatform {
    YOUTUBE,
    SPOTIFY
}

/**
 * Smart Multimedia Recommendation Card (YouTube & Spotify)
 * Supports:
 * 1. Detecting actual tracks recently played on the device (YouTube / Spotify / Zing MP3)
 * 2. Diverse categories: V-Pop & Artists, Pop/Ballad/Rap, Podcasts, Lofi/Acoustic
 * 3. Quick search and selection of favorite artists
 * 4. One-tap button to open the personal media app directly
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
        RecentMediaTracker.init(context)
        YouTubeRecommendationHelper.fetchLiveYouTubeVideos(context)
    }

    val recentPlayback by RecentMediaTracker.currentPlaybackState.collectAsStateWithLifecycle()

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

    val currentDisplayTitle = remember(selectedPlatform, spotifyRec, filteredVideos, activeTrackTitle, recentPlayback) {
        if (activeTrackTitle.isNotBlank()) {
            activeTrackTitle
        } else if (recentPlayback != null && recentPlayback!!.title.isNotBlank()) {
            recentPlayback!!.title
        } else if (selectedPlatform == MediaPlatform.SPOTIFY) {
            spotifyRec.title
        } else {
            filteredVideos.firstOrNull()?.title ?: "YouTube Media Feed"
        }
    }

    val currentDisplaySubtitle = remember(selectedPlatform, spotifyRec, filteredVideos, activeTrackSubtitle, recentPlayback) {
        if (activeTrackSubtitle.isNotBlank()) {
            activeTrackSubtitle
        } else if (recentPlayback != null && recentPlayback!!.title.isNotBlank()) {
            recentPlayback!!.displaySubtitle
        } else if (selectedPlatform == MediaPlatform.SPOTIFY) {
            spotifyRec.genreTag
        } else {
            filteredVideos.firstOrNull()?.channelTitle ?: "Live stream"
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
    val timeTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF757575)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, strokeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // -------------------------------------------------------------
            // [TOP ROW]: Platform Switcher + Open App Button
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
                                text = "YouTube",
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

                // Open Official App Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            if (selectedPlatform == MediaPlatform.YOUTUBE) {
                                YouTubeRecommendationHelper.openYouTubeApp(context)
                            } else {
                                MusicRecommendationHelper.openSpotifyApp(context)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (selectedPlatform == MediaPlatform.YOUTUBE) "Open YouTube" else "Open Spotify",
                        color = activeAccentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Open App",
                        tint = activeAccentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // [DEVICE RECENT PLAYBACK BANNER]: Actual track recently played on device
            // -------------------------------------------------------------
            if (recentPlayback != null && recentPlayback!!.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                        .border(
                            0.8.dp,
                            if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (recentPlayback!!.packageName.contains("spotify")) {
                                MusicRecommendationHelper.openMusicApp(context, "${recentPlayback!!.title} ${recentPlayback!!.artist}")
                            } else {
                                YouTubeRecommendationHelper.openSearchQuery(context, "${recentPlayback!!.title} ${recentPlayback!!.artist}")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Recently played: ${recentPlayback!!.title}",
                                color = titleColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = recentPlayback!!.displaySubtitle,
                                color = timeTextColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Replay",
                        tint = activeAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -------------------------------------------------------------
            // [CAPSULE MINI PLAYER]: One-tap Mini Player Bar
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

            Spacer(modifier = Modifier.height(12.dp))

            // -------------------------------------------------------------
            // [QUICK ARTIST CHIPS]: Trending artists & preference-based search
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val artistList = if (selectedPlatform == MediaPlatform.YOUTUBE) {
                    YouTubeRecommendationHelper.POPULAR_ARTISTS
                } else {
                    MusicRecommendationHelper.POPULAR_SPOTIFY_CHIPS
                }

                artistList.forEach { artist ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                            .clickable {
                                if (selectedPlatform == MediaPlatform.YOUTUBE) {
                                    YouTubeRecommendationHelper.openSearchQuery(context, "$artist Official")
                                } else {
                                    MusicRecommendationHelper.openMusicApp(context, artist)
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = activeAccentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = artist,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = cat.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) youtubeRed else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // List of Curated YouTube Videos
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isDark) Color(0xFF330000) else Color(0xFFFEE2E2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = youtubeRed,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

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
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = video.channelTitle,
                                                color = timeTextColor,
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.NorthEast,
                                            contentDescription = "Open video",
                                            tint = timeTextColor,
                                            modifier = Modifier.size(15.dp)
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF0A2614) else Color(0xFFE8F8EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Spotify Icon",
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(24.dp)
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = spotifyRec.subtitle,
                                    color = timeTextColor,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) spotifyGreen else Color(0xFFE8F8EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Listen on Spotify",
                                    tint = if (isDark) Color.White else spotifyGreen,
                                    modifier = Modifier.size(22.dp)
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
 * One UI Capsule Mini Player Bar with animated audio waveform
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
            initialValue = 6f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w3"
        )
        val w4 by infiniteTransition.animateFloat(
            initialValue = 14f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w4"
        )
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
            label = "rot"
        )
        listOf(w1, w2, w3, w4, rot)
    } else {
        listOf(6f, 10f, 8f, 5f, 0f)
    }

    val capsuleBg = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.70f)
    }

    val capsuleBorder = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color(0xFFE2E8F0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(capsuleBg)
            .border(BorderStroke(1.dp, capsuleBorder), RoundedCornerShape(20.dp))
            .clickable { onOpenAppClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        )
                        .graphicsLayer { rotationZ = vinylRotation },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (platform == MediaPlatform.YOUTUBE) Icons.Default.SmartDisplay else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(wave1.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(wave2.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(wave3.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(wave4.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPauseClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
