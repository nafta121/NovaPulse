package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

private val MASK_GRADIENT_COLORS = listOf(
    Color.Black,       // Alpha = 1.0 (Show text)
    Color.Transparent  // Alpha = 0.0 (Hide text)
)

/**
 * Custom Modifier for Jetpack Compose creating a "Text Alpha Mask Reveal / Wipe" effect.
 * Text gradually fades in along a 45-degree diagonal sweep from Top-Left to Bottom-Right.
 *
 * @param durationMillis Duration of one complete reveal cycle (Default: 5000ms = 5 seconds)
 * @param featherPx Soft edge feathering width along the sweep line (Default: 180f px, ~150px - 200px)
 * @param repeatMode Repeat mode: true -> infinite loop, false -> single sweep when opening the screen
 */
@Composable
fun Modifier.textAlphaMaskReveal(
    durationMillis: Int = 5000,
    featherPx: Float = 180f,
    repeatMode: Boolean = false
): Modifier {
    // Animation Progress from 0.0f (Fully hidden, Alpha=0) -> 1.0f (Fully revealed, Alpha=1)
    val progress: Float = if (repeatMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "TextAlphaMaskTransition")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "MaskRevealProgress"
        ).value
    } else {
        var animatedProgress by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            ) {
                animatedProgress = this.value
            }
        }
        animatedProgress
    }

    return if (progress >= 1f) {
        // Once the one-shot animation completes (progress = 1.0f), skip Offscreen Layer for optimal performance
        this
    } else {
        this
            // STEP 1: Use CompositingStrategy.Offscreen to render text onto an isolated offscreen buffer
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            // STEP 2: Multiply LinearGradient Alpha Mask onto the text using BlendMode.DstIn
            .drawWithContent {
                // Draw original content (Text, Column, etc.) onto the offscreen layer
                drawContent()

                val canvasWidth = size.width
                val canvasHeight = size.height

                if (canvasWidth > 0f && canvasHeight > 0f) {
                    val totalSpan = canvasWidth + canvasHeight + featherPx * 2f
                    val edgeStart = -featherPx + progress * totalSpan
                    val edgeEnd = edgeStart + featherPx

                    val startOffset = Offset(edgeStart / 2f, edgeStart / 2f)
                    val endOffset = Offset(edgeEnd / 2f, edgeEnd / 2f)

                    val maskBrush = Brush.linearGradient(
                        colors = MASK_GRADIENT_COLORS,
                        start = startOffset,
                        end = endOffset
                    )

                    drawRect(
                        brush = maskBrush,
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    }
}
