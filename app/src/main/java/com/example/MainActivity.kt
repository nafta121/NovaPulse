package com.example

import android.os.Bundle
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

        // Schedule periodic 15-minute Worker & refresh Widget immediately
        WidgetUpdateWorker.schedule(applicationContext)
        WidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)

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

    override fun onResume() {
        super.onResume()
        // Automatically refresh App & Glance Widget data whenever the user reopens the app
        WidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
        viewModel.fetchRealWeather()
        viewModel.fetchLifestyleTips()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_NOW_BRIEF", false)) {
            viewModel.generateNowBrief()
        }
    }
}
