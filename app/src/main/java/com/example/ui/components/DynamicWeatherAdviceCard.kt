package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.repository.RealWeatherData

/**
 * Model dữ liệu gợi ý đồ dùng / hành động theo thời tiết
 */
data class WeatherAdviceItem(
    val title: String,
    val description: String,
    val actionBadges: List<String>,
    val containerBg: List<Color>,
    val borderColor: Color,
    val textColor: Color,
    val descColor: Color,
    val iconBg: Color,
    val iconTint: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val dismissBg: Color,
    val dismissTint: Color
)

object WeatherAdviceEngine {

    fun generateAdvice(weather: RealWeatherData?, isNight: Boolean, isDark: Boolean): WeatherAdviceItem {
        val temp = weather?.currentTempC?.let { Math.round(it).toInt() } ?: 29
        val code = weather?.weatherCode ?: 0

        return if (!isDark) {
            // === LIGHT THEME (TRANSLUCENT FROSTED GLASS ONE UI 9) ===
            when {
                // 1. Mưa / Dông
                code in 51..67 || code in 80..99 -> {
                    WeatherAdviceItem(
                        title = "Cảnh báo mưa dông",
                        description = "Trời đang/sắp có mưa rào. Bạn nhớ mang theo ô (dù) hoặc áo mưa khi đi ra ngoài nhé!",
                        actionBadges = listOf("☂️ Mang ô/dù", "🧥 Áo mưa", "🛵 Lái xe an toàn"),
                        containerBg = listOf(Color(0xFFE0F2FE).copy(alpha = 0.55f), Color(0xFFBAE6FD).copy(alpha = 0.45f)),
                        borderColor = Color.White.copy(alpha = 0.65f),
                        textColor = Color(0xFF0369A1),
                        descColor = Color(0xFF0C4A6E),
                        iconBg = Color(0xFFDBEAFE).copy(alpha = 0.7f),
                        iconTint = Color(0xFF0284C7),
                        badgeBg = Color(0xFF0284C7).copy(alpha = 0.15f),
                        badgeText = Color(0xFF0369A1),
                        dismissBg = Color(0xFF0C4A6E).copy(alpha = 0.08f),
                        dismissTint = Color(0xFF0369A1)
                    )
                }
                // 2. Nắng gắt / Nóng bức (temp >= 30°C)
                !isNight && temp >= 30 -> {
                    val isDirectSun = code in 0..1
                    WeatherAdviceItem(
                        title = if (isDirectSun) "Nắng gắt • Chỉ số UV cao" else "Trời nóng oi bức • Cần bù nước",
                        description = if (isDirectSun)
                            "Nhiệt độ ngoài trời ~$temp°C. Bạn hãy đeo khẩu trang, đội mũ nón và thoa kem chống nắng khi di chuyển."
                        else
                            "Nhiệt độ ngoài trời ~$temp°C khá oi bức. Hãy bổ sung nước thường xuyên và hạn chế vận động nặng ngoài trời giữa trưa.",
                        actionBadges = if (isDirectSun)
                            listOf("🧢 Đội mũ/nón", "🧴 Thoa kem chống nắng", "💧 Mang bình nước")
                        else
                            listOf("💧 Uống đủ nước", "🥤 Bổ sung khoáng", "🌳 Tránh nắng gắt"),
                        containerBg = listOf(Color(0xFFFFF7ED).copy(alpha = 0.55f), Color(0xFFFFEDD5).copy(alpha = 0.45f)),
                        borderColor = Color.White.copy(alpha = 0.65f),
                        textColor = Color(0xFF9A3412),
                        descColor = Color(0xFF7C2D12),
                        iconBg = Color(0xFFFEF3C7).copy(alpha = 0.7f),
                        iconTint = Color(0xFFD97706),
                        badgeBg = Color(0xFFEA580C).copy(alpha = 0.15f),
                        badgeText = Color(0xFF9A3412),
                        dismissBg = Color(0xFF7C2D12).copy(alpha = 0.08f),
                        dismissTint = Color(0xFF9A3412)
                    )
                }
                // 3. Ban đêm
                isNight -> {
                    WeatherAdviceItem(
                        title = "Thời tiết về đêm • Dịu mát",
                        description = "Nhiệt độ hạ dần ~$temp°C. Khoác nhẹ một chiếc áo mỏng nếu bạn có lịch trình đi ra ngoài muộn nhé.",
                        actionBadges = listOf("🧥 Áo khoác mỏng", "🍵 Trà ấm", "🌙 Nghỉ ngơi sớm"),
                        containerBg = listOf(Color(0xFFF5F3FF).copy(alpha = 0.55f), Color(0xFFEDE9FE).copy(alpha = 0.45f)),
                        borderColor = Color.White.copy(alpha = 0.65f),
                        textColor = Color(0xFF3730A3),
                        descColor = Color(0xFF1E1B4B),
                        iconBg = Color(0xFFE0E7FF).copy(alpha = 0.7f),
                        iconTint = Color(0xFF4F46E5),
                        badgeBg = Color(0xFF6366F1).copy(alpha = 0.15f),
                        badgeText = Color(0xFF3730A3),
                        dismissBg = Color(0xFF1E1B4B).copy(alpha = 0.08f),
                        dismissTint = Color(0xFF3730A3)
                    )
                }
                // 4. Trời se lạnh (temp <= 22°C)
                temp <= 22 -> {
                    WeatherAdviceItem(
                        title = "Thời tiết se lạnh",
                        description = "Nhiệt độ ngoài trời ~$temp°C se lạnh. Hãy giữ ấm cơ thể khi ra ngoài để bảo vệ sức khỏe.",
                        actionBadges = listOf("🧥 Áo khoác ấm", "🧣 Giữ ấm", "🍵 Nước ấm"),
                        containerBg = listOf(Color(0xFFF0F9FF).copy(alpha = 0.55f), Color(0xFFE0F2FE).copy(alpha = 0.45f)),
                        borderColor = Color.White.copy(alpha = 0.65f),
                        textColor = Color(0xFF0369A1),
                        descColor = Color(0xFF0C4A6E),
                        iconBg = Color(0xFFBAE6FD).copy(alpha = 0.7f),
                        iconTint = Color(0xFF0284C7),
                        badgeBg = Color(0xFF0284C7).copy(alpha = 0.15f),
                        badgeText = Color(0xFF0369A1),
                        dismissBg = Color(0xFF0C4A6E).copy(alpha = 0.08f),
                        dismissTint = Color(0xFF0369A1)
                    )
                }
                // 5. Mát mẻ / Thời tiết lý tưởng (23°C - 29°C)
                else -> {
                    WeatherAdviceItem(
                        title = "Thời tiết lý tưởng",
                        description = "Thời tiết hôm nay ~$temp°C rất dịu mát và dễ chịu. Rất thích hợp cho các hoạt động ngoài trời hoặc đi dạo.",
                        actionBadges = listOf("🏃 Vận động ngoài trời", "☕ Đi dạo / Cà phê", "🌿 Hít thở không khí"),
                        containerBg = listOf(Color(0xFFECFDF5).copy(alpha = 0.55f), Color(0xFFD1FAE5).copy(alpha = 0.45f)),
                        borderColor = Color.White.copy(alpha = 0.65f),
                        textColor = Color(0xFF065F46),
                        descColor = Color(0xFF064E3B),
                        iconBg = Color(0xFFD1FAE5).copy(alpha = 0.7f),
                        iconTint = Color(0xFF059669),
                        badgeBg = Color(0xFF10B981).copy(alpha = 0.15f),
                        badgeText = Color(0xFF065F46),
                        dismissBg = Color(0xFF064E3B).copy(alpha = 0.08f),
                        dismissTint = Color(0xFF065F46)
                    )
                }
            }
        } else {
            // === DARK THEME (TRANSLUCENT DARK FROSTED GLASS ONE UI 9) ===
            when {
                code in 51..67 || code in 80..99 -> {
                    WeatherAdviceItem(
                        title = "Cảnh báo mưa dông",
                        description = "Trời đang/sắp có mưa rào. Bạn nhớ mang theo ô (dù) hoặc áo mưa khi đi ra ngoài nhé!",
                        actionBadges = listOf("☂️ Mang ô/dù", "🧥 Áo mưa", "🛵 Lái xe an toàn"),
                        containerBg = listOf(Color(0xFF1E3A8A).copy(alpha = 0.55f), Color(0xFF1E293B).copy(alpha = 0.65f)),
                        borderColor = Color.White.copy(alpha = 0.22f),
                        textColor = Color.White,
                        descColor = Color.White.copy(alpha = 0.92f),
                        iconBg = Color.White.copy(alpha = 0.18f),
                        iconTint = Color(0xFF60A5FA),
                        badgeBg = Color(0xFF3B82F6).copy(alpha = 0.28f),
                        badgeText = Color(0xFF93C5FD),
                        dismissBg = Color.White.copy(alpha = 0.12f),
                        dismissTint = Color.White.copy(alpha = 0.8f)
                    )
                }
                !isNight && temp >= 30 -> {
                    val isDirectSun = code in 0..1
                    WeatherAdviceItem(
                        title = if (isDirectSun) "Nắng gắt • Chỉ số UV cao" else "Trời nóng oi bức • Cần bù nước",
                        description = if (isDirectSun)
                            "Nhiệt độ ngoài trời ~$temp°C. Bạn hãy đeo khẩu trang, đội mũ nón và thoa kem chống nắng khi di chuyển."
                        else
                            "Nhiệt độ ngoài trời ~$temp°C khá oi bức. Hãy bổ sung nước thường xuyên và hạn chế vận động nặng ngoài trời giữa trưa.",
                        actionBadges = if (isDirectSun)
                            listOf("🧢 Đội mũ/nón", "🧴 Thoa kem chống nắng", "💧 Mang bình nước")
                        else
                            listOf("💧 Uống đủ nước", "🥤 Bổ sung khoáng", "🌳 Tránh nắng gắt"),
                        containerBg = listOf(Color(0xFF7C2D12).copy(alpha = 0.55f), Color(0xFF1E293B).copy(alpha = 0.65f)),
                        borderColor = Color.White.copy(alpha = 0.22f),
                        textColor = Color.White,
                        descColor = Color.White.copy(alpha = 0.92f),
                        iconBg = Color.White.copy(alpha = 0.18f),
                        iconTint = Color(0xFFFB923C),
                        badgeBg = Color(0xFFEA580C).copy(alpha = 0.28f),
                        badgeText = Color(0xFFFDBA74),
                        dismissBg = Color.White.copy(alpha = 0.12f),
                        dismissTint = Color.White.copy(alpha = 0.8f)
                    )
                }
                isNight -> {
                    WeatherAdviceItem(
                        title = "Thời tiết về đêm • Dịu mát",
                        description = "Nhiệt độ hạ dần ~$temp°C. Khoác nhẹ một chiếc áo mỏng nếu bạn có lịch trình đi ra ngoài muộn nhé.",
                        actionBadges = listOf("🧥 Áo khoác mỏng", "🍵 Trà ấm", "🌙 Nghỉ ngơi sớm"),
                        containerBg = listOf(Color(0xFF312E81).copy(alpha = 0.55f), Color(0xFF0F172A).copy(alpha = 0.65f)),
                        borderColor = Color.White.copy(alpha = 0.22f),
                        textColor = Color.White,
                        descColor = Color.White.copy(alpha = 0.92f),
                        iconBg = Color.White.copy(alpha = 0.18f),
                        iconTint = Color(0xFFA5B4FC),
                        badgeBg = Color(0xFF6366F1).copy(alpha = 0.28f),
                        badgeText = Color(0xFFC7D2FE),
                        dismissBg = Color.White.copy(alpha = 0.12f),
                        dismissTint = Color.White.copy(alpha = 0.8f)
                    )
                }
                temp <= 22 -> {
                    WeatherAdviceItem(
                        title = "Thời tiết se lạnh",
                        description = "Nhiệt độ ngoài trời ~$temp°C se lạnh. Hãy giữ ấm cơ thể khi ra ngoài để bảo vệ sức khỏe.",
                        actionBadges = listOf("🧥 Áo khoác ấm", "🧣 Giữ ấm", "🍵 Nước ấm"),
                        containerBg = listOf(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color(0xFF0F172A).copy(alpha = 0.65f)),
                        borderColor = Color.White.copy(alpha = 0.22f),
                        textColor = Color.White,
                        descColor = Color.White.copy(alpha = 0.92f),
                        iconBg = Color.White.copy(alpha = 0.18f),
                        iconTint = Color(0xFF60A5FA),
                        badgeBg = Color(0xFF3B82F6).copy(alpha = 0.28f),
                        badgeText = Color(0xFF93C5FD),
                        dismissBg = Color.White.copy(alpha = 0.12f),
                        dismissTint = Color.White.copy(alpha = 0.8f)
                    )
                }
                else -> {
                    WeatherAdviceItem(
                        title = "Thời tiết lý tưởng",
                        description = "Thời tiết hôm nay ~$temp°C rất dịu mát và dễ chịu. Rất thích hợp cho các hoạt động ngoài trời hoặc đi dạo.",
                        actionBadges = listOf("🏃 Vận động ngoài trời", "☕ Đi dạo / Cà phê", "🌿 Hít thở không khí"),
                        containerBg = listOf(Color(0xFF065F46).copy(alpha = 0.55f), Color(0xFF0F172A).copy(alpha = 0.65f)),
                        borderColor = Color.White.copy(alpha = 0.22f),
                        textColor = Color.White,
                        descColor = Color.White.copy(alpha = 0.92f),
                        iconBg = Color.White.copy(alpha = 0.18f),
                        iconTint = Color(0xFF6EE7B7),
                        badgeBg = Color(0xFF10B981).copy(alpha = 0.28f),
                        badgeText = Color(0xFFA7F3D0),
                        dismissBg = Color.White.copy(alpha = 0.12f),
                        dismissTint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Card Nhắc nhở Thời tiết Động với hiệu ứng Kính mờ Trong suốt Glassmorphism
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicWeatherAdviceCard(
    weather: RealWeatherData?,
    isNight: Boolean,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var checkedBadges by remember { mutableStateOf(setOf<String>()) }

    val advice = remember(weather, isNight, isDark) {
        WeatherAdviceEngine.generateAdvice(weather, isNight, isDark)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, advice.borderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(advice.containerBg),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row: Badge Title + Dismiss Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(advice.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = advice.iconTint,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = advice.title,
                                color = advice.textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Dismiss Icon
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(advice.dismissBg)
                                .clickable { isVisible = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ẩn thẻ",
                                tint = advice.dismissTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hero Row: Text Content + Floating Weather Sticker (68dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = advice.description,
                                color = advice.descColor,
                                fontSize = 13.5.sp,
                                lineHeight = 19.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // High-Res Hero Weather Sticker với Nhún nhảy Floating Anim
                        WeatherStickerImage(
                            weatherCode = weather?.weatherCode ?: 0,
                            isNight = isNight,
                            size = 68.dp,
                            animate = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Badges Row (Tương tác tích chọn vật dụng/hành động đã chuẩn bị)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        advice.actionBadges.forEach { badge ->
                            val isChecked = checkedBadges.contains(badge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isChecked) advice.badgeBg.copy(alpha = 0.5f) else advice.badgeBg
                                    )
                                    .clickable {
                                        checkedBadges = if (isChecked) {
                                            checkedBadges - badge
                                        } else {
                                            checkedBadges + badge
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = advice.badgeText,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(end = 4.dp)
                                    )
                                }

                                Text(
                                    text = if (isChecked) "$badge (Đã chuẩn bị)" else badge,
                                    color = advice.badgeText,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
