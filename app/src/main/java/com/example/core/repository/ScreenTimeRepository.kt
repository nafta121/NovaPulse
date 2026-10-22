package com.example.core.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

data class ScreenTimeDayStats(
    val dayLabel: String,
    val totalTimeMillis: Long,
    val isToday: Boolean
)

data class RealScreenTimeData(
    val totalScreenTimeTodayMillis: Long,
    val formattedDuration: String,
    val weeklyHistory: List<ScreenTimeDayStats>,
    val topAppName: String = "",
    val hasPermission: Boolean = false
)

class ScreenTimeRepository(private val context: Context) {

    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getScreenTimeStats(): RealScreenTimeData {
        if (!isUsageAccessGranted()) {
            return RealScreenTimeData(
                totalScreenTimeTodayMillis = 0L,
                formattedDuration = "0h 0m",
                weeklyHistory = emptyList(),
                hasPermission = false
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return RealScreenTimeData(0L, "0h 0m", emptyList(), hasPermission = false)

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val now = System.currentTimeMillis()
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // In Java Calendar, SUNDAY=1, MONDAY=2, ... SATURDAY=7
        val todayIdx = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - 2

        val weekStartCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -todayIdx)
        }
        val startOfWeek = weekStartCal.timeInMillis
        val dailyTotals = LongArray(7)
        val todayAppTotals = mutableMapOf<String, Long>()

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfWeek,
                now
            )
            if (stats != null) {
                val bucketCal = Calendar.getInstance()
                for (u in stats) {
                    val fgTime = u.totalTimeInForeground
                    if (fgTime <= 0L) continue
                    val stamp = if (u.firstTimeStamp >= startOfWeek) u.firstTimeStamp else u.lastTimeStamp
                    if (stamp < startOfWeek) continue
                    bucketCal.timeInMillis = stamp
                    val dow = bucketCal.get(Calendar.DAY_OF_WEEK)
                    val dayIdx = if (dow == Calendar.SUNDAY) 6 else dow - 2
                    if (dayIdx in 0..6) {
                        dailyTotals[dayIdx] += fgTime
                        if (dayIdx == todayIdx) {
                            val pkg = u.packageName.orEmpty()
                            if (pkg.isNotBlank() &&
                                !pkg.contains("launcher", ignoreCase = true) &&
                                !pkg.equals("com.android.systemui", ignoreCase = true)
                            ) {
                                todayAppTotals[pkg] = (todayAppTotals[pkg] ?: 0L) + fgTime
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Guard against system service errors
        }

        val topPkg = todayAppTotals.maxByOrNull { it.value }?.key.orEmpty()
        val topAppLabel = if (topPkg.isNotBlank()) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(topPkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                topPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        } else ""

        val todayScreenTimeMillis = dailyTotals[todayIdx]
        val hours = (todayScreenTimeMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((todayScreenTimeMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val formatted = "${hours}h ${minutes}m"

        // Calculate last 7 days history
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val history = dayLabels.mapIndexed { idx, label ->
            val isToday = idx == todayIdx
            ScreenTimeDayStats(
                dayLabel = label,
                totalTimeMillis = dailyTotals[idx],
                isToday = isToday
            )
        }

        return RealScreenTimeData(
            totalScreenTimeTodayMillis = todayScreenTimeMillis,
            formattedDuration = formatted,
            weeklyHistory = history,
            topAppName = topAppLabel,
            hasPermission = true
        )
    }
}
