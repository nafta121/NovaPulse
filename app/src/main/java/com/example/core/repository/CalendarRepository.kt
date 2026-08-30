package com.example.core.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Calendar

data class RealCalendarEvent(
    val id: Long,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String = "",
    val isAllDay: Boolean = false,
    val accountName: String = "",
    val accountType: String = "",
    val calendarDisplayName: String = ""
) {
    val displaySourceLabel: String
        get() = when {
            accountType.contains("com.google", ignoreCase = true) -> "Google Calendar"
            accountName.contains("sec.contact", ignoreCase = true) ||
                    accountName.contains("my_local", ignoreCase = true) ||
                    accountType.contains("sec", ignoreCase = true) -> "Samsung Calendar"
            calendarDisplayName.isNotBlank() -> calendarDisplayName
            accountName.isNotBlank() -> accountName
            else -> "Lịch thiết bị"
        }
}

class CalendarRepository(private val context: Context) {

    private var calendarObserver: ContentObserver? = null

    fun registerContentObserver(onChange: () -> Unit) {
        if (!isPermissionGranted()) return
        try {
            if (calendarObserver == null) {
                calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        super.onChange(selfChange)
                        Log.d("CalendarRepository", "Detected system calendar DB change via ContentObserver")
                        onChange()
                    }
                }
                context.contentResolver.registerContentObserver(
                    CalendarContract.Events.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
                Log.d("CalendarRepository", "Successfully registered Calendar ContentObserver")
            }
        } catch (e: Exception) {
            Log.w("CalendarRepository", "Failed to register ContentObserver: ${e.message}")
        }
    }

    fun unregisterContentObserver() {
        calendarObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {}
            calendarObserver = null
        }
    }

    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Lấy các sự kiện CÒN LẠI hoặc ĐANG DIỄN RA của HÔM NAY
     */
    fun getRemainingTodayEvents(): List<RealCalendarEvent> {
        if (!isPermissionGranted()) return emptyList()

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfToday = calendar.timeInMillis

        val allToday = queryInstances(startOfToday, endOfToday)
        return allToday.filter { e ->
            e.isAllDay || e.endTimeMillis >= now - 15 * 60 * 1000L || e.startTimeMillis >= now - 15 * 60 * 1000L
        }
    }

    /**
     * Lấy các sự kiện của NGÀY MAI (từ 00:00:00 ngày mai đến 23:59:59 ngày mai)
     */
    fun getTomorrowEvents(): List<RealCalendarEvent> {
        if (!isPermissionGranted()) return emptyList()

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfTomorrow = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfTomorrow = calendar.timeInMillis

        return queryInstances(startOfTomorrow, endOfTomorrow)
    }

    /**
     * Truy vấn thông minh: Lấy sự kiện còn lại hôm nay, nếu hết thì lấy sự kiện ngày mai
     */
    fun getUpcomingEvents(): Pair<String, List<RealCalendarEvent>> {
        val remainingToday = getRemainingTodayEvents()
        if (remainingToday.isNotEmpty()) {
            return Pair("Lịch trình hôm nay", remainingToday)
        }
        val tomorrow = getTomorrowEvents()
        if (tomorrow.isNotEmpty()) {
            return Pair("Lịch trình ngày mai", tomorrow)
        }
        return Pair("Lịch trình hôm nay", emptyList())
    }

    fun getTodayEvents(): List<RealCalendarEvent> {
        return getRemainingTodayEvents()
    }

    /**
     * Sử dụng CalendarContract.Instances.CONTENT_URI để đọc chính xác Samsung Calendar & Google Calendar
     */
    private fun queryInstances(startMs: Long, endMs: Long): List<RealCalendarEvent> {
        val events = mutableListOf<RealCalendarEvent>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMs)
        ContentUris.appendId(builder, endMs)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Events.ACCOUNT_TYPE,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val locCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_TYPE)
                val calDisplayCol = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "(Không có tiêu đề)"
                    val dtStart = cursor.getLong(beginCol)
                    val dtEnd = cursor.getLong(endCol)
                    val location = cursor.getString(locCol) ?: ""
                    val isAllDay = cursor.getInt(allDayCol) == 1
                    val accountName = if (accNameCol >= 0) cursor.getString(accNameCol) ?: "" else ""
                    val accountType = if (accTypeCol >= 0) cursor.getString(accTypeCol) ?: "" else ""
                    val calDisplayName = if (calDisplayCol >= 0) cursor.getString(calDisplayCol) ?: "" else ""

                    events.add(
                        RealCalendarEvent(
                            id = id,
                            title = title,
                            startTimeMillis = dtStart,
                            endTimeMillis = dtEnd,
                            location = location,
                            isAllDay = isAllDay,
                            accountName = accountName,
                            accountType = accountType,
                            calendarDisplayName = calDisplayName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error querying Instances: ${e.message}")
        }

        if (events.isNotEmpty()) return events

        // Fallback: Direct query to Events table
        return queryFallbackEvents(startMs, endMs)
    }

    private fun queryFallbackEvents(startMs: Long, endMs: Long): List<RealCalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Events.ACCOUNT_TYPE,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )
        val selection = "(${CalendarContract.Events.DELETED} = 0)"
        val list = queryCalendarEvents(projection, selection, emptyArray(), "${CalendarContract.Events.DTSTART} ASC")
        return list.filter { it.endTimeMillis >= startMs || it.startTimeMillis >= startMs }
    }

    private fun queryCalendarEvents(
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String
    ): List<RealCalendarEvent> {
        val events = mutableListOf<RealCalendarEvent>()
        val contentResolver = context.contentResolver

        try {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val locCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_TYPE)
                val calDisplayCol = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "(Không có tiêu đề)"
                    val dtStart = cursor.getLong(startCol)
                    val dtEnd = cursor.getLong(endCol)
                    val location = cursor.getString(locCol) ?: ""
                    val isAllDay = cursor.getInt(allDayCol) == 1
                    val accountName = if (accNameCol >= 0) cursor.getString(accNameCol) ?: "" else ""
                    val accountType = if (accTypeCol >= 0) cursor.getString(accTypeCol) ?: "" else ""
                    val calDisplayName = if (calDisplayCol >= 0) cursor.getString(calDisplayCol) ?: "" else ""

                    events.add(
                        RealCalendarEvent(
                            id = id,
                            title = title,
                            startTimeMillis = dtStart,
                            endTimeMillis = dtEnd,
                            location = location,
                            isAllDay = isAllDay,
                            accountName = accountName,
                            accountType = accountType,
                            calendarDisplayName = calDisplayName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Guard against security exceptions or unsupported calendar providers
        }

        return events
    }

    companion object {
        fun createOpenEventIntent(eventId: Long): Intent {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            return Intent(Intent.ACTION_VIEW).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        fun createOpenCalendarAppIntent(): Intent {
            return Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        fun openEventDetail(context: Context, eventId: Long) {
            try {
                context.startActivity(createOpenEventIntent(eventId))
            } catch (e: Exception) {
                openCalendarApp(context)
            }
        }

        fun openCalendarApp(context: Context) {
            try {
                context.startActivity(createOpenCalendarAppIntent())
            } catch (e: Exception) {
                // Fallback safely
            }
        }
    }
}
