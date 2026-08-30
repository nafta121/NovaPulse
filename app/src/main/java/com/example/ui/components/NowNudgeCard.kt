package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.NowNudgeItem

/**
 * Thẻ Now Nudge chuẩn One UI 9 Liquid Glassmorphism:
 * Thiết kế đồng bộ 100% với hệ thống thẻ Dashboard (Bo góc 26dp, Padding 20dp, Kính mờ xuyên thấu).
 */
@Composable
fun NowNudgeCard(
    nudgeItem: NowNudgeItem,
    isDark: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "nudge_card_scale"
    )

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nudge_button_scale"
    )

    // Container Glass Tint đồng bộ với One UI 9 Dashboard
    val containerGlassColor = if (isDark) {
        Color(0xFF1E1B4B).copy(alpha = 0.22f)
    } else {
        Color(0xFF4338CA).copy(alpha = 0.06f)
    }

    OneUIGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onActionClick
            ),
        shape = RoundedCornerShape(26.dp),
        isDark = isDark,
        containerColor = containerGlassColor,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Icon Badge + Chip + Emoji
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0xFF6366F1).copy(alpha = 0.35f), Color(0xFF4338CA).copy(alpha = 0.20f))
                                    } else {
                                        listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = nudgeItem.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                        )
                        Text(
                            text = "Gợi ý thông minh",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = nudgeItem.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Headline & Subtext
            Text(
                text = nudgeItem.headline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = nudgeItem.subtext,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive One UI 9 Pill Action Button (Translucent Pastel / One UI Mờ)
            val buttonContainerColor = if (isDark) {
                Color(0xFF6366F1).copy(alpha = 0.22f)
            } else {
                Color(0xFF4F46E5).copy(alpha = 0.10f)
            }
            val buttonContentColor = if (isDark) {
                Color(0xFFC7D2FE)
            } else {
                Color(0xFF4338CA)
            }
            val buttonBorder = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isDark) Color(0xFF818CF8).copy(alpha = 0.28f) else Color(0xFF4F46E5).copy(alpha = 0.16f)
            )

            Button(
                onClick = onActionClick,
                interactionSource = buttonInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    },
                shape = RoundedCornerShape(14.dp),
                border = buttonBorder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = buttonContentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = nudgeItem.actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonContentColor
                )
            }
        }
    }
}
