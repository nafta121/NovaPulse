package com.example.core.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HourlyWeatherItem(
    val timeLabel: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val weatherCode: Int
)

data class DailyWeatherItem(
    val dayLabel: String,
    val tempMinC: Double,
    val tempMaxC: Double,
    val weatherCode: Int
)

data class RealWeatherData(
    val currentTempC: Double,
    val apparentTempC: Double,
    val weatherCode: Int,
    val conditionDescription: String,
    val hourlyForecast: List<HourlyWeatherItem>,
    val dailyForecast: List<DailyWeatherItem> = emptyList(),
    val locationName: String = "TP. Hồ Chí Minh",
    val isFromCache: Boolean = false,
    val cacheTimestampMs: Long = 0L
)

class WeatherRepository(private val context: Context) {

    companion object {
        const val DEFAULT_LAT = 10.8231 // TP. Hồ Chí Minh
        const val DEFAULT_LON = 106.6297
        const val DEFAULT_LOCATION_NAME = "TP. Hồ Chí Minh"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun isLocationPermissionGranted(): Boolean {
        return com.example.core.location.LocationHelper.isLocationPermissionGranted(context)
    }

    suspend fun fetchWeather(lat: Double? = null, lon: Double? = null): RealWeatherData? = withContext(Dispatchers.IO) {
        val location = if (lat != null && lon != null) {
            Pair(lat, lon)
        } else {
            com.example.core.location.LocationHelper.getCurrentLocation(context)
        }

        val targetLat = location?.first ?: DEFAULT_LAT
        val targetLon = location?.second ?: DEFAULT_LON

        val locationName = com.example.core.location.LocationHelper.getCityName(context, targetLat, targetLon)

        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$targetLat&longitude=$targetLon&current=temperature_2m,apparent_temperature,weather_code&hourly=temperature_2m,apparent_temperature,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=7"

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext WeatherCacheManager.load(context)
                val bodyStr = response.body?.string() ?: return@withContext WeatherCacheManager.load(context)
                val json = JSONObject(bodyStr)

                val current = json.getJSONObject("current")
                val currentTemp = current.getDouble("temperature_2m")
                val apparentTemp = current.getDouble("apparent_temperature")
                val weatherCode = current.getInt("weather_code")

                val hourly = json.getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val apparentTemps = hourly.getJSONArray("apparent_temperature")
                val codes = hourly.getJSONArray("weather_code")

                val currentTimeIso = current.optString("time", "")
                var startIdx = 0
                for (i in 0 until times.length()) {
                    if (times.getString(i) >= currentTimeIso) {
                        startIdx = i
                        break
                    }
                }

                val hourlyList = mutableListOf<HourlyWeatherItem>()
                val count = minOf(24, times.length() - startIdx)
                for (i in 0 until count) {
                    val rawTime = times.getString(startIdx + i)
                    val hourLabel = if (i == 0) "Bây giờ" else {
                        val parts = rawTime.split("T")
                        if (parts.size > 1) parts[1].take(5) else rawTime
                    }
                    val rawHourlyTemp = temps.getDouble(startIdx + i)
                    val rawApparentTemp = apparentTemps.getDouble(startIdx + i)
                    val code = codes.getInt(startIdx + i)
                    hourlyList.add(HourlyWeatherItem(hourLabel, rawHourlyTemp, rawApparentTemp, code))
                }

                // Parse daily forecast (7 ngày)
                val dailyList = mutableListOf<DailyWeatherItem>()
                val daily = json.optJSONObject("daily")
                if (daily != null) {
                    val dailyTimes = daily.getJSONArray("time")
                    val dailyMaxTemps = daily.getJSONArray("temperature_2m_max")
                    val dailyMinTemps = daily.getJSONArray("temperature_2m_min")
                    val dailyCodes = daily.getJSONArray("weather_code")
                    val dayNames = arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")

                    for (i in 0 until dailyTimes.length()) {
                        val dateStr = dailyTimes.getString(i) // "2026-08-26"
                        val dayLabel = if (i == 0) {
                            "Hôm nay"
                        } else {
                            try {
                                val parts = dateStr.split("-")
                                val cal = java.util.Calendar.getInstance()
                                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                                dayNames[dow - 1]
                            } catch (_: Exception) { dateStr.takeLast(5) }
                        }
                        dailyList.add(
                            DailyWeatherItem(
                                dayLabel = dayLabel,
                                tempMinC = dailyMinTemps.getDouble(i),
                                tempMaxC = dailyMaxTemps.getDouble(i),
                                weatherCode = dailyCodes.getInt(i)
                            )
                        )
                    }
                }

                val conditionDesc = parseWmoCode(weatherCode)

                val freshData = RealWeatherData(
                    currentTempC = currentTemp,
                    apparentTempC = apparentTemp,
                    weatherCode = weatherCode,
                    conditionDescription = conditionDesc,
                    hourlyForecast = hourlyList,
                    dailyForecast = dailyList,
                    locationName = locationName,
                    isFromCache = false,
                    cacheTimestampMs = System.currentTimeMillis()
                )
                // Lưu cache sau khi fetch thành công
                WeatherCacheManager.save(context, freshData)
                freshData
            }
        } catch (e: Exception) {
            // Fallback sang cache khi mất mạng / API lỗi
            WeatherCacheManager.load(context)
        }
    }

    private fun parseWmoCode(code: Int): String {
        return when (code) {
            0 -> "Trời quang đãng"
            1 -> "Trời hầu như quang"
            2 -> "Mây rải rác"
            3 -> "Nhiều mây / Âm u"
            45, 48 -> "Có sương mù"
            51, 53, 55 -> "Mưa phùn"
            56, 57 -> "Mưa phùn lạnh giá"
            61 -> "Mưa nhỏ"
            63 -> "Mưa vừa"
            65 -> "Mưa to"
            66, 67 -> "Mưa băng giá"
            71, 73, 75 -> "Tuyết rơi"
            77 -> "Mưa tuyết nhỏ"
            80, 81, 82 -> "Mưa rào"
            85, 86 -> "Mưa tuyết rào"
            95 -> "Có dông sét"
            96, 99 -> "Dông kèm mưa đá"
            else -> "Thời tiết không xác định"
        }
    }
}

/**
 * Tìm thời tiết dự báo tại một mốc thời gian cụ thể (timeMillis)
 */
fun RealWeatherData.getWeatherForTimestamp(timeMillis: Long): HourlyWeatherItem? {
    if (hourlyForecast.isEmpty()) return null
    val cal = java.util.Calendar.getInstance().apply { this.timeInMillis = timeMillis }
    val targetHour = cal.get(java.util.Calendar.HOUR_OF_DAY)

    return hourlyForecast.firstOrNull { item ->
        val hourInLabel = item.timeLabel.replace("Bây giờ", "").trim().split(":").firstOrNull()?.toIntOrNull()
        hourInLabel == targetHour
    } ?: hourlyForecast.firstOrNull()
}
