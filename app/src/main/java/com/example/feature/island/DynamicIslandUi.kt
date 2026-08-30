package com.example.feature.island

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.ai.BriefingContentProvider
import com.example.core.database.TaskEntity
import com.example.core.engine.NowNudgeEngine
import com.example.core.engine.NowNudgeItem
import com.example.feature.widget.BriefingSession
import com.example.feature.widget.WidgetThemeHelper
import com.example.feature.widget.cleanEmoji
import com.example.ui.components.ActivityRingsCanvas
import com.example.ui.components.WeatherStickerImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

@Composable
fun DynamicIslandView(
    energyScore: Int = 92,
    weatherTemp: String = "29°C",
    weatherCode: Int = 0,
    isBottom: Boolean = false,
    aiBriefingText: String = "",
    upcomingTasks: List<TaskEntity> = emptyList(),
    currentSteps: Int = 4320,
    stepGoal: Int = 6000,
    currentCalories: Int = 240,
    calorieGoal: Int = 500,
    customWidthDp: Int = 230,
    customHeightDp: Int = 42,
    opacity: Float = 0.65f,
    colorPreset: String = "DYNAMIC",
    isFrostedGlass: Boolean = true,
    onExpandStateChanged: (Boolean) -> Unit = {},
    collapseTrigger: Int = 0,
    onOpenApp: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(collapseTrigger) {
        if (collapseTrigger > 0 && isExpanded) {
            isExpanded = false
            onExpandStateChanged(false)
        }
    }

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = System.currentTimeMillis()
            delay(15_000) // Re-evaluate time theme every 15s
        }
    }

    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val isNight = hour >= 18 || hour < 6

    // Auto-collapse after 8 seconds
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            onExpandStateChanged(true)
            delay(8000)
            isExpanded = false
        } else {
            onExpandStateChanged(false)
        }
    }

    // Time-of-day dynamic theme identical to NowBriefWidget
    val timeTheme = remember(hour, minute) {
        WidgetThemeHelper.getThemeForCurrentTime(hour, minute)
    }
    val currentSession = remember(hour, minute) {
        BriefingSession.fromHourAndMinute(hour, minute)
    }
    val activeGreeting = remember(currentSession) {
        currentSession.getRandomGreeting()
    }
    val islandTitle = activeGreeting.title
    val islandSubtitle = activeGreeting.subtitle

    val tempInt = remember(weatherTemp) {
        weatherTemp.filter { it.isDigit() }.toIntOrNull() ?: 29
    }

    // Gradient colors exactly as in NowBriefWidget.kt
    val rawGradientColors = remember(timeTheme, colorPreset) {
        when (colorPreset) {
            "DARK_GLASS" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            "LIGHT_GLASS" -> listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
            "MATERIAL_YOU" -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF60A5FA))
            else -> timeTheme.gradientColors
        }
    }

    val effectiveOpacity = opacity.coerceIn(0.05f, 1.0f)
    val dynamicColors = remember(rawGradientColors, effectiveOpacity) {
        rawGradientColors.map { it.copy(alpha = effectiveOpacity) }
    }

    val isLightGlass = colorPreset == "LIGHT_GLASS"
    val titleColor = if (isLightGlass) Color(0xFF0F172A) else Color.White
    val subtitleColor = if (isLightGlass) Color(0xFF475569) else Color.White.copy(alpha = 0.88f)

    val gradientBrush = remember(rawGradientColors, dynamicColors, effectiveOpacity, isExpanded) {
        if (isExpanded) {
            val baseColor = if (isLightGlass) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            Brush.verticalGradient(
                listOf(
                    baseColor.copy(alpha = 0.96f),
                    rawGradientColors.first().copy(alpha = 0.94f),
                    rawGradientColors.last().copy(alpha = 0.97f)
                )
            )
        } else {
            Brush.horizontalGradient(dynamicColors)
        }
    }

    val borderBrush = remember(isLightGlass, effectiveOpacity, isExpanded) {
        if (isExpanded) {
            Brush.verticalGradient(
                if (isLightGlass) {
                    listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.15f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.32f)
                    )
                }
            )
        } else {
            Brush.horizontalGradient(
                if (isLightGlass) {
                    listOf(
                        Color.Black.copy(alpha = 0.15f * effectiveOpacity),
                        Color.Black.copy(alpha = 0.05f * effectiveOpacity),
                        Color.Black.copy(alpha = 0.12f * effectiveOpacity)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.35f * effectiveOpacity),
                        Color.White.copy(alpha = 0.12f * effectiveOpacity),
                        Color.White.copy(alpha = 0.30f * effectiveOpacity)
                    )
                }
            )
        }
    }

    // Dedicated compact briefing text for Dynamic Island Pill
    val fullBriefingText = remember(upcomingTasks, currentSession, tempInt) {
        when {
            upcomingTasks.isNotEmpty() -> upcomingTasks.first().title
            else -> BriefingContentProvider.getIslandSummary(currentSession, tempInt, "mát mẻ")
        }.cleanEmoji()
    }

    // Machine Learning Context-Aware Weighted Card Selection Engine (Pre-computed)
    var mlCardIndex by remember {
        mutableIntStateOf(selectMlCardIndex(hour, tempInt, weatherCode, upcomingTasks.size))
    }

    // Dynamic Content Height: Auto-adjusts perfectly to each card's content footprint
    val targetExpandedHeight = remember(mlCardIndex) {
        when (mlCardIndex) {
            0 -> 192.dp // Weather Advice (OneUI padded)
            1 -> 178.dp // Now Nudge (OneUI padded)
            2 -> 238.dp // Activity Energy Rings Canvas + stats + 7 days
            3 -> 152.dp // One UI Calendar Task
            4 -> 188.dp // Weekly Habit Insights
            5 -> 148.dp // Sleep & Rest Status
            else -> 178.dp
        }
    }

    // Premium Samsung One UI 7 Fluid Spring Motion Physics
    val targetPillWidth = customWidthDp.coerceIn(160, 420).dp
    val targetPillHeight = customHeightDp.coerceIn(28, 64).dp

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val expandedTargetWidth = remember(screenWidthDp, targetPillWidth) {
        maxOf(screenWidthDp - 14.dp, targetPillWidth)
    }

    val targetWidth = if (isExpanded) expandedTargetWidth else targetPillWidth
    val animatedWidth = targetWidth
    val animatedHeight = if (isExpanded) targetExpandedHeight else targetPillHeight
    val animatedCornerRadius = if (isExpanded) 26.dp else (targetPillHeight / 2)
    val compactAlpha = if (isExpanded) 0f else 1f
    val expandedAlpha = if (isExpanded) 1f else 0f
    val expandedScale = 1.0f
    val animatedElevation = if (isExpanded) 10.dp else 2.dp

    val shape = RoundedCornerShape(animatedCornerRadius)

    val nudgeItem = remember(hour, upcomingTasks.size, currentSteps) {
        NowNudgeEngine.generateNudge(hour, upcomingTasks.size, currentSteps, 800)
    }

    // Local interactive states for feature cards
    var waterDrunkMl by remember { mutableIntStateOf(1250) }

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .graphicsLayer {
                this.shadowElevation = animatedElevation.toPx()
                this.shape = shape
                this.clip = true
            }
            .background(brush = gradientBrush)
            .then(
                if (isFrostedGlass && !isExpanded) {
                    Modifier.border(
                        width = 1.dp,
                        brush = borderBrush,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isExpanded) {
                    mlCardIndex = selectMlCardIndex(hour, tempInt, weatherCode, upcomingTasks.size)
                    onExpandStateChanged(true)
                    isExpanded = true
                } else {
                    onExpandStateChanged(false)
                    isExpanded = false
                }
            },
        contentAlignment = if (isExpanded) Alignment.TopCenter else if (isBottom) Alignment.BottomCenter else Alignment.TopCenter
    ) {
        // 1. COMPACT PILL MODE - Pinned at exact resting position
        if (compactAlpha > 0.01f) {
            val iconBoxSize = (customHeightDp * 0.76f).coerceIn(20f, 44f).dp
            val iconSize = (customHeightDp * 0.58f).coerceIn(15f, 34f).dp
            val titleFontSize = (customHeightDp * 0.30f).coerceIn(10f, 15f).sp
            val subtitleFontSize = (customHeightDp * 0.24f).coerceIn(8.5f, 12.5f).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetPillHeight)
                    .graphicsLayer { alpha = compactAlpha }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(timeTheme.iconGradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_capsule_horizon),
                        contentDescription = "NowBrief Logo",
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = islandTitle,
                        color = titleColor,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fullBriefingText,
                        color = subtitleColor,
                        fontSize = subtitleFontSize,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // 2. EXPANDED MODE - Authentic Samsung One UI 7/8.5/9 Card Designs
        if (expandedAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer {
                        alpha = expandedAlpha
                        scaleX = expandedScale
                        scaleY = expandedScale
                        transformOrigin = TransformOrigin(0.5f, if (isBottom) 1.0f else 0.0f)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // One UI 7/9 Header: Squircle Logo + Session + Weather Badge + Close Glass Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // OneUI Squircle App Icon (34dp, RoundedCornerShape 11dp)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .border(
                                    0.8.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.40f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    ),
                                    RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_capsule_horizon),
                                contentDescription = "NowBrief Logo",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "NowBrief",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = islandTitle,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // OneUI Weather Pill Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(
                                    0.8.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            WeatherStickerImage(
                                weatherCode = weatherCode,
                                isNight = isNight,
                                size = 20.dp,
                                animate = true
                            )
                            Text(
                                text = weatherTemp,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // OneUI Glass Close Button (32dp)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(
                                    0.8.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.38f),
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .clickable { isExpanded = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Inner Cards Floating on Frosted Glass Background
                when (mlCardIndex) {
                    0 -> {
                        // 1. Dynamic Weather Advice (One UI 9 Frosted Card)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
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
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFFF59E0B),
                                                        Color(0xFFFBBF24)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Khuyến nghị thời tiết",
                                        color = titleColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = if (isLightGlass) 0.5f else 0.14f))
                                        .border(0.7.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (tempInt >= 30) "Nắng nóng" else if (isNight) "Đêm mát" else "Dễ chịu",
                                        color = if (tempInt >= 30) Color(0xFFF59E0B) else if (isNight) Color(0xFF818CF8) else Color(
                                            0xFF10B981
                                        ),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (tempInt >= 30) "Nhiệt độ ngoài trời $weatherTemp oi bức. Hãy bù nước thường xuyên và hạn chế nắng gắt."
                                    else if (isNight) "Nhiệt độ $weatherTemp dịu mát. Không khí trong lành, thích hợp để nghỉ ngơi phục hồi."
                                    else "Nhiệt độ $weatherTemp mát mẻ. Thời tiết thuận lợi cho các hoạt động và làm việc hiệu quả.",
                                    color = subtitleColor,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                WeatherStickerImage(
                                    weatherCode = weatherCode,
                                    isNight = isNight,
                                    size = 40.dp,
                                    animate = true
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pillBg = Color.White.copy(alpha = if (isLightGlass) 0.48f else 0.16f)
                                val pillBorder = Color.White.copy(alpha = if (isLightGlass) 0.6f else 0.25f)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .border(0.7.dp, pillBorder, RoundedCornerShape(8.dp))
                                        .clickable { waterDrunkMl = (waterDrunkMl + 250).coerceAtMost(3000) }
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "💧 Uống nước",
                                        color = titleColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .border(0.7.dp, pillBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🥤 Bổ sung khoáng",
                                        color = titleColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(pillBg)
                                        .border(0.7.dp, pillBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🌿 Thư giãn",
                                        color = titleColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // 2. Now Nudge Smart Recommendation (One UI 9 Frosted Card)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF8B5CF6),
                                                        Color(0xFFEC4899)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Now Nudge • Gợi ý AI",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF8B5CF6).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Chủ động",
                                        color = Color(0xFFC084FC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Bứt phá mục tiêu hôm nay",
                                color = titleColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Theo dõi tiến độ lịch trình hoặc mở nhanh danh sách việc quan trọng cần hoàn thành.",
                                color = subtitleColor,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (isLightGlass) 0.5f else 0.16f))
                                    .border(0.7.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .clickable { onOpenApp() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▶ Xem lịch trình chi tiết",
                                    color = titleColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    2 -> {
                        // 3. Activity Energy Samsung Health (One UI 9 Frosted Card)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF10B981),
                                                        Color(0xFF06B6D4)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Hoạt động thể chất",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF10B981).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Samsung Health",
                                        color = Color(0xFF86EFAC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ActivityRingsCanvas(
                                    stepsProgress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f),
                                    activeMinutesProgress = 0.45f,
                                    caloriesProgress = (currentCalories.toFloat() / calorieGoal).coerceIn(0f, 1f),
                                    sizeDp = 52.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDCFCE7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsWalk,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentSteps / $stepGoal bước",
                                            color = titleColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFCFFAFE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFF0891B2),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "35 / 60 phút",
                                            color = titleColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF3E8FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color(0xFF9333EA),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentCalories / $calorieGoal kcal",
                                            color = titleColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { idx, day ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day,
                                            color = if (idx == 6) Color(0xFFEF4444) else subtitleColor,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (idx <= 4) Color(0xFFA855F7) else Color.White.copy(alpha = 0.20f))
                                                .border(
                                                    0.8.dp,
                                                    if (idx <= 4) Color(0xFFA855F7) else Color.White.copy(alpha = 0.35f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // 4. One UI Calendar Task (Samsung Reminder Frosted Card)
                        val topTask = upcomingTasks.firstOrNull()
                        val taskTimeStr = remember(topTask?.datetime) {
                            if (topTask != null && topTask.datetime > 0) {
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(topTask.datetime))
                            } else "22:00"
                        }
                        val taskApp = topTask?.sourceApp?.ifBlank { null } ?: "Nhắc nhở công việc"

                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF38BDF8),
                                                        Color(0xFF0284C7)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = taskApp,
                                        color = subtitleColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Sắp diễn ra",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val taskTitle = topTask?.title ?: "Vào học bài mới trên Duolingo 💚"
                                val taskPred = remember(taskTitle) {
                                    com.example.core.ml.TaskProcrastinationPredictor.predict(taskTitle)
                                }

                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (taskPred.urgency == com.example.core.ml.TaskUrgency.CRITICAL) Color(0xFFEF4444)
                                            else Color(taskPred.category.colorHex)
                                        )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${taskPred.category.emoji} $taskTitle",
                                            color = titleColor,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        Text(
                                            text = if (taskPred.urgency == com.example.core.ml.TaskUrgency.CRITICAL) "${taskPred.urgency.tagLabel} • ${taskPred.durationLabel}"
                                            else "${taskPred.durationLabel} • ${taskPred.risk.tagLabel}",
                                            color = if (taskPred.urgency == com.example.core.ml.TaskUrgency.CRITICAL) Color(0xFFEF4444) else Color(taskPred.risk.colorHex),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = subtitleColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "$taskTimeStr • ${taskPred.microActionStep}",
                                            color = subtitleColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // 5. Energy & Habit Insights (One UI 9 Frosted Card)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFFF59E0B),
                                                        Color(0xFFEF4444)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Hiệu suất sinh học",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = if (isLightGlass) 0.45f else 0.14f))
                                        .border(0.6.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable { onOpenApp() }
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Chi tiết >",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Metric 1: Năng lượng AI
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ Năng lượng AI",
                                            color = subtitleColor,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "85%",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { 0.85f },
                                        modifier = Modifier.fillMaxWidth().height(4.5.dp).clip(CircleShape),
                                        color = Color(0xFF10B981),
                                        trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                                    )
                                }

                                // Metric 2: Tập trung
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Tập trung làm việc",
                                            color = subtitleColor,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "72%",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { 0.72f },
                                        modifier = Modifier.fillMaxWidth().height(4.5.dp).clip(CircleShape),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                                    )
                                }

                                // Metric 3: Phục hồi
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌿 Thư giãn & Bù nước",
                                            color = subtitleColor,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "90%",
                                            color = Color(0xFFA855F7),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { 0.90f },
                                        modifier = Modifier.fillMaxWidth().height(4.5.dp).clip(CircleShape),
                                        color = Color(0xFFA855F7),
                                        trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // 6. Sleep & Rest Status (One UI 9 Frosted Card)
                        IslandInnerCardContainer(isLightGlass = isLightGlass) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF7C3AED),
                                                        Color(0xFFDB2777)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NightlightRound,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Giấc ngủ & Phục hồi",
                                        color = titleColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF7C3AED).copy(alpha = 0.40f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Ổn định",
                                        color = Color(0xFFC084FC),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Thời gian ngủ: 6h 30m",
                                    color = titleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mục tiêu 8h (81%)",
                                    color = subtitleColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { 0.81f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = Color(0xFFA855F7),
                                trackColor = Color.White.copy(alpha = if (isLightGlass) 0.25f else 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun selectMlCardIndex(hour: Int, tempInt: Int, weatherCode: Int, tasksCount: Int): Int {
    val weights = mutableMapOf<Int, Int>()
    if (hour in 5..10) {
        weights[1] = 35 // Now Nudge Kế hoạch sáng
        weights[5] = 30 // Giấc ngủ đêm qua & phục hồi
        weights[4] = 20 // Báo cáo thói quen khung giờ vàng
        weights[0] = 15 // Thời tiết sáng
    } else if (hour in 11..16) {
        weights[0] = if (tempInt >= 30 || weatherCode in 51..99) 40 else 25 // Cảnh báo nắng nóng / mưa
        weights[2] = 30 // Hoạt động thể chất / Bước chân / Calo
        weights[4] = 25 // Báo cáo năng lượng
        weights[1] = 20 // Kế hoạch chiều
    } else {
        weights[3] = if (tasksCount > 0) 40 else 20 // Lịch trình sắp tới
        weights[5] = 30 // Giấc ngủ & phục hồi đêm
        weights[4] = 25 // Báo cáo thói quen
        weights[0] = 15 // Thời tiết đêm
    }
    val pool = mutableListOf<Int>()
    for (i in 0..5) {
        val w = weights[i] ?: 15
        repeat(w) { pool.add(i) }
    }
    return pool.random()
}

@Composable
private fun IslandInnerCardContainer(
    isLightGlass: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val cardBg = if (isLightGlass) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    val cardBorder = if (isLightGlass) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(0.8.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

