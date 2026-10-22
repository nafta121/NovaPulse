package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OneUiCubicEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/**
 * 2-Phase 120Hz One UI 7 Entrance Animation for the Briefing screen (with One-Shot mechanism)
 * @param index Item index (0 for Dynamic Header, 1..N for the Card list)
 * @param hasPlayedAnimation Flag indicating whether the animation has already played
 * @param onAnimationFinished Callback invoked when the item animation finishes
 * @param isHeader Marks whether the item is the Header cluster (Phase 1) or a child Card (Phase 2)
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    hasPlayedAnimation: Boolean = false,
    onAnimationFinished: (() -> Unit)? = null,
    isHeader: Boolean = (index == 0),
    initialOffsetY: Dp = 80.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (hasPlayedAnimation) {
        // CASE 1: Already played -> Render 100% static on GPU, no recalculation on scroll
        Box(
            modifier = modifier.graphicsLayer {
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
            }
        ) {
            content()
        }
        return
    }

    // CASE 2: First run on app launch (One-Shot Entrance Animation)
    val density = LocalDensity.current
    val initialOffsetYPx = remember(initialOffsetY, density) {
        with(density) { initialOffsetY.toPx() }
    }

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(if (isHeader) 0.96f else 1.0f) }
    val offsetYAnim = remember { Animatable(if (isHeader) 0f else initialOffsetYPx) }

    LaunchedEffect(index, isHeader) {
        if (isHeader) {
            // PHASE 1: Header Focus Entrance (0ms -> 1000ms, alpha 0f -> 1f, scale 0.96f -> 1.0f, translationY = 0dp)
            coroutineScope {
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1000, easing = OneUiCubicEasing)
                    )
                }
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(durationMillis = 1000, easing = OneUiCubicEasing)
                    )
                }
            }
        } else {
            // PHASE 2: Staggered Cards Slide-up (Start delay = 400ms, stagger = 80ms/card, duration = 650ms)
            val cardDelay = 400L + ((index - 1).coerceAtLeast(0) * 80L)
            delay(cardDelay)
            coroutineScope {
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 650, easing = OneUiCubicEasing)
                    )
                }
                launch {
                    offsetYAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 650, easing = OneUiCubicEasing)
                    )
                }
            }
            onAnimationFinished?.invoke()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = alphaAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                translationY = offsetYAnim.value
            }
    ) {
        content()
    }
}
