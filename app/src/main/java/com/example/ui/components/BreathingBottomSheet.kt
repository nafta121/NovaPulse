package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class BreathingPhase(val label: String, val seconds: Int, val targetScale: Float, val color: Color) {
    INHALE("Hít vào từ từ (4s)", 4, 1.0f, Color(0xFF06B6D4)),
    HOLD("Giữ nín hơi (7s)", 7, 1.0f, Color(0xFF3B82F6)),
    EXHALE("Thở ra chậm rãi (8s)", 8, 0.45f, Color(0xFF8B5CF6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    isNight: Boolean = false
) {
    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var currentCycle by remember { mutableIntStateOf(1) }
    var totalCycles by remember { mutableIntStateOf(4) }
    var secondsLeftInPhase by remember { mutableIntStateOf(BreathingPhase.INHALE.seconds) }
    var isFinished by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // Animated Circle Scale & Color
    val circleScale by animateFloatAsState(
        targetValue = if (isRunning && !isFinished) currentPhase.targetScale else 0.5f,
        animationSpec = tween(
            durationMillis = currentPhase.seconds * 1000,
            easing = if (currentPhase == BreathingPhase.HOLD) LinearEasing else FastOutSlowInEasing
        ),
        label = "breathing_circle_scale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isFinished) Color(0xFF10B981) else currentPhase.color,
        animationSpec = tween(800),
        label = "breathing_color"
    )

    // Breathing Timer Loop
    LaunchedEffect(isRunning, currentPhase, isFinished) {
        if (isRunning && !isFinished) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            secondsLeftInPhase = currentPhase.seconds

            while (secondsLeftInPhase > 0) {
                delay(1000L)
                secondsLeftInPhase--
            }

            // Phase Transition
            when (currentPhase) {
                BreathingPhase.INHALE -> currentPhase = BreathingPhase.HOLD
                BreathingPhase.HOLD -> currentPhase = BreathingPhase.EXHALE
                BreathingPhase.EXHALE -> {
                    if (currentCycle < totalCycles) {
                        currentCycle++
                        currentPhase = BreathingPhase.INHALE
                    } else {
                        isFinished = true
                        isRunning = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isNight) Color(0xFF0F172A) else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title + Close Button
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
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tập thở 4-7-8",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNight) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Thả lỏng nhịp tim & xua tan căng thẳng",
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

            Spacer(modifier = Modifier.height(32.dp))

            // Canvas Animation Vòng Tròn Thở 4-7-8 (Size 220dp)
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Glow Circle
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.15f),
                        radius = (size.minDimension / 2f) * circleScale
                    )
                    drawCircle(
                        color = ringColor.copy(alpha = 0.35f),
                        radius = (size.minDimension / 2.3f) * circleScale
                    )
                }

                // Inner Main Circle
                Box(
                    modifier = Modifier
                        .size((160 * circleScale).dp.coerceAtLeast(70.dp))
                        .clip(CircleShape)
                        .background(ringColor)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFinished) "Hoàn thành!" else "${secondsLeftInPhase}s",
                        fontSize = if (isFinished) 16.sp else 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Subtitle phase & Cycle counter
            Text(
                text = if (isFinished) "Bạn đã hoàn thành 4 chu kỳ thở thả lỏng 🌟" else currentPhase.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFinished) "Tâm trí bạn đã dịu lại và nhịp tim ổn định hơn." else "Chu kỳ $currentCycle/$totalCycles",
                fontSize = 13.sp,
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Control Buttons (Start / Pause / Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFinished) {
                    Button(
                        onClick = {
                            isFinished = false
                            currentCycle = 1
                            currentPhase = BreathingPhase.INHALE
                            isRunning = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tập lại", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF06B6D4)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Tạm dừng" else "Bắt đầu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
