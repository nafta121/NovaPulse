package com.example.feature.widget

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * Cặp Tiêu đề và Phụ đề chúc mừng tương ứng
 */
data class BriefingGreeting(
    val title: String,
    val subtitle: String
)

/**
 * Phân loại 7 khung giờ chi tiết trong ngày chuẩn One UI 9
 */
enum class BriefingSession(
    val greetings: List<BriefingGreeting>,
    val sessionTag: String,
    val emoji: String,
    val iconGradient: List<Color>
) {
    EARLY_MORNING(
        greetings = listOf(
            BriefingGreeting("Good morning", "Khởi đầu ngày mới tràn đầy hứng khởi và năng lượng."),
            BriefingGreeting("Have a good day", "Chúc bạn một ngày làm việc thật thuận lợi và suôn sẻ."),
            BriefingGreeting("Rise and shine", "Thức giấc đón ánh bình minh và nạp một ly nước ấm."),
            BriefingGreeting("Start your day", "Sắp xếp mục tiêu quan trọng và bắt đầu ngày mới rực rỡ."),
            BriefingGreeting("Morning sunshine", "Tận hưởng không khí trong lành và chuẩn bị ngày mới tươi đẹp."),
            BriefingGreeting("Fresh start", "Một ngày mới với những cơ hội và trải nghiệm mới mẻ."),
            BriefingGreeting("Ready for today", "Tự tin và sẵn sàng chinh phục mọi kế hoạch hôm nay."),
            BriefingGreeting("Early bird", "Dậy sớm đón bình minh để làm chủ ngày dài năng động."),
            BriefingGreeting("Wake up & thrive", "Đánh thức cơ thể, hít thở sâu và bắt nhịp ngày mới.")
        ),
        sessionTag = "Bình minh",
        emoji = "🌅",
        iconGradient = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    ),
    MID_MORNING(
        greetings = listOf(
            BriefingGreeting("Morning focus", "Khung giờ vàng cho sự tập trung và hiệu suất đỉnh cao."),
            BriefingGreeting("Stay productive", "Giữ vững nhịp độ làm việc và hoàn thành các mục tiêu đề ra."),
            BriefingGreeting("Keep the momentum", "Tiếp tục đà bứt phá để chinh phục các nhiệm vụ hôm nay."),
            BriefingGreeting("Deep work", "Dành trọn sự chú ý cho những công việc quan trọng nhất."),
            BriefingGreeting("In the zone", "Duy trì trạng thái tập trung cao độ để bứt phá hiệu quả."),
            BriefingGreeting("Crush your goals", "Từng bước hoàn thành xuất sắc danh sách việc cần làm."),
            BriefingGreeting("Stay sharp", "Tinh thần minh mẫn, giải quyết công việc nhanh gọn."),
            BriefingGreeting("Focus mode", "Tạm gác phân tâm, tập trung tối đa cho kết quả tốt nhất."),
            BriefingGreeting("Peak performance", "Thời điểm vàng để đạt năng suất làm việc vượt trội.")
        ),
        sessionTag = "Buổi sáng",
        emoji = "☀️",
        iconGradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
    ),
    NOON(
        greetings = listOf(
            BriefingGreeting("Midday recap", "Điểm lại buổi sáng và nạp lại năng lượng giữa ngày."),
            BriefingGreeting("Lunch break", "Thưởng thức bữa trưa ngon miệng và để mắt nghỉ ngơi."),
            BriefingGreeting("Time to recharge", "Chợp mắt 15-20 phút để sẵn sàng cho buổi chiều bùng nổ."),
            BriefingGreeting("Midday breath", "Tạm rời màn hình, thả lỏng cơ thể và thư giãn đôi chút."),
            BriefingGreeting("Refuel & relax", "Nạp đầy năng lượng với bữa ăn đủ chất và tâm trạng thoải mái."),
            BriefingGreeting("Halfway there", "Đã đi được nửa chặng đường thành công của ngày hôm nay."),
            BriefingGreeting("Take a pause", "Dành một khoảng lặng ngắn để làm mới lại tinh thần."),
            BriefingGreeting("Noon refresh", "Uống một ngụm nước mát và khởi động lại sự tỉnh táo.")
        ),
        sessionTag = "Buổi trưa",
        emoji = "🌤",
        iconGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
    ),
    AFTERNOON(
        greetings = listOf(
            BriefingGreeting("Afternoon focus", "Duy trì sự tỉnh táo và đẩy nhanh tiến độ công việc."),
            BriefingGreeting("Stay hydrated", "Uống thêm nước hoặc trà chiều để giữ tinh thần sảng khoái."),
            BriefingGreeting("Power through", "Bứt phá vượt qua uể oải, hoàn tất các nhiệm vụ còn lại."),
            BriefingGreeting("Good afternoon", "Duy trì nhịp làm việc hiệu quả và giữ vững phong độ."),
            BriefingGreeting("Afternoon boost", "Thêm tách trà hoặc cà phê nhẹ để lấy lại sự hăng say."),
            BriefingGreeting("Keep going", "Chỉ còn một chút nữa là hoàn thành trọn vẹn mục tiêu ngày."),
            BriefingGreeting("Second wind", "Tìm lại nguồn cảm hứng mới cho các dự án buổi chiều."),
            BriefingGreeting("Sprint finish", "Tập trung tăng tốc để về đích ngày làm việc thành công.")
        ),
        sessionTag = "Buổi chiều",
        emoji = "⛅",
        iconGradient = listOf(Color(0xFF22D3EE), Color(0xFF0D9488))
    ),
    EVENING(
        greetings = listOf(
            BriefingGreeting("Evening wind-down", "Gác lại công việc, tận hưởng hoàng hôn và vận động nhẹ."),
            BriefingGreeting("Wrap up your day", "Tổng kết kết quả ngày hôm nay và chuẩn bị rời bàn làm việc."),
            BriefingGreeting("Heading home", "Thư giãn trên đường về nhà và đón chào bữa tối ấm cúng."),
            BriefingGreeting("Evening brief", "Thả lỏng tinh thần sau một ngày làm việc chăm chỉ."),
            BriefingGreeting("Sunset vibes", "Ngắm ánh hoàng hôn buông xuống và cảm nhận sự bình yên."),
            BriefingGreeting("Day completed", "Tự hào vì bạn đã nỗ lực hết mình trong suốt ngày hôm nay."),
            BriefingGreeting("Clocking out", "Đóng lại công việc, mở ra khoảng thời gian cho gia đình."),
            BriefingGreeting("Time to unwind", "Bước ra ngoài hít thở không khí dịu mát của buổi chiều tà.")
        ),
        sessionTag = "Hoàng hôn",
        emoji = "🌇",
        iconGradient = listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFBBF24))
    ),
    NIGHT(
        greetings = listOf(
            BriefingGreeting("Night unwind", "Thư giãn, ăn tối và dành thời gian trọn vẹn cho bản thân."),
            BriefingGreeting("Relax and recharge", "Giải tỏa căng thẳng bằng âm nhạc hoặc cuốn sách yêu thích."),
            BriefingGreeting("Personal time", "Khoảnh khắc ấm áp bên người thân và chăm sóc chính mình."),
            BriefingGreeting("Cozy evening", "Tận hưởng không gian ấm cúng, dễ chịu tại ngôi nhà của bạn."),
            BriefingGreeting("Mindful evening", "Lắng đọng tâm hồn và biết ơn những điều tốt đẹp hôm nay."),
            BriefingGreeting("Dinner & chill", "Thưởng thức bữa tối ngon lành và trò chuyện cùng người thân."),
            BriefingGreeting("Peace of mind", "Để lại mọi áp lực phía sau và thư thái tận hưởng buổi tối."),
            BriefingGreeting("Slow down", "Chậm lại một nhịp, lắng nghe cơ thể và chăm sóc bản thân.")
        ),
        sessionTag = "Buổi tối",
        emoji = "🌙",
        iconGradient = listOf(Color(0xFF818CF8), Color(0xFF6366F1))
    ),
    LATE_NIGHT(
        greetings = listOf(
            BriefingGreeting("Sleep well", "Thả lỏng tâm trí, chuẩn bị cho một giấc ngủ thật sâu."),
            BriefingGreeting("Late night", "Đêm đã về khuya, hãy để cơ thể được nghỉ ngơi phục hồi."),
            BriefingGreeting("Sweet dreams", "Tắt các thiết bị điện tử và chìm vào giấc mơ ngọt ngào."),
            BriefingGreeting("Time to rest", "Tạm gác mọi âu lo, chúc bạn có một đêm an giấc."),
            BriefingGreeting("Late night recap", "Khép lại một ngày dài và nạp lại năng lượng cho ngày mai."),
            BriefingGreeting("Night owl", "Nếu vẫn còn thức, hãy giữ ấm cơ thể và chuẩn bị đi ngủ nhé."),
            BriefingGreeting("Quiet hours", "Không gian tĩnh lặng, thời điểm hoàn hảo để hồi phục năng lượng."),
            BriefingGreeting("Drift away", "Thở chậm lại, thả lỏng toàn thân và chìm vào giấc ngủ ngon."),
            BriefingGreeting("Restful sleep", "Chúc bạn có một giấc ngủ trọn vẹn để thức dậy tươi mới vào sáng mai.")
        ),
        sessionTag = "Đêm muộn",
        emoji = "🌌",
        iconGradient = listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
    );

    val titles: List<String>
        get() = greetings.map { it.title }

    val subtitles: List<String>
        get() = greetings.map { it.subtitle }

    val title: String
        get() = greetings.first().title

    val subtitle: String
        get() = greetings.first().subtitle

    fun getRandomGreeting(): BriefingGreeting = greetings.random()
    fun getRandomTitle(): String = titles.random()
    fun getSubtitleForTitle(title: String): String =
        greetings.firstOrNull { it.title.equals(title, ignoreCase = true) }?.subtitle ?: subtitle

    companion object {
        fun fromHourAndMinute(hour: Int, minute: Int = 0): BriefingSession {
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

        fun getCurrentSession(): BriefingSession {
            val now = LocalTime.now()
            return fromHourAndMinute(now.hour, now.minute)
        }
    }
}

/**
 * Information data structure cho Briefing Session
 */
data class BriefingSessionInfo(
    val session: BriefingSession,
    val title: String,
    val subtitle: String,
    val hour: Int,
    val titles: List<String> = session.titles,
    val greetings: List<BriefingGreeting> = session.greetings
)

/**
 * Helper quản lý Tiêu đề & Subtitle chúc mừng linh hoạt theo 7 khung giờ trong ngày
 */
object BriefingSessionHelper {

    fun getSessionInfo(hour: Int, minute: Int = 0): BriefingSessionInfo {
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        return BriefingSessionInfo(
            session = session,
            title = session.title,
            subtitle = session.subtitle,
            hour = hour,
            titles = session.titles,
            greetings = session.greetings
        )
    }

    fun getCurrentSessionInfo(): BriefingSessionInfo {
        val now = LocalTime.now()
        return getSessionInfo(now.hour, now.minute)
    }

    /**
     * Hàm Guard làm sạch và kiểm tra khớp chuỗi Widget giữa Tiêu đề và Nội dung
     */
    fun sanitizeWidgetText(rawText: String, session: BriefingSession, currentTemp: Int = 28): String {
        // Tự động thay thế số nhiệt độ cũ trong văn bản bằng nhiệt độ thực tế hiện tại
        val updatedTempText = rawText
            .replace("\\d+°C".toRegex(), "${currentTemp}°C")
            .replace("\\d+°".toRegex(), "${currentTemp}°")

        if (session == BriefingSession.MID_MORNING || session == BriefingSession.EARLY_MORNING) {
            if (updatedTempText.contains(
                    "Nhiệt độ hạ dần",
                    ignoreCase = true
                ) || updatedTempText.contains("Trời về đêm", ignoreCase = true)
            ) {
                return "Trời nắng nhẹ $currentTemp°C. Hãy tập trung cho các mục tiêu quan trọng sáng nay."
            }
        }
        if (session == BriefingSession.NOON) {
            if (updatedTempText.contains("Chuẩn bị nghỉ ngơi", ignoreCase = true)) {
                return "Giờ nghỉ trưa $currentTemp°C. Nạp lại năng lượng với bữa ăn và nghỉ ngơi nhẹ."
            }
        }
        return updatedTempText
    }
}
