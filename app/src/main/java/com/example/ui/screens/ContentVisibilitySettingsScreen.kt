package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

data class ContentCategoryItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconBgColor: Color
)

@Composable
fun ContentVisibilitySettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledCards by viewModel.enabledContentCards.collectAsStateWithLifecycle()

    val categories = listOf(
        ContentCategoryItem(
            id = "health_energy",
            title = "Physical & mental wellness",
            icon = Icons.Default.Favorite,
            iconBgColor = Color(0xFF22C55E) // Vibrant Green
        ),
        ContentCategoryItem(
            id = "calendar_schedule",
            title = "Events & tasks",
            icon = Icons.Default.CalendarMonth,
            iconBgColor = Color(0xFF0284C7) // Vibrant Blue
        ),
        ContentCategoryItem(
            id = "now_nudge",
            title = "Now Nudge & AI suggestions",
            icon = Icons.Default.AutoAwesome,
            iconBgColor = Color(0xFF6366F1) // Indigo Purple
        ),
        ContentCategoryItem(
            id = "weather",
            title = "Weather & environment",
            icon = Icons.Default.WbSunny,
            iconBgColor = Color(0xFF0EA5E9) // Sky Blue
        ),
        ContentCategoryItem(
            id = "habit_insights",
            title = "Habit reports & productivity",
            icon = Icons.Default.Bolt,
            iconBgColor = Color(0xFFF59E0B) // Warm Amber
        ),
        ContentCategoryItem(
            id = "sleep_recovery",
            title = "Sleep & recovery",
            icon = Icons.Default.NightlightRound,
            iconBgColor = Color(0xFF8B5CF6) // Neon Violet
        ),
        ContentCategoryItem(
            id = "smart_music",
            title = "Music & entertainment",
            icon = Icons.Default.MusicNote,
            iconBgColor = Color(0xFFEC4899) // Rose Pink
        ),
        ContentCategoryItem(
            id = "digital_wellbeing",
            title = "Digital wellbeing & screen time",
            icon = Icons.Default.PhoneAndroid,
            iconBgColor = Color(0xFF10B981) // Emerald Green
        ),
        ContentCategoryItem(
            id = "news_market",
            title = "News & markets",
            icon = Icons.Default.Article,
            iconBgColor = Color(0xFFEF4444) // Coral Red
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Top App Bar One UI Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Content to include",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Explanation Description
            Text(
                text = "Choose the types of content that appear in your Now brief. Content from selected categories will be shown when relevant. Unselected categories will not be displayed.",
                fontSize = 13.5.sp,
                color = Color(0xFF64748B),
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Category Group Container (One UI Rounded Glass / Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    categories.forEachIndexed { index, item ->
                        val isChecked = enabledCards.contains(item.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setContentCardEnabled(item.id, !isChecked)
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Circular Colorful Badge
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(item.iconBgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Text(
                                    text = item.title,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // One UI Styled Switch
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    viewModel.setContentCardEnabled(item.id, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2563EB),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }

                        // Divider between rows
                        if (index < categories.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 66.dp, end = 16.dp)
                                    .height(0.6.dp)
                                    .background(Color(0xFFF1F5F9))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reset to Default Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { viewModel.resetContentCardsToDefault() },
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Restore default settings",
                        color = Color(0xFF64748B),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
