package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.repository.RealHealthData

/**
 * Card Trạng thái Giấc ngủ & Phục hồi chuẩn One UI 7 tối giản & tinh tế
 */
@Composable
fun SleepRestStatusCard(
    healthData: RealHealthData?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val sleepDurationFormatted = if (healthData != null && healthData.sleepDurationMinutes > 0) {
        healthData.sleepDurationFormatted
    } else {
        "7h 45m"
    }

    val sleepSummary = if (healthData != null && healthData.sleepStageSummary.isNotBlank()) {
        healthData.sleepStageSummary
    } else {
        "Giấc ngủ đêm qua sâu & phục hồi tốt"
    }

    val sleepMins = if (healthData != null && healthData.sleepDurationMinutes > 0) healthData.sleepDurationMinutes else 465L
    val progress = (sleepMins.toFloat() / 480f).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()

    OneUIGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Moon Icon (38x38dp) + Expanded Text Column (No Heart Rate Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Mặt trăng tím mềm mại (38x38dp)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = "Sleep Moon Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Column rộng rãi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Trạng thái giấc ngủ & Phục hồi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = sleepSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = (if (isDark) Color.White else Color(0xFF0F172A)).copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            // Stats row & Linear Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thời gian ngủ: $sleepDurationFormatted",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        text = "Mục tiêu 8h ($progressPercent%)",
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF0F172A)).copy(alpha = 0.70f)
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF818CF8),
                    trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE0E7FF)
                )
            }
        }
    }
}
