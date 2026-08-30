package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MoodOption(val emoji: String, val label: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoodTrackerBottomSheet(
    onDismiss: () -> Unit,
    onSaveMood: (MoodOption, List<String>) -> Unit,
    sheetState: SheetState,
    isNight: Boolean = false
) {
    val moodOptions = remember {
        listOf(
            MoodOption("😄", "Rất tốt", Color(0xFF10B981)),
            MoodOption("🙂", "Tốt", Color(0xFF3B82F6)),
            MoodOption("😐", "Bình thường", Color(0xFFF59E0B)),
            MoodOption("🥱", "Mệt mỏi", Color(0xFF8B5CF6)),
            MoodOption("😫", "Căng thẳng", Color(0xFFEF4444))
        )
    }

    val availableTags = remember {
        listOf("Công việc", "Học tập", "Tình cảm", "Sức khỏe", "Thời tiết")
    }

    var selectedMood by remember { mutableStateOf(moodOptions[1]) }
    val selectedTags = remember { mutableStateListOf<String>("Công việc") }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isNight) Color(0xFF0F172A) else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = null,
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ghi nhận tâm trạng",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Bạn cảm thấy thế nào hôm nay?",
                            fontSize = 12.sp,
                            color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5 Mood Emoji Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                moodOptions.forEach { option ->
                    val isSelected = option == selectedMood
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "mood_emoji_scale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedMood = option
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(scaleAnim)
                                .clip(CircleShape)
                                .background(if (isSelected) option.color.copy(alpha = 0.20f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) option.color else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = option.emoji, fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = option.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) option.color else (if (isNight) Color(0xFFCBD5E1) else Color(0xFF64748B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Yếu tố ảnh hưởng chính:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNight) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tags FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTags.forEach { tag ->
                    val isTagSelected = selectedTags.contains(tag)
                    FilterChip(
                        selected = isTagSelected,
                        onClick = {
                            if (isTagSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text(text = tag, fontSize = 12.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedMood.color.copy(alpha = 0.20f),
                            selectedLabelColor = selectedMood.color
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Mood Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveMood(selectedMood, selectedTags.toList())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = selectedMood.color),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Lưu tâm trạng (${selectedMood.emoji} ${selectedMood.label})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
