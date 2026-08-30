package com.example.ui.screens

import android.widget.Toast
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ml.InternalMlEngine
import com.example.core.ml.RoutineInsight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.feature.island.DynamicIslandAccessibilityService
import com.example.feature.island.DynamicIslandView
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    onOpenContentVisibility: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val widgetTransparency by viewModel.widgetTransparency.collectAsStateWithLifecycle()
    val widgetColorPreset by viewModel.widgetColorPreset.collectAsStateWithLifecycle()
    val widgetShape by viewModel.widgetShape.collectAsStateWithLifecycle()
    val widgetShowSubtext by viewModel.widgetShowSubtext.collectAsStateWithLifecycle()
    val dynamicIslandEnabled by viewModel.dynamicIslandEnabled.collectAsStateWithLifecycle()
    val dynamicIslandYOffset by viewModel.dynamicIslandYOffset.collectAsStateWithLifecycle()
    val dynamicIslandLockscreen by viewModel.dynamicIslandLockscreen.collectAsStateWithLifecycle()
    val dynamicIslandLockscreenOnly by viewModel.dynamicIslandLockscreenOnly.collectAsStateWithLifecycle()
    val dynamicIslandScale by viewModel.dynamicIslandScale.collectAsStateWithLifecycle()
    val dynamicIslandWidth by viewModel.dynamicIslandWidth.collectAsStateWithLifecycle()
    val dynamicIslandHeight by viewModel.dynamicIslandHeight.collectAsStateWithLifecycle()
    val dynamicIslandOpacity by viewModel.dynamicIslandOpacity.collectAsStateWithLifecycle()
    val dynamicIslandFrostedGlass by viewModel.dynamicIslandFrostedGlass.collectAsStateWithLifecycle()
    val dynamicIslandPosition by viewModel.dynamicIslandPosition.collectAsStateWithLifecycle()
    val dynamicIslandColorPreset by viewModel.dynamicIslandColorPreset.collectAsStateWithLifecycle()

    val isSystemDevMode = remember(context) {
        try {
            AndroidSettings.Global.getInt(context.contentResolver, AndroidSettings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        } catch (e: Exception) {
            false
        }
    }
    var isDevUnlocked by rememberSaveable { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }
    val isDeveloper = isSystemDevMode || isDevUnlocked

    val tabTitles = remember(isDeveloper) {
        if (isDeveloper) {
            listOf("Widget", "Dynamic Island", "🧠 Máy học (Dev)")
        } else {
            listOf("Widget", "Dynamic Island")
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val safeSelectedTab = selectedTab.coerceIn(0, tabTitles.size - 1)
    val activeTab = tabTitles[safeSelectedTab]

    var showResetConfirm by remember { mutableStateOf(false) }

    // Fetch routine learning insights from InternalMlEngine
    val routineInsight by produceState(initialValue = RoutineInsight(), key1 = safeSelectedTab) {
        value = InternalMlEngine.getInstance(context).getRoutineInsight()
    }

    val mediaStats by produceState(initialValue = com.example.core.ml.MediaPreferenceStats(), key1 = safeSelectedTab) {
        value = InternalMlEngine.getInstance(context).getMediaPreferenceStats()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Top Header (One UI Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF0F172A)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDeveloper) "Cài đặt & Máy học (Dev)" else "Cài đặt & Tùy biến",
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = if (isDeveloper) "Chế độ Nhà phát triển • ML On-Device Active" else "Tùy biến Widget & Dynamic Island",
                    color = if (isDeveloper) Color(0xFF2563EB) else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = if (isDeveloper) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        // Material 3 TabRow: 2 Tabs for standard users, 3 Tabs for developers
        PrimaryTabRow(
            selectedTabIndex = safeSelectedTab,
            containerColor = Color(0xFFF1F5F9),
            contentColor = Color(0xFF2563EB),
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isTabSelected = safeSelectedTab == index
                Tab(
                    selected = isTabSelected,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTabSelected) Color(0xFF1E293B) else Color(0xFF64748B)
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // One UI Card: Nội dung muốn đưa vào (Content Visibility)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenContentVisibility?.invoke() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Nội dung muốn đưa vào",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Tùy chỉnh bật / tắt các thẻ trên NowBrief",
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            when (activeTab) {
                // TAB: NATIVE ON-DEVICE ML ENGINE (DEVELOPER ONLY)
                "🧠 Máy học (Dev)" -> {
                    // Header Banner Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Mô hình Máy học Nội tại (On-Device ML)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                    Text(
                                        text = "100% Cục bộ • Không tốn RAM • Không cần mạng",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Mô hình tự động phân tích tín hiệu hệ thống (khóa/mở màn hình, cắm sạc) và tương tác card để tự động cá nhân hóa trải nghiệm của bạn.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Card 1: Segmented Routine Learning Insight Card (Weekday vs Weekend)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dự đoán Thói quen Sinh hoạt",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                    Text(
                                        text = if (routineInsight.isTodayWeekend) "Hôm nay: Cuối tuần" else "Hôm nay: Ngày làm việc",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = routineInsight.confidenceLevel,
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Weekdays Row
                            Text(
                                text = "Ngày làm việc (T2 - T6)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Thức dậy", fontSize = 10.sp, color = Color(0xFF92400E))
                                            Text(
                                                routineInsight.weekday.wakeTimeFormatted,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF78350F)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEDE9FE))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NightsStay,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Đi ngủ", fontSize = 10.sp, color = Color(0xFF5B21B6))
                                            Text(
                                                routineInsight.weekday.bedtimeFormatted,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4C1D95)
                                            )
                                        }
                                    }
                                }
                            }

                            // Weekend Row
                            Text(
                                text = "Cuối tuần (T7 - CN)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFECFDF5))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Thức dậy", fontSize = 10.sp, color = Color(0xFF065F46))
                                            Text(
                                                routineInsight.weekend.wakeTimeFormatted,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFEE2E2))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NightsStay,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Đi ngủ", fontSize = 10.sp, color = Color(0xFF991B1B))
                                            Text(
                                                routineInsight.weekend.bedtimeFormatted,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Card Preference Bandit Learning Card
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thuật toán Xếp hạng Nội dung (Bandit UCB1)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                )
                            }
                            Text(
                                text = "Hệ thống tự học card nội dung bạn quan tâm nhất (qua các lượt tap & xem) để tự điều chỉnh thứ tự hiển thị ưu tiên.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Card 3: Media Preference & Peak Productivity AI Card
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartDisplay,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sở thích Nội dung & Khung giờ Vàng",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF2F2))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Học máy AI",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Chủ đề hay xem", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(
                                            mediaStats.topCategory,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1
                                        )
                                        Text(
                                            "${mediaStats.topCategoryPercentage}% mức độ yêu thích",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF2563EB),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Khung giờ tập trung", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                                        Text(
                                            "09:15 - 11:30",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                        Text(
                                            "Giờ vàng hiệu suất",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reset Data Button
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_all_data_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xóa bộ nhớ học & Reset Logs")
                    }

                    // Privacy Guarantee Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bảo mật 100% On-Device: Toàn bộ quá trình phân tích thói quen và xếp hạng nội dung diễn ra cục bộ trong thiết bị.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF1E40AF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // TAB: WIDGET CUSTOMIZER & PREVIEW
                "Widget" -> {
                    com.example.feature.widget.WidgetCustomizerContent(
                        initialTransparency = widgetTransparency,
                        initialColorPreset = widgetColorPreset,
                        initialShape = widgetShape,
                        initialShowSubtext = widgetShowSubtext,
                        onSaveConfig = { transparency, colorPreset, shape, showSubtext ->
                            viewModel.saveWidgetConfig(
                                transparency = transparency,
                                colorPreset = colorPreset,
                                shape = shape,
                                showSubtext = showSubtext
                            )
                        }
                    )
                }

                // TAB: DYNAMIC ISLAND (LOCKSCREEN & ACCESSIBILITY)
                "Dynamic Island" -> {
                    val isAccessibilityGranted = remember(safeSelectedTab) {
                        DynamicIslandAccessibilityService.isServiceEnabled(context)
                    }

                    // Header Banner Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NowBrief Dynamic Island",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                    Text(
                                        text = "Màn hình khóa • Vùng nốt ruồi Camera • Quyền Trợ năng",
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Hiển thị viên thuốc thông minh (Dynamic Island) quanh camera đục lỗ trên cả Màn hình khóa và mọi ứng dụng mà không cần xin quyền vẽ đè (SYSTEM_ALERT_WINDOW).",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Accessibility Permission Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAccessibilityGranted) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isAccessibilityGranted) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                        )
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAccessibilityGranted) Icons.Default.CheckCircle else Icons.Default.Accessibility,
                                        contentDescription = null,
                                        tint = if (isAccessibilityGranted) Color(0xFF16A34A) else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isAccessibilityGranted) "Quyền Trợ năng đã bật" else "Cần cấp Quyền Trợ năng",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isAccessibilityGranted) Color(0xFF15803D) else Color(0xFFB45309)
                                    )
                                }
                            }

                            Text(
                                text = if (isAccessibilityGranted)
                                    "Hệ thống đã sẵn sàng vẽ Dynamic Island trên màn hình khóa."
                                else
                                    "Nhấn nút bên dưới để mở Cài đặt Trợ năng, tìm 'NowBrief Dynamic Island' và bật công tắc.",
                                fontSize = 12.sp,
                                color = if (isAccessibilityGranted) Color(0xFF166534) else Color(0xFF92400E),
                                lineHeight = 16.sp
                            )

                            if (!isAccessibilityGranted) {
                                Button(
                                    onClick = {
                                        DynamicIslandAccessibilityService.openAccessibilitySettings(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bật Quyền Trợ Năng", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Configuration & Controls Card
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Tùy chọn hiển thị",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )

                            // Toggle 1: Enable Dynamic Island
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bật Dynamic Island",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Hiển thị viên thuốc thông minh nổi trên màn hình",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = dynamicIslandEnabled,
                                    onCheckedChange = { viewModel.setDynamicIslandEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                                )
                            }

                            // Toggle 2: Show on Lockscreen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hiển thị ngoài Màn hình khóa",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Xem nhanh năng lượng và thời tiết trên màn hình khóa",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = dynamicIslandLockscreen,
                                    onCheckedChange = { viewModel.setDynamicIslandLockscreen(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                                )
                            }

                            // Toggle 3: Lockscreen Only (Only show on lockscreen, hide when unlocked)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chỉ hiện ở Màn hình khóa",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Tự động ẩn Dynamic Island khi mở khóa máy để tránh che màn hình",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = dynamicIslandLockscreenOnly,
                                    onCheckedChange = { viewModel.setDynamicIslandLockscreenOnly(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                                )
                            }

                            // Selection: Screen Position (TOP vs BOTTOM)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Vị trí hiển thị trên màn hình",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isTop = dynamicIslandPosition.equals("TOP", ignoreCase = true)
                                    // Top Button
                                    Button(
                                        onClick = { viewModel.setDynamicIslandPosition("TOP") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTop) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            contentColor = if (isTop) Color.White else Color(0xFF475569)
                                        )
                                    ) {
                                        Text(text = "⬆️ Đỉnh màn hình", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Bottom Button
                                    Button(
                                        onClick = { viewModel.setDynamicIslandPosition("BOTTOM") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isTop) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            contentColor = if (!isTop) Color.White else Color(0xFF475569)
                                        )
                                    ) {
                                        Text(text = "⬇️ Đáy màn hình", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            var localYOffset by remember(dynamicIslandYOffset) { mutableFloatStateOf(dynamicIslandYOffset.toFloat()) }
                            var localWidth by remember(dynamicIslandWidth) { mutableFloatStateOf(dynamicIslandWidth.toFloat()) }
                            var localHeight by remember(dynamicIslandHeight) { mutableFloatStateOf(dynamicIslandHeight.toFloat()) }
                            var localOpacity by remember(dynamicIslandOpacity) { mutableFloatStateOf(dynamicIslandOpacity) }

                            // Slider 1: Y-Offset for Position Calibration
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (dynamicIslandPosition.equals("BOTTOM", ignoreCase = true)) "Khoảng cách Y (Cách đáy)" else "Khoảng cách Y (Notch Camera)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${localYOffset.toInt()} dp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Slider(
                                    value = localYOffset,
                                    onValueChange = {
                                        localYOffset = it
                                        viewModel.setDynamicIslandYOffset(it.toInt())
                                    },
                                    valueRange = 0f..80f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF2563EB),
                                        activeTrackColor = Color(0xFF2563EB)
                                    )
                                )
                            }

                            // Slider 2: Width Calibration
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chiều rộng viên thuốc (Width)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${localWidth.toInt()} dp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Slider(
                                    value = localWidth.coerceIn(180f, 390f),
                                    onValueChange = {
                                        localWidth = it
                                        viewModel.setDynamicIslandWidth(it.toInt())
                                    },
                                    valueRange = 180f..390f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF2563EB),
                                        activeTrackColor = Color(0xFF2563EB)
                                    )
                                )
                            }

                            // Slider 3: Height Calibration
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chiều cao viên thuốc (Height)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${localHeight.toInt()} dp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Slider(
                                    value = localHeight.coerceIn(28f, 60f),
                                    onValueChange = {
                                        localHeight = it
                                        viewModel.setDynamicIslandHeight(it.toInt())
                                    },
                                    valueRange = 28f..60f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF2563EB),
                                        activeTrackColor = Color(0xFF2563EB)
                                    )
                                )
                            }

                            // Slider 4: Opacity / Transparency
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Độ trong suốt kính (Opacity)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${(localOpacity * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Slider(
                                    value = localOpacity.coerceIn(0.15f, 1.0f),
                                    onValueChange = {
                                        localOpacity = it
                                        viewModel.setDynamicIslandOpacity(it)
                                    },
                                    valueRange = 0.15f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF2563EB),
                                        activeTrackColor = Color(0xFF2563EB)
                                    )
                                )
                            }

                            // Color Presets Selector (Matching NowBriefWidget.kt)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Bảng màu kính & Phong cách",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                val presets = listOf(
                                    Triple("DYNAMIC", "🌅 Tự động", "Theo 7 khung giờ"),
                                    Triple("DARK_GLASS", "🌌 Kính tối", "Dark Glass"),
                                    Triple("LIGHT_GLASS", "☁️ Kính sáng", "Light Glass"),
                                    Triple("MATERIAL_YOU", "🎨 Material", "Ocean Blue")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.forEach { (presetKey, label, _) ->
                                        val isSelected = dynamicIslandColorPreset == presetKey
                                        Button(
                                            onClick = { viewModel.setDynamicIslandColorPreset(presetKey) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                                contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                            )
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Switch: Frosted Glass Effect
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hiệu ứng Kính mờ (Frosted Glass)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Tạo viền phản quang nhẹ và hiệu ứng phủ sương One UI",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = dynamicIslandFrostedGlass,
                                    onCheckedChange = { viewModel.setDynamicIslandFrostedGlass(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                                )
                            }
                        }
                    }

                    // Dynamic Island Interactive Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Xem trước Dynamic Island",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "(Chạm để mở rộng/thu gọn)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            // Dynamic Island Preview Component
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DynamicIslandView(
                                    energyScore = 92,
                                    weatherTemp = "29°C",
                                    aiBriefingText = "Khung giờ vàng hiệu suất: Hãy ưu tiên giải quyết các task quan trọng trước 11:30.",
                                    customWidthDp = dynamicIslandWidth,
                                    customHeightDp = dynamicIslandHeight,
                                    opacity = dynamicIslandOpacity,
                                    colorPreset = dynamicIslandColorPreset,
                                    isFrostedGlass = dynamicIslandFrostedGlass,
                                    onOpenApp = {},
                                    onRefresh = {}
                                )
                            }
                        }
                    }
                }
            }

            // Developer Mode Easter Egg & App Info Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        devTapCount++
                        if (devTapCount >= 7 && !isDevUnlocked) {
                            isDevUnlocked = true
                            Toast.makeText(context, "🎉 Đã mở khóa Tab Máy học (Developer Mode)!", Toast.LENGTH_SHORT).show()
                        } else if (devTapCount in 3..6 && !isDevUnlocked) {
                            Toast.makeText(context, "Chạm thêm ${7 - devTapCount} lần nữa để mở Chế độ Nhà phát triển", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "NowBrief • Phiên bản 1.0 (One UI 9 Edition)",
                    fontSize = 12.5.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
                if (isDeveloper) {
                    Text(
                        text = "🚀 Chế độ Nhà phát triển đang Bật (Hiện Tab Máy học)",
                        fontSize = 11.5.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Confirm Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Xác nhận xóa bộ nhớ học?") },
            text = { Text("Toàn bộ lịch sử thói quen và xếp hạng card đã học sẽ bị xóa về trạng thái ban đầu.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirm = false
                    }
                ) {
                    Text("Xóa dữ liệu", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
