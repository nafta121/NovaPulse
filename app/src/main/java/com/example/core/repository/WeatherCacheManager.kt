package com.example.core.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cache thời tiết vào SharedPreferences dưới dạng JSON.
 * Tự động fallback khi không có mạng hoặc API fail.
 * Cache hết hạn sau 30 phút.
 */
object WeatherCacheManager {

    private const val PREFS_NAME = "weather_cache"
    private const val KEY_DATA = "cached_weather_json"
    private const val KEY_TIMESTAMP = "cached_at_ms"
    private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 phút

    fun save(context: Context, data: RealWeatherData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("currentTempC", data.currentTempC)
            put("apparentTempC", data.apparentTempC)
            put("weatherCode", data.weatherCode)
            put("conditionDescription", data.conditionDescription)
            put("locationName", data.locationName)
            val hourlyArr = JSONArray()
            data.hourlyForecast.forEach { item ->
                val obj = JSONObject().apply {
                    put("timeLabel", item.timeLabel)
                    put("temperatureC", item.temperatureC)
                    put("apparentTemperatureC", item.apparentTemperatureC)
                    put("weatherCode", item.weatherCode)
                }
                hourlyArr.put(obj)
            }
            put("hourlyForecast", hourlyArr)
            val dailyArr = JSONArray()
            data.dailyForecast.forEach { item ->
                val obj = JSONObject().apply {
                    put("dayLabel", item.dayLabel)
                    put("tempMinC", item.tempMinC)
                    put("tempMaxC", item.tempMaxC)
                    put("weatherCode", item.weatherCode)
                }
                dailyArr.put(obj)
            }
            put("dailyForecast", dailyArr)
        }
        prefs.edit()
            .putString(KEY_DATA, json.toString())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): RealWeatherData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_DATA, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val hourlyArr = json.getJSONArray("hourlyForecast")
            val hourlyList = mutableListOf<HourlyWeatherItem>()
            for (i in 0 until hourlyArr.length()) {
                val obj = hourlyArr.getJSONObject(i)
                hourlyList.add(
                    HourlyWeatherItem(
                        timeLabel = obj.getString("timeLabel"),
                        temperatureC = obj.getDouble("temperatureC"),
                        apparentTemperatureC = obj.getDouble("apparentTemperatureC"),
                        weatherCode = obj.getInt("weatherCode")
                    )
                )
            }
            val dailyList = mutableListOf<DailyWeatherItem>()
            val dailyArr = json.optJSONArray("dailyForecast")
            if (dailyArr != null) {
                for (i in 0 until dailyArr.length()) {
                    val obj = dailyArr.getJSONObject(i)
                    dailyList.add(
                        DailyWeatherItem(
                            dayLabel = obj.getString("dayLabel"),
                            tempMinC = obj.getDouble("tempMinC"),
                            tempMaxC = obj.getDouble("tempMaxC"),
                            weatherCode = obj.getInt("weatherCode")
                        )
                    )
                }
            }
            RealWeatherData(
                currentTempC = json.getDouble("currentTempC"),
                apparentTempC = json.getDouble("apparentTempC"),
                weatherCode = json.getInt("weatherCode"),
                conditionDescription = json.getString("conditionDescription"),
                locationName = json.getString("locationName"),
                hourlyForecast = hourlyList,
                dailyForecast = dailyList,
                isFromCache = true,
                cacheTimestampMs = prefs.getLong(KEY_TIMESTAMP, 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(KEY_TIMESTAMP, 0L)
        return savedAt == 0L || (System.currentTimeMillis() - savedAt) > CACHE_TTL_MS
    }

    fun getCacheAgeMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(KEY_TIMESTAMP, 0L)
        if (savedAt == 0L) return -1
        return ((System.currentTimeMillis() - savedAt) / 60_000L).toInt()
    }
}
