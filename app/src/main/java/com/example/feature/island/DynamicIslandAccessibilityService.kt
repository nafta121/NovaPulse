package com.example.feature.island

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.MainActivity
import com.example.NovaPulseApp
import com.example.core.overlay.ServiceLifecycleOwner
import com.example.core.preferences.PreferencesManager
import com.example.feature.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class PassthroughFrameLayout(context: Context) : FrameLayout(context) {
    var isExpanded: Boolean = false
    var activePillWidthDp: Int = 230
    var onCollapseRequested: (() -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false
        if (isExpanded) {
            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                onCollapseRequested?.invoke()
                return false
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

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

    private var overlayView: View? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    private var isEnabled = false
    private var currentYOffset = 36
    private var isLockscreenAllowed = true
    private var isLockscreenOnly = true
    private var overlayParams: WindowManager.LayoutParams? = null
    private var isIslandExpanded = false

    // BroadcastReceiver for screen on/off and user unlock (USER_PRESENT)
    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON received.")
                    lifecycleOwner?.onResume()
                    if (overlayView == null && isEnabled && isLockscreenAllowed) {
                        showOrUpdateIsland(
                            yOffsetDp = currentYOffset,
                            showOnLockscreen = true,
                            position = currentPosition
                        )
                    } else if (isEnabled) {
                        evaluateIslandVisibility()
                    }
                }

                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User unlocked device (USER_PRESENT).")
                    if (isLockscreenOnly) {
                        overlayView?.visibility = android.view.View.GONE
                    } else {
                        evaluateIslandVisibility()
                    }
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF received.")
                    overlayView?.visibility = android.view.View.GONE
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

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            lockscreenReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
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

    private fun evaluateIslandVisibility() {
        if (!isEnabled) {
            overlayView?.visibility = android.view.View.GONE
            return
        }

        val isLocked = keyguardManager?.isKeyguardLocked ?: false

        val shouldShow = if (isLockscreenOnly) {
            isLocked && isLockscreenAllowed && !isSystemUiOccluded(null)
        } else {
            !isSystemUiOccluded(null) || !isLocked
        }

        if (isEnabled) {
            showOrUpdateIsland(yOffsetDp = currentYOffset, showOnLockscreen = true, position = currentPosition)
        }

        overlayView?.visibility = if (shouldShow) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showOrUpdateIsland(yOffsetDp: Int, showOnLockscreen: Boolean, position: String = "BOTTOM") {
        val wm = windowManager ?: return
        val density = resources.displayMetrics.density
        val yOffsetPx = (yOffsetDp * density).toInt()

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

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

            val composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                owner.attachToView(this)

                setContent {
                    var collapseTrigger by remember { mutableIntStateOf(0) }
                    rootLayout.onCollapseRequested = {
                        collapseTrigger++
                    }

                    val energyScore by prefsManager.widgetAiEnergyScore.collectAsState(initial = 92)
                    val aiBriefing by prefsManager.widgetAiBriefing.collectAsState(initial = "")
                    val customWidthDp by prefsManager.dynamicIslandWidth.collectAsState(initial = 230)
                    rootLayout.activePillWidthDp = customWidthDp
                    val customHeightDp by prefsManager.dynamicIslandHeight.collectAsState(initial = 42)
                    val opacity by prefsManager.dynamicIslandOpacity.collectAsState(initial = 0.65f)
                    val colorPreset by prefsManager.dynamicIslandColorPreset.collectAsState(initial = "DYNAMIC")
                    val isFrostedGlass by prefsManager.dynamicIslandFrostedGlass.collectAsState(initial = true)
                    val lastUpdated by prefsManager.widgetLastUpdated.collectAsState(initial = 0L)

                    val database =
                        com.example.core.database.AppDatabase.getInstance(this@DynamicIslandAccessibilityService)
                    val upcomingTasks by database.taskDao().getUpcomingTasks(3).collectAsState(initial = emptyList())

                    val cachedWeather by androidx.compose.runtime.produceState<com.example.core.repository.RealWeatherData?>(
                        initialValue = null,
                        key1 = lastUpdated
                    ) {
                        value = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            com.example.core.repository.WeatherCacheManager.load(this@DynamicIslandAccessibilityService)
                        }
                    }
                    val tempInt = if (cachedWeather != null) Math.round(cachedWeather.currentTempC).toInt() else 29
                    val weatherCode = cachedWeather?.weatherCode ?: 0
                    val weatherTempStr = "${tempInt}°C"

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
                            isBottom = isBottom,
                            aiBriefingText = aiBriefing,
                            upcomingTasks = upcomingTasks,
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
                            onOpenApp = {
                                val intent =
                                    Intent(this@DynamicIslandAccessibilityService, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        putExtra("EXTRA_OPEN_NOW_BRIEF", true)
                                    }
                                startActivity(intent)
                            },
                            onRefresh = {
                                WidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
                            }
                        )
                    }
                }
            }

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
                overlayView = null
                Log.d(TAG, "Dynamic Island removed from WindowManager.")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing Dynamic Island overlay", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isEnabled) return

        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (!isLocked) {
            if (isLockscreenOnly && overlayView?.visibility != android.view.View.GONE) {
                overlayView?.visibility = android.view.View.GONE
            }
            return
        }

        val eventPkg = event.packageName?.toString().orEmpty()
        val myPkg = packageName

        // ULTRA-FAST 0ms PATH: When Camera, Phone, Alarm or any other app opens, hide immediately in 0ms!
        if (eventPkg.isNotBlank() &&
            !eventPkg.equals("com.android.systemui", ignoreCase = true) &&
            !eventPkg.equals(myPkg, ignoreCase = true)
        ) {
            if (overlayView?.visibility != android.view.View.GONE) {
                overlayView?.visibility = android.view.View.GONE
            }
            return
        }

        val isOccluded = isSystemUiOccluded(event)
        if (isOccluded) {
            if (overlayView?.visibility != android.view.View.GONE) {
                overlayView?.visibility = android.view.View.GONE
            }
        } else {
            if (isLockscreenAllowed && overlayView?.visibility != android.view.View.VISIBLE) {
                overlayView?.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun isSystemUiOccluded(event: AccessibilityEvent?): Boolean {
        // 1. Password / PIN input flag
        if (event?.isPassword == true) return true
        try {
            if (event?.source?.isPassword == true) return true
        } catch (e: Exception) {
            // ignore
        }

        val myPkg = packageName
        val eventPkg = event?.packageName?.toString().orEmpty()

        // 2. Check if another app is opened over the lockscreen (Camera, Call, Clock/Alarm, Calculator, etc.)
        if (eventPkg.isNotBlank() &&
            !eventPkg.equals("com.android.systemui", ignoreCase = true) &&
            !eventPkg.equals(myPkg, ignoreCase = true)
        ) {
            return true
        }

        // 3. View Resource ID check
        try {
            val viewId = event?.source?.viewIdResourceName.orEmpty()
            if (viewId.contains("pin", ignoreCase = true) ||
                viewId.contains("password", ignoreCase = true) ||
                viewId.contains("pattern", ignoreCase = true) ||
                viewId.contains("bouncer", ignoreCase = true) ||
                viewId.contains("sec_qs", ignoreCase = true) ||
                viewId.contains("quick_settings", ignoreCase = true) ||
                viewId.contains("expanded_qs", ignoreCase = true) ||
                viewId.contains("now_bar", ignoreCase = true) ||
                viewId.contains("sec_now_bar", ignoreCase = true) ||
                viewId.contains("live_activity", ignoreCase = true) ||
                viewId.contains("ongoing_activity", ignoreCase = true) ||
                viewId.contains("live_notification", ignoreCase = true) ||
                viewId.contains("facewidget_now_bar", ignoreCase = true)
            ) {
                return true
            }
        } catch (e: Exception) {
            // ignore
        }

        // 4. Check all active system windows in AccessibilityService
        try {
            val winList = windows
            if (!winList.isNullOrEmpty()) {
                for (w in winList) {
                    val title = w.title?.toString().orEmpty()
                    if (title.contains("QuickSettings", ignoreCase = true) ||
                        title.contains("SecQuick", ignoreCase = true) ||
                        title.contains("Bouncer", ignoreCase = true) ||
                        title.contains("KeyguardSecurity", ignoreCase = true) ||
                        title.contains("NowBar", ignoreCase = true)
                    ) {
                        if (w.isFocused || w.isActive) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // 5. Class name and content description checks for active PIN, Quick Settings, or Samsung Now Bar
        if (event != null) {
            val className = event.className?.toString().orEmpty()
            val desc = event.contentDescription?.toString().orEmpty()
            val textStr = event.text.joinToString(" ")

            if (eventPkg.contains("systemui", ignoreCase = true)) {
                // Specific Bouncer Keypad classes
                if (className.contains("PinPad", ignoreCase = true) ||
                    className.contains("SecPin", ignoreCase = true) ||
                    className.contains("SecPassword", ignoreCase = true) ||
                    className.contains("SecPattern", ignoreCase = true) ||
                    className.contains("KeyguardSecurity", ignoreCase = true) ||
                    className.contains("KeyguardBouncer", ignoreCase = true) ||
                    className.contains("Bouncer", ignoreCase = true)
                ) {
                    return true
                }

                // Quick Settings panel pull-down
                if (className.contains("QSPanel", ignoreCase = true) ||
                    className.contains("SecQS", ignoreCase = true) ||
                    className.contains("QuickSettings", ignoreCase = true) ||
                    className.contains("SecQuick", ignoreCase = true) ||
                    className.contains("QSContainer", ignoreCase = true) ||
                    className.contains("NotificationStackScroll", ignoreCase = true) ||
                    className.contains("NotificationPanel", ignoreCase = true)
                ) {
                    return true
                }

                // Samsung Now Bar / Live Activity
                if (className.contains("NowBar", ignoreCase = true) ||
                    className.contains("SecNowBar", ignoreCase = true) ||
                    className.contains("LiveActivity", ignoreCase = true)
                ) {
                    return true
                }
            }

            if (desc.contains("Quick settings", ignoreCase = true) ||
                desc.contains("Quick Settings", ignoreCase = true) ||
                desc.contains("Notification panel", ignoreCase = true) ||
                desc.contains("Notification shade", ignoreCase = true) ||
                desc.contains("Now bar", ignoreCase = true) ||
                desc.contains("Now Bar", ignoreCase = true)
            ) {
                return true
            }

            if (textStr.contains("PIN code", ignoreCase = true) ||
                textStr.contains("Passcode", ignoreCase = true) ||
                textStr.contains("Password", ignoreCase = true) ||
                textStr.contains("Pattern", ignoreCase = true) ||
                textStr.contains("Enter PIN", ignoreCase = true) ||
                textStr.contains("Draw pattern", ignoreCase = true)
            ) {
                return true
            }
        }

        // 6. Check root in active window for PIN bouncer, Quick Settings, Samsung Now Bar, or opened apps
        try {
            val root = rootInActiveWindow
            if (root != null) {
                val rootPkg = root.packageName?.toString().orEmpty()
                if (rootPkg.isNotBlank() &&
                    !rootPkg.equals("com.android.systemui", ignoreCase = true) &&
                    !rootPkg.equals(myPkg, ignoreCase = true)
                ) {
                    return true // An app (Camera, Phone, etc.) is currently covering the lockscreen
                }

                val occludedViewIds = listOf(
                    // PIN / Bouncer
                    "com.android.systemui:id/keyguard_pin_view",
                    "com.android.systemui:id/sec_keyguard_pin_view",
                    "com.android.systemui:id/pin_pad",
                    "com.android.systemui:id/keyguard_password_view",
                    "com.android.systemui:id/keyguard_pattern_view",
                    "com.android.systemui:id/lockPatternView",
                    "com.android.systemui:id/keyguard_security_container",
                    // Quick Settings
                    "com.android.systemui:id/sec_quick_settings_page",
                    "com.android.systemui:id/sec_qs_panel",
                    "com.android.systemui:id/quick_settings_panel",
                    "com.android.systemui:id/header_settings_container",
                    // Samsung Now Bar & Live Activity (One UI 7/8.5/9)
                    "com.android.systemui:id/sec_now_bar",
                    "com.android.systemui:id/now_bar",
                    "com.android.systemui:id/sec_now_bar_container",
                    "com.android.systemui:id/sec_live_notification_container",
                    "com.android.systemui:id/sec_ongoing_activity_container",
                    "com.android.systemui:id/sec_live_activity_view",
                    "com.android.systemui:id/now_bar_view",
                    "com.android.systemui:id/facewidget_now_bar",
                    "com.android.systemui:id/sec_media_container",
                    "com.android.systemui:id/face_widget_media_page",
                    "com.android.systemui:id/keyguard_media_controller"
                )
                for (id in occludedViewIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(id)
                    if (nodes.isNotEmpty()) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
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
        prefsJob?.cancel()
        serviceScope.cancel()
        removeIsland()
        Log.d(TAG, "DynamicIslandAccessibilityService destroyed.")
    }
}
