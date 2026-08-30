package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.ActionType
import com.example.core.engine.ContextActionCard
import com.example.core.engine.UserFeedbackTracker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Card Lịch trình Ngữ cảnh Thông minh chuẩn Samsung One UI & Glassmorphism
 * Đã tinh chỉnh phối màu mờ nhạt, độ tương phản mượt mà & cụm Feedback dạng Nút tròn/Menu
 */
@Composable
fun CrossContextAlertCard(
    card: ContextActionCard,
    isDark: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val feedbackTracker = remember { UserFeedbackTracker(context) }
    var isDismissed by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }

    if (isDismissed) return

    // Glassmorphism Frosted Background
    val cardBgColor = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val strokeColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }

    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    // Tag mờ nhạt dịu mắt (rgba 0.08 opacity)
    val mutedTagBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)
    val mutedTagText = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)
    val scoreText = "★ ${String.format("%.1f", card.calculatedScore)}"

    val (timeStatusText, isUrgentDot) = remember(card) { formatCardTimeStatus(card) }
    val hasAction = card.actionLabel != null || card.actionType != null || card.meetUrl != null || card.location.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBgColor)
            .border(1.dp, strokeColor, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // -------------------------------------------------------------
            // [HEADER HÀNG TRÊN]: Icon Lịch + Nguồn/Tag + Score Tối giản + Feedback Nút Tròn
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                        modifier = Modifier.size(17.dp)
                    )

                    // Tag mờ nhạt tối giản: "Ngữ cảnh thông minh" / Nguồn
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(mutedTagBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = card.subtitle.ifBlank { "Ngữ cảnh thông minh" },
                            color = mutedTagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Score tối giản dạng Text mờ (ví dụ: ★ 8.5)
                    Text(
                        text = scoreText,
                        color = subtitleColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Cụm Feedback Nút Tròn Nhỏ gọn (Circular Surface)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(mutedTagBg)
                            .clickable {
                                isLiked = !isLiked
                                if (isLiked) {
                                    feedbackTracker.recordPositiveFeedback(card.category)
                                    Toast.makeText(context, "Đã ghi nhận gợi ý hữu ích ✨", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Thích",
                            tint = if (isLiked) Color(0xFF10B981) else subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(mutedTagBg)
                            .clickable {
                                isDismissed = true
                                feedbackTracker.recordNegativeFeedback(card.category)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Ẩn",
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -------------------------------------------------------------
            // [BODY Ở GIỮA]: Tiêu đề + Thời gian/Trạng thái + Địa điểm
            // -------------------------------------------------------------
            Text(
                text = card.title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dòng Thời gian & Trạng thái (Text mờ rgba 0.6)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Thời gian",
                    tint = subtitleColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = timeStatusText,
                    color = subtitleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Chấm cam khẩn cấp nếu có
                if (isUrgentDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF97316))
                    )
                }

                // Địa điểm (nếu có)
                if (card.location.isNotBlank()) {
                    Text(
                        text = "•",
                        color = subtitleColor,
                        fontSize = 13.sp
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Địa điểm",
                        tint = subtitleColor,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = card.location,
                        color = subtitleColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // -------------------------------------------------------------
            // [FOOTER HÀNG DƯỚI - ACTION SHORTCUT]: Nút bấm Pill (16dp)
            // -------------------------------------------------------------
            if (hasAction) {
                Spacer(modifier = Modifier.height(12.dp))

                val isMeet = card.actionType == ActionType.JOIN_MEET || card.meetUrl != null
                val isMaps = card.actionType == ActionType.OPEN_MAPS || card.location.isNotBlank()

                val buttonLabel = card.actionLabel ?: when {
                    isMeet -> "Tham gia Google Meet"
                    isMaps -> "Chỉ đường Maps"
                    else -> "Mở chi tiết"
                }

                val buttonIcon = when {
                    isMeet -> Icons.Default.Videocam
                    isMaps -> Icons.Default.Map
                    else -> Icons.Default.NorthEast
                }

                val buttonBg = if (isMeet) {
                    Color(0xFF2563EB) // Primary Blue
                } else if (isDark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color(0xFFF1F5F9)
                }

                val buttonTextColor = if (isMeet || isDark) Color.White else Color(0xFF0F172A)

                Button(
                    onClick = {
                        feedbackTracker.recordPositiveFeedback(card.category)
                        executeAction(context, card)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonTextColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatCardTimeStatus(card: ContextActionCard): Pair<String, Boolean> {
    if (card.isAllDay) {
        return Pair("Cả ngày", false)
    }

    val now = System.currentTimeMillis()
    val startMs = card.rawTimeMillis
    val endMs = card.endTimeMillis

    val calNow = Calendar.getInstance().apply { timeInMillis = now }
    val calStart = Calendar.getInstance().apply { timeInMillis = startMs }

    val isSameDay = calNow.get(Calendar.YEAR) == calStart.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calStart.get(Calendar.DAY_OF_YEAR)

    val isTomorrow = calNow.get(Calendar.YEAR) == calStart.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) + 1 == calStart.get(Calendar.DAY_OF_YEAR)

    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startMs))

    return when {
        now in startMs..endMs -> Pair("Đang diễn ra ($timeStr)", true)
        isSameDay -> {
            val deltaMin = (startMs - now) / (60 * 1000)
            if (deltaMin in 0..60) {
                Pair("$timeStr (Còn $deltaMin phút)", true)
            } else if (deltaMin > 60) {
                val hours = deltaMin / 60
                val mins = deltaMin % 60
                Pair("$timeStr (Còn ${hours}h${mins}p)", false)
            } else {
                Pair("Hôm nay, $timeStr", false)
            }
        }
        isTomorrow -> Pair("Ngày mai, $timeStr", false)
        else -> {
            val dateStr = SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault()).format(Date(startMs))
            Pair(dateStr, false)
        }
    }
}

private fun executeAction(context: Context, card: ContextActionCard) {
    try {
        if (card.actionIntent != null) {
            context.startActivity(card.actionIntent)
        } else if (card.meetUrl != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(card.meetUrl)))
        } else if (card.location.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(card.location)}")))
        } else {
            Toast.makeText(context, "Mở chi tiết: ${card.title}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("CrossContextAlertCard", "Lỗi thực thi action: ${e.message}")
        Toast.makeText(context, "Không thể mở ứng dụng tương ứng", Toast.LENGTH_SHORT).show()
    }
}
