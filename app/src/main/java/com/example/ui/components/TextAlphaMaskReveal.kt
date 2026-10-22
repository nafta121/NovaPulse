package com.example.ui.components

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.TextView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Modifier for Jetpack Compose creating a "Text Alpha Mask Reveal / Wipe" effect.
 * Text gradually fades in along a 45-degree diagonal sweep from Top-Left to Bottom-Right.
 *
 * @param durationMillis Duration of one complete reveal cycle (Default: 5000ms = 5 seconds)
 * @param featherPx Soft edge feathering width along the sweep line (Default: 180f px, ~150px - 200px)
 * @param repeatMode Repeat mode: true -> infinite loop, false -> single sweep when opening the screen
 */
fun Modifier.textAlphaMaskReveal(
    durationMillis: Int = 5000,
    featherPx: Float = 180f,
    repeatMode: Boolean = false
): Modifier = composed {
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

    if (progress >= 1f) {
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
                        colors = listOf(
                            Color.Black,       // Alpha = 1.0 (Show text)
                            Color.Transparent  // Alpha = 0.0 (Hide text)
                        ),
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

/**
 * Custom TextView for Android View / XML Layout performing the Text Alpha Mask Reveal / Wipe effect.
 * Uses android.graphics.LinearGradient Shader combined with ValueAnimator (5000ms).
 */
open class TextAlphaMaskRevealTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private var maskDurationMillis: Long = 5000L
    private var featherPx: Float = 180f
    private var animator: android.animation.ValueAnimator? = null
    private var progress: Float = 0f

    init {
        startMaskRevealAnimation()
    }

    /**
     * Starts the text sweep animation from Top-Left to Bottom-Right
     */
    fun startMaskRevealAnimation(
        durationMs: Long = 5000L,
        feather: Float = 180f,
        repeatModeOption: Int = android.animation.ValueAnimator.INFINITE
    ) {
        this.maskDurationMillis = durationMs
        this.featherPx = feather

        animator?.cancel()
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = maskDurationMillis
            interpolator = android.view.animation.LinearInterpolator()
            repeatCount = repeatModeOption
            repeatMode = android.animation.ValueAnimator.RESTART

            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                updateShader()
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    private fun updateShader() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 45° diagonal sweep line
        val totalSpan = w + h + featherPx * 2f
        val edgeStart = -featherPx + progress * totalSpan
        val edgeEnd = edgeStart + featherPx

        val x0 = edgeStart / 2f
        val y0 = edgeStart / 2f
        val x1 = edgeEnd / 2f
        val y1 = edgeEnd / 2f

        val textCurrentColor = currentTextColor
        val opaqueColor = textCurrentColor
        val transparentColor = textCurrentColor and 0x00FFFFFF

        val gradientShader = LinearGradient(
            x0, y0, x1, y1,
            intArrayOf(opaqueColor, transparentColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradientShader
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

/**
 * Convenience extension function for any TextView in XML / Android View
 */
fun TextView.animateAlphaMaskReveal(
    durationMillis: Long = 5000L,
    featherPx: Float = 180f
) {
    val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = durationMillis
        interpolator = android.view.animation.LinearInterpolator()
        repeatCount = android.animation.ValueAnimator.INFINITE

        addUpdateListener { anim ->
            val progress = anim.animatedValue as Float
            val w = width.toFloat()
            val h = height.toFloat()
            if (w > 0f && h > 0f) {
                val totalSpan = w + h + featherPx * 2f
                val edgeStart = -featherPx + progress * totalSpan
                val edgeEnd = edgeStart + featherPx

                val textCurrentColor = currentTextColor
                val opaqueColor = textCurrentColor
                val transparentColor = textCurrentColor and 0x00FFFFFF

                paint.shader = LinearGradient(
                    edgeStart / 2f, edgeStart / 2f,
                    edgeEnd / 2f, edgeEnd / 2f,
                    intArrayOf(opaqueColor, transparentColor),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                invalidate()
            }
        }
    }
    animator.start()
}

/**
 * Demo Component displaying the Now Brief Header text cluster with the Text Alpha Mask Reveal effect
 */
@Composable
fun NowBriefHeaderDemoContent(
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .textAlphaMaskReveal(durationMillis = 5000, featherPx = 180f, repeatMode = true)
            .padding(16.dp)
    ) {
        // Title: "Morning focus" (Bold, 24sp)
        Text(
            text = "Morning focus",
            color = if (isDark) Color.White else Color(0xFF0F172A),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle 1: "Peak productivity window! 31°C and partly cloudy. Confidently tackle your most challenging tasks."
        Text(
            text = "Peak productivity window! 31°C and partly cloudy. Confidently tackle your most challenging tasks.",
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle 2: "Partly cloudy, 31°C in Ho Chi Minh City. Great weather for outdoor activities."
        Text(
            text = "Partly cloudy, 31°C in Ho Chi Minh City. Great weather for outdoor activities.",
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 19.sp
        )
    }
}
