package com.example.ui.components

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Model representing a single schedule event in OneUICalendarCard.
 */
data class OneUICalendarEventItem(
    val title: String,
    val timeSubtitle: String,
    val source: String = "Google Calendar",
    val eventId: Long? = null,
    val accentColor: Color = Color(0xFF38BDF8),
    val location: String = ""
)

/**
 * Multi-event Schedule Card following Samsung One UI 9 design language.
 * Supports displaying a single event or a stacked list of multiple events with colored indicator bars.
 */
@Composable
fun OneUICalendarCard(
    modifier: Modifier = Modifier,
    events: List<OneUICalendarEventItem> = emptyList(),
    // Backward compatibility for single event
    eventTitle: String? = null,
    timeSubtitle: String? = null,
    calendarSource: String = "Google Calendar",
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null,
    onItemClick: ((OneUICalendarEventItem) -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    maxDisplayCount: Int = 3
) {
    // Aggregate event list from parameters
    val eventList = when {
        events.isNotEmpty() -> events
        !eventTitle.isNullOrBlank() -> listOf(
            OneUICalendarEventItem(
                title = eventTitle,
                timeSubtitle = timeSubtitle ?: "All day",
                source = calendarSource
            )
        )
        else -> emptyList()
    }

    val containerBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val strokeColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }

    val titleColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val appNameColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF5F6368)
    val timeTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF757575)

    val iconBgColor = if (isDark) Color(0xFF312E81) else Color(0xFFE8F0FE)
    val iconTintColor = if (isDark) Color(0xFF818CF8) else Color(0xFF1A73E8)

    val displayedItems = eventList.take(maxDisplayCount)
    val remainingCount = (eventList.size - maxDisplayCount).coerceAtLeast(0)

    val accentColors = listOf(
        Color(0xFF38BDF8), // Cyan Blue
        Color(0xFF818CF8), // Indigo Purple
        Color(0xFFA855F7), // Neon Violet
        Color(0xFF34D399), // Emerald Green
        Color(0xFFFB923C)  // Warm Amber
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (onClick != null && eventList.size <= 1) Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, strokeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // -------------------------------------------------------------
            // [HEADER]: Squircle Icon + App/Source Title + Badge Count + More Menu
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // App Icon inside Squircle Container (36dp, RoundedCornerShape 12dp)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "App Icon",
                            tint = iconTintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (eventList.size > 1) "Today's Schedule" else (eventList.firstOrNull()?.source ?: calendarSource),
                            color = appNameColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (eventList.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(iconBgColor)
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = "${eventList.size} events",
                                color = iconTintColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Action Icon (Three-dot Menu)
                    IconButton(
                        onClick = { onMoreClick?.invoke() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Action Menu",
                            tint = appNameColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // [BODY]: Event list (Empty / Single Hero / Multi-List)
            // -------------------------------------------------------------
            if (eventList.isEmpty()) {
                // Empty state
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "No upcoming events",
                            color = titleColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "All scheduled plans for today are completed",
                            color = timeTextColor,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (eventList.size == 1) {
                // Display a single event (Hero Single View)
                val singleEvent = eventList.first()
                val cleanTime = singleEvent.timeSubtitle.replace("🕒", "").trim().ifBlank { "All day" }

                Text(
                    text = singleEvent.title,
                    color = titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time Icon",
                        tint = timeTextColor,
                        modifier = Modifier.size(15.dp)
                    )

                    Text(
                        text = if (singleEvent.location.isNotBlank()) "$cleanTime • ${singleEvent.location}" else cleanTime,
                        color = timeTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                // Display multiple events as a One UI stacked list (Multi-Event Stack View)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    displayedItems.forEachIndexed { index, item ->
                        val cleanTime = item.timeSubtitle.replace("🕒", "").trim().ifBlank { "All day" }
                        val accentBarColor = accentColors[index % accentColors.size]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (onItemClick != null) onItemClick(item)
                                    else onClick?.invoke()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Colored vertical accent bar
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accentBarColor)
                            )

                            // Event details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    color = titleColor,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = timeTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (item.location.isNotBlank()) "$cleanTime • ${item.location}" else cleanTime,
                                        color = timeTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Thin divider between events
                        if (index < displayedItems.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                            )
                        }
                    }

                    // Expand button if there are more events
                    if (remainingCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                                .clickable { onMoreClick?.invoke() ?: onClick?.invoke() }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+$remainingCount more events today",
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.NorthEast,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Single Event Preview")
@Composable
private fun OneUICalendarCardSinglePreview() {
    Surface(color = Color(0xFF0F172A)) {
        Box(modifier = Modifier.padding(16.dp)) {
            OneUICalendarCard(
                eventTitle = "Q3 Product Strategy Meeting",
                timeSubtitle = "08:30 - 11:30 AM",
                calendarSource = "Google Calendar",
                isDark = true
            )
        }
    }
}

@Preview(name = "Multi-Event Preview")
@Composable
private fun OneUICalendarCardMultiPreview() {
    Surface(color = Color(0xFF0F172A)) {
        Box(modifier = Modifier.padding(16.dp)) {
            OneUICalendarCard(
                events = listOf(
                    OneUICalendarEventItem("Q3 Product Strategy Meeting", "08:30 - 10:00 AM", location = "Meeting Room A"),
                    OneUICalendarEventItem("One UI 9 Design Review", "10:30 - 11:30 AM", location = "Google Meet"),
                    OneUICalendarEventItem("Learn a new lesson on Duolingo 💚", "14:00 - 14:30 PM"),
                    OneUICalendarEventItem("Evening gym & run", "18:00 - 19:00 PM")
                ),
                isDark = true
            )
        }
    }
}
