package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.example.NovaPulseApp
import com.example.core.ai.DeviceCapabilityDetector
import com.example.core.ai.DeviceSpecs
import com.example.core.ai.ModelDownloadState
import com.example.core.ai.ParsedNotificationResult
import com.example.core.database.ExpenseEntity
import com.example.core.database.NotificationLogEntity
import com.example.core.database.TaskEntity
import com.example.core.notification.SmartNotificationListenerService
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.RealCalendarEvent
import com.example.core.repository.RealHealthData
import com.example.core.repository.RealScreenTimeData
import com.example.core.repository.RealWeatherData
import com.example.core.repository.ScreenTimeRepository
import com.example.core.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class DashboardMetrics(
    val totalIntercepted: Int = 0,
    val regexMatched: Int = 0,
    val skippedZeroCost: Int = 0,
    val passRatePercent: Int = 0,
    val pendingTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalExpensesVnd: Double = 0.0
)

data class PermissionsState(
    val hasCalendarPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasNotificationListenerPermission: Boolean = false,
    val hasHealthPermission: Boolean = false,
    val hasActivityRecognitionPermission: Boolean = false
)

data class DynamicNowBrief(
    val title: String = "",
    val oneLiner: String = "",
    val weatherHighlight: String = "",
    val scheduleHighlight: String = "",
    val healthHighlight: String = "",
    val actionAdvice: String = "",
    val fullBriefText: String = "",
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val engineName: String = "Qwen2.5-0.5B (Local SLM)"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaPulseApp
    private val repository = app.repository
    private val downloadManager = app.downloadManager
    private val llmManager = app.llmManager
    private val preferencesManager = app.preferencesManager
    private val calendarRepository: CalendarRepository = app.calendarRepository
    private val weatherRepository: WeatherRepository = app.weatherRepository
    private val screenTimeRepository: ScreenTimeRepository = app.screenTimeRepository
    private val healthConnectRepository: HealthConnectRepository = app.healthConnectRepository
    private val geminiRepository = com.example.core.repository.GeminiAiRepository(application)

    val deviceSpecs: DeviceSpecs = DeviceCapabilityDetector.detect(application)

    val isOnboardingCompleted: StateFlow<Boolean> = preferencesManager.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = downloadManager.downloadStates
    val activeModelId: StateFlow<String> = downloadManager.activeModelId

    // Real Data from Room Database
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<NotificationLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListenerConnected: StateFlow<Boolean> = SmartNotificationListenerService.isConnected

    val blacklistedApps: StateFlow<Set<String>> = preferencesManager.blacklistedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isRegexPrefilterEnabled: StateFlow<Boolean> = preferencesManager.isRegexPrefilterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customPrompt: StateFlow<String> = preferencesManager.customPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Widget Customization Flows
    val widgetTransparency: StateFlow<Float> = preferencesManager.widgetTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val widgetColorPreset: StateFlow<String> = preferencesManager.widgetColorPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")

    val widgetShape: StateFlow<String> = preferencesManager.widgetShape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CAPSULE")

    val widgetShowSubtext: StateFlow<Boolean> = preferencesManager.widgetShowSubtext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Dynamic Island & Samsung Now Bar Flows
    val nowBarLiveEnabled: StateFlow<Boolean> = preferencesManager.nowBarLiveEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicIslandEnabled: StateFlow<Boolean> = preferencesManager.dynamicIslandEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dynamicIslandYOffset: StateFlow<Int> = preferencesManager.dynamicIslandYOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val dynamicIslandLockscreen: StateFlow<Boolean> = preferencesManager.dynamicIslandLockscreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicIslandLockscreenOnly: StateFlow<Boolean> = preferencesManager.dynamicIslandLockscreenOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicIslandScale: StateFlow<Float> = preferencesManager.dynamicIslandScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val dynamicIslandWidth: StateFlow<Int> = preferencesManager.dynamicIslandWidth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 230)

    val dynamicIslandHeight: StateFlow<Int> = preferencesManager.dynamicIslandHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 42)

    val dynamicIslandOpacity: StateFlow<Float> = preferencesManager.dynamicIslandOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.95f)

    val dynamicIslandFrostedGlass: StateFlow<Boolean> = preferencesManager.dynamicIslandFrostedGlass
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicIslandPosition: StateFlow<String> = preferencesManager.dynamicIslandPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TOP")

    val dynamicIslandColorPreset: StateFlow<String> = preferencesManager.dynamicIslandColorPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DYNAMIC")

    val enabledContentCards: StateFlow<Set<String>> = preferencesManager.enabledContentCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesManager.DEFAULT_ENABLED_CARDS)

    fun setContentCardEnabled(cardId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setContentCardEnabled(cardId, isEnabled)
        }
    }

    fun toggleContentCard(cardId: String) {
        viewModelScope.launch {
            preferencesManager.toggleContentCard(cardId)
        }
    }

    fun resetContentCardsToDefault() {
        viewModelScope.launch {
            preferencesManager.resetContentCardsToDefault()
        }
    }

    fun setNowBarLiveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNowBarLiveEnabled(enabled)
            if (enabled) {
                com.example.feature.island.NowBarLiveService.startOrRefresh(app)
            } else {
                com.example.feature.island.NowBarLiveService.stop(app)
            }
        }
    }

    fun setDynamicIslandEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandEnabled(enabled)
        }
    }

    fun setDynamicIslandYOffset(offsetDp: Int) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandYOffset(offsetDp)
        }
    }

    fun setDynamicIslandLockscreen(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandLockscreen(enabled)
        }
    }

    fun setDynamicIslandLockscreenOnly(lockscreenOnly: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandLockscreenOnly(lockscreenOnly)
        }
    }

    fun setDynamicIslandScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandScale(scale)
        }
    }

    fun setDynamicIslandWidth(widthDp: Int) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandWidth(widthDp)
        }
    }

    fun setDynamicIslandHeight(heightDp: Int) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandHeight(heightDp)
        }
    }

    fun setDynamicIslandOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandOpacity(opacity)
        }
    }

    fun setDynamicIslandFrostedGlass(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandFrostedGlass(enabled)
        }
    }

    fun setDynamicIslandPosition(position: String) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandPosition(position)
        }
    }

    fun setDynamicIslandColorPreset(preset: String) {
        viewModelScope.launch {
            preferencesManager.setDynamicIslandColorPreset(preset)
        }
    }

    fun updateWidgetConfig(
        transparency: Float,
        colorPreset: String,
        shape: String,
        showSubtext: Boolean
    ) {
        viewModelScope.launch {
            preferencesManager.setWidgetConfig(transparency, colorPreset, shape, showSubtext)
            try {
                com.example.feature.widget.NowBriefWidget().updateAll(app)
                com.example.feature.widget.GlanceContextWidget().updateAll(app)
            } catch (e: Exception) {
                // Ignore glance update errors if widget is not placed
            }
            com.example.feature.widget.NowBriefWidgetReceiver.triggerUpdate(app)
        }
    }

    fun saveWidgetConfig(
        transparency: Float,
        colorPreset: String,
        shape: String,
        showSubtext: Boolean
    ) {
        updateWidgetConfig(transparency, colorPreset, shape, showSubtext)
    }

    // Permissions State Flow
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    // Real Data Sources Flows
    private val _todayCalendarEvents = MutableStateFlow<List<RealCalendarEvent>>(emptyList())
    val todayCalendarEvents: StateFlow<List<RealCalendarEvent>> = _todayCalendarEvents.asStateFlow()

    private val _realWeatherData = MutableStateFlow<RealWeatherData?>(null)
    val realWeatherData: StateFlow<RealWeatherData?> = _realWeatherData.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private val _realScreenTimeData = MutableStateFlow<RealScreenTimeData?>(null)
    val realScreenTimeData: StateFlow<RealScreenTimeData?> = _realScreenTimeData.asStateFlow()

    // Real Health Connect Data (Samsung Health / Sleep & Heart Rate)
    private val _realHealthData = MutableStateFlow<RealHealthData?>(null)
    val realHealthData: StateFlow<RealHealthData?> = _realHealthData.asStateFlow()

    private val _isHealthLoading = MutableStateFlow(false)
    val isHealthLoading: StateFlow<Boolean> = _isHealthLoading.asStateFlow()

    private val _lifestyleTips = MutableStateFlow<List<com.example.core.ai.LifestyleTip>>(emptyList())
    val lifestyleTips: StateFlow<List<com.example.core.ai.LifestyleTip>> = _lifestyleTips.asStateFlow()

    // Dashboard Combined Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        repository.totalIntercepted,
        repository.regexMatched,
        repository.pendingTaskCount,
        repository.completedTaskCount,
        repository.totalSpent
    ) { intercepted, matched, pending, completed, spent ->
        val skipped = (intercepted - matched).coerceAtLeast(0)
        val passRate = if (intercepted > 0) ((matched.toFloat() / intercepted) * 100).toInt() else 0
        DashboardMetrics(
            totalIntercepted = intercepted,
            regexMatched = matched,
            skippedZeroCost = skipped,
            passRatePercent = passRate,
            pendingTasksCount = pending,
            completedTasksCount = completed,
            totalExpensesVnd = spent ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Audio / Voice Brief State
    private var tts: TextToSpeech? = null
    private val _isAudioBriefPlaying = MutableStateFlow(false)
    val isAudioBriefPlaying: StateFlow<Boolean> = _isAudioBriefPlaying.asStateFlow()

    // Dynamic Capsule Now Brief (Local AI Qwen)
    private val _nowBrief = MutableStateFlow<DynamicNowBrief?>(null)
    val nowBrief: StateFlow<DynamicNowBrief?> = _nowBrief.asStateFlow()

    private val _isGeneratingNowBrief = MutableStateFlow(false)
    val isGeneratingNowBrief: StateFlow<Boolean> = _isGeneratingNowBrief.asStateFlow()

    // Offline Local AI On-Device Inference States
    private val _briefingAiSummaryText = MutableStateFlow("")
    val briefingAiSummaryText: StateFlow<String> = _briefingAiSummaryText.asStateFlow()

    private val _isAiInferring = MutableStateFlow(false)
    val isAiInferring: StateFlow<Boolean> = _isAiInferring.asStateFlow()

    private val _selectedAiModelMode = MutableStateFlow(com.example.core.ai.AiModelMode.STANDARD_350MB)
    val selectedAiModelMode: StateFlow<com.example.core.ai.AiModelMode> = _selectedAiModelMode.asStateFlow()

    private val _aiInferenceError = MutableStateFlow<String?>(null)
    val aiInferenceError: StateFlow<String?> = _aiInferenceError.asStateFlow()

    // One-Shot Entrance Animation State
    private val _hasPlayedEntranceAnimation = MutableStateFlow(false)
    val hasPlayedEntranceAnimation: StateFlow<Boolean> = _hasPlayedEntranceAnimation.asStateFlow()

    fun markEntranceAnimationPlayed() {
        _hasPlayedEntranceAnimation.value = true
    }

    private val stepCounterManager = com.example.core.sensor.StepCounterManager(application)
    private var lastRefreshTimestampMs: Long = 0L

    init {
        stepCounterManager.startListening()
        checkAndRefreshPermissions()

        // Register ContentObserver for system calendar changes
        calendarRepository.registerContentObserver {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (calendarRepository.isPermissionGranted()) {
                    val today = calendarRepository.getRemainingTodayEvents()
                    val tomorrow = calendarRepository.getTomorrowEvents()
                    _todayCalendarEvents.value = (today + tomorrow).distinctBy { it.id }
                }
                com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(getApplication())
            }
        }

        refreshAllRealData()

        viewModelScope.launch {
            stepCounterManager.stepDataState.collect { stepData ->
                if (stepData.steps > 0) {
                    val current = _realHealthData.value
                        ?: com.example.core.repository.RealHealthData(
                            sleepDurationMinutes = 0L,
                            sleepDurationFormatted = "",
                            sleepStageSummary = "Sleep not synced yet",
                            averageHeartRateBpm = 68,
                            minHeartRateBpm = 58,
                            maxHeartRateBpm = 86
                        )
                    val updatedSteps = maxOf(current.currentSteps, stepData.steps)
                    val updatedActive = maxOf(current.currentActiveMinutes, stepData.activeMinutes)
                    val updatedCal = maxOf(current.currentCalories, stepData.calories)

                    _realHealthData.value = current.copy(
                        currentSteps = updatedSteps,
                        currentActiveMinutes = updatedActive,
                        currentCalories = updatedCal,
                        isDataAvailable = true
                    )
                }
            }
        }

        try {
            tts = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val langResult = tts?.setLanguage(Locale.US)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.ENGLISH
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isAudioBriefPlaying.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isAudioBriefPlaying.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isAudioBriefPlaying.value = false
                        }
                    })
                }
            }
        } catch (e: Exception) {
            // Safe fallback if TTS is unavailable in test runner
        }
    }

    fun checkAndRefreshPermissions() {
        val hasCal = calendarRepository.isPermissionGranted()
        val hasLoc = weatherRepository.isLocationPermissionGranted()
        val hasUsage = screenTimeRepository.isUsageAccessGranted()
        val hasNotif = SmartNotificationListenerService.isPermissionGranted(app)
        val hasActivityPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        viewModelScope.launch {
            val prevLoc = _permissionsState.value.hasLocationPermission
            val hasHealth = healthConnectRepository.hasAllPermissions()
            _permissionsState.value = PermissionsState(
                hasCalendarPermission = hasCal,
                hasLocationPermission = hasLoc,
                hasUsageStatsPermission = hasUsage,
                hasNotificationListenerPermission = hasNotif,
                hasHealthPermission = hasHealth,
                hasActivityRecognitionPermission = hasActivityPermission
            )
            if (!prevLoc && hasLoc) {
                fetchRealWeather(force = true)
            }
        }
    }

    fun refreshAllRealData(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshTimestampMs < 10_000L) {
            return
        }
        lastRefreshTimestampMs = now

        checkAndRefreshPermissions()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Fetch Calendar Events off the Main thread if permission is granted (Today + Tomorrow)
            if (calendarRepository.isPermissionGranted()) {
                val today = calendarRepository.getRemainingTodayEvents()
                val tomorrow = calendarRepository.getTomorrowEvents()
                _todayCalendarEvents.value = (today + tomorrow).distinctBy { it.id }
            } else {
                _todayCalendarEvents.value = emptyList()
            }

            // 2. Fetch Screen Time Stats off the Main thread if permission is granted
            if (screenTimeRepository.isUsageAccessGranted()) {
                _realScreenTimeData.value = screenTimeRepository.getScreenTimeStats()
            } else {
                _realScreenTimeData.value = null
            }

            // 2.5 Refresh active MediaSession state (YouTube Music / Spotify / YouTube)
            com.example.core.music.RecentMediaTracker.refreshActiveSessions(getApplication())
        }

        // 3. Fetch Weather from Open-Meteo API
        fetchRealWeather(force = force)

        // 4. Fetch Health Connect Sleep & Heart Rate Records
        fetchRealHealthData()

        // 5. Run On-Device Local AI Inference & Lifestyle Recommendations
        runOnDeviceAiInference()
    }

    fun runOnDeviceAiInference(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isAiInferring.value = true
            _aiInferenceError.value = null
            try {
                val cal = java.util.Calendar.getInstance()
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val minute = cal.get(java.util.Calendar.MINUTE)
                val session = com.example.feature.widget.BriefingSession.fromHourAndMinute(hour, minute)

                val weather = _realWeatherData.value
                val tempC = weather?.currentTempC ?: 28.0
                val condition = weather?.conditionDescription ?: "cool"

                val health = _realHealthData.value
                val steps = health?.currentSteps ?: 34

                val tempRounded = Math.round(tempC).toInt()
                val summary = geminiRepository.getBriefingRecap(
                    temp = tempRounded,
                    weatherDesc = condition,
                    steps = steps,
                    sessionName = session.sessionTag,
                    forceRefresh = forceRefresh
                )
                _briefingAiSummaryText.value = summary

                // 2. Lifestyle Tips Recommendations
                val runner = com.example.core.ai.OnDeviceModelRunner(getApplication())
                val dynamicTips = com.example.core.ai.LifestyleRecommendationEngine.generateLifestyleRecommendations(
                    temp = tempRounded,
                    weather = condition,
                    session = session.sessionTag,
                    runner = runner
                )
                _lifestyleTips.value =
                    if (dynamicTips.isNotEmpty()) dynamicTips else com.example.core.ai.BriefingContentProvider.getLifestyleTips(
                        session,
                        tempRounded,
                        condition,
                        6
                    )

                // 3. Sync data to Glance Widget
                com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("LOCAL_AI", "Text generation error: ${e.message}")
                _aiInferenceError.value = "AI Error: ${e.message}"
            } finally {
                _isAiInferring.value = false
            }
        }
    }

    fun toggleLifestyleTipDone(tipId: String) {
        _lifestyleTips.value = _lifestyleTips.value.map { tip ->
            if (tip.id == tipId) tip.copy(isDone = !tip.isDone) else tip
        }
    }

    fun fetchLifestyleTips(forceRefresh: Boolean = false) {
        runOnDeviceAiInference(forceRefresh)
    }

    fun fetchRealHealthData() {
        viewModelScope.launch {
            _isHealthLoading.value = true
            val hasHealth = healthConnectRepository.hasAllPermissions()
            val health = if (hasHealth) {
                healthConnectRepository.fetchHealthData()
            } else {
                null
            }

            val stepData = stepCounterManager.getTodayStepData(
                healthConnectSteps = health?.currentSteps ?: 0,
                healthConnectCal = health?.currentCalories ?: 0,
                healthConnectActive = health?.currentActiveMinutes ?: 0
            )

            val baseHealth = health ?: com.example.core.repository.RealHealthData(
                sleepDurationMinutes = 0L,
                sleepDurationFormatted = "",
                sleepStageSummary = "Sleep not synced yet",
                averageHeartRateBpm = 68,
                minHeartRateBpm = 58,
                maxHeartRateBpm = 86
            )

            val mergedHealth = baseHealth.copy(
                currentSteps = if ((health?.currentSteps ?: 0) > 0) health!!.currentSteps else stepData.steps,
                currentActiveMinutes = if ((health?.currentActiveMinutes
                        ?: 0) > 0
                ) health!!.currentActiveMinutes else stepData.activeMinutes,
                currentCalories = if ((health?.currentCalories
                        ?: 0) > 0
                ) health!!.currentCalories else stepData.calories,
                isDataAvailable = true
            )

            _realHealthData.value = mergedHealth
            _isHealthLoading.value = false
        }
    }

    fun openHealthConnectSettings(context: Context) {
        healthConnectRepository.openHealthConnectSettings(context)
    }

    fun fetchRealWeather(lat: Double? = null, lon: Double? = null, force: Boolean = false) {
        viewModelScope.launch {
            // Read cached data off the Main thread to avoid disk I/O + JSON parsing jank
            val (cached, isStale) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val c = com.example.core.repository.WeatherCacheManager.load(getApplication())
                val stale = com.example.core.repository.WeatherCacheManager.isStale(getApplication())
                c to stale
            }
            if (cached != null && _realWeatherData.value == null) {
                _realWeatherData.value = cached
            }

            // Check if cache is still fresh (<30 mins) and not forced to refresh -> skip API call
            if (!force && !isStale && lat == null && lon == null && cached != null) {
                _isWeatherLoading.value = false
                return@launch
            }

            _isWeatherLoading.value = true
            val weather = weatherRepository.fetchWeather(lat, lon)
            _realWeatherData.value = weather
            _isWeatherLoading.value = false
            fetchLifestyleTips(forceRefresh = true)
        }
    }

    /** ML On-Device Card Interaction Recording */
    fun onCardClicked(cardId: String) {
        com.example.core.ml.InternalMlEngine.getInstance(getApplication()).recordCardClick(cardId)
    }

    fun onCardDismissed(cardId: String) {
        com.example.core.ml.InternalMlEngine.getInstance(getApplication()).recordCardDismiss(cardId)
    }

    fun resetAllData() {
        viewModelScope.launch {
            com.example.core.repository.RoutineRepository(getApplication()).clearAllRoutineLogs()
            com.example.core.ml.InternalMlEngine.getInstance(getApplication()).resetAllMlData()
            // Clear SharedPreferences for card preferences
            getApplication<android.app.Application>()
                .getSharedPreferences("card_preference_ml_stats", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun toggleAudioBrief(summaryText: String) {
        if (_isAudioBriefPlaying.value) {
            stopAudioBrief()
        } else {
            playAudioBrief(summaryText)
        }
    }

    fun playAudioBrief(text: String) {
        if (tts != null) {
            _isAudioBriefPlaying.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_pulse_daily_brief")
        } else {
            _isAudioBriefPlaying.value = !_isAudioBriefPlaying.value
        }
    }

    fun stopAudioBrief() {
        try {
            tts?.stop()
        } catch (e: Exception) {
        }
        _isAudioBriefPlaying.value = false
    }

    fun generateNowBrief(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        viewModelScope.launch {
            _isGeneratingNowBrief.value = true
            kotlinx.coroutines.delay(450) // Smooth on-device SLM latency feel

            val fallbackTitle = when (hour) {
                in 5..10 -> "Morning brief"
                in 11..16 -> "Good Afternoon"
                in 17..21 -> "Evening brief"
                else -> "Late night recap"
            }

            val weather = _realWeatherData.value
            val weatherSnippet = if (weather != null) {
                "${weather.conditionDescription}, ${
                    Math.round(weather.currentTempC).toInt()
                }°C (feels like ${Math.round(weather.apparentTempC).toInt()}°C) in ${weather.locationName}."
            } else {
                when (hour) {
                    in 5..10 -> "Clear morning sky, 26°C - 31°C, fresh and airy."
                    in 11..16 -> "Sunny and warm, 33°C, great for focused work."
                    in 17..21 -> "Cool evening breeze, 28°C, very pleasant weather."
                    else -> "Cool and quiet night, 24°C, ideal for resting."
                }
            }

            val pendingTasks = allTasks.value.filter { !it.isCompleted }
            val calEvents = _todayCalendarEvents.value
            val scheduleSnippet = when {
                calEvents.isNotEmpty() && pendingTasks.isNotEmpty() ->
                    "You have ${calEvents.size} calendar events and ${pendingTasks.size} pending tasks. Highlight: \"${calEvents.first().title}\"."

                calEvents.isNotEmpty() ->
                    "Today's schedule: ${calEvents.size} events. Up next: \"${calEvents.first().title}\"."

                pendingTasks.isNotEmpty() ->
                    "You have ${pendingTasks.size} unfinished tasks. Priority: \"${pendingTasks.first().title}\"."

                else -> "No conflicting schedules or pending tasks. You have free time."
            }

            val health = _realHealthData.value
            val healthSnippet = if (health != null && health.isDataAvailable) {
                "Sleep reached ${health.sleepDurationFormatted}, average heart rate ${health.averageHeartRateBpm} BPM (${health.sleepStageSummary})."
            } else {
                when (hour) {
                    in 5..10 -> "Your body is ready for a new day. Estimated energy: 92%."
                    in 11..16 -> "Energy maintained at 78%. Remember to stay hydrated and rest your eyes."
                    in 17..21 -> "Most energy has been spent (65%). Good time for a walk or stretching."
                    else -> "Circadian rhythm is shifting to rest mode. Time to unwind completely."
                }
            }

            val advice = when (hour) {
                in 5..10 -> "Drink a glass of warm water and prioritize your hardest task before 10:30."
                in 11..16 -> "Have a balanced lunch, maintain good posture, and take a 15-minute power nap if needed."
                in 17..21 -> "Wrap up your workday, take a light walk, and enjoy your dinner."
                else -> "Mute work notifications and enable night mode for a deep, restful sleep."
            }

            val briefingGenerator = com.example.core.ai.BriefingAiGenerator(app, llmManager)
            val briefingContext = com.example.core.ai.BriefingContext(
                hourOfDay = hour,
                weatherTempC = weather?.currentTempC,
                weatherCondition = weather?.conditionDescription,
                energyScore = 85,
                sleepHoursFormatted = health?.sleepDurationFormatted,
                remainingCalendarEventsCount = calEvents.size,
                topTaskTitle = pendingTasks.firstOrNull()?.title,
                screenTimeFormatted = _realScreenTimeData.value?.formattedDuration,
                stepsCount = health?.currentSteps ?: 0
            )
            val aiResult = briefingGenerator.generate(briefingContext)

            // Save to DataStore & trigger Glance widget update
            try {
                preferencesManager.setWidgetAiBriefing(
                    briefing = aiResult.dynamicBriefing,
                    energyScore = aiResult.energyScore
                )
                com.example.feature.widget.NowBriefWidgetReceiver.triggerUpdate(app)
            } catch (e: Exception) {
            }

            val oneLiner = aiResult.dynamicBriefing
            val title = aiResult.greetingHeadline

            val fullText = "$title. $weatherSnippet $scheduleSnippet $healthSnippet Advice for you: $advice"

            val activeModelName = when (activeModelId.value) {
                "qwen2.5_0.5b" -> "Qwen2.5-0.5B (Local SLM)"
                "deepseek_r1_1.5b" -> "DeepSeek-R1-1.5B (Local SLM)"
                else -> "Qwen-NovaPulse Engine (On-Device)"
            }

            _nowBrief.value = DynamicNowBrief(
                title = title,
                oneLiner = oneLiner,
                weatherHighlight = weatherSnippet,
                scheduleHighlight = scheduleSnippet,
                healthHighlight = healthSnippet,
                actionAdvice = advice,
                fullBriefText = fullText,
                generatedAtMillis = System.currentTimeMillis(),
                engineName = activeModelName
            )

            _isGeneratingNowBrief.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
        }
        llmManager.close()
        calendarRepository.unregisterContentObserver()
    }

    fun dismissCatchUpLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    fun confirmTaskFromCatchUp(log: NotificationLogEntity) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = if (log.summary.isNotBlank()) log.summary else log.title,
                    datetime = System.currentTimeMillis() + 2 * 3600 * 1000L, // default 2 hours later
                    priority = "HIGH",
                    sourceApp = log.appName,
                    rawSnippet = log.content
                )
            )
        }
    }

    fun confirmExpenseFromCatchUp(log: NotificationLogEntity) {
        viewModelScope.launch {
            val isUsd = log.content.contains("$") || log.content.contains("usd", ignoreCase = true)
            val rawNum = com.example.core.ai.LocalLlmManager.parseCurrencyAmount(log.content)
            repository.insertExpense(
                ExpenseEntity(
                    amount = rawNum,
                    currency = if (isUsd) "USD" else "VND",
                    category = if (log.content.contains("grab", true) || log.content.contains(
                            "food",
                            true
                        )
                    ) "Food & Dining" else "Bills & Utilities",
                    note = if (log.summary.isNotBlank()) log.summary else log.title,
                    sourceApp = log.appName,
                    rawSnippet = log.content
                )
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun startModelDownload(modelId: String) {
        downloadManager.startOrResumeDownload(modelId)
    }

    fun pauseModelDownload(modelId: String) {
        downloadManager.pauseDownload(modelId)
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            downloadManager.setActiveModel(modelId)
            preferencesManager.setActiveModel(modelId)
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            downloadManager.deleteModelFile(modelId)
            if (activeModelId.value == modelId) {
                preferencesManager.setActiveModel("novapulse_hybrid")
            }
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, !task.isCompleted)
            com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(getApplication())
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
            com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(getApplication())
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            com.example.feature.widget.WidgetUpdateWorker.enqueueImmediateUpdate(getApplication())
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun toggleBlacklist(packageName: String) {
        viewModelScope.launch {
            preferencesManager.toggleBlacklistApp(packageName)
        }
    }

    fun setRegexPrefilter(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setRegexPrefilter(enabled)
        }
    }

    fun setCustomPrompt(prompt: String) {
        viewModelScope.launch {
            preferencesManager.setCustomPrompt(prompt)
        }
    }

    fun simulateIncomingNotification(
        sourceApp: String,
        packageName: String,
        title: String,
        content: String
    ) {
        app.parserAgent.processNotification(
            packageName = packageName,
            appName = sourceApp,
            title = title,
            content = content
        )
    }

    fun simulateDefaultFeed() {
        simulateIncomingNotification(
            sourceApp = "Zalo",
            packageName = "com.zing.zalo",
            title = "Tuan (Project Lead)",
            content = "Tomorrow at 9 AM we have a sprint review meeting and quarterly report deadline, everyone!"
        )
        simulateIncomingNotification(
            sourceApp = "Vietcombank",
            packageName = "com.vcb.digibank",
            title = "Account Balance Update",
            content = "Acct 1029384758: -250,000 VND at 14:30. Note: Lunch payment ShopeeFood. Balance: 15,240,000 VND."
        )
    }
}
