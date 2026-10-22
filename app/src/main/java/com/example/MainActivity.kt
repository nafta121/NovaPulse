package com.example

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.feature.widget.WidgetUpdateWorker
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.NovaPulseTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLockscreenLaunch(intent)

        // Schedule periodic 15-minute Worker & refresh Widget immediately
        WidgetUpdateWorker.schedule(applicationContext)
        WidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)

        // Initialize Samsung Now Bar / Live Notification channels and start if enabled
        com.example.feature.island.NowBarLiveService.createNotificationChannels(applicationContext)
        val hasNotifPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasNotifPerm && viewModel.nowBarLiveEnabled.value) {
            com.example.feature.island.NowBarLiveService.startOrRefresh(applicationContext)
        }

        if (intent?.getBooleanExtra("EXTRA_OPEN_NOW_BRIEF", false) == true) {
            viewModel.generateNowBrief()
        }

        setContent {
            NovaPulseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLockscreenLaunch(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_NOW_BRIEF", false)) {
            viewModel.generateNowBrief()
        }
    }

    private fun handleLockscreenLaunch(launchIntent: android.content.Intent?) {
        if (launchIntent?.getBooleanExtra("EXTRA_OPEN_NOW_BRIEF", false) != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val km = getSystemService(KeyguardManager::class.java)
        if (km?.isKeyguardLocked == true) {
            km.requestDismissKeyguard(this, null)
        }
    }
}
