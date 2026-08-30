package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card Lộ Trình Di Chuyển Thông Minh chuẩn Liquid Glassmorphism One UI 9 Mở Google Maps một chạm
 */
@Composable
fun SmartCommuteCard(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "commute_scale"
    )

    val openMapsAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=Công+ty")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {}
        }
    }

    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val commuteSubtext = remember(currentHour) {
        if (currentHour in 5..12) {
            "Lộ trình đến nơi làm việc / trường học • 18 phút (7.2 km)"
        } else {
            "Lộ trình trở về nhà • 18 phút (7.2 km)"
        }
    }

    val amberGlassBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A).copy(alpha = 0.22f),
                Color(0xFFD97706).copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color(0xFFFFFBEB).copy(alpha = 0.16f)
            )
        )
    }

    OneUIGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = openMapsAction
            ),
        shape = RoundedCornerShape(26.dp),
        isDark = isDark,
        customBrush = amberGlassBrush
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Điều hướng / Map (44x44dp) màu Cam Vàng Glassmorphism 3D
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                                Color(0xFFFBBF24).copy(alpha = 0.80f),
                                Color(0xFFD97706)
                            )
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.80f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = "Commute Route",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nội dung Thông tin Di chuyển
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lộ trình di chuyển & Giao thông",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = commuteSubtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Icon Mở Google Maps (Pill Action Button với hiệu ứng bóng kính)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(3.dp, CircleShape, spotColor = Color(0xFFF59E0B))
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFEF3C7),
                                Color(0xFFFDE68A)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.80f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = "Open Google Maps",
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
