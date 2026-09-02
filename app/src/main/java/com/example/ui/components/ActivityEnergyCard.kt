package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class DailyHeartRingData(
    val dayLabel: String,
    val isSunday: Boolean = false,
    val stepsProgress: Float = 0f,
    val activeProgress: Float = 0f,
    val calProgress: Float = 0f,
    val stepsCount: Int = 0,
    val activeMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val fullDayName: String = ""
)

/**
 * Card Hoạt Động & Năng Lượng Samsung Health với Biểu Đồ Trái Tim 3 Vòng
 * Hỗ trợ bấm chọn từng ngày (T2..CN) để xem chi tiết lịch sử bước chân & calo.
 * Tích hợp hiệu ứng Goal Achieved (Phát sáng nhẹ + Biểu tượng tích xanh hoàn thành mục tiêu).
 */
@Composable
fun ActivityEnergyCard(
    currentSteps: Int = 0,
    stepGoal: Int = 6000,
    currentActiveMinutes: Int = 0,
    activeGoalMinutes: Int = 60,
    currentCalories: Int = 0,
    calorieGoal: Int = 500,
    historyList: List<DailyHeartRingData> = defaultHistoryList(),
    isNight: Boolean = false,
    onSyncClick: () -> Unit = {}
) {
    // Trạng thái ngày đang được chọn (null = Xem Hôm nay)
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    val selectedDay = selectedDayIndex?.let { historyList.getOrNull(it) }
    val isSelectingHistoricalDay = selectedDay != null

    // Chỉ số hiển thị: nếu chọn ngày trong lịch sử thì lấy ngày đó, ngược lại lấy Hôm nay
    val displayedSteps = if (isSelectingHistoricalDay) selectedDay!!.stepsCount else currentSteps
    val displayedActiveMinutes = if (isSelectingHistoricalDay) selectedDay!!.activeMinutes else currentActiveMinutes
    val displayedCalories = if (isSelectingHistoricalDay) selectedDay!!.caloriesBurned else currentCalories

    val stepsProgress = (displayedSteps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val activeProgress = (displayedActiveMinutes.toFloat() / activeGoalMinutes).coerceIn(0f, 1f)
    val calProgress = (displayedCalories.toFloat() / calorieGoal).coerceIn(0f, 1f)

    val isAllGoalsAchieved = stepsProgress >= 1f && activeProgress >= 1f && calProgress >= 1f

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    OneUIGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_energy_card"),
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isNight) Color(0xFF1E293B).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // -------------------------------------------------------------
            // Header Row: Tiêu đề Thẻ / Tên ngày đã chọn + Badge hoàn thành hoặc Nút về Hôm nay
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSelectingHistoricalDay) "Chi tiết: ${selectedDay?.fullDayName?.ifBlank { selectedDay.dayLabel }}" else "Hoạt động hằng ngày",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                    )
                    if (isSelectingHistoricalDay) {
                        Text(
                            text = "Chạm lại để về Hôm nay",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        )
                    }
                }

                when {
                    isSelectingHistoricalDay -> {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isNight) Color(0xFF38BDF8).copy(alpha = 0.20f) else Color(0xFFE0F2FE))
                                .clickable { selectedDayIndex = null }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Về hôm nay",
                                color = if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isAllGoalsAchieved -> {
                        // Badge chúc mừng hoàn thành 100% cả 3 vòng
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = if (isNight) 0.22f else 0.14f))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Đạt cả 3 mục tiêu",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "Samsung Health",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
                            ),
                            modifier = Modifier.clickable { onSyncClick() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // NỬA TRÊN: Canvas Trái Tim 3 Vòng (Bên trái) + 3 Thông Số (Bên phải)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Trái tim 3 vòng Samsung Health 96x96dp (có tâm tích xanh nếu đạt 100%)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    SamsungHeartRingCanvas(
                        stepsProgress = stepsProgress,
                        activeMinutesProgress = activeProgress,
                        caloriesProgress = calProgress,
                        sizeDp = 96.dp
                    )

                    // Biểu tượng tích xanh nổi bật ở tâm khi hoàn thành 100% cả 3 vòng
                    if (isAllGoalsAchieved) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Goal Achieved",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

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
                        valueText = numberFormatter.format(displayedSteps),
                        goalText = "/${numberFormatter.format(stepGoal)} bước",
                        isCompleted = stepsProgress >= 1f,
                        isNight = isNight
                    )

                    // Dòng 2: Thời gian vận động (Xanh Cyan #22D3EE)
                    MetricRowItem(
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF22D3EE),
                        valueText = "$displayedActiveMinutes",
                        goalText = "/$activeGoalMinutes phút",
                        isCompleted = activeProgress >= 1f,
                        isNight = isNight
                    )

                    // Dòng 3: Calo tiêu thụ (Tím Neon #C084FC)
                    MetricRowItem(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFC084FC),
                        valueText = "$displayedCalories",
                        goalText = "/$calorieGoal kcal",
                        isCompleted = calProgress >= 1f,
                        isNight = isNight
                    )
                }
            }

            // -------------------------------------------------------------
            // Banner tóm tắt khi đang chọn ngày lịch sử
            // -------------------------------------------------------------
            AnimatedVisibility(
                visible = isSelectingHistoricalDay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNight) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedDay?.fullDayName?.ifBlank { selectedDay.dayLabel }}: ${numberFormatter.format(selectedDay?.stepsCount ?: 0)} bước • ${selectedDay?.activeMinutes ?: 0}p • ${selectedDay?.caloriesBurned ?: 0} kcal",
                            color = if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng chi tiết",
                            tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { selectedDayIndex = null }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider mỏng phân cách 2 nửa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isNight) Color.White.copy(alpha = 0.10f) else Color(0xFFF1F5F9))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // NỬA DƯỚI: Hàng Lịch Sử 7 Ngày (T2 -> CN) có thể bấm chọn tương tác
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                historyList.take(7).forEachIndexed { index, dayData ->
                    val isSelected = selectedDayIndex == index
                    val isDayCompleted = dayData.stepsProgress >= 1f && dayData.activeProgress >= 1f && dayData.calProgress >= 1f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) (if (isNight) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f))
                                else Color.Transparent
                            )
                            .clickable {
                                selectedDayIndex = if (selectedDayIndex == index) null else index
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dayData.dayLabel,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected || dayData.isSunday) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> (if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7))
                                dayData.isSunday -> Color(0xFFEF4444)
                                isNight -> Color(0xFFCBD5E1)
                                else -> Color(0xFF475569)
                            }
                        )

                        // Trái tim 3 vòng mini 24x24dp (có chấm tích xanh nhỏ ở tâm nếu hoàn thành 100%)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                        ) {
                            SamsungHeartRingCanvas(
                                stepsProgress = dayData.stepsProgress,
                                activeMinutesProgress = dayData.activeProgress,
                                caloriesProgress = dayData.calProgress,
                                sizeDp = 24.dp
                            )

                            if (isDayCompleted) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
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
    isCompleted: Boolean = false,
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

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
                modifier = Modifier.padding(bottom = 1.dp)
            )
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Hoàn thành",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(bottom = 1.dp)
                )
            }
        }
    }
}

fun defaultHistoryList(): List<DailyHeartRingData> {
    return listOf(
        DailyHeartRingData("T2", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Hai"),
        DailyHeartRingData("T3", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Ba"),
        DailyHeartRingData("T4", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Tư"),
        DailyHeartRingData("T5", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Năm"),
        DailyHeartRingData("T6", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Sáu"),
        DailyHeartRingData("T7", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thứ Bảy"),
        DailyHeartRingData("CN", isSunday = true, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Chủ Nhật")
    )
}
