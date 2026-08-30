package com.example

import android.app.Application
import com.example.core.ai.LocalLlmManager
import com.example.core.ai.ModelDownloadManager
import com.example.core.database.AppDatabase
import com.example.core.database.NovaPulseRepository
import com.example.core.notification.NotificationParserAgent
import com.example.core.preferences.PreferencesManager
import com.example.core.repository.CalendarRepository
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.RoutineRepository
import com.example.core.repository.ScreenTimeRepository
import com.example.core.repository.WeatherRepository
import com.example.core.routine.UserRoutineDetector
import com.example.feature.widget.WidgetUpdateWorker

class NovaPulseApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: NovaPulseRepository
        private set
    lateinit var downloadManager: ModelDownloadManager
        private set
    lateinit var llmManager: LocalLlmManager
        private set
    lateinit var parserAgent: NotificationParserAgent
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var calendarRepository: CalendarRepository
        private set
    lateinit var weatherRepository: WeatherRepository
        private set
    lateinit var screenTimeRepository: ScreenTimeRepository
        private set
    lateinit var healthConnectRepository: HealthConnectRepository
        private set
    lateinit var routineRepository: RoutineRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getInstance(this)
        repository = NovaPulseRepository(database)
        downloadManager = ModelDownloadManager(this)
        llmManager = LocalLlmManager(this, downloadManager)
        parserAgent = NotificationParserAgent(this, repository, llmManager)
        preferencesManager = PreferencesManager(this)
        calendarRepository = CalendarRepository(this)
        weatherRepository = WeatherRepository(this)
        screenTimeRepository = ScreenTimeRepository(this)
        healthConnectRepository = HealthConnectRepository(this)
        routineRepository = RoutineRepository(this)

        // Register passive routine signals detector for sleep/wake prediction
        UserRoutineDetector.registerReceiver(this)

        // Schedule periodic background widget refresh (4 times a day)
        WidgetUpdateWorker.schedule(this)
    }
}
