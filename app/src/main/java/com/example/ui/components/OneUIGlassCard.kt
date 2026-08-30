package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Container Liquid Glassmorphism chuẩn One UI 9 với hiệu ứng kính mờ xuyên thấu,
 * viền phản quang Gradient Specular Highlight và độ đổ bóng mềm mại (Soft Ambient Shadow).
 */
@Composable
fun OneUIGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    isDark: Boolean = false,
    containerColor: Color? = null,
    customBrush: Brush? = null,
    borderWidth: Dp = 1.2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassBrush = customBrush
        ?: if (containerColor != null) {
            Brush.linearGradient(
                listOf(
                    containerColor,
                    containerColor.copy(alpha = (containerColor.alpha * 0.8f).coerceAtLeast(0.1f))
                )
            )
        } else if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.25f),
                    Color(0xFF0F172A).copy(alpha = 0.15f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        }

    val specularBorder = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.35f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 4.dp else 6.dp,
                shape = shape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF64748B).copy(alpha = 0.12f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF0284C7).copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(glassBrush)
            .border(
                width = borderWidth,
                brush = specularBorder,
                shape = shape
            )
    ) {
        content()
    }
}
