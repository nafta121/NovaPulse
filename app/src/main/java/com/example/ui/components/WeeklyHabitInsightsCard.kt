package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.HabitCategoryStat
import com.example.core.engine.WeeklyHabitInsightsData

/**
 * Weekly Habit Insights Card following Samsung One UI 9 Glassmorphism design
 */
@Composable
fun WeeklyHabitInsightsCard(
    data: WeeklyHabitInsightsData = defaultHabitData(),
    isDark: Boolean = true,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // -------------------------------------------------------------
    // [COLORS & TINTING]
    // -------------------------------------------------------------
    val cardGradient = if (isDark) {
        listOf(
            Color(0xFF1E293B).copy(alpha = 0.25f),
            Color(0xFF0F172A).copy(alpha = 0.15f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.28f),
            Color(0xFFF8FAFC).copy(alpha = 0.16f)
        )
    }

    val cardBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val focusWindowBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.22f)
    val focusWindowBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.50f)

    val aiBoxBg = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.20f)
    val aiBoxBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.50f)
    val sparkleColor = Color(0xFF38BDF8)
    val aiTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(cardGradient),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // [1. HEADER ROW]: Title + "This week" + "Details >" Button
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Habit Report",
                        color = titleColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "This week",
                        color = subtitleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDetailClick?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Details",
                        color = subtitleColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Detail Arrow",
                        tint = subtitleColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // [2. HIGHLIGHT BANNER]: "Peak Focus"
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(focusWindowBg)
                    .border(BorderStroke(0.5.dp, focusWindowBorder), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF9F0A).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Focus Lightning",
                            tint = Color(0xFFFF9F0A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peak Focus: ${data.peakEnergyHoursLabel.ifBlank { "08:30 – 11:30" }}",
                            color = titleColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = data.peakEnergyDescription.ifBlank { "Highest work productivity window of the day." },
                            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6C707A),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // [3. CONCENTRIC RINGS CHART & LEGEND]: Optimized Layout (Size 114dp)
            // -------------------------------------------------------------
            ConcentricActivityChart(
                stats = data.habitStats,
                overallScore = data.overallEnergyScore,
                isDark = isDark
            )

            // -------------------------------------------------------------
            // [4. AI INSIGHT BOX]: Tinted Frosted Glass
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(aiBoxBg)
                    .border(BorderStroke(1.dp, aiBoxBorder), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Sparkle",
                        tint = sparkleColor,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )

                    Text(
                        text = data.aiWeeklySummary.ifBlank { "Your momentum is steady this week. Keep tracking your schedule with NowBrief." },
                        color = aiTextColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Optimized Concentric Rings Canvas Chart (Size 114dp, stroke 7dp, gap 3.5dp)
 * Leaves ample room for the text legend to display without truncation.
 */
@Composable
fun ConcentricActivityChart(
    stats: List<HabitCategoryStat>,
    overallScore: Int,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val titleColor = if (isDark) Color.White else Color(0xFF191C1E)
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF74777F)

    val ringColors = listOf(
        Color(0xFF5856D6), // Outer Ring: #5856D6
        Color(0xFF34C759), // Middle Ring: #34C759
        Color(0xFFFF9500)  // Inner Ring: #FF9500
    )

    var triggerAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        triggerAnimation = true
    }

    val animatedProgresses = stats.mapIndexed { index, stat ->
        val target = if (triggerAnimation) (stat.percentage.coerceIn(0, 100) / 100f) else 0f
        val anim by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            label = "RingProgress_$index"
        )
        anim
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Concentric Rings Canvas + Center Text Visual (Compact 114dp to leave room for text)
        Box(
            modifier = Modifier.size(114.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 7.dp.toPx()
                val gapPx = 3.5.dp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.minDimension / 2f) - (strokeWidthPx / 2f)

                val displayCount = stats.size.coerceAtMost(3)
                for (i in 0 until displayCount) {
                    val radius = maxRadius - i * (strokeWidthPx + gapPx)
                    if (radius <= 0f) break

                    val arcSize = Size(radius * 2f, radius * 2f)
                    val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                    val color = ringColors.getOrElse(i) { Color.Gray }
                    val progress = animatedProgresses.getOrElse(i) { 0f }

                    // Track Background (16% Alpha)
                    drawArc(
                        color = color.copy(alpha = 0.16f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Active Arc Sweep (-90° = 12 o'clock top position)
                    if (progress > 0f) {
                        drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Center Visual: Score + Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$overallScore%",
                    color = titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Energy",
                    color = subtitleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Clean Legend Column with Optimal Spacing (5dp between Dot & Text)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEachIndexed { index, stat ->
                val ringColor = ringColors.getOrElse(index % 3) { Color.Gray }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Squircle color pill (7x7.dp)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(ringColor)
                            )

                            Text(
                                text = stat.categoryName,
                                color = titleColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        Text(
                            text = "${stat.percentage}%",
                            color = titleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Actual metric detail line
                    if (stat.subDetail.isNotBlank()) {
                        Text(
                            text = stat.subDetail,
                            color = subtitleColor,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Default sample data for Preview & Demo
 */
private fun defaultHabitData(): WeeklyHabitInsightsData {
    return WeeklyHabitInsightsData(
        overallEnergyScore = 52,
        peakEnergyHoursLabel = "08:30 – 11:30",
        peakEnergyDescription = "Highest work productivity window of the day.",
        habitStats = listOf(
            HabitCategoryStat("Schedule Engagement", 60, 0xFF5856D6, "Completed 3/5 tasks"),
            HabitCategoryStat("Health Consistency", 40, 0xFF34C759, "4,200 / 6,000 steps"),
            HabitCategoryStat("Rest & Recovery", 55, 0xFFFF9500, "Reached 7.2h / 8h sleep")
        ),
        aiWeeklySummary = "Your momentum is steady this week. Keep tracking your schedule with NowBrief."
    )
}

@Preview(name = "Light Mode Glassmorphic Habit Card")
@Composable
private fun WeeklyHabitInsightsCardLightPreview() {
    Surface(color = Color(0xFFF2F4F7)) {
        Box(modifier = Modifier.padding(16.dp)) {
            WeeklyHabitInsightsCard(isDark = false)
        }
    }
}

@Preview(name = "Dark Mode Glassmorphic Habit Card")
@Composable
private fun WeeklyHabitInsightsCardDarkPreview() {
    Surface(color = Color(0xFF0F172A)) {
        Box(modifier = Modifier.padding(16.dp)) {
            WeeklyHabitInsightsCard(isDark = true)
        }
    }
}
