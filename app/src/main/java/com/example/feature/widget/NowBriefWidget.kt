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
import androidx.glance.layout.fillMaxHeight
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
        val aiEnergyScore = try {
            prefsManager.widgetAiEnergyScore.first()
        } catch (e: Exception) {
            92
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
        val timeGreeting = activeGreeting.subtitle.cleanEmoji()

        val bgDrawableRes = when (colorPreset) {
            "DARK_GLASS" -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_dark_glass else R.drawable.bg_capsule_dark_glass
            "LIGHT_GLASS" -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_light_glass else R.drawable.bg_capsule_light_glass
            "MATERIAL_YOU" -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_material_you else R.drawable.bg_capsule_material_you
            else -> when (hour) {
                in 5..10 -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_morning else R.drawable.bg_capsule_morning
                in 11..16 -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_afternoon else R.drawable.bg_capsule_afternoon
                in 17..19 -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_evening else R.drawable.bg_capsule_evening
                else -> if (shape == "SQUIRCLE") R.drawable.bg_squircle_night else R.drawable.bg_capsule_night
            }
        }

        val cornerRadiusDp = if (shape == "SQUIRCLE") 24.dp else 100.dp

        val (titleColor, subtitleColor) = if (colorPreset == "LIGHT_GLASS") {
            Pair(Color(0xFF0F172A), Color(0xFF475569))
        } else {
            Pair(timeTheme.titleTextColor, Color.White.copy(alpha = 0.85f))
        }

        val isLightGlass = colorPreset == "LIGHT_GLASS"
        val containerCardBg = if (isLightGlass) R.drawable.bg_widget_box_light else R.drawable.bg_widget_box_dark
        val subCardBg = if (isLightGlass) R.drawable.bg_widget_sub_card_light else R.drawable.bg_widget_sub_card_dark

        val cachedWeather = com.example.core.repository.WeatherCacheManager.load(context)
        val currentTempInt = if (cachedWeather != null) Math.round(cachedWeather.currentTempC).toInt() else 29
        val tempDisplayStr = "${currentTempInt}°C"

        val providerSummary =
            com.example.core.ai.BriefingContentProvider.getHeaderSummary(currentSession, currentTempInt, "trời mát mẻ")
        val nextTaskTitle = upcomingTasks.firstOrNull()?.title
        val subtitleRaw = when {
            !nextTaskTitle.isNullOrBlank() -> nextTaskTitle
            aiBriefing.isNotBlank() -> aiBriefing
            else -> activeGreeting.subtitle
        }
        val subtitle = BriefingSessionHelper.sanitizeWidgetText(subtitleRaw, currentSession, currentTempInt).cleanEmoji()

        val task1 = BriefingSessionHelper.sanitizeWidgetText(
            (upcomingTasks.getOrNull(0)?.title ?: (if (aiBriefing.isNotBlank()) aiBriefing else activeGreeting.subtitle)),
            currentSession,
            currentTempInt
        ).cleanEmoji()
        val task2 = (upcomingTasks.getOrNull(1)?.title ?: "Uống 500ml nước & thở sâu 3 phút").cleanEmoji()

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
                    // Kích thước nhỏ 2x1, 3x1, 4x1: Render dạng thanh Capsule 56dp (Google Bar Style)
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
                    // Kích thước lớn 3x3, 4x4: Render dạng Thẻ Lớn Full Card (Frosted Glass Blur)
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

    private data class QuadDrawables(
        val morningRes: Int,
        val afternoonRes: Int,
        val eveningRes: Int,
        val nightRes: Int
    )

    // =========================================================================
    // 1. CAPSULE PILL VIEW (Kích thước mỏng < 80dp — 2x1, 3x1, 4x1 Google Bar Style)
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
                // Icon tròn bên trái: Cố định GlanceModifier.size(46.dp).cornerRadius(23.dp)
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

    @Composable
    private fun PillWidget4x1(
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
        CapsulePillView(
            context,
            title,
            subtitle,
            showSubtext,
            gradientColors,
            iconGradientColors,
            opacity,
            cornerRadiusDp,
            titleColor,
            subtitleColor
        )
    }

    // =========================================================================
    // 2. FULL DASHBOARD CARD VIEW (Kích thước lớn >= 80dp — 3x3, 4x4 Now Brief Card Style)
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
                // Góc trên: Icon tròn mặt trời hoàng hôn (Size 44dp)
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

                // Spacer giãn cách tự động
                Spacer(modifier = GlanceModifier.defaultWeight())

                // Góc dưới: Tiêu đề & Nội dung tóm tắt AI
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

    // =========================================================================
    // 2. SIZE VUÔNG 2x2 (Thời tiết + Điểm năng lượng + Mascot/Biểu tượng)
    // =========================================================================
    @Composable
    private fun SquareWidget2x2(
        context: Context,
        title: String,
        timeGreeting: String,
        taskTitle: String,
        energyScore: Int = 92,
        isLight: Boolean,
        containerRes: Int,
        subCardRes: Int,
        titleColor: Color,
        subtitleColor: Color,
        tempDisplayStr: String = "29°C"
    ) {
        val textColor = if (isLight) Color(0xFF0F172A) else Color.White
        val subColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(containerRes))
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Header: Horizon + Weather
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconBgProvider = createVerticalGradientIconProvider(
                        listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFFB923C)),
                        32
                    )
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(iconBgProvider)
                            .cornerRadius(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_capsule_horizon),
                            contentDescription = "Horizon",
                            modifier = GlanceModifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_weather_sun),
                            contentDescription = "Weather",
                            modifier = GlanceModifier.size(16.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = tempDisplayStr,
                            style = TextStyle(
                                color = ColorProvider(textColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Greeting & Energy Score
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.padding(vertical = 2.dp)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_energy),
                        contentDescription = "Energy",
                        modifier = GlanceModifier.size(14.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "$energyScore • -9 Energy",
                        style = TextStyle(
                            color = ColorProvider(if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Task Sub-card
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ImageProvider(subCardRes))
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_task),
                            contentDescription = "Task",
                            modifier = GlanceModifier.size(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = taskTitle,
                            style = TextStyle(
                                color = ColorProvider(textColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 3. SIZE TRUNG BÌNH 4x2 (Capsule Briefing + Lịch trình & Phím tắt)
    // =========================================================================
    @Composable
    private fun MediumWidget4x2(
        context: Context,
        title: String,
        subtitle: String,
        task1: String,
        bgDrawableRes: Int,
        containerRes: Int,
        subCardRes: Int,
        isLight: Boolean,
        titleColor: Color,
        subtitleColor: Color,
        tempDisplayStr: String = "29°C"
    ) {
        val textColor = if (isLight) Color(0xFF0F172A) else Color.White
        val subColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(containerRes))
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Top Capsule Header Banner
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(ImageProvider(bgDrawableRes))
                        .cornerRadius(100.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconBgProvider = createVerticalGradientIconProvider(
                            listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFFB923C)),
                            28
                        )
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .background(iconBgProvider)
                                .cornerRadius(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_capsule_horizon),
                                contentDescription = "Horizon",
                                modifier = GlanceModifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(titleColor),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_weather_sun),
                                contentDescription = "Weather",
                                modifier = GlanceModifier.size(15.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = tempDisplayStr,
                                style = TextStyle(
                                    color = ColorProvider(titleColor),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom Content: Cụm 3 Mini Card Chánh niệm (Tâm trạng / Thở 3 phút / Thiền & Nhạc)
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Card 1: Tâm trạng
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(14.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_task),
                                contentDescription = "Tâm trạng",
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Tâm trạng",
                                style = TextStyle(
                                    color = ColorProvider(if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Card 2: Thở 3 phút
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(14.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_mindful),
                                contentDescription = "Thở",
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Thở 3 phút",
                                style = TextStyle(
                                    color = ColorProvider(if (isLight) Color(0xFF7C3AED) else Color(0xFFA78BFA)),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Card 3: Thiền & Nhạc
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(14.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_music),
                                contentDescription = "Thiền",
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Thiền & Nhạc",
                                style = TextStyle(
                                    color = ColorProvider(if (isLight) Color(0xFF059669) else Color(0xFF34D399)),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. SIZE LỚN 4x4 (Capsule Briefing + Lịch trình + Mini Chánh niệm & Âm nhạc)
    // =========================================================================
    @Composable
    private fun LargeWidget4x4(
        context: Context,
        title: String,
        timeGreeting: String,
        task1: String,
        task2: String,
        energyScore: Int = 92,
        bgDrawableRes: Int,
        containerRes: Int,
        subCardRes: Int,
        isLight: Boolean,
        titleColor: Color,
        subtitleColor: Color,
        tempDisplayStr: String = "29°C"
    ) {
        val textColor = if (isLight) Color(0xFF0F172A) else Color.White
        val subColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(containerRes))
                .cornerRadius(28.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Top Capsule Header Banner
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ImageProvider(bgDrawableRes))
                        .cornerRadius(100.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_capsule_horizon),
                            contentDescription = "Horizon",
                            modifier = GlanceModifier.size(30.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = title,
                                style = TextStyle(
                                    color = ColorProvider(titleColor),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = timeGreeting,
                                style = TextStyle(
                                    color = ColorProvider(subtitleColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                maxLines = 1
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_weather_sun),
                                contentDescription = "Weather",
                                modifier = GlanceModifier.size(16.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = tempDisplayStr,
                                style = TextStyle(
                                    color = ColorProvider(titleColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Energy & Focus Metric row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_energy),
                            contentDescription = "Energy",
                            modifier = GlanceModifier.size(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "Năng lượng: $energyScore%",
                            style = TextStyle(
                                color = ColorProvider(if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Text(
                        text = "Nova AI Active ⚡",
                        style = TextStyle(
                            color = ColorProvider(subColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Schedule Section: Tasks List wrapped in nested Column (< 10 elements per container)
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lịch trình & Việc ưu tiên",
                        style = TextStyle(
                            color = ColorProvider(subColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Task item 1
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_task),
                                contentDescription = "Task 1",
                                modifier = GlanceModifier.size(15.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = task1,
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(5.dp))

                    // Task item 2
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_task),
                                contentDescription = "Task 2",
                                modifier = GlanceModifier.size(15.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = task2,
                                style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Bottom Section: Mini Widgets (Chánh niệm & Âm nhạc thư giãn)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chánh niệm Card
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(14.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_mindful),
                                contentDescription = "Mindful",
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Chánh niệm",
                                    style = TextStyle(
                                        color = ColorProvider(if (isLight) Color(0xFF7C3AED) else Color(0xFFA78BFA)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "3 phút thở sâu",
                                    style = TextStyle(
                                        color = ColorProvider(subColor),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Âm nhạc Card
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ImageProvider(subCardRes))
                            .cornerRadius(14.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_music),
                                contentDescription = "Music",
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Âm nhạc Lofi",
                                    style = TextStyle(
                                        color = ColorProvider(if (isLight) Color(0xFFDB2777) else Color(0xFFF472B6)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Sóng não Beta",
                                    style = TextStyle(
                                        color = ColorProvider(subColor),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
