package com.example.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NovaPulseGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = GlanceContextWidget()

    companion object {
        fun triggerUpdate(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    GlanceContextWidget().updateAll(context)
                } catch (e: Exception) {
                    // Widget might not be placed on home screen yet
                }
            }
        }
    }
}
