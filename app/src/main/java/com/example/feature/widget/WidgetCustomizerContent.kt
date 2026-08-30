package com.example.feature.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizerContent(
    initialTransparency: Float,
    initialColorPreset: String,
    initialShape: String,
    initialShowSubtext: Boolean,
    onSaveConfig: (Float, String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var transparency by rememberSaveable { mutableFloatStateOf(initialTransparency) }
    var colorPreset by rememberSaveable { mutableStateOf(initialColorPreset) }
    var shape by rememberSaveable { mutableStateOf(initialShape) }
    var showSubtext by rememberSaveable { mutableStateOf(initialShowSubtext) }
    var isSavedSuccess by remember { mutableStateOf(false) }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val currentSession = remember(currentHour) { NowBarSession.fromHour(currentHour) }

    // Compute preview background gradient & colors
    val (previewBrush, previewTitleColor, previewSubtitleColor, borderStrokeColor) = remember(
        colorPreset,
        currentHour,
        transparency
    ) {
        when (colorPreset) {
            "DARK_GLASS" -> Quadruple(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F172A).copy(alpha = transparency * 0.95f),
                        Color(0xFF1E293B).copy(alpha = transparency * 0.90f)
                    )
                ),
                Color.White,
                Color.White.copy(alpha = 0.80f),
                Color.White.copy(alpha = 0.30f * transparency)
            )

            "LIGHT_GLASS" -> Quadruple(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFFFFF).copy(alpha = transparency * 0.95f),
                        Color(0xFFF1F5F9).copy(alpha = transparency * 0.90f)
                    )
                ),
                Color(0xFF0F172A),
                Color(0xFF475569),
                Color(0xFFCBD5E1).copy(alpha = 0.60f * transparency)
            )

            "MATERIAL_YOU" -> Quadruple(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1E3A8A).copy(alpha = transparency),
                        Color(0xFF2563EB).copy(alpha = transparency),
                        Color(0xFF60A5FA).copy(alpha = transparency)
                    )
                ),
                Color.White,
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.35f * transparency)
            )

            else -> { // "AUTO" (Time-based auto spectrum from WidgetThemeHelper)
                val timeTheme = WidgetThemeHelper.getThemeForCurrentTime(currentHour)
                val gradientColors = timeTheme.getColorsWithOpacity(transparency)
                Quadruple(
                    Brush.horizontalGradient(gradientColors),
                    timeTheme.titleTextColor,
                    timeTheme.subtitleTextColor,
                    Color.White.copy(alpha = 0.30f * transparency)
                )
            }
        }
    }

    val previewCornerRadius = if (shape == "SQUIRCLE") 24.dp else 100.dp

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------
        // 1. LIVE PREVIEW CARD (Mô phỏng chân thực Home Screen Wallpaper)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF312E81),
                                Color(0xFF4338CA),
                                Color(0xFF6366F1)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vibrant Wallpaper Pattern Blobs (to demonstrate real-time glass transparency)
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = Color(0xFFEC4899).copy(alpha = 0.45f),
                        radius = size.width * 0.28f,
                        center = Offset(size.width * 0.25f, size.height * 0.5f)
                    )
                    drawCircle(
                        color = Color(0xFF06B6D4).copy(alpha = 0.45f),
                        radius = size.width * 0.26f,
                        center = Offset(size.width * 0.75f, size.height * 0.5f)
                    )
                }

                // Background Simulated Desktop Header Label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mô phỏng Widget trên màn hình chính",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Live Glass Preview",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                // WIDGET LIVE PREVIEW CAPSULE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(previewCornerRadius),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(previewCornerRadius))
                        .background(previewBrush)
                        .border(
                            BorderStroke(1.2.dp, borderStrokeColor),
                            RoundedCornerShape(previewCornerRadius)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon tròn 32dp phủ Gradient từ top đến bottom
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(currentSession.iconGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_capsule_horizon),
                                contentDescription = "Session Icon",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (showSubtext) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentSession.title,
                                    color = previewTitleColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = currentSession.fallbackText,
                                    color = previewSubtitleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = currentSession.title,
                                color = previewTitleColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. TRANSPARENCY / BLUR SLIDER (20% - 100%)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Độ trong suốt nền (Transparency)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = "Mức độ xuyên thấu hình nền (15% trong suốt - 100% đặc)",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Text(
                            text = "${(transparency * 100).toInt()}%",
                            color = Color(0xFF7C3AED),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Slider(
                    value = transparency,
                    onValueChange = {
                        transparency = it
                        isSavedSuccess = false
                    },
                    onValueChangeFinished = {
                        onSaveConfig(transparency, colorPreset, shape, showSubtext)
                    },
                    valueRange = 0.15f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C3AED),
                        activeTrackColor = Color(0xFF7C3AED),
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 3. COLOR PRESET PICKER
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Chủ đề màu nền (Color Presets)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp
                            )
                        )
                        Text(
                            text = "Bộ phối màu gradient tự động hoặc kính đơn sắc",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                val gridPresets = listOf(
                    GridPresetOption("AUTO", "Tự động theo giờ", "🕒", listOf(Color(0xFF00C6FF), Color(0xFFDA22FF))),
                    GridPresetOption("DARK_GLASS", "Kính tối Minimal", "🌑", listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
                    GridPresetOption("LIGHT_GLASS", "Kính sáng Minimal", "⚪", listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))),
                    GridPresetOption("MATERIAL_YOU", "Material You", "🎨", listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)))
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gridPresets.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = colorPreset == preset.key
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                            ),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            colorPreset = preset.key
                                            isSavedSuccess = false
                                            onSaveConfig(transparency, preset.key, shape, showSubtext)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Brush.horizontalGradient(preset.colors))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = preset.emoji, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. SHAPE & LAYOUT STYLE (Capsule 100dp vs Squircle 24dp)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RoundedCorner,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kiểu dáng & Bo góc (Widget Shape)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp
                            )
                        )
                        Text(
                            text = "Viên thuốc bo tròn hoàn toàn hoặc Bo góc mềm",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Capsule shape button
                    val isCapsule = shape == "CAPSULE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCapsule) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(
                                BorderStroke(
                                    if (isCapsule) 1.5.dp else 1.dp,
                                    if (isCapsule) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                shape = "CAPSULE"
                                isSavedSuccess = false
                                onSaveConfig(transparency, colorPreset, "CAPSULE", showSubtext)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isCapsule) Color(0xFF2563EB) else Color(0xFF94A3B8))
                            )
                            Text(
                                text = "Viên thuốc (Capsule)",
                                fontSize = 12.sp,
                                fontWeight = if (isCapsule) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCapsule) Color(0xFF1E40AF) else Color(0xFF475569)
                            )
                            Text("Bo tròn 100dp", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Squircle shape button
                    val isSquircle = shape == "SQUIRCLE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSquircle) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(
                                BorderStroke(
                                    if (isSquircle) 1.5.dp else 1.dp,
                                    if (isSquircle) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                shape = "SQUIRCLE"
                                isSavedSuccess = false
                                onSaveConfig(transparency, colorPreset, "SQUIRCLE", showSubtext)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSquircle) Color(0xFF2563EB) else Color(0xFF94A3B8))
                            )
                            Text(
                                text = "Bo góc nhẹ (Squircle)",
                                fontSize = 12.sp,
                                fontWeight = if (isSquircle) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSquircle) Color(0xFF1E40AF) else Color(0xFF475569)
                            )
                            Text("Bo góc 24dp", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 5. SUBTEXT TOGGLE SWITCH
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShortText,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hiển thị dòng phụ (Sub-text)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp
                            )
                        )
                        Text(
                            text = if (showSubtext) "2 dòng: Tiêu đề + Nội dung tóm tắt" else "1 dòng: Thanh siêu mỏng tối giản",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Switch(
                    checked = showSubtext,
                    onCheckedChange = {
                        showSubtext = it
                        isSavedSuccess = false
                        onSaveConfig(transparency, colorPreset, shape, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2563EB)
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // 6. SAVE & APPLY BUTTON
        // -------------------------------------------------------------
        Button(
            onClick = {
                onSaveConfig(transparency, colorPreset, shape, showSubtext)
                isSavedSuccess = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSavedSuccess) Color(0xFF16A34A) else Color(0xFF2563EB)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (isSavedSuccess) Icons.Default.Check else Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSavedSuccess) "Đã lưu & Cập nhật Widget ra Home Screen" else "Lưu & Áp dụng cấu hình Widget",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class GridPresetOption(
    val key: String,
    val label: String,
    val emoji: String,
    val colors: List<Color>
)

private data class PresetOption(
    val key: String,
    val label: String,
    val description: String,
    val colors: List<Color>
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
