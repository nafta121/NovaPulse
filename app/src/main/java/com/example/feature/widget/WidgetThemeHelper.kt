package com.example.feature.widget

import androidx.compose.ui.graphics.Color
import java.util.Calendar

/**
 * Data class chứa cấu hình Theme động của Widget theo 7 khung giờ trong ngày
 */
data class WidgetTimeThemeData(
    val phaseKey: String,
    val title: String,
    val greetingSubtitle: String,
    val isDark: Boolean,
    val baseColor: Color,
    val gradientColors: List<Color>,
    val iconGradientColors: List<Color>,
    val titleTextColor: Color = Color.White,
    val subtitleTextColor: Color = Color.White.copy(alpha = 0.85f),
    val fallbackText: String
) {
    fun getColorsWithOpacity(opacity: Float): List<Color> {
        val clamped = opacity.coerceIn(0.05f, 1.0f)
        return gradientColors.map { color ->
            color.copy(alpha = clamped)
        }
    }
}

/**
 * Helper tập trung quản lý Màu sắc & Tiêu đề Widget đồng bộ theo Thời gian thực
 */
object WidgetThemeHelper {

    fun getThemeForCurrentTime(hour: Int, minute: Int = 0): WidgetTimeThemeData {
        val session = NowBarSession.fromHourAndMinute(hour, minute)
        val isDark = session == NowBarSession.EVENING || session == NowBarSession.NIGHT || session == NowBarSession.LATE_NIGHT

        return WidgetTimeThemeData(
            phaseKey = session.name,
            title = session.title,
            greetingSubtitle = session.greetingSubtitle,
            isDark = isDark,
            baseColor = session.pillGradient.first(),
            gradientColors = session.pillGradient,
            iconGradientColors = session.iconGradient,
            titleTextColor = Color.White,
            subtitleTextColor = Color.White.copy(alpha = 0.85f),
            fallbackText = session.fallbackText
        )
    }

    fun getThemeForNow(): WidgetTimeThemeData {
        val cal = Calendar.getInstance()
        return getThemeForCurrentTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}
