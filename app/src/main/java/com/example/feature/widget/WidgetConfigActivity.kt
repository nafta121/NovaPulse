package com.example.feature.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.updateAll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.core.preferences.PreferencesManager
import com.example.ui.theme.NovaPulseTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set result to CANCELED initially in case user exits without saving
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        preferencesManager = PreferencesManager(applicationContext)

        setContent {
            NovaPulseTheme {
                val transparency by preferencesManager.widgetTransparency.collectAsStateWithLifecycle(initialValue = 1.0f)
                val colorPreset by preferencesManager.widgetColorPreset.collectAsStateWithLifecycle(initialValue = "AUTO")
                val shape by preferencesManager.widgetShape.collectAsStateWithLifecycle(initialValue = "CAPSULE")
                val showSubtext by preferencesManager.widgetShowSubtext.collectAsStateWithLifecycle(initialValue = true)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF8FAFC)
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { finish() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Customize NowBrief Widget",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                )
                                Text(
                                    text = "Personalize the Capsule Pill appearance on your home screen",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            WidgetCustomizerContent(
                                initialTransparency = transparency,
                                initialColorPreset = colorPreset,
                                initialShape = shape,
                                initialShowSubtext = showSubtext,
                                onSaveConfig = { newTrans, newPreset, newShape, newShowSub ->
                                    lifecycleScope.launch {
                                        preferencesManager.setWidgetConfig(
                                            transparency = newTrans,
                                            colorPreset = newPreset,
                                            shape = newShape,
                                            showSubtext = newShowSub
                                        )
                                        try {
                                            NowBriefWidget().updateAll(applicationContext)
                                            GlanceContextWidget().updateAll(applicationContext)
                                        } catch (e: Exception) {}
                                        NowBriefWidgetReceiver.triggerUpdate(applicationContext)

                                        val resultValue = Intent().apply {
                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                        }
                                        setResult(Activity.RESULT_OK, resultValue)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
