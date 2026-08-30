package com.example.feature.widget

import androidx.compose.ui.graphics.Color

/**
 * Hàm hỗ trợ lọc sạch toàn bộ Emoji & ký tự thừa trước khi hiển thị lên Widget
 */
fun String.cleanEmoji(): String {
    return this
        .replace("(?![\u00B0\u2103\u2109])[\\p{So}\\p{Cn}\\p{Cs}\\p{Extended_Pictographic}]".toRegex(), "")
        .replace("  +", " ")
        .trim()
}

/**
 * Bảng màu chuẩn 1:1 theo Samsung One UI Now Bar cho 7 khung giờ chi tiết trong ngày
 */
enum class NowBarSession(
    val pillGradient: List<Color>,
    val iconGradient: List<Color>,
    val title: String,
    val greetingSubtitle: String,
    val fallbackText: String
) {
    EARLY_MORNING(
        pillGradient = listOf(Color(0xFF38BDF8), Color(0xFF67E8F9), Color(0xFFA7F3D0)),
        iconGradient = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
        title = "Morning brief",
        greetingSubtitle = "Khởi đầu ngày mới",
        fallbackText = "Khởi đầu ngày mới tràn đầy hứng khởi. Nắng sáng dịu mát 30°C."
    ),
    MID_MORNING(
        pillGradient = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF7DD3C7)),
        iconGradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
        title = "Morning focus",
        greetingSubtitle = "Tập trung làm việc",
        fallbackText = "Khung giờ vàng cho sự tập trung và hiệu suất công việc."
    ),
    NOON(
        pillGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFFEF08A)),
        iconGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
        title = "Midday recap",
        greetingSubtitle = "Nghỉ trưa & Thưởng thức",
        fallbackText = "Nghỉ ngơi, nạp lại năng lượng với bữa trưa ngon miệng."
    ),
    AFTERNOON(
        pillGradient = listOf(Color(0xFF0D9488), Color(0xFF22D3EE), Color(0xFFA7F3D0)),
        iconGradient = listOf(Color(0xFF22D3EE), Color(0xFF0D9488)),
        title = "Good Afternoon",
        greetingSubtitle = "Buổi chiều năng lượng",
        fallbackText = "Duy trì nhịp làm việc, uống đủ nước và tách trà chiều."
    ),
    EVENING(
        pillGradient = listOf(Color(0xFF7C72C7), Color(0xFFA855F7), Color(0xFFF472B6)),
        iconGradient = listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFBBF24)),
        title = "Evening brief",
        greetingSubtitle = "Hoàng hôn thư giãn",
        fallbackText = "Gác lại công việc, tận hưởng hoàng hôn và vận động nhẹ."
    ),
    NIGHT(
        pillGradient = listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFFA855F7)),
        iconGradient = listOf(Color(0xFF818CF8), Color(0xFF6366F1)),
        title = "Night unwind",
        greetingSubtitle = "Thư giãn buổi tối",
        fallbackText = "Thư giãn, ăn tối và dành thời gian chất lượng cho bản thân."
    ),
    LATE_NIGHT(
        pillGradient = listOf(Color(0xFF3730A3), Color(0xFF581C87), Color(0xFF701A75)),
        iconGradient = listOf(Color(0xFF7C3AED), Color(0xFFDB2777)),
        title = "Late night recap",
        greetingSubtitle = "Nghỉ ngơi đêm muộn",
        fallbackText = "Thả lỏng tâm trí, chuẩn bị cho một giấc ngủ sâu phục hồi."
    );

    companion object {
        fun fromHour(hour: Int): NowBarSession {
            return fromHourAndMinute(hour, 0)
        }

        fun fromHourAndMinute(hour: Int, minute: Int = 0): NowBarSession {
            val totalMins = hour * 60 + minute
            return when {
                totalMins in (5 * 60)..(8 * 60 + 29) -> EARLY_MORNING   // 05:00 - 08:29
                totalMins in (8 * 60 + 30)..(11 * 60 + 29) -> MID_MORNING // 08:30 - 11:29
                totalMins in (11 * 60 + 30)..(13 * 60 + 59) -> NOON       // 11:30 - 13:59
                totalMins in (14 * 60)..(16 * 60 + 59) -> AFTERNOON       // 14:00 - 16:59
                totalMins in (17 * 60)..(19 * 60 + 29) -> EVENING         // 17:00 - 19:29
                totalMins in (19 * 60 + 30)..(22 * 60 + 29) -> NIGHT      // 19:30 - 22:29
                else -> LATE_NIGHT                                        // 22:30 - 04:59
            }
        }
    }
}
