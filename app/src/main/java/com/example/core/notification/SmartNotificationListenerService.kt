package com.example.core.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.NovaPulseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class SmartNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "SmartNotifService"
        private const val DEBOUNCE_INTERVAL_MS = 60_000L // Tối đa 1 thông báo mỗi 60 giây

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        // Atomic timestamp để đảm bảo thread-safe debounce 60s
        private val lastProcessedTimestamp = AtomicLong(0L)

        // Danh sách các package hệ thống cần lọc bỏ hoàn toàn (như sạc pin, pin yếu, chụp màn hình)
        private val SYSTEM_IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.systemui",
            "com.sec.android.app.systemui",
            "android",
            "com.google.android.gms",
            "com.android.vending",
            "com.android.providers.downloads",
            "com.google.android.googlequicksearchbox"
        )

        fun isPermissionGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, SmartNotificationListenerService::class.java)
            return flat.contains(cn.flattenToString())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        Log.d(TAG, "SmartNotificationListenerService connected and active.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        Log.d(TAG, "SmartNotificationListenerService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. FILTER: Loại bỏ hoàn toàn ứng dụng nội bộ và các thông báo hệ thống (com.android.systemui, sạc pin, v.v.)
        if (packageName == applicationContext.packageName ||
            SYSTEM_IGNORED_PACKAGES.contains(packageName.lowercase()) ||
            packageName.lowercase().contains("systemui")
        ) {
            Log.d(TAG, "Filter: Ignored system/internal notification from $packageName")
            return
        }

        val notification = sbn.notification ?: return

        // Bỏ qua các thông báo đang diễn ra (Ongoing / Foreground service như sạc pin, nghe nhạc nền, định vị)
        if (sbn.isOngoing ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        ) {
            Log.d(TAG, "Filter: Ignored ongoing/background service notification from $packageName")
            return
        }

        // Bỏ qua notification thuộc category hệ thống hoặc tiến trình
        val category = notification.category
        if (category == Notification.CATEGORY_SYSTEM ||
            category == Notification.CATEGORY_SERVICE ||
            category == Notification.CATEGORY_PROGRESS
        ) {
            Log.d(TAG, "Filter: Ignored system category notification ($category)")
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // 2. DEBOUNCE (Ngưỡng thời gian): Chỉ cho phép xử lý tối đa 1 thông báo mỗi 60 giây
        val now = System.currentTimeMillis()
        val lastTime = lastProcessedTimestamp.get()
        if (now - lastTime < DEBOUNCE_INTERVAL_MS) {
            val remainingSec = (DEBOUNCE_INTERVAL_MS - (now - lastTime)) / 1000
            Log.d(TAG, "Debounce: Throttled notification from $packageName. Remaining window: ${remainingSec}s")
            return
        }

        // Cập nhật timestamp hợp lệ
        if (!lastProcessedTimestamp.compareAndSet(lastTime, now)) {
            // Trường hợp race condition xảy ra đồng thời
            return
        }

        val fullContent = if (subText.isNotBlank()) "$text ($subText)" else text

        val appName = try {
            val pm = applicationContext.packageManager
            val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull() ?: packageName
        }

        Log.d(TAG, "Processing valid debounced notification from $appName ($packageName)")
        val app = application as? NovaPulseApp
        app?.parserAgent?.processNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            content = fullContent
        )
    }
}
