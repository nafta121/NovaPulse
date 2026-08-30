package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * 4 Diurnal Phases according to Samsung Now Brief Specification:
 * 1. Sáng (05h - 11h): Cyan -> Xanh Lam (#00E5FF -> #0091EA)
 * 2. Trưa/Chiều (11h - 17h): Vàng Cam -> Xanh Ngọc (#FFD54F -> #26A69A)
 * 3. Tối (17h - 22h): Hồng Hoàng Hôn -> Tím Pastel (#FF80AB -> #AB47BC)
 * 4. Đêm (22h - 05h): Tím Đậm -> Xanh Navy (#7C4DFF -> #303F9F)
 */
enum class DiurnalPhase(
    val titleVi: String,
    val greeting: String,
    val subtitle: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val secondaryColor: Color,
    val titleTextColor: Color,
    val subtitleTextColor: Color,
    val bodyTextColor: Color,
    val cardBgColor: Color,
    val cardBorderColor: Color,
    val bgGradientColors: List<Color>
) {
    MORNING(
        titleVi = "Bình minh",
        greeting = "Chào buổi sáng",
        subtitle = "Bắt đầu ngày mới tràn đầy năng lượng.",
        isDark = false,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF0091EA),
        titleTextColor = Color(0xFF0F172A),
        subtitleTextColor = Color(0xFF475569),
        bodyTextColor = Color(0xFF334155),
        cardBgColor = Color.White.copy(alpha = 0.7f),
        cardBorderColor = Color.White.copy(alpha = 0.4f),
        bgGradientColors = listOf(
            Color(0xFFE0F7FA),
            Color(0xFFFFF9C4)
        )
    ),
    AFTERNOON(
        titleVi = "Ban ngày",
        greeting = "Chào buổi chiều",
        subtitle = "Chúc bạn một buổi chiều làm việc hiệu quả.",
        isDark = false,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFF26A69A),
        titleTextColor = Color(0xFF0F172A),
        subtitleTextColor = Color(0xFF475569),
        bodyTextColor = Color(0xFF334155),
        cardBgColor = Color.White.copy(alpha = 0.7f),
        cardBorderColor = Color.White.copy(alpha = 0.4f),
        bgGradientColors = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFE8F5E9)
        )
    ),
    EVENING(
        titleVi = "Hoàng hôn",
        greeting = "Hoàng hôn",
        subtitle = "Thư giãn và tận hưởng ánh chiều tà.",
        isDark = false,
        primaryColor = Color(0xFFFF80AB),
        secondaryColor = Color(0xFFAB47BC),
        titleTextColor = Color(0xFF0F172A),
        subtitleTextColor = Color(0xFF475569),
        bodyTextColor = Color(0xFF334155),
        cardBgColor = Color.White.copy(alpha = 0.7f),
        cardBorderColor = Color.White.copy(alpha = 0.4f),
        bgGradientColors = listOf(
            Color(0xFFFFE0B2),
            Color(0xFFE1BEE7)
        )
    ),
    NIGHT(
        titleVi = "Buổi tối",
        greeting = "Chào buổi tối",
        subtitle = "Thư giãn & nạp lại năng lượng cho ngày mai.",
        isDark = true,
        primaryColor = Color(0xFF7C4DFF),
        secondaryColor = Color(0xFF303F9F),
        titleTextColor = Color(0xFFF8FAFC),
        subtitleTextColor = Color(0xFF94A3B8),
        bodyTextColor = Color(0xFFCBD5E1),
        cardBgColor = Color(0xFF111827).copy(alpha = 0.75f),
        cardBorderColor = Color.White.copy(alpha = 0.12f),
        bgGradientColors = listOf(
            Color(0xFF0F172A),
            Color(0xFF111827)
        )
    );

    fun getIconBrush(): Brush {
        return Brush.linearGradient(listOf(primaryColor, secondaryColor))
    }

    fun getBackgroundBrush(): Brush {
        return Brush.verticalGradient(bgGradientColors)
    }

    companion object {
        fun getCurrentHour(): Int {
            return try {
                java.time.LocalTime.now().hour
            } catch (e: Throwable) {
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            }
        }

        fun getCurrentMinute(): Int {
            return try {
                java.time.LocalTime.now().minute
            } catch (e: Throwable) {
                Calendar.getInstance().get(Calendar.MINUTE)
            }
        }

        /** Đồng bộ chính xác theo 7 mốc của BriefingSession (giống widget) */
        fun fromHourAndMinute(hour: Int, minute: Int = 0): DiurnalPhase {
            val totalMins = hour * 60 + minute
            return when {
                totalMins in (5 * 60)..(19 * 60 + 29) -> when {
                    totalMins < (11 * 60 + 30) -> MORNING    // 05:00 - 11:29 (Bình minh + Buổi sáng)
                    totalMins < (17 * 60) -> AFTERNOON       // 11:30 - 16:59 (Trưa + Chiều)
                    else -> EVENING                          // 17:00 - 19:29 (Hoàng hôn)
                }
                else -> NIGHT                               // 19:30 - 04:59 (Tối + Đêm muộn)
            }
        }

        fun fromHour(hour: Int): DiurnalPhase = fromHourAndMinute(hour, 0)

        fun current(): DiurnalPhase {
            return fromHourAndMinute(getCurrentHour(), getCurrentMinute())
        }
    }
}

/**
 * Dynamic Sunrise/Horizon Icon (Phong cách Samsung Now Brief)
 * - Nền: Squircle / Bo tròn đổ Gradient theo DiurnalPhase
 * - Nghệ thuật bên trong: Hình bán nguyệt mặt trời mọc/lặn ở đường chân trời + 2 vạch sóng nước uốn lượn
 */
@Composable
fun DynamicHorizonIcon(
    modifier: Modifier = Modifier,
    phase: DiurnalPhase = DiurnalPhase.current(),
    size: Dp = 48.dp,
    shapeRadius: Dp = 14.dp,
    elevation: Dp = 3.dp
) {
    val animatedColor1 by animateColorAsState(
        targetValue = phase.primaryColor,
        animationSpec = tween(600),
        label = "horizon_c1"
    )
    val animatedColor2 by animateColorAsState(
        targetValue = phase.secondaryColor,
        animationSpec = tween(600),
        label = "horizon_c2"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation, RoundedCornerShape(shapeRadius))
            .clip(RoundedCornerShape(shapeRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(animatedColor1, animatedColor2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height

            // Semicircle Sun Rising on Horizon
            val sunRadius = w * 0.28f
            val sunCenterY = h * 0.45f
            val sunRectTop = sunCenterY - sunRadius

            // Draw Sun Semicircle (upper half)
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.5f - sunRadius, sunRectTop),
                size = Size(sunRadius * 2f, sunRadius * 2f),
                style = Fill
            )

            // Horizon Base Bar (Top Horizontal Pill Capsule)
            val strokeW1 = (w * 0.085f).coerceAtLeast(3f)
            val bar1Y = h * 0.45f
            drawLine(
                color = Color.White,
                start = Offset(w * 0.12f, bar1Y),
                end = Offset(w * 0.88f, bar1Y),
                strokeWidth = strokeW1,
                cap = StrokeCap.Round
            )

            // Middle Parallel Flat Horizontal Capsule Bar
            val strokeW2 = (w * 0.075f).coerceAtLeast(2.5f)
            val bar2Y = h * 0.62f
            drawLine(
                color = Color.White,
                start = Offset(w * 0.22f, bar2Y),
                end = Offset(w * 0.78f, bar2Y),
                strokeWidth = strokeW2,
                cap = StrokeCap.Round
            )

            // Bottom Parallel Flat Horizontal Capsule Bar (Shorter)
            val strokeW3 = (w * 0.075f).coerceAtLeast(2.5f)
            val bar3Y = h * 0.78f
            drawLine(
                color = Color.White,
                start = Offset(w * 0.34f, bar3Y),
                end = Offset(w * 0.66f, bar3Y),
                strokeWidth = strokeW3,
                cap = StrokeCap.Round
            )
        }
    }
}
