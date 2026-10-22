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
 * Samsung Health Activity & Energy Card with 3-Ring Heart Chart.
 * Supports tapping individual days (Mon..Sun) to view detailed step and calorie history.
 * Includes a Goal Achieved effect (soft glow + green checkmark icon upon goal completion).
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
    // Currently selected day state (null = Viewing Today)
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    val selectedDay = selectedDayIndex?.let { historyList.getOrNull(it) }
    val isSelectingHistoricalDay = selectedDay != null

    // Displayed metrics: use the selected historical day if chosen, otherwise use Today
    val displayedSteps = if (isSelectingHistoricalDay) selectedDay!!.stepsCount else currentSteps
    val displayedActiveMinutes = if (isSelectingHistoricalDay) selectedDay!!.activeMinutes else currentActiveMinutes
    val displayedCalories = if (isSelectingHistoricalDay) selectedDay!!.caloriesBurned else currentCalories

    val stepsProgress = (displayedSteps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val activeProgress = (displayedActiveMinutes.toFloat() / activeGoalMinutes).coerceIn(0f, 1f)
    val calProgress = (displayedCalories.toFloat() / calorieGoal).coerceIn(0f, 1f)

    val isAllGoalsAchieved = stepsProgress >= 1f && activeProgress >= 1f && calProgress >= 1f

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }

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
            // Header Row: Card Title / Selected Day Name + Completion Badge or Back to Today Button
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSelectingHistoricalDay) "Details: ${selectedDay?.fullDayName?.ifBlank { selectedDay.dayLabel }}" else "Daily Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                    )
                    if (isSelectingHistoricalDay) {
                        Text(
                            text = "Tap again to return to Today",
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
                                text = "Back to Today",
                                color = if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isAllGoalsAchieved -> {
                        // Celebration badge for completing 100% of all 3 rings
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
                                    text = "All 3 goals met",
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
            // TOP HALF: 3-Ring Heart Canvas (Left) + 3 Metrics (Right)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Samsung Health 3-ring heart 96x96dp (with green center checkmark if 100% achieved)
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

                    // Prominent green checkmark icon in the center when 100% of all 3 rings are completed
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

                // Right column: 3 metric rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Steps (Green #A3E635)
                    MetricRowItem(
                        icon = Icons.Default.DirectionsWalk,
                        iconTint = Color(0xFFA3E635),
                        valueText = numberFormatter.format(displayedSteps),
                        goalText = "/${numberFormatter.format(stepGoal)} steps",
                        isCompleted = stepsProgress >= 1f,
                        isNight = isNight
                    )

                    // Row 2: Active time (Cyan #22D3EE)
                    MetricRowItem(
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF22D3EE),
                        valueText = "$displayedActiveMinutes",
                        goalText = "/$activeGoalMinutes mins",
                        isCompleted = activeProgress >= 1f,
                        isNight = isNight
                    )

                    // Row 3: Calories burned (Neon Purple #C084FC)
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
            // Summary banner when a historical day is selected
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
                            text = "${selectedDay?.fullDayName?.ifBlank { selectedDay.dayLabel }}: ${numberFormatter.format(selectedDay?.stepsCount ?: 0)} steps • ${selectedDay?.activeMinutes ?: 0}m • ${selectedDay?.caloriesBurned ?: 0} kcal",
                            color = if (isNight) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details",
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

            // Thin divider separating the two halves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isNight) Color.White.copy(alpha = 0.10f) else Color(0xFFF1F5F9))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // BOTTOM HALF: 7-Day History Row (Mon -> Sun) with interactive selection
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

                        // Mini 3-ring heart 24x24dp (with small green dot in center if 100% completed)
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
                    contentDescription = "Completed",
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
        DailyHeartRingData("Mon", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Monday"),
        DailyHeartRingData("Tue", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Tuesday"),
        DailyHeartRingData("Wed", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Wednesday"),
        DailyHeartRingData("Thu", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Thursday"),
        DailyHeartRingData("Fri", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Friday"),
        DailyHeartRingData("Sat", isSunday = false, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Saturday"),
        DailyHeartRingData("Sun", isSunday = true, stepsProgress = 0f, activeProgress = 0f, calProgress = 0f, stepsCount = 0, fullDayName = "Sunday")
    )
}
