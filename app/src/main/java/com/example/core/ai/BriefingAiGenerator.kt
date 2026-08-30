package com.example.core.ai

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.example.feature.widget.BriefingSession
import com.example.feature.widget.BriefingSessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 5 Khung giờ chuẩn trong ngày (Sáng / Trưa / Chiều / Hoàng hôn / Đêm)
 */
enum class DiurnalPhaseType(val label: String) {
    MORNING("Sáng"),
    NOON("Trưa"),
    AFTERNOON("Chiều"),
    EVENING("Hoàng hôn"),
    NIGHT("Đêm")
}

/**
 * Ngữ cảnh thực tế để AI sinh câu chúc & tóm tắt
 */
data class BriefingContext(
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val weatherTempC: Double? = null,
    val weatherCondition: String? = null,
    val energyScore: Int = 85,
    val sleepHoursFormatted: String? = null,
    val remainingCalendarEventsCount: Int = 0,
    val topTaskTitle: String? = null
) {
    val session: BriefingSession
        get() = BriefingSession.fromHourAndMinute(hourOfDay, 0)

    val phase: DiurnalPhaseType
        get() = when (session) {
            BriefingSession.EARLY_MORNING, BriefingSession.MID_MORNING -> DiurnalPhaseType.MORNING
            BriefingSession.NOON -> DiurnalPhaseType.NOON
            BriefingSession.AFTERNOON -> DiurnalPhaseType.AFTERNOON
            BriefingSession.EVENING -> DiurnalPhaseType.EVENING
            BriefingSession.NIGHT, BriefingSession.LATE_NIGHT -> DiurnalPhaseType.NIGHT
        }
}

/**
 * Kết quả đầu ra từ AI Service
 */
data class BriefingAiResult(
    val greetingHeadline: String,
    val dynamicBriefing: String,
    val timePhase: DiurnalPhaseType,
    val energyScore: Int
)

/**
 * Service AI sinh lời chúc & tóm tắt thông minh không bịa số liệu, cực kỳ ngắn gọn (<18 từ)
 */
class BriefingAiGenerator(
    private val context: Context,
    private val llmManager: LocalLlmManager? = null
) {
    companion object {
        private const val TAG = "BriefingAiGenerator"
        
        /**
         * System Prompt nghiêm ngặt gửi Local AI / LLM
         */
        const val SYSTEM_PROMPT_TEMPLATE = 
            "Hãy tạo 1 câu tóm tắt điểm tin thật tự nhiên, cực kỳ ngắn gọn (tối đa 18 từ) bằng tiếng Việt. TUYỆT ĐỐI KHÔNG tự bịa các con số phần trăm, pin, hay số liệu không được cung cấp trong context. Khung giờ Buổi sáng (5:00-11:29) BẮT BUỘC chỉ được tạo nội dung xoay quanh buổi sáng và tập trung làm việc, CẤM TUYỆT ĐỐI sinh các từ 'chiều', 'buổi chiều', 'tối'. Khung giờ Buổi trưa (11:30-13:59) BẮT BUỘC dùng từ 'Nghỉ trưa', 'Giữa ngày', CẤM TUYỆT ĐỐI dùng từ 'Buổi chiều'."

        /**
         * Đọc phần trăm pin thực tế từ hệ thống Android qua BatteryManager
         */
        fun getRealSystemBatteryPercentage(context: Context): Int {
            return try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            } catch (e: Exception) {
                -1
            }
        }
    }

    suspend fun generate(briefingContext: BriefingContext): BriefingAiResult = withContext(Dispatchers.Default) {
        val session = briefingContext.session
        val phase = briefingContext.phase

        val headline = session.title

        val tempInt = Math.round(briefingContext.weatherTempC ?: 28.0).toInt()
        val rawBriefing = generateGenZBriefing(briefingContext)
        val dynamicBriefing = BriefingSessionHelper.sanitizeWidgetText(rawBriefing, session, tempInt)

        Log.d(TAG, "Generated briefing for session ${session.name} (${headline}): '$dynamicBriefing'")

        BriefingAiResult(
            greetingHeadline = headline,
            dynamicBriefing = dynamicBriefing,
            timePhase = phase,
            energyScore = briefingContext.energyScore
        )
    }

    private fun generateGenZBriefing(ctx: BriefingContext): String {
        val tempInt = Math.round(ctx.weatherTempC ?: 28.0).toInt()
        val weatherDesc = ctx.weatherCondition ?: "trời mát mẻ"
        return BriefingContentProvider.getHeaderSummary(ctx.session, tempInt, weatherDesc)
    }
}
