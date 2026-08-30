package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 3 Concentric Activity Rings Component (3 Vòng Tròn Đồng Tâm Samsung Health / Apple Fitness Style)
 * - Vòng 1 (Ngoài cùng - Xanh lá #A3E635): Số bước chân
 * - Vòng 2 (Ở giữa - Xanh ngọc #22D3EE): Thời gian hoạt động (phút)
 * - Vòng 3 (Trong cùng - Tím #C084FC): Calo tiêu hao (kcal)
 */
@Composable
fun ActivityRingsCanvas(
    stepsProgress: Float,
    activeMinutesProgress: Float,
    caloriesProgress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp
) {
    val animatedSteps by animateFloatAsState(
        targetValue = stepsProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "steps_ring_anim"
    )
    val animatedActive by animateFloatAsState(
        targetValue = activeMinutesProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "active_ring_anim"
    )
    val animatedCal by animateFloatAsState(
        targetValue = caloriesProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "cal_ring_anim"
    )

    val greenColor = Color(0xFFA3E635) // Outer Green Steps
    val cyanColor = Color(0xFF22D3EE)  // Middle Cyan Active Mins
    val purpleColor = Color(0xFFC084FC) // Inner Purple Calories

    Canvas(modifier = modifier.size(sizeDp)) {
        val minDim = size.minDimension
        val strokeWidth = (minDim * 0.11f).coerceAtLeast(2.5f)
        val spacing = strokeWidth * 1.15f

        val outerRadius = (minDim - strokeWidth) / 2f
        val middleRadius = outerRadius - spacing
        val innerRadius = middleRadius - spacing

        // Helper function to draw track & progress arc
        fun drawRing(radius: Float, progress: Float, color: Color) {
            if (radius <= 0f) return
            val topLeft = Offset(center.x - radius, center.y - radius)
            val ringSize = Size(radius * 2f, radius * 2f)

            // 1. Background Track (20% Opacity)
            drawArc(
                color = color.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Glowing Active Progress Arc (Starts from top -90 deg)
            val sweepAngle = (progress * 360f).coerceIn(0f, 360f)
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = ringSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Draw 3 Concentric Rings from Outer to Inner
        drawRing(outerRadius, animatedSteps, greenColor)
        drawRing(middleRadius, animatedActive, cyanColor)
        drawRing(innerRadius, animatedCal, purpleColor)
    }
}
