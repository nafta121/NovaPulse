package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
 * 3 Concentric Activity Rings Component (Samsung Health / Apple Fitness Style)
 * - Ring 1 (Outermost - Green #A3E635): Step count
 * - Ring 2 (Middle - Cyan #22D3EE): Active time (minutes)
 * - Ring 3 (Innermost - Purple #C084FC): Calories burned (kcal)
 *
 * Supports a Breathing Aura / soft glow effect when 100% of all 3 rings are completed (Goal Achieved).
 */
@Composable
fun ActivityRingsCanvas(
    stepsProgress: Float,
    activeMinutesProgress: Float,
    caloriesProgress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp
) {
    val isAllCompleted = stepsProgress >= 1f && activeMinutesProgress >= 1f && caloriesProgress >= 1f

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

    // Gentle breathing glow effect only when 100% of goals are completed
    val glowAlphaState = if (isAllCompleted) {
        val infiniteTransition = rememberInfiniteTransition(label = "ring_glow_transition")
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "goal_glow_alpha"
        )
    } else null

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

        // 0. When all 3 goals are completed: Draw a soft glowing aura
        val glowAlpha = glowAlphaState?.value ?: 0f
        if (isAllCompleted && glowAlpha > 0f) {
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = glowAlpha * 0.35f),
                radius = outerRadius + strokeWidth * 0.6f,
                center = center
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.20f),
                radius = middleRadius + strokeWidth * 0.4f,
                center = center
            )
        }

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

                // If this ring reaches 100%: Add a subtle glossy highlight border
                if (progress >= 1f && isAllCompleted) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = ringSize,
                        style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Draw 3 Concentric Rings from Outer to Inner
        drawRing(outerRadius, animatedSteps, greenColor)
        drawRing(middleRadius, animatedActive, cyanColor)
        drawRing(innerRadius, animatedCal, purpleColor)
    }
}
