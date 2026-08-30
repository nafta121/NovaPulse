package com.example.feature.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NowBriefWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NowBriefWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextSessionUpdate(context)
        triggerUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_TIME_TICK ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            triggerUpdate(context)
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            WidgetUpdateScheduler.scheduleNextSessionUpdate(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NowBriefWidget().updateAll(context)
                } catch (e: Exception) {
                    // Widget might not be placed on home screen yet
                }
            }
        }
    }
}
