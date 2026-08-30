package com.example.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/**
 * Receiver nhận tín hiệu báo thức AlarmManager để cập nhật Widget mượt mà
 */
class WidgetUpdateSchedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WidgetScheduler", "Triggered Glance Widget 7-session refresh")
        NowBriefWidgetReceiver.triggerUpdate(context)
        WidgetUpdateScheduler.scheduleNextSessionUpdate(context)
    }
}

/**
 * Quản lý lên lịch AlarmManager tự động cập nhật Glance Widget chính xác
 * vào 7 mốc thời gian chuyển giao: 05:00, 08:30, 11:30, 14:00, 17:00, 19:30, 22:30
 */
object WidgetUpdateScheduler {

    private const val TAG = "WidgetUpdateScheduler"
    const val ACTION_SESSION_UPDATE = "com.example.feature.widget.ACTION_SESSION_UPDATE"
    private const val REQUEST_CODE = 9988

    // 7 mốc thời gian chuyển giao trong ngày (Giờ, Phút)
    private val SESSION_TRANSITIONS = listOf(
        Pair(5, 0),    // EARLY_MORNING (05:00)
        Pair(8, 30),   // MID_MORNING (08:30)
        Pair(11, 30),  // NOON (11:30)
        Pair(14, 0),   // AFTERNOON (14:00)
        Pair(17, 0),   // EVENING (17:00)
        Pair(19, 30),  // NIGHT (19:30)
        Pair(22, 30)   // LATE_NIGHT (22:30)
    )

    fun scheduleNextSessionUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = Calendar.getInstance()

        var nextTriggerTime: Calendar? = null

        // Tìm mốc chuyển giao tiếp theo trong ngày hôm nay
        for ((hour, min) in SESSION_TRANSITIONS) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.after(now)) {
                nextTriggerTime = candidate
                break
            }
        }

        // Nếu hôm nay không còn mốc nào -> Lấy mốc 05:00 ngày mai
        if (nextTriggerTime == null) {
            nextTriggerTime = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 5)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val intent = Intent(context, WidgetUpdateSchedulerReceiver::class.java).apply {
            action = ACTION_SESSION_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next Glance Widget session refresh at: ${nextTriggerTime.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling AlarmManager widget refresh: ${e.message}")
        }
    }
}
