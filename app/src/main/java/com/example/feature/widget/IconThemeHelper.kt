package com.example.feature.widget

import androidx.compose.ui.graphics.Color

/**
 * Helper managing standard gradient colors for the circular icon on the left of the Now Bar
 */
object IconThemeHelper {

    fun getIconGradientForHour(hour: Int): List<Color> {
        return when (hour) {
            in 5..10 -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
            in 11..16 -> listOf(Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF38BDF8))
            in 17..19 -> listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFBBF24))
            else -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFFB923C))
        }
    }
}
