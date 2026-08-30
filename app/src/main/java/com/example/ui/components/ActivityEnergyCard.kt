package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DailyHeartRingData(
    val dayLabel: String,
    val isSunday: Boolean = false,
    val stepsProgress: Float = 0.5f,
    val activeProgress: Float = 0.4f,
    val calProgress: Float = 0.3f
)

/**
 * Card Hoạt Động & Năng Lượng Samsung Health với Biểu Đồ Trái Tim 3 Vòng
 */
@Composable
fun ActivityEnergyCard(
    currentSteps: Int = 431,
    stepGoal: Int = 1000,
    currentActiveMinutes: Int = 4,
    activeGoalMinutes: Int = 90,
    currentCalories: Int = 13,
    calorieGoal: Int = 500,
    historyList: List<DailyHeartRingData> = defaultHistoryList(),
    isNight: Boolean = false,
    onSyncClick: () -> Unit = {}
) {
    val stepsProgress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val activeProgress = (currentActiveMinutes.toFloat() / activeGoalMinutes).coerceIn(0f, 1f)
    val calProgress = (currentCalories.toFloat() / calorieGoal).coerceIn(0f, 1f)

    OneUIGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSyncClick() }
            .testTag("activity_energy_card"),
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isNight) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Tiêu đề Thẻ Hoạt động Samsung Health
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hoạt động hằng ngày",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isNight) Color.White else Color(0xFF0F172A)
                    )
                )
                Text(
                    text = "Samsung Health",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NỬA TRÊN: Canvas Trái Tim 3 Vòng (Bên trái) + 3 Thông Số (Bên phải)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Trái tim 3 vòng Samsung Health 96x96dp
                SamsungHeartRingCanvas(
                    stepsProgress = stepsProgress,
                    activeMinutesProgress = activeProgress,
                    caloriesProgress = calProgress,
                    sizeDp = 96.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Cột bên phải: 3 dòng thông số
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dòng 1: Bước chân (Xanh lá #A3E635)
                    MetricRowItem(
                        icon = Icons.Default.DirectionsWalk,
                        iconTint = Color(0xFFA3E635),
                        valueText = "$currentSteps",
                        goalText = "/$stepGoal bước",
                        isNight = isNight
                    )

                    // Dòng 2: Thời gian vận động (Xanh Cyan #22D3EE)
                    MetricRowItem(
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF22D3EE),
                        valueText = "$currentActiveMinutes",
                        goalText = "/$activeGoalMinutes phút",
                        isNight = isNight
                    )

                    // Dòng 3: Calo tiêu thụ (Tím Neon #C084FC)
                    MetricRowItem(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFC084FC),
                        valueText = "$currentCalories",
                        goalText = "/$calorieGoal kcal",
                        isNight = isNight
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider mỏng phân cách 2 nửa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isNight) Color.White.copy(alpha = 0.10f) else Color(0xFFF1F5F9))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // NỬA DƯỚI: Hàng Lịch Sử 7 Ngày (T2 -> CN) với Trái tim mini 24x24dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                historyList.take(7).forEach { dayData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dayData.dayLabel,
                            fontSize = 11.5.sp,
                            fontWeight = if (dayData.isSunday) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                dayData.isSunday -> Color(0xFFEF4444)
                                isNight -> Color(0xFFCBD5E1)
                                else -> Color(0xFF475569)
                            }
                        )

                        // Trái tim 3 vòng mini 24x24dp cho từng ngày
                        SamsungHeartRingCanvas(
                            stepsProgress = dayData.stepsProgress,
                            activeMinutesProgress = dayData.activeProgress,
                            caloriesProgress = dayData.calProgress,
                            sizeDp = 24.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    valueText: String,
    goalText: String,
    isNight: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f))
                .border(1.dp, iconTint.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = valueText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNight) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = goalText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }
    }
}

fun defaultHistoryList(): List<DailyHeartRingData> {
    return listOf(
        DailyHeartRingData("T2", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("T3", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("T4", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("T5", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("T6", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("T7", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f),
        DailyHeartRingData("CN", isSunday = true, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f)
    )
}
