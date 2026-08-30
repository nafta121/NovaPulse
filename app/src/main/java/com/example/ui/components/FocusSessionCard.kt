package com.example.ui.components

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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card Khung Giờ Tập Trung chuẩn Liquid Glassmorphism One UI 9 kích hoạt Pomodoro Deep Work 25 phút một chạm
 */
@Composable
fun FocusSessionCard(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocusing by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val btnInteractionSource = remember { MutableInteractionSource() }
    val isBtnPressed by btnInteractionSource.collectIsPressedAsState()

    val btnScale by animateFloatAsState(
        targetValue = if (isBtnPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "focus_btn_scale"
    )

    val dynamicFocusSubtext = remember {
        val cal = java.util.Calendar.getInstance()
        val min = cal.get(java.util.Calendar.MINUTE)
        val remainder = min % 5
        if (remainder != 0) {
            cal.add(java.util.Calendar.MINUTE, 5 - remainder)
        }
        cal.set(java.util.Calendar.SECOND, 0)
        val startStr = String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))

        cal.add(java.util.Calendar.MINUTE, 25)
        val endStr = String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))

        "Khuyên dùng 25 phút tập trung không thông báo từ $startStr - $endStr"
    }

    val purpleGlassBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A).copy(alpha = 0.22f),
                Color(0xFF7C3AED).copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color(0xFFFAF5FF).copy(alpha = 0.16f)
            )
        )
    }

    OneUIGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        isDark = isDark,
        customBrush = purpleGlassBrush
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Đồng hồ cát / Tập trung Tím Neon trong quả cầu Glass 3D (44x44dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                                Color(0xFFA855F7).copy(alpha = 0.80f),
                                Color(0xFF7C3AED)
                            )
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.80f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Focus Hourglass",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nội dung Thông tin Chế độ Tập trung
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Khung giờ tập trung",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFocusing) "Đang bật phiên tập trung 25 phút • Tắt thông báo" else dynamicFocusSubtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val btnBrush = if (isFocusing) {
                Brush.horizontalGradient(listOf(Color(0xFF16A34A), Color(0xFF22C55E)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7)))
            }

            // Nút Bật Chế độ Tập trung Pill Shape với Gradient bóng bẩy và viền Specular
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .shadow(4.dp, CircleShape, spotColor = if (isFocusing) Color(0xFF22C55E) else Color(0xFF7C3AED))
                    .clip(CircleShape)
                    .background(btnBrush)
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.60f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .clickable(
                        interactionSource = btnInteractionSource,
                        indication = ripple(bounded = true),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFocusing = !isFocusing
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isFocusing) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Focus",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = if (isFocusing) "Đang bật" else "25 phút",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
