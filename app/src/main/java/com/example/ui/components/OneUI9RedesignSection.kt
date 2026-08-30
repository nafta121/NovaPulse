package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Section tổng hợp các Card chuẩn Samsung One UI 9.
 * Đạt chuẩn ergonomic guidelines với spacing 16dp và padding 16-20dp.
 */
@Composable
fun OneUI9RedesignSection(
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Event Card (Google Calendar)
        OneUICalendarCard(
            eventTitle = "Họp chiến lược sản phẩm Q3",
            timeSubtitle = "08:30 - 11:30 AM",
            calendarSource = "Google Calendar",
            isDark = isDark
        )

        // 2. Habit Report Card
        WeeklyHabitInsightsCard(
            isDark = isDark
        )
    }
}

@Preview(name = "Light Mode - One UI 9 Redesign Section")
@Composable
private fun OneUI9RedesignSectionLightPreview() {
    Surface(color = Color(0xFFF2F4F7)) {
        OneUI9RedesignSection(isDark = false)
    }
}

@Preview(name = "Dark Mode - One UI 9 Redesign Section")
@Composable
private fun OneUI9RedesignSectionDarkPreview() {
    Surface(color = Color(0xFF0F172A)) {
        OneUI9RedesignSection(isDark = true)
    }
}
