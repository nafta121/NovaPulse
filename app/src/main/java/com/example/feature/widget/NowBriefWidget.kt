package com.example.feature.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.core.database.AppDatabase
import com.example.core.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

class NowBriefWidget : GlanceAppWidget() {

    companion object {
        val SIZE_2X1 = DpSize(110.dp, 72.dp)   // 2x1, 3x1, 4x1 (Capsule Bar 72dp)
        val SIZE_2X2 = DpSize(150.dp, 150.dp)  // 2x2 (Square Card)
        val SIZE_3X2 = DpSize(260.dp, 150.dp)  // 3x2, 4x2 (Expanded Suggestion Card)
        val SIZE_3X3 = DpSize(260.dp, 280.dp)  // 3x3, 4x4 (Full Dashboard Widget)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_2X1, SIZE_2X2, SIZE_3X2, SIZE_3X3)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val upcomingTasks = try {
            database.taskDao().getUpcomingTasksSync(3)
        } catch (e: Exception) {
            emptyList()
        }

        val prefsManager = PreferencesManager(context)
        val colorPreset = try {
            prefsManager.widgetColorPreset.first()
        } catch (e: Exception) {
            "AUTO"
        }
        val shape = try {
            prefsManager.widgetShape.first()
        } catch (e: Exception) {
            "CAPSULE"
        }
        val showSubtext = try {
            prefsManager.widgetShowSubtext.first()
        } catch (e: Exception) {
            true
        }
        val aiBriefing = try {
            prefsManager.widgetAiBriefing.first()
        } catch (e: Exception) {
            ""
        }

        val transparency = try {
            prefsManager.widgetTransparency.first()
        } catch (e: Exception) {
            0.45f
        }

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeTheme = WidgetThemeHelper.getThemeForCurrentTime(hour, minute)

        val currentSession = BriefingSession.fromHourAndMinute(hour, minute)
        val activeGreeting = currentSession.getRandomGreeting()

        val greetingTitle = activeGreeting.title

        val cornerRadiusDp = if (shape == "SQUIRCLE") 24.dp else 100.dp

        val (titleColor, subtitleColor) = if (colorPreset == "LIGHT_GLASS") {
            Pair(Color(0xFF0F172A), Color(0xFF475569))
        } else {
            Pair(timeTheme.titleTextColor, Color.White.copy(alpha = 0.85f))
        }

        val cachedWeather = com.example.core.repository.WeatherCacheManager.load(context)
        val currentTempInt = if (cachedWeather != null) Math.round(cachedWeather.currentTempC).toInt() else 29

        val nextTaskTitle = upcomingTasks.firstOrNull()?.title
        val subtitleRaw = when {
            !nextTaskTitle.isNullOrBlank() -> nextTaskTitle
            aiBriefing.isNotBlank() -> aiBriefing
            else -> activeGreeting.subtitle
        }
        val subtitle = BriefingSessionHelper.sanitizeWidgetText(subtitleRaw, currentSession, currentTempInt).cleanEmoji()

        val fullCardTitle = activeGreeting.title
        val rawSummary = (if (aiBriefing.isNotBlank()) aiBriefing else activeGreeting.subtitle)
        val fullCardSummary = BriefingSessionHelper.sanitizeWidgetText(rawSummary, currentSession, currentTempInt).cleanEmoji()

        val opacity = transparency.coerceIn(0.05f, 1.0f)
        val gradientColors = when (colorPreset) {
            "DARK_GLASS" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            "LIGHT_GLASS" -> listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
            "MATERIAL_YOU" -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF60A5FA))
            else -> timeTheme.gradientColors
        }
        val iconGradientColors = timeTheme.iconGradientColors

        provideContent {
            GlanceTheme {
                val size = LocalSize.current

                if (size.height < 80.dp) {
                    // Small sizes 2x1, 3x1, 4x1: Render as 56dp Capsule Bar (Google Bar Style)
                    CapsulePillView(
                        context = context,
                        title = greetingTitle,
                        subtitle = subtitle,
                        showSubtext = showSubtext,
                        gradientColors = gradientColors,
                        iconGradientColors = iconGradientColors,
                        opacity = opacity,
                        cornerRadiusDp = cornerRadiusDp,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor
                    )
                } else {
                    // Large sizes 3x3, 4x4: Render as Full Card (Frosted Glass Blur)
                    FullDashboardCardView(
                        context = context,
                        title = fullCardTitle,
                        summary = fullCardSummary,
                        gradientColors = gradientColors,
                        iconGradientColors = iconGradientColors,
                        opacity = opacity
                    )
                }
            }
        }
    }

    private fun createLinearGradientBackgroundProvider(
        colors: List<Color>,
        alphaPercent: Float,
        width: Int = 400,
        height: Int = 100
    ): ImageProvider {
        val clampedAlpha = alphaPercent.coerceIn(0.05f, 1.0f)
        val argbColors = colors.map { color ->
            android.graphics.Color.argb(
                (clampedAlpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }.toIntArray()

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            argbColors,
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            setShader(shader)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return ImageProvider(bitmap)
    }

    private fun createVerticalGradientIconProvider(
        colors: List<Color>,
        sizeDp: Int = 46
    ): ImageProvider {
        val px = (sizeDp * 2).coerceAtLeast(64)
        val argbColors = colors.map { color ->
            android.graphics.Color.argb(
                255,
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }.toIntArray()

        val bitmap = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, px.toFloat(),
            argbColors,
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            setShader(shader)
        }
        canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
        return ImageProvider(bitmap)
    }

    // =========================================================================
    // 1. CAPSULE PILL VIEW (Compact height < 80dp — 2x1, 3x1, 4x1 Google Bar Style)
    // =========================================================================
    @Composable
    private fun CapsulePillView(
        context: Context,
        title: String,
        subtitle: String,
        showSubtext: Boolean,
        gradientColors: List<Color>,
        iconGradientColors: List<Color>,
        opacity: Float,
        cornerRadiusDp: androidx.compose.ui.unit.Dp,
        titleColor: Color,
        subtitleColor: Color
    ) {
        val bgProvider = createLinearGradientBackgroundProvider(gradientColors, opacity)
        val iconBgProvider = createVerticalGradientIconProvider(iconGradientColors, 46)

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(71.dp)
                .background(bgProvider)
                .cornerRadius(cornerRadiusDp)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left circular icon: Fixed GlanceModifier.size(46.dp).cornerRadius(23.dp)
                Box(
                    modifier = GlanceModifier
                        .size(46.dp)
                        .background(iconBgProvider)
                        .cornerRadius(23.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_capsule_horizon),
                        contentDescription = "Sunrise Horizon",
                        modifier = GlanceModifier.size(46.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                if (showSubtext) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(titleColor),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                color = ColorProvider(subtitleColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(titleColor),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 2. FULL DASHBOARD CARD VIEW (Large size >= 80dp — 3x3, 4x4 Now Brief Card Style)
    // =========================================================================
    @Composable
    private fun FullDashboardCardView(
        context: Context,
        title: String,
        summary: String,
        gradientColors: List<Color>,
        iconGradientColors: List<Color>,
        opacity: Float
    ) {
        val bgProvider = createLinearGradientBackgroundProvider(gradientColors, opacity)
        val iconBgProvider = createVerticalGradientIconProvider(iconGradientColors, 44)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgProvider)
                .cornerRadius(32.dp)
                .padding(24.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Top corner: Circular sunset horizon icon (Size 44dp)
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(iconBgProvider)
                        .cornerRadius(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_capsule_horizon),
                        contentDescription = "Sunset Horizon Icon",
                        modifier = GlanceModifier.size(44.dp)
                    )
                }

                // Auto-expanding spacer
                Spacer(modifier = GlanceModifier.defaultWeight())

                // Bottom corner: Title & AI summary content
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = summary,
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.85f)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 4
                    )
                }
            }
        }
    }
}
