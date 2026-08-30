package com.example.core.ai

import com.example.feature.widget.BriefingSession

/**
 * Nhà cung cấp dữ liệu động (Dynamic Content Provider) quản lý kho câu tóm tắt Header 
 * và Thẻ gợi ý Lifestyle Carousel phong phú 5-6 thẻ chuẩn One UI 9.
 */
object BriefingContentProvider {

    /**
     * Lấy câu tóm tắt Header động theo Session, tự động chèn nhiệt độ thật (${temp}°C) và thời tiết (${weather}).
     */
    fun getFallbackRecap(sessionName: String, temp: Int, weatherDesc: String): String {
        val session = BriefingSession.values().firstOrNull {
            it.sessionTag.equals(sessionName, ignoreCase = true) || it.name.equals(sessionName, ignoreCase = true)
        } ?: BriefingSession.getCurrentSession()
        return getHeaderSummary(session, temp, weatherDesc)
    }

    fun getHeaderSummary(session: BriefingSession, temp: Int, weather: String): String {
        val weatherClean = weather.lowercase()
        val tempStr = "${temp}°C"

        val options = when (session) {
            BriefingSession.EARLY_MORNING -> listOf(
                "Bình minh trong lành! Nhiệt độ $tempStr $weatherClean. Hãy nạp một ly nước ấm để đánh thức cơ thể.",
                "Chào ngày mới! Không khí $tempStr dễ chịu, thời điểm tuyệt vời để khởi động nhẹ và hít thở sâu.",
                "Sẵn sàng cho ngày mới rực rỡ! Nhiệt độ $tempStr, chúc bạn một ngày làm việc tràn đầy năng lượng.",
                "Nắng sớm dịu mát! Nhiệt độ $tempStr $weatherClean. Thưởng thức tách cà phê và bắt đầu ngày mới tràn hứng khởi."
            )

            BriefingSession.MID_MORNING -> listOf(
                "Khung giờ vàng cho sự tập trung! Nhiệt độ $tempStr $weatherClean. Hoàn thành công việc quan trọng nhất ngay bây giờ.",
                "Sáng làm việc hiệu quả! Nhiệt độ $tempStr, giữ nhịp tập trung và hoàn thành các mục tiêu đề ra.",
                "Tinh thần minh mẫn! Không khí $tempStr mát mẻ, giải quyết nhanh gọn danh sách công việc hôm nay.",
                "Khung giờ bứt phá hiệu suất! Nhiệt độ $tempStr $weatherClean. Tự tin chinh phục các nhiệm vụ thử thách nhất."
            )

            BriefingSession.NOON -> listOf(
                "Đã đến giờ nạp năng lượng! Nhiệt độ $tempStr, thưởng thức bữa trưa thật ngon miệng nhé.",
                "Nghỉ ngơi tái tạo sức lao động! Nhiệt độ ngoài trời $tempStr, hãy để mắt nghỉ ngơi sau buổi sáng làm việc.",
                "Chợp mắt 15-20 phút buổi trưa để sẵn sàng cho một buổi chiều bùng nổ hiệu suất.",
                "Nạp năng lượng giữa ngày! Không khí $tempStr $weatherClean, thưởng thức bữa trưa và thả lỏng tinh thần."
            )

            BriefingSession.AFTERNOON -> listOf(
                "Duy trì năng lượng buổi chiều! Nhiệt độ $tempStr, uống thêm nước hoặc trà để giữ sự tỉnh táo.",
                "Tiếp tục bứt phá! Không khí $tempStr $weatherClean, hoàn thành nốt các nhiệm vụ còn lại trong ngày.",
                "Vận động nhẹ tại chỗ để giải tỏa căng thẳng và duy trì năng suất làm việc đỉnh cao.",
                "Buổi chiều tỉnh táo! Nhiệt độ $tempStr, nhâm nhi tách trà chiều và duy trì nhịp làm việc mượt mà."
            )

            BriefingSession.EVENING -> listOf(
                "Hoàng hôn buông xuống! Nhiệt độ $tempStr dịu mát, gác lại công việc và tận hưởng buổi chiều tối.",
                "Kết thúc ngày làm việc chăm chỉ! Không khí $tempStr $weatherClean, chuẩn bị về nhà và thư giãn cùng người thân.",
                "Tiết trời hoàng hôn dễ chịu! Nhiệt độ $tempStr, vận động nhẹ nhàng hoặc đi dạo để giải tỏa mệt mỏi.",
                "Chiều muộn thư thái! Nhiệt độ $tempStr, thả lỏng đầu óc chuẩn bị cho bữa tối ấm áp."
            )

            BriefingSession.NIGHT -> listOf(
                "Buổi tối an lành! Nhiệt độ $tempStr mát mẻ, dành thời gian chăm sóc bản thân và giải trí nhẹ nhàng.",
                "Chuẩn bị cho giấc ngủ sâu! Nhiệt độ $tempStr $weatherClean, tắt bớt thiết bị điện tử để cơ thể phục hồi trọn vẹn đêm nay.",
                "Buổi tối yên tĩnh! Không khí $tempStr, thưởng thức bữa tối ngon miệng và thư giãn cùng gia đình.",
                "Trời đêm thoáng đãng! Nhiệt độ $tempStr, nhâm nhi tách trà ấm và tận hưởng không gian yên bình."
            )

            BriefingSession.LATE_NIGHT -> listOf(
                "Đêm muộn thư thái! Nhiệt độ $tempStr $weatherClean. Thả lỏng tâm trí và chuẩn bị cho giấc ngủ sâu.",
                "Đã đến giờ nghỉ ngơi! Không khí $tempStr mát mẻ, tạm gác mọi lo âu để cơ thể phục hồi hoàn toàn.",
                "Đêm dịu mát! Nhiệt độ $tempStr, giữ không gian phòng ngủ thoáng đãng và ngủ thật ngon nhé.",
                "Giấc ngủ đêm chất lượng! Nhiệt độ $tempStr, tắt máy đi ngủ sớm nạp lại 100% năng lượng cho sáng mai."
            )
        }

        return options.random()
    }

    /**
     * Bộ tóm tắt ngắn gọn, súc tích (Compact Briefing) thiết kế RIÊNG cho Dynamic Island
     * Đảm bảo hiển thị vừa vặn trên thanh Island pill mà không bị tràn hay quá dài.
     */
    fun getIslandSummary(session: BriefingSession, temp: Int, weather: String = "mát mẻ"): String {
        val options = when (session) {
            BriefingSession.EARLY_MORNING -> listOf(
                "Chào ngày mới tràn năng lượng!",
                "Khởi đầu ngày mới thật vui!",
                "Đón ánh bình minh ngày mới!",
                "Sáng sớm trong lành đón nắng!",
                "Chúc bạn ngày mới tốt lành!"
            )

            BriefingSession.MID_MORNING -> listOf(
                "Khung giờ vàng để tập trung!",
                "Tập trung cao độ làm việc!",
                "Bứt phá hiệu suất đỉnh cao!",
                "Làm việc hiệu quả hôm nay!",
                "Giữ vững tinh thần làm việc!",
                "Tự tin chinh phục mục tiêu!"
            )

            BriefingSession.NOON -> listOf(
                "Nghỉ trưa nạp lại năng lượng!",
                "Thưởng thức bữa trưa ngon miệng!",
                "Chợp mắt nghỉ ngơi giữa ngày!",
                "Thả lỏng đôi mắt thư giãn!",
                "Tái tạo năng lượng buổi chiều!"
            )

            BriefingSession.AFTERNOON -> listOf(
                "Duy trì năng lượng buổi chiều!",
                "Uống thêm nước và trà chiều!",
                "Tập trung tăng tốc về đích!",
                "Chiều làm việc thật hiệu quả!",
                "Giữ vững phong độ làm việc!"
            )

            BriefingSession.EVENING -> listOf(
                "Gác lại công việc về nhà!",
                "Tan ca thư giãn buổi chiều!",
                "Hoàng hôn buông xuống dịu mát!",
                "Thưởng thức bữa tối ấm cúng!",
                "Thả lỏng tinh thần chiều tà!"
            )

            BriefingSession.NIGHT -> listOf(
                "Buổi tối an lành ấm cúng!",
                "Thư giãn trọn vẹn buổi tối!",
                "Dành thời gian cho bản thân!",
                "Nghe bản nhạc nhẹ thư giãn!",
                "Tận hưởng không gian yên bình!"
            )

            BriefingSession.LATE_NIGHT -> listOf(
                "Tắt máy đi ngủ thôi nào!",
                "Chúc bạn có giấc ngủ ngon!",
                "Đêm đã về khuya nghỉ ngơi!",
                "Chúc bạn có giấc mơ đẹp!",
                "Tạm gác lại mọi âu lo!"
            )
        }

        return options.random()
    }

    /**
     * Lấy danh sách thẻ gợi ý Lifestyle Carousel phân loại phong phú 5-6 thẻ ngẫu nhiên mỗi phiên.
     * Tự động điều chỉnh theo thời tiết/nhiệt độ thật (nóng >32°C vs mát <28°C).
     */
    fun getLifestyleTips(
        session: BriefingSession,
        temp: Int,
        weather: String,
        count: Int = 6
    ): List<LifestyleTip> {
        val currentTemp = temp
        val tempStr = "${currentTemp}°C"
        val isHot = currentTemp > 32
        val isCool = currentTemp < 28

        val pool = when (session) {
            BriefingSession.EARLY_MORNING, BriefingSession.MID_MORNING -> listOf(
                // Thẻ 1: Ẩm thực sáng
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Bữa sáng",
                    title = "Phở bò tái lăn nóng hổi",
                    description = "Bát phở thơm nức nạp đạm và năng lượng cho cả sáng.",
                    emoji = "🍜"
                ),
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Bữa sáng",
                    title = "Bánh mì ốp la giòn rụm",
                    description = "Bữa sáng nhanh gọn, giòn thơm tràn năng lượng.",
                    emoji = "🥖"
                ),

                // Thẻ 2: Thức uống theo nhiệt độ
                LifestyleTip(
                    category = "Thức uống",
                    tag = if (isHot) "Giải nhiệt" else "Tỉnh táo",
                    title = if (isHot) "Nước ép dưa hấu mát lạnh" else "Cà phê muối thơm lừng",
                    description = if (isHot) "Thời tiết $tempStr nóng oi, giải nhiệt tức thì cho cơ thể." else "Nhiệt độ $tempStr dịu mát, nhâm nhi tách cà phê tỉnh táo.",
                    emoji = if (isHot) "🍉" else "☕"
                ),

                // Thẻ 3: Vận động / Sức khỏe
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thở sâu",
                    title = "Hít thở sâu 5 phút",
                    description = "Nhiệt độ $tempStr, dành 5 phút hít thở sâu để cân bằng lại tinh thần.",
                    emoji = "🧘‍♂️"
                ),
                LifestyleTip(
                    category = "Vận động",
                    tag = "Sức khỏe",
                    title = if (isCool) "Chạy bộ đón gió sương sớm" else "Giãn cơ buổi sáng 5 phút",
                    description = if (isCool) "Không khí $tempStr trong lành, lý tưởng cho 15 phút chạy bộ." else "Đánh thức cơ thể nhẹ nhàng, giảm căng thẳng vai gáy.",
                    emoji = if (isCool) "🏃‍♂️" else "🧘‍♂️"
                ),

                // Thẻ 4: Thư giãn / Tinh thần
                LifestyleTip(
                    category = "Tinh thần",
                    tag = "Học hỏi",
                    title = "Nghe Podcast phát triển bản thân",
                    description = "Nạp tư duy tích cực 10 phút trước khi bắt đầu công việc.",
                    emoji = "🎙️"
                ),

                // Thẻ 5: Mẹo công việc / Năng suất
                LifestyleTip(
                    category = "Năng suất",
                    tag = "Tập trung",
                    title = "Kỹ thuật Pomodoro 25 phút",
                    description = "Tập trung cao độ giải quyết mục tiêu quan trọng nhất.",
                    emoji = "⏱️"
                ),

                // Thẻ 6: Lối sống
                LifestyleTip(
                    category = "Lối sống",
                    tag = "Hydrate",
                    title = "Uống đủ 500ml nước ấm",
                    description = "Kích hoạt hệ tiêu hóa và bổ sung độ ẩm đầu ngày.",
                    emoji = "🥛"
                )
            )

            BriefingSession.NOON -> listOf(
                // Thẻ 1: Ẩm thực trưa
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Bữa trưa",
                    title = "Cơm tấm sườn bì chả",
                    description = "Thưởng thức bữa trưa thanh mát, nạp lại năng lượng giữa ngày.",
                    emoji = "🍛"
                ),
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Thanh mát",
                    title = "Bún chả tươi mát & Canh chua",
                    description = "Thực đơn thanh nhẹ giải nhiệt bữa trưa cực ngon miệng.",
                    emoji = "🍲"
                ),

                // Thẻ 2: Thức uống trưa
                LifestyleTip(
                    category = "Thức uống",
                    tag = "Giải nhiệt",
                    title = if (isHot) "Nước dừa xiêm mọng nước" else "Trà đào cam sả mát",
                    description = "Nhiệt độ ngoài trời $tempStr, bổ sung điện giải tự nhiên.",
                    emoji = if (isHot) "🥥" else "🍹"
                ),

                // Thẻ 3: Vận động / Nghỉ ngơi
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Nghỉ trưa",
                    title = "Chợp mắt 15-20 phút",
                    description = "Cho não bộ thả lỏng, sẵn sàng cho ca chiều hiệu suất.",
                    emoji = "💤"
                ),

                // Thẻ 4: Thư giãn / Thở sâu
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thở sâu",
                    title = "Hít thở sâu 5 phút",
                    description = "Nhiệt độ $tempStr, dành 5 phút hít thở sâu để cân bằng lại tinh thần.",
                    emoji = "🧘‍♀️"
                ),
                LifestyleTip(
                    category = "Thư giãn",
                    tag = "Phục hồi",
                    title = "Massage mắt & Thở 4-7-8",
                    description = "Giảm mỏi mắt sau nhiều giờ nhìn màn hình máy tính.",
                    emoji = "👀"
                ),

                // Thẻ 5: Năng suất
                LifestyleTip(
                    category = "Năng suất",
                    tag = "Mẹo hay",
                    title = "Dọn dẹp lại không gian làm việc",
                    description = "Bàn làm việc ngăn nắp giúp tinh thần chiều thêm tươi mới.",
                    emoji = "🧹"
                ),

                // Thẻ 6: Lối sống
                LifestyleTip(
                    category = "Lối sống",
                    tag = "Râm mát",
                    title = "Nghỉ ngơi nơi không gian râm mát",
                    description = "Nhiệt độ $tempStr, tránh ánh nắng gắt giữa trưa.",
                    emoji = "🌿"
                )
            )

            BriefingSession.AFTERNOON -> listOf(
                // Thẻ 1: Ẩm thực xế chiều
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Ăn nhẹ",
                    title = "Bánh mì chảo & Sinh tố bơ",
                    description = "Món ăn xế chiều thơm ngon nạp lại sự tỉnh táo.",
                    emoji = "🥑"
                ),

                // Thẻ 2: Thức uống chiều
                LifestyleTip(
                    category = "Thức uống",
                    tag = "Trà chiều",
                    title = "Trà đào cam sả thanh mát",
                    description = "Vị trà trái cây giải nhiệt sảng khoái xế chiều.",
                    emoji = "🍹"
                ),

                // Thẻ 3: Vận động
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thở sâu",
                    title = "Hít thở sâu 5 phút",
                    description = "Nhiệt độ $tempStr, dành 5 phút hít thở sâu để cân bằng lại tinh thần.",
                    emoji = "🧘‍♂️"
                ),
                LifestyleTip(
                    category = "Vận động",
                    tag = "Giãn cơ",
                    title = "Vận động nhẹ tại chỗ 3 phút",
                    description = "Vươn vai giãn cơ lưng gáy giúp máu lưu thông não bộ.",
                    emoji = "🤸‍♂️"
                ),

                // Thẻ 4: Thư giãn tinh thần
                LifestyleTip(
                    category = "Tinh thần",
                    tag = "Âm nhạc",
                    title = "Nghe nhạc Lofi không lời",
                    description = "Giai điệu lofi dịu nhẹ duy trì sự tập trung làm việc.",
                    emoji = "🎧"
                ),

                // Thẻ 5: Năng suất
                LifestyleTip(
                    category = "Năng suất",
                    tag = "Mẹo hay",
                    title = "Uống thêm 350ml nước lọc",
                    description = "Duy trì độ ẩm cơ thể và giữ làn da tươi tắn cả ngày.",
                    emoji = "💧"
                ),

                // Thẻ 6: Thư giãn
                LifestyleTip(
                    category = "Thư giãn",
                    tag = "Đọc sách",
                    title = "Đọc 5 trang sách yêu thích",
                    description = "Gác lại màn hình 10 phút để tâm trí được thả lỏng.",
                    emoji = "📖"
                )
            )

            BriefingSession.EVENING -> listOf(
                // Thẻ 1: Ẩm thực tối
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Bữa tối",
                    title = "Lẩu nấm thảo mộc thanh nhẹ",
                    description = "Thưởng thức bữa tối thanh mát, nạp lại năng lượng sau giờ làm.",
                    emoji = "🍲"
                ),

                // Thẻ 2: Thức uống tối
                LifestyleTip(
                    category = "Thức uống",
                    tag = "Thư thái",
                    title = "Trà hoa cúc ấm áp",
                    description = "Nhâm nhi trà ấm hỗ trợ thư giãn thần kinh tan tầm.",
                    emoji = "🍵"
                ),

                // Thẻ 3: Vận động
                LifestyleTip(
                    category = "Vận động",
                    tag = "Đi bộ",
                    title = "Đi dạo công viên 15 phút",
                    description = "Nhiệt độ $tempStr mát mẻ, đi bộ nhẹ hỗ trợ tiêu hóa tốt.",
                    emoji = "🚶‍♂️"
                ),
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thở sâu",
                    title = "Hít thở sâu 5 phút",
                    description = "Nhiệt độ $tempStr, dành 5 phút hít thở sâu để cân bằng lại tinh thần.",
                    emoji = "🧘‍♂️"
                ),

                // Thẻ 4: Tinh thần
                LifestyleTip(
                    category = "Tinh thần",
                    tag = "Mindfulness",
                    title = "Thả lỏng tinh thần tan tầm",
                    description = "Gác lại deadline công việc và chuẩn bị trở về nhà.",
                    emoji = "🌇"
                ),

                // Thẻ 5: Lối sống
                LifestyleTip(
                    category = "Lối sống",
                    tag = "Thư giãn",
                    title = "Đạp xe ngắm cảnh hoàng hôn",
                    description = "Tiết trời $tempStr dễ chịu, tận hưởng chiều tối nhẹ nhàng.",
                    emoji = "🚴‍♂️"
                ),

                // Thẻ 6: Chuẩn bị
                LifestyleTip(
                    category = "Lối sống",
                    tag = "Chuẩn bị",
                    title = "Soạn sẵn đồ dùng cho ngày mai",
                    description = "Chuẩn bị đồ đạc giúp buổi sáng mai luôn thong thả.",
                    emoji = "👕"
                )
            )

            BriefingSession.NIGHT, BriefingSession.LATE_NIGHT -> listOf(
                // Thẻ 1: Món ăn nhẹ đêm
                LifestyleTip(
                    category = "Ẩm thực",
                    tag = "Ăn nhẹ",
                    title = "Sữa ấm & Hạt ngũ cốc",
                    description = "Món ăn nhẹ dễ tiêu hóa hỗ trợ giấc ngủ sâu.",
                    emoji = "🥛"
                ),

                // Thẻ 2: Thức uống
                LifestyleTip(
                    category = "Thức uống",
                    tag = "Dưỡng tâm",
                    title = "Trà hoa cúc mật ong ấm",
                    description = "Thức uống xoa dịu thần kinh, chuẩn bị đi ngủ.",
                    emoji = "🍵"
                ),

                // Thẻ 3: Thư giãn tinh thần
                LifestyleTip(
                    category = "Tinh thần",
                    tag = "Giấc ngủ",
                    title = "Tắt ánh sáng xanh thiết bị",
                    description = "Để điện thoại xa giường 30 phút trước khi đi ngủ.",
                    emoji = "📱"
                ),

                // Thẻ 4: Sức khỏe
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thiền định",
                    title = "Thiền 5 phút chìm vào giấc ngủ",
                    description = "Tập trung nhịp thở đều đặn nạp năng lượng cho ngày mai.",
                    emoji = "🧘‍♀️"
                ),
                LifestyleTip(
                    category = "Sức khỏe",
                    tag = "Thở sâu",
                    title = "Hít thở sâu 5 phút",
                    description = "Nhiệt độ $tempStr, dành 5 phút hít thở sâu để cân bằng lại tinh thần.",
                    emoji = "🧘‍♂️"
                ),

                // Thẻ 5: Giải trí nhẹ
                LifestyleTip(
                    category = "Tinh thần",
                    tag = "Âm nhạc",
                    title = "Nghe nhạc Lofi & Tiếng mưa",
                    description = "Giai điệu dịu nhẹ giúp thả lỏng tâm trí tuyệt đối.",
                    emoji = "🎧"
                ),

                // Thẻ 6: Lối sống
                LifestyleTip(
                    category = "Lối sống",
                    tag = "Phòng ngủ",
                    title = "Giữ phòng ngủ thoáng mát",
                    description = "Nhiệt độ $tempStr mát mẻ, cho giấc ngủ trọn vẹn đêm nay.",
                    emoji = "🌙"
                )
            )
        }

        return pool.shuffled().take(count)
    }
}
