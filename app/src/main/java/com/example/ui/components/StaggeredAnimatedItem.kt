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
import kotlinx.coroutines.delay

private val OneUiCubicEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/**
 * Hiệu ứng Animation 2 PHA chuẩn 120Hz One UI 7 cho màn hình Briefing (Kèm cơ chế One-Shot)
 * @param index Thứ tự item (0 cho Dynamic Header, 1..N cho danh sách Card)
 * @param hasPlayedAnimation Cờ trạng thái đã chạy animation trước đó hay chưa
 * @param onAnimationFinished Callback báo hoàn tất sau khi item chạy xong
 * @param isHeader Đánh dấu item là cụm Header (Pha 1) hay Card con (Pha 2)
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
        // TRƯỜNG HỢP 1: Đã chạy trước đó -> Render tĩnh 100% trên GPU, không recalculate khi cuộn
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

    // TRƯỜNG HỢP 2: Chạy lần đầu khi mở app (One-Shot Entrance Animation)
    val density = LocalDensity.current
    val initialOffsetYPx = remember(initialOffsetY, density) {
        with(density) { initialOffsetY.toPx() }
    }

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(if (isHeader) 0.96f else 1.0f) }
    val offsetYAnim = remember { Animatable(if (isHeader) 0f else initialOffsetYPx) }

    LaunchedEffect(index, isHeader) {
        if (isHeader) {
            // PHA 1: Header Focus Entrance (0ms -> 1000ms, alpha 0f -> 1f, scale 0.96f -> 1.0f, translationY = 0dp)
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = OneUiCubicEasing)
            )
        } else {
            // PHA 2: Cards Slide-up So Le (Start delay = 400ms, stagger = 80ms/card, duration = 650ms)
            val cardDelay = 400L + ((index - 1).coerceAtLeast(0) * 80L)
            delay(cardDelay)
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = OneUiCubicEasing)
            )
            onAnimationFinished?.invoke()
        }
    }

    LaunchedEffect(index, isHeader) {
        if (isHeader) {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000, easing = OneUiCubicEasing)
            )
        } else {
            val cardDelay = 400L + ((index - 1).coerceAtLeast(0) * 80L)
            delay(cardDelay)
            offsetYAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 650, easing = OneUiCubicEasing)
            )
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
