package com.example.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.database.AppDatabase
import com.example.core.engine.CrossContextEngine
import com.example.core.repository.CalendarRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GlanceContextWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val upcomingTasks = try { database.taskDao().getUpcomingTasksSync(5) } catch (e: Exception) { emptyList() }
        val calendarRepo = CalendarRepository(context)
        val calendarEvents = try { calendarRepo.getRemainingTodayEvents() } catch (e: Exception) { emptyList() }

        // Evaluate context via CrossContextEngine & Priority Scoring Pipeline
        val crossContextEngine = CrossContextEngine(context)
        val scoredCards = try { crossContextEngine.evaluateContextCards() } catch (e: Exception) { emptyList() }
        val topScoredCard = scoredCards.firstOrNull()

        val batteryPct = getBatteryPercentage(context)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val now = System.currentTimeMillis()

        // 1. Classify Session directly from BriefingSession enum (DRY standardization)
        val session = BriefingSession.fromHourAndMinute(hour, minute)
        val primaryColor = session.iconGradient.firstOrNull() ?: Color(0xFF38BDF8)
        val secondaryColor = session.iconGradient.getOrNull(1) ?: Color(0xFF0284C7)
        val greetingText = "${session.emoji} ${session.title} · ${session.sessionTag}"

        val darkBgColor = when (session) {
            BriefingSession.EARLY_MORNING -> Color(0xFF0C1929)
            BriefingSession.MID_MORNING -> Color(0xFF0C1E2E)
            BriefingSession.NOON -> Color(0xFF1A1400)
            BriefingSession.AFTERNOON -> Color(0xFF001A1A)
            BriefingSession.EVENING -> Color(0xFF1A0010)
            BriefingSession.NIGHT -> Color(0xFF0A0A1A)
            BriefingSession.LATE_NIGHT -> Color(0xFF0D0010)
        }

        // 2. Aggregate & filter UPCOMING schedule/tasks
        val combinedItems = mutableListOf<WidgetScheduleItem>()

        // Prioritize displaying the top Cross-Context Alert if it has a high score (>7.0)
        if (topScoredCard != null && topScoredCard.calculatedScore >= 7.0f) {
            combinedItems.add(
                WidgetScheduleItem(
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(topScoredCard.rawTimeMillis)),
                    title = "${topScoredCard.emoji} ${topScoredCard.title}",
                    subtitle = topScoredCard.subtitle,
                    rawTime = topScoredCard.rawTimeMillis,
                    isCalendar = false,
                    priority = "HIGH"
                )
            )
        }

        for (e in calendarEvents) {
            if (e.endTimeMillis > now || e.startTimeMillis >= now - 5 * 60 * 1000L) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(e.startTimeMillis))
                val locStr = if (e.location.isNotBlank()) " • ${e.location}" else ""
                combinedItems.add(
                    WidgetScheduleItem(
                        time = timeStr,
                        title = e.title,
                        subtitle = "${e.displaySourceLabel}$locStr",
                        rawTime = e.startTimeMillis,
                        isCalendar = true
                    )
                )
            }
        }

        for (t in upcomingTasks) {
            if (!t.isCompleted && t.datetime >= now - 15 * 60 * 1000L) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t.datetime))
                combinedItems.add(
                    WidgetScheduleItem(
                        time = timeStr,
                        title = t.title,
                        subtitle = "Source: ${t.sourceApp}",
                        rawTime = t.datetime,
                        isCalendar = false,
                        priority = t.priority
                    )
                )
            }
        }

        // 3. Quick summary of upcoming events or Cross-Context Alert
        val summaryLine = if (topScoredCard != null && topScoredCard.calculatedScore >= 7.0f) {
            "⚡ ${topScoredCard.title}"
        } else if (combinedItems.isNotEmpty()) {
            val first = combinedItems.first()
            "✨ Upcoming ${first.time}: ${first.title}"
        } else {
            session.subtitle
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = context,
                    greeting = greetingText,
                    summaryLine = summaryLine,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    darkBgColor = darkBgColor,
                    batteryPct = batteryPct,
                    items = combinedItems
                )
            }
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 85
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        greeting: String,
        summaryLine: String,
        primaryColor: Color,
        secondaryColor: Color,
        darkBgColor: Color,
        batteryPct: Int,
        items: List<WidgetScheduleItem>
    ) {
        val cardBg = Color(0x99111827)
        val textPrimary = Color(0xFFF8FAFC)
        val textSecondary = Color(0xCDB0BFCF)
        val emerald = Color(0xFF10B981)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(darkBgColor))
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // Header Row: Session Tag + Summary + Battery
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(5.dp)
                            .height(34.dp)
                            .background(ColorProvider(primaryColor))
                            .cornerRadius(3.dp)
                    ) {}
                    Spacer(modifier = GlanceModifier.width(9.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = greeting,
                            style = TextStyle(
                                color = ColorProvider(primaryColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = summaryLine,
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 10.5.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Battery Pill
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(cardBg))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val battIcon = when {
                            batteryPct >= 80 -> "🔋"
                            batteryPct >= 40 -> "🔋"
                            else -> "🪫"
                        }
                        Text(
                            text = "$battIcon $batteryPct%",
                            style = TextStyle(
                                color = ColorProvider(if (batteryPct >= 20) emerald else Color(0xFFF43F5E)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Schedule items list / Empty state
                if (items.isEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .background(ColorProvider(cardBg))
                            .cornerRadius(12.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨ No upcoming schedule.",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items.take(2).forEach { item ->
                            ScheduleRow(
                                item = item,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accentColor = primaryColor
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ScheduleRow(
        item: WidgetScheduleItem,
        cardBg: Color,
        textPrimary: Color,
        textSecondary: Color,
        accentColor: Color
    ) {
        val indicatorColor = if (item.isCalendar) {
            Color(0xFF38BDF8)
        } else {
            when (item.priority) {
                "HIGH" -> Color(0xFFF43F5E)
                "MEDIUM" -> Color(0xFFF59E0B)
                else -> accentColor
            }
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(cardBg))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(ColorProvider(indicatorColor))
                    .cornerRadius(2.dp)
            ) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = item.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(textPrimary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "${item.subtitle} · ${item.time}",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

private data class WidgetScheduleItem(
    val time: String,
    val title: String,
    val subtitle: String,
    val rawTime: Long,
    val isCalendar: Boolean,
    val priority: String = "LOW"
)
