package com.example.feature.island

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.NovaPulseApp
import com.example.core.database.AppDatabase
import com.example.core.overlay.ServiceLifecycleOwner
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.PredictedSleepWindow
import com.example.core.repository.RealCalendarEvent
import com.example.core.repository.RealHealthData
import com.example.core.repository.RealScreenTimeData
import com.example.core.repository.RealWeatherData
import com.example.core.repository.RoutineRepository
import com.example.core.repository.ScreenTimeRepository
import com.example.core.repository.WeatherCacheManager
import com.example.core.repository.WeatherRepository
import com.example.core.sensor.StepCounterManager
import com.example.feature.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PassthroughFrameLayout(context: Context) : FrameLayout(context) {
    var isExpanded: Boolean = false
    var activePillWidthDp: Int = 230
    var onCollapseRequested: (() -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false
        if (ev.action == MotionEvent.ACTION_OUTSIDE && isExpanded) {
            onCollapseRequested?.invoke()
            return false
        }
        return super.dispatchTouchEvent(ev)
    }
}

private data class IslandRealDeviceSnapshot(
    val weather: RealWeatherData? = null,
    val calendarLabel: String = "Today's Schedule",
    val calendarEvents: List<RealCalendarEvent> = emptyList(),
    val screenTime: RealScreenTimeData? = null,
    val healthData: RealHealthData? = null,
    val sleepWindow: PredictedSleepWindow = PredictedSleepWindow()
)

class DynamicIslandAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DynamicIslandService"

        var isRunning: Boolean = false
            private set

        fun isServiceEnabled(context: Context): Boolean {
            val expectedServiceName =
                "${context.packageName}/${DynamicIslandAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var prefsJob: Job? = null

    private var windowManager: WindowManager? = null
    private var keyguardManager: KeyguardManager? = null
    private lateinit var prefsManager: PreferencesManager
    private var stepCounterManager: StepCounterManager? = null

    private var overlayView: View? = null
    private var composeChildView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    private var isEnabled = false
    private var currentYOffset = 10
    private var isLockscreenAllowed = true
    private var isLockscreenOnly = true
    private var overlayParams: WindowManager.LayoutParams? = null
    private var isIslandExpanded = false
    private val externalRefreshTick = kotlinx.coroutines.flow.MutableStateFlow(0)

    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON received.")
                    lifecycleOwner?.onResume()
                    stepCounterManager?.startListening()
                    externalRefreshTick.value++
                    if (overlayView == null && isEnabled && isLockscreenAllowed) {
                        showOrUpdateIsland(
                            yOffsetDp = currentYOffset,
                            showOnLockscreen = true,
                            position = currentPosition
                        )
                    }
                    if (isEnabled) {
                        evaluateIslandVisibility()
                    }
                }

                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User unlocked device (USER_PRESENT).")
                    if (isLockscreenOnly) {
                        setOverlayVisible(false)
                    } else {
                        evaluateIslandVisibility()
                    }
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF received.")
                    setOverlayVisible(false)
                    stepCounterManager?.stopListening()
                    lifecycleOwner?.onPause()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "DynamicIslandAccessibilityService connected.")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 150L
        serviceInfo = info

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        prefsManager = (application as? NovaPulseApp)?.preferencesManager ?: PreferencesManager(this)
        stepCounterManager = StepCounterManager(this).also { it.startListening() }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            lockscreenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startObservingPreferences()
    }

    private data class ConfigSnapshot(
        val enabled: Boolean,
        val yOffset: Int,
        val widthDp: Int,
        val heightDp: Int,
        val showOnLockscreen: Boolean,
        val lockscreenOnly: Boolean,
        val position: String
    )

    private var currentPosition: String = "BOTTOM"
    private var currentWidthDp = 320
    private var currentHeightDp = 42

    private fun startObservingPreferences() {
        prefsJob?.cancel()
        prefsJob = serviceScope.launch {
            combine(
                prefsManager.dynamicIslandEnabled,
                prefsManager.dynamicIslandYOffset,
                prefsManager.dynamicIslandWidth,
                prefsManager.dynamicIslandHeight,
                prefsManager.dynamicIslandLockscreen,
                prefsManager.dynamicIslandLockscreenOnly,
                prefsManager.dynamicIslandPosition
            ) { args: Array<Any> ->
                ConfigSnapshot(
                    enabled = args[0] as Boolean,
                    yOffset = args[1] as Int,
                    widthDp = args[2] as Int,
                    heightDp = args[3] as Int,
                    showOnLockscreen = args[4] as Boolean,
                    lockscreenOnly = args[5] as Boolean,
                    position = args[6] as String
                )
            }.collect { config ->
                isEnabled = config.enabled
                currentYOffset = config.yOffset
                currentWidthDp = config.widthDp
                currentHeightDp = config.heightDp
                isLockscreenAllowed = config.showOnLockscreen
                isLockscreenOnly = config.lockscreenOnly
                currentPosition = config.position

                evaluateIslandVisibility()
            }
        }
    }

    private fun setOverlayVisible(visible: Boolean) {
        val view = overlayView ?: return
        if (visible) {
            // Ensure Compose WindowRecomposer frame clock is always active when visible on Lock Screen
            lifecycleOwner?.onResume()
            stepCounterManager?.startListening()
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
            }
        } else {
            if (isIslandExpanded) {
                isIslandExpanded = false
                (view as? PassthroughFrameLayout)?.let {
                    it.isExpanded = false
                    it.onCollapseRequested?.invoke()
                }
                updateOverlayWindowDimensions(false)
            }
            if (view.visibility != View.GONE) {
                view.visibility = View.GONE
            }
        }
    }

    private fun evaluateIslandVisibility() {
        if (!isEnabled) {
            setOverlayVisible(false)
            return
        }

        val isLocked = keyguardManager?.isKeyguardLocked ?: false

        val shouldShow = if (isLockscreenOnly) {
            isLocked && isLockscreenAllowed && !isSystemUiOccluded(null)
        } else {
            !isSystemUiOccluded(null) || !isLocked
        }

        showOrUpdateIsland(
            yOffsetDp = currentYOffset,
            showOnLockscreen = true,
            position = currentPosition
        )

        setOverlayVisible(shouldShow)
    }

    private fun showOrUpdateIsland(yOffsetDp: Int, showOnLockscreen: Boolean, position: String = "BOTTOM") {
        val wm = windowManager ?: return
        val density = resources.displayMetrics.density
        val yOffsetPx = (yOffsetDp * density).toInt()

        // Use FLAG_LAYOUT_IN_SCREEN instead of FLAG_LAYOUT_NO_LIMITS so One UI Lock Screen hit-testing stays accurate
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        if (showOnLockscreen) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        }

        val gravityVal = if (position.equals("BOTTOM", ignoreCase = true)) {
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        val targetWidth = if (isIslandExpanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            ((currentWidthDp + 24) * density).toInt()
        }
        val params = (overlayView?.layoutParams as? WindowManager.LayoutParams ?: WindowManager.LayoutParams(
            targetWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )).apply {
            this.flags = flags
            this.gravity = gravityVal
            this.y = yOffsetPx
            this.windowAnimations = 0
            this.width = targetWidth

            @Suppress("DEPRECATION")
            this.preferredRefreshRate = 120.0f
        }
        overlayParams = params

        if (overlayView == null) {
            val owner = ServiceLifecycleOwner()
            lifecycleOwner = owner

            val rootLayout = PassthroughFrameLayout(this)
            owner.attachToView(rootLayout)

            val app = application as? NovaPulseApp
            val database = app?.database ?: AppDatabase.getInstance(this)
            val weatherRepository = app?.weatherRepository ?: WeatherRepository(this)
            val calendarRepository = app?.calendarRepository ?: CalendarRepository(this)
            val screenTimeRepository = app?.screenTimeRepository ?: ScreenTimeRepository(this)
            val healthConnectRepository = app?.healthConnectRepository ?: HealthConnectRepository(this)
            val routineRepository = app?.routineRepository ?: RoutineRepository(this)

            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                owner.attachToView(this)

                setContent {
                    var collapseTrigger by remember { mutableIntStateOf(0) }
                    var manualRefreshTrigger by remember { mutableIntStateOf(0) }
                    val screenOnTick by externalRefreshTick.collectAsState()

                    val energyScore by prefsManager.widgetAiEnergyScore.collectAsState(initial = 88)
                    val aiBriefing by prefsManager.widgetAiBriefing.collectAsState(initial = "")
                    val customWidthDp by prefsManager.dynamicIslandWidth.collectAsState(initial = 320)
                    val customHeightDp by prefsManager.dynamicIslandHeight.collectAsState(initial = 42)
                    val opacity by prefsManager.dynamicIslandOpacity.collectAsState(initial = 0.85f)
                    val colorPreset by prefsManager.dynamicIslandColorPreset.collectAsState(initial = "DYNAMIC")
                    val isFrostedGlass by prefsManager.dynamicIslandFrostedGlass.collectAsState(initial = true)
                    val lastUpdated by prefsManager.widgetLastUpdated.collectAsState(initial = 0L)

                    SideEffect {
                        rootLayout.activePillWidthDp = customWidthDp
                        rootLayout.onCollapseRequested = {
                            collapseTrigger++
                        }
                    }

                    val upcomingTasks by database.taskDao().getUpcomingTasks(5).collectAsState(initial = emptyList())

                    val startOfTodayMs = remember(screenOnTick) {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val todayExpenseTotal by database.expenseDao().getSpentSince(startOfTodayMs).collectAsState(initial = 0.0)
                    val recentExpenses by database.expenseDao().getRecentExpenses(1).collectAsState(initial = emptyList())

                    val stepSensorData by (stepCounterManager?.stepDataState
                        ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.core.sensor.StepCounterData())).collectAsState()

                    val realSnapshot by produceState(
                        initialValue = IslandRealDeviceSnapshot(),
                        key1 = lastUpdated,
                        key2 = manualRefreshTrigger,
                        key3 = screenOnTick
                    ) {
                        value = withContext(Dispatchers.IO) {
                            var weather = WeatherCacheManager.load(this@DynamicIslandAccessibilityService)
                            val now = System.currentTimeMillis()
                            if (weather == null || (now - weather.cacheTimestampMs) > 30 * 60_000L) {
                                val fresh = try { weatherRepository.fetchWeather() } catch (_: Exception) { null }
                                if (fresh != null) weather = fresh
                            }

                            val (calLabel, calEvents) = try {
                                calendarRepository.getUpcomingEvents()
                            } catch (_: Exception) {
                                "Today's Schedule" to emptyList()
                            }

                            val screenTime = try {
                                screenTimeRepository.getScreenTimeStats()
                            } catch (_: Exception) {
                                null
                            }

                            val health = try {
                                healthConnectRepository.fetchHealthData()
                            } catch (_: Exception) {
                                null
                            }

                            val sleepWindow = try {
                                routineRepository.predictSleepWindow()
                            } catch (_: Exception) {
                                PredictedSleepWindow()
                            }

                            IslandRealDeviceSnapshot(
                                weather = weather,
                                calendarLabel = calLabel,
                                calendarEvents = calEvents,
                                screenTime = screenTime,
                                healthData = health,
                                sleepWindow = sleepWindow
                            )
                        }
                    }

                    val weather = realSnapshot.weather
                    val tempInt = weather?.currentTempC?.let { Math.round(it).toInt() }
                    val weatherTempStr = if (tempInt != null) "${tempInt}°C" else "--°C"
                    val weatherCode = weather?.weatherCode ?: 0
                    val weatherLocation = weather?.locationName?.takeIf { it.isNotBlank() } ?: "Lokasi Anda"
                    val weatherCondition = weather?.conditionDescription?.takeIf { it.isNotBlank() } ?: "Memuat cuaca..."
                    val weatherHighC = weather?.dailyForecast?.firstOrNull()?.tempMaxC?.let { Math.round(it).toInt() }
                    val weatherLowC = weather?.dailyForecast?.firstOrNull()?.tempMinC?.let { Math.round(it).toInt() }

                    val realSteps = maxOf(stepSensorData.steps, realSnapshot.healthData?.currentSteps ?: 0)
                    val realStepGoal = realSnapshot.healthData?.stepGoal ?: 6000
                    val realCalories = maxOf(stepSensorData.calories, realSnapshot.healthData?.currentCalories ?: 0)
                    val realCalorieGoal = realSnapshot.healthData?.calorieGoal ?: 500
                    val realActiveMinutes = maxOf(stepSensorData.activeMinutes, realSnapshot.healthData?.currentActiveMinutes ?: 0)
                    val realActiveGoalMinutes = realSnapshot.healthData?.activeGoalMinutes ?: 60

                    val positionPref by prefsManager.dynamicIslandPosition.collectAsState(initial = currentPosition)
                    val isBottom = positionPref.equals("BOTTOM", ignoreCase = true)

                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = if (isBottom) Alignment.BottomCenter else Alignment.TopCenter
                    ) {
                        DynamicIslandView(
                            energyScore = energyScore,
                            weatherTemp = weatherTempStr,
                            weatherCode = weatherCode,
                            weatherLocation = weatherLocation,
                            weatherCondition = weatherCondition,
                            weatherHighC = weatherHighC,
                            weatherLowC = weatherLowC,
                            isBottom = isBottom,
                            aiBriefingText = aiBriefing,
                            upcomingTasks = upcomingTasks,
                            calendarLabel = realSnapshot.calendarLabel,
                            calendarEvents = realSnapshot.calendarEvents,
                            currentSteps = realSteps,
                            stepGoal = realStepGoal,
                            currentCalories = realCalories,
                            calorieGoal = realCalorieGoal,
                            currentActiveMinutes = realActiveMinutes,
                            activeGoalMinutes = realActiveGoalMinutes,
                            screenTimeData = realSnapshot.screenTime,
                            healthSleepDurationFormatted = realSnapshot.healthData?.sleepDurationFormatted.orEmpty(),
                            healthSleepSummary = realSnapshot.healthData?.sleepStageSummary.orEmpty(),
                            predictedSleepWindow = realSnapshot.sleepWindow,
                            todayExpenseTotal = todayExpenseTotal ?: 0.0,
                            latestExpense = recentExpenses.firstOrNull(),
                            customWidthDp = customWidthDp,
                            customHeightDp = customHeightDp,
                            opacity = opacity,
                            colorPreset = colorPreset,
                            isFrostedGlass = isFrostedGlass,
                            collapseTrigger = collapseTrigger,
                            onExpandStateChanged = { expanded ->
                                isIslandExpanded = expanded
                                rootLayout.isExpanded = expanded
                                updateOverlayWindowDimensions(expanded)
                            },
                            onCompleteTask = { taskId ->
                                serviceScope.launch(Dispatchers.IO) {
                                    try {
                                        database.taskDao().setCompleted(taskId, true)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error completing task from lockscreen: ${e.message}")
                                    }
                                }
                            },
                            onOpenApp = {
                                val intent = Intent(this@DynamicIslandAccessibilityService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("EXTRA_OPEN_NOW_BRIEF", true)
                                }
                                startActivity(intent)
                            },
                            onRefresh = {
                                manualRefreshTrigger++
                                WidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
                            }
                        )
                    }
                }
            }
            composeChildView = composeView

            rootLayout.addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = gravityVal
                }
            )

            owner.onResume()
            overlayView = rootLayout

            try {
                wm.addView(rootLayout, params)
                Log.d(TAG, "Dynamic Island added to WindowManager.")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding Dynamic Island overlay", e)
            }
        } else {
            (composeChildView?.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                if (lp.gravity != gravityVal) {
                    lp.gravity = gravityVal
                    composeChildView?.layoutParams = lp
                }
            }
            try {
                wm.updateViewLayout(overlayView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Dynamic Island overlay layout", e)
            }
        }
    }

    private fun updateOverlayWindowDimensions(expanded: Boolean) {
        val wm = windowManager ?: return
        val view = overlayView ?: return
        val params = overlayParams ?: return
        val density = resources.displayMetrics.density

        val targetW = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            ((currentWidthDp + 24) * density).toInt()
        }

        if (params.width != targetW) {
            params.width = targetW
            try {
                wm.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Dynamic Island overlay dimensions", e)
            }
        }
    }

    private fun removeIsland() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                lifecycleOwner?.onPause()
                lifecycleOwner?.onDestroy()
                lifecycleOwner = null
                composeChildView = null
                overlayView = null
                Log.d(TAG, "Dynamic Island removed from WindowManager.")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing Dynamic Island overlay", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isEnabled) return

        val eventPkg = event.packageName?.toString().orEmpty()
        val myPkg = packageName

        // Ignore events triggered by our own Dynamic Island overlay window so interactions never get interrupted
        if (eventPkg.equals(myPkg, ignoreCase = true)) {
            return
        }

        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (!isLocked) {
            if (isLockscreenOnly) {
                setOverlayVisible(false)
            }
            return
        }

        // Keep the expanded Live Notification open while the user is actively interacting with it
        // unless another full-screen app (Camera, Phone, Alarm) or PIN bouncer explicitly opens.
        if (eventPkg.isNotBlank() &&
            !eventPkg.equals("com.android.systemui", ignoreCase = true) &&
            !eventPkg.contains("aodservice", ignoreCase = true) &&
            !eventPkg.contains("inputmethod", ignoreCase = true) &&
            !eventPkg.contains("honeyboard", ignoreCase = true) &&
            !eventPkg.equals(myPkg, ignoreCase = true)
        ) {
            setOverlayVisible(false)
            return
        }

        val isOccluded = isSystemUiOccluded(event)
        if (isOccluded) {
            setOverlayVisible(false)
        } else if (isLockscreenAllowed) {
            setOverlayVisible(true)
        }
    }

    private fun isSystemUiOccluded(event: AccessibilityEvent?): Boolean {
        if (event?.isPassword == true) return true
        try {
            if (event?.source?.isPassword == true) return true
        } catch (_: Exception) {
        }

        val myPkg = packageName
        val eventPkg = event?.packageName?.toString().orEmpty()

        if (eventPkg.isNotBlank() &&
            !eventPkg.equals("com.android.systemui", ignoreCase = true) &&
            !eventPkg.contains("aodservice", ignoreCase = true) &&
            !eventPkg.contains("inputmethod", ignoreCase = true) &&
            !eventPkg.contains("honeyboard", ignoreCase = true) &&
            !eventPkg.equals(myPkg, ignoreCase = true)
        ) {
            return true
        }

        if (event != null && eventPkg.contains("systemui", ignoreCase = true)) {
            val className = event.className?.toString().orEmpty()
            val textStr = event.text.joinToString(" ")

            if (className.contains("PinPad", ignoreCase = true) ||
                className.contains("SecPin", ignoreCase = true) ||
                className.contains("SecPassword", ignoreCase = true) ||
                className.contains("SecPattern", ignoreCase = true) ||
                className.contains("KeyguardBouncer", ignoreCase = true)
            ) {
                return true
            }

            if (textStr.contains("PIN code", ignoreCase = true) ||
                textStr.contains("Passcode", ignoreCase = true) ||
                textStr.contains("Enter PIN", ignoreCase = true) ||
                textStr.contains("Masukkan PIN", ignoreCase = true) ||
                textStr.contains("Draw pattern", ignoreCase = true) ||
                textStr.contains("Gambar pola", ignoreCase = true)
            ) {
                return true
            }
        }

        // Verify visible PIN/Password bouncer or expanded Quick Settings in active window
        try {
            val root = rootInActiveWindow
            if (root != null) {
                val rootPkg = root.packageName?.toString().orEmpty()
                if (rootPkg.isNotBlank() &&
                    !rootPkg.equals("com.android.systemui", ignoreCase = true) &&
                    !rootPkg.contains("aodservice", ignoreCase = true) &&
                    !rootPkg.contains("inputmethod", ignoreCase = true) &&
                    !rootPkg.contains("honeyboard", ignoreCase = true) &&
                    !rootPkg.equals(myPkg, ignoreCase = true)
                ) {
                    return true
                }

                val bouncerViewIds = listOf(
                    "com.android.systemui:id/keyguard_pin_view",
                    "com.android.systemui:id/sec_keyguard_pin_view",
                    "com.android.systemui:id/pin_pad",
                    "com.android.systemui:id/keyguard_password_view",
                    "com.android.systemui:id/keyguard_pattern_view",
                    "com.android.systemui:id/lockPatternView"
                )
                val rect = Rect()
                for (id in bouncerViewIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(id)
                    val hasVisibleBouncer = nodes.any { node ->
                        node.getBoundsInScreen(rect)
                        node.isVisibleToUser && rect.width() > 100 && rect.height() > 100
                    }
                    if (hasVisibleBouncer) return true
                }

                val qsViewIds = listOf(
                    "com.android.systemui:id/sec_quick_settings_page",
                    "com.android.systemui:id/qs_frame"
                )
                val minQsHeight = resources.displayMetrics.heightPixels / 3
                for (id in qsViewIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(id)
                    val hasExpandedQs = nodes.any { node ->
                        node.getBoundsInScreen(rect)
                        node.isVisibleToUser && rect.height() > minQsHeight
                    }
                    if (hasExpandedQs) return true
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            unregisterReceiver(lockscreenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        stepCounterManager?.stopListening()
        prefsJob?.cancel()
        serviceScope.cancel()
        removeIsland()
        Log.d(TAG, "DynamicIslandAccessibilityService destroyed.")
    }
}
