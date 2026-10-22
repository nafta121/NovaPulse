package com.example.core.ml

import java.text.Normalizer
import java.util.Calendar
import java.util.regex.Pattern

enum class TaskUrgency(val label: String, val tagLabel: String, val colorHex: Long) {
    CRITICAL("Critical", "🔥 Critical", 0xFFEF4444),
    URGENT("Urgent", "⚡ Urgent", 0xFFF59E0B),
    NORMAL("Normal", "⏳ Normal", 0xFF3B82F6)
}

enum class TaskCategory(val label: String, val emoji: String, val colorHex: Long) {
    DEEP_WORK("Deep Focus", "🧠", 0xFF8B5CF6),
    COMMUNICATION("Communication", "💬", 0xFF06B6D4),
    FINANCE_ADMIN("Finance & Admin", "💳", 0xFF10B981),
    HEALTH_WELLNESS("Health & Fitness", "🏃", 0xFFF43F5E),
    CHORES_ERRANDS("Chores & Errands", "🛒", 0xFFF59E0B),
    LEISURE_RELAXATION("Leisure & Recovery", "☕", 0xFF6366F1)
}

enum class CognitiveLoad(val label: String, val defaultMinutes: Int) {
    LIGHT("Light", 15),
    MODERATE("Moderate", 30),
    HIGH_FOCUS("High Focus", 60)
}

data class VietnameseNlpAnalysis(
    val category: TaskCategory,
    val urgency: TaskUrgency,
    val cognitiveLoad: CognitiveLoad,
    val estimatedMinutes: Int,
    val isDeepWork: Boolean,
    val optimalFocusWindow: String,
    val microActionStep: String,
    val timingAdvice: String
)

object VietnameseTaskNlpEngine {

    private val SHORTHAND_MAP = mapOf(
        "dc" to "duoc",
        "ko" to "khong",
        "k" to "khong",
        "vs" to "voi",
        "nv" to "nhan vien",
        "kh" to "khach hang",
        "bc" to "bao cao",
        "dl" to "deadline",
        "mt" to "meeting",
        "ck" to "chuyen khoan",
        "tt" to "thanh toan",
        "hd" to "hop dong",
        "cv" to "cong viec",
        "st" to "sieu thi",
        "bv" to "benh vien",
        "gym" to "tap gym",
        "pt" to "phan tich",
        "tk" to "thiet ke"
    )

    private val CRITICAL_KEYWORDS = listOf(
        "gap", "khan", "khan cap", "ngay lap tuc", "asap", "urgent",
        "deadline", "han chot", "nop ngay", "chot so", "quan trong", "critical", "immediately"
    )

    private val URGENT_KEYWORDS = listOf(
        "hom nay", "chieu nay", "toi nay", "sang nay", "som", "nhanh",
        "nhac", "24h", "truoc", "today", "tonight", "this morning", "this afternoon", "soon", "remind"
    )

    private val DEEP_WORK_KEYWORDS = listOf(
        "bao cao", "thiet ke", "lap trinh", "code", "viet", "phan tich",
        "nghien cuu", "ke hoach", "chien luoc", "slide", "thuyet trinh",
        "on thi", "hoc", "doc sach", "luan van", "do an", "debug", "toi uu",
        "architecture", "proposal", "review", "tai lieu", "spec",
        "report", "design", "program", "write", "analyze", "research", "plan",
        "strategy", "presentation", "study", "read", "thesis", "project", "optimize", "document"
    )

    private val COMMUNICATION_KEYWORDS = listOf(
        "hop", "meeting", "goi", "call", "email", "gui mail", "nhan tin",
        "tra loi", "lien he", "phong van", "gap", "thao luan",
        "sync", "standup", "1-1", "khach hang", "doi tac", "mentor",
        "reply", "contact", "interview", "meet", "discuss", "client", "partner", "message"
    )

    private val FINANCE_ADMIN_KEYWORDS = listOf(
        "thanh toan", "chuyen khoan", "dong tien", "hoa don", "dien", "nuoc",
        "internet", "thue", "luong", "ngan hang", "bao hiem", "hop dong",
        "giay to", "thu tuc", "dang ky", "gia han", "the tin dung", "no",
        "pay", "transfer", "bill", "electricity", "water", "tax", "salary",
        "bank", "insurance", "contract", "paperwork", "register", "renew", "credit card"
    )

    private val HEALTH_KEYWORDS = listOf(
        "tap", "gym", "chay bo", "yoga", "the duc", "boi", "dap xe",
        "kham", "bac si", "uong thuoc", "vitamin", "ngu", "thien", "stretching",
        "workout", "run", "exercise", "swim", "cycle", "doctor", "medicine", "sleep", "meditate"
    )

    private val CHORES_KEYWORDS = listOf(
        "mua", "di cho", "sieu thi", "don", "giat", "rua", "nau an",
        "sua", "do rac", "lay do", "ship", "giao hang", "cat toc", "rua xe",
        "buy", "groceries", "supermarket", "clean", "laundry", "wash", "cook",
        "repair", "trash", "pickup", "delivery", "haircut"
    )

    private val LEISURE_KEYWORDS = listOf(
        "cafe", "ca phe", "xem phim", "nghe nhac", "choi", "du lich",
        "nghi ngoi", "thu gian", "massage", "spa", "hen ho", "an toi",
        "coffee", "movie", "music", "play", "travel", "rest", "relax", "date", "dinner"
    )

    fun normalize(text: String): String {
        val lower = text.lowercase().trim()
        val noAccent = removeDiacritics(lower)
        val tokens = noAccent.split(Regex("\\s+"))
        val expanded = tokens.joinToString(" ") { token ->
            SHORTHAND_MAP[token] ?: token
        }
        return expanded
    }

    fun removeDiacritics(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("")
            .replace('\u0111', 'd')
            .replace('\u0110', 'd')
    }

    fun analyze(
        title: String,
        scheduledTimestampMs: Long = System.currentTimeMillis(),
        createdAtMs: Long = System.currentTimeMillis(),
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): VietnameseNlpAnalysis {
        val norm = normalize(title)
        val now = System.currentTimeMillis()
        val hoursUntilDue = (scheduledTimestampMs - now) / (1000.0 * 3600.0)

        val category = when {
            DEEP_WORK_KEYWORDS.any { norm.contains(it) } -> TaskCategory.DEEP_WORK
            COMMUNICATION_KEYWORDS.any { norm.contains(it) } -> TaskCategory.COMMUNICATION
            FINANCE_ADMIN_KEYWORDS.any { norm.contains(it) } -> TaskCategory.FINANCE_ADMIN
            HEALTH_KEYWORDS.any { norm.contains(it) } -> TaskCategory.HEALTH_WELLNESS
            CHORES_KEYWORDS.any { norm.contains(it) } -> TaskCategory.CHORES_ERRANDS
            LEISURE_KEYWORDS.any { norm.contains(it) } -> TaskCategory.LEISURE_RELAXATION
            else -> if (title.trim().split(Regex("\\s+")).size >= 7) {
                TaskCategory.DEEP_WORK
            } else {
                TaskCategory.CHORES_ERRANDS
            }
        }

        val urgency = when {
            CRITICAL_KEYWORDS.any { norm.contains(it) } || (hoursUntilDue in -12.0..2.5) -> TaskUrgency.CRITICAL
            URGENT_KEYWORDS.any { norm.contains(it) } || (hoursUntilDue in 2.5..8.0) -> TaskUrgency.URGENT
            else -> TaskUrgency.NORMAL
        }

        val (cognitiveLoad, baseMins) = when (category) {
            TaskCategory.DEEP_WORK -> CognitiveLoad.HIGH_FOCUS to 60
            TaskCategory.COMMUNICATION -> CognitiveLoad.MODERATE to 25
            TaskCategory.FINANCE_ADMIN -> CognitiveLoad.LIGHT to 15
            TaskCategory.HEALTH_WELLNESS -> CognitiveLoad.MODERATE to 45
            TaskCategory.CHORES_ERRANDS -> CognitiveLoad.LIGHT to 20
            TaskCategory.LEISURE_RELAXATION -> CognitiveLoad.LIGHT to 30
        }

        val wordCountBonus = (title.trim().split(Regex("\\s+")).size / 5) * 5
        val estimatedMinutes = (baseMins + wordCountBonus).coerceIn(10, 90)
        val isDeepWork = (category == TaskCategory.DEEP_WORK)

        val optimalWindow = when (category) {
            TaskCategory.DEEP_WORK -> if (currentHour < 11) "09:00 - 11:30 (Morning Peak)" else "14:00 - 16:30 (Afternoon Focus)"
            TaskCategory.COMMUNICATION -> "10:00 - 11:30 or 15:00 - 17:00"
            TaskCategory.FINANCE_ADMIN -> "11:30 - 12:00 or 16:30 - 17:30"
            TaskCategory.HEALTH_WELLNESS -> "06:00 - 07:30 or 17:30 - 19:00"
            TaskCategory.CHORES_ERRANDS -> "17:30 - 19:30 (After work)"
            TaskCategory.LEISURE_RELAXATION -> "20:00 - 22:00 (Evening relaxation)"
        }

        val microStep = when (category) {
            TaskCategory.DEEP_WORK -> "2-min rule: Open your document, write 3 main bullet points, and turn on Do Not Disturb."
            TaskCategory.COMMUNICATION -> "2-min rule: Jot down the 2 key points to communicate before messaging or calling."
            TaskCategory.FINANCE_ADMIN -> "2-min rule: Open your banking/invoice app and complete it in one go."
            TaskCategory.HEALTH_WELLNESS -> "2-min rule: Put on your workout shoes and drink a small glass of water right now."
            TaskCategory.CHORES_ERRANDS -> "2-min rule: Set a 15-minute timer to tidy up or list items to buy."
            TaskCategory.LEISURE_RELAXATION -> "Put away work screens and fully enjoy your break."
        }

        val timingAdvice = when {
            urgency == TaskUrgency.CRITICAL -> "🔥 High priority: Clear 30 minutes to handle this right away and relieve pressure."
            isDeepWork && currentHour in 8..11 -> "⚡ Morning golden hour: Your brain is sharpest right now—start immediately!"
            isDeepWork && currentHour >= 21 -> "🌙 Late night: Only outline key ideas, save heavy work for tomorrow at 09:00."
            category == TaskCategory.FINANCE_ADMIN -> "💳 Quick 10-15 min task: Knock it out between bigger work blocks."
            else -> "✨ Complete on schedule ($optimalWindow) to maintain your streak."
        }

        return VietnameseNlpAnalysis(
            category = category,
            urgency = urgency,
            cognitiveLoad = cognitiveLoad,
            estimatedMinutes = estimatedMinutes,
            isDeepWork = isDeepWork,
            optimalFocusWindow = optimalWindow,
            microActionStep = microStep,
            timingAdvice = timingAdvice
        )
    }
}
