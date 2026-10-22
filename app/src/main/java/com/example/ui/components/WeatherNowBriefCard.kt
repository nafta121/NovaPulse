package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.repository.RealWeatherData
import com.example.ui.theme.DiurnalPhase
import java.util.Calendar

@Composable
fun WeatherNowBriefCard(
    weatherData: RealWeatherData?,
    isLoading: Boolean,
    hasLocationPermission: Boolean,
    currentHour: Int,
    diurnalPhase: DiurnalPhase,
    onRequestLocationPermission: () -> Unit,
    onRefresh: () -> Unit,
    onCardClick: () -> Unit = {}
) {
    val isNight = diurnalPhase.isDark || currentHour >= 19 || currentHour < 5
    val cardBg = Color(0xFF2563EB).copy(alpha = 0.4f)
    val cardBorder = Color.White.copy(alpha = 0.2f)

    val currentTempStr =
        if (weatherData != null) "${Math.round(weatherData.currentTempC).toInt()}°" else if (isNight) "29°" else "32°"
    val feelsLikeStr =
        if (weatherData != null) "Feels like ${Math.round(weatherData.apparentTempC).toInt()}°" else ""
    val conditionText = weatherData?.conditionDescription ?: if (isNight) "Cool night" else "Fresh morning"
    val locationLabel = weatherData?.locationName ?: "Current location"

    // "Saved at HH:mm" badge when displaying cached data
    val cacheLabel = if (weatherData?.isFromCache == true && weatherData.cacheTimestampMs > 0L) {
        val cal = Calendar.getInstance().also { it.timeInMillis = weatherData.cacheTimestampMs }
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        "Saved at $h:$m"
    } else null

    val weatherIcon = when (weatherData?.weatherCode) {
        0 -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
        1, 2, 3 -> if (isNight) Icons.Default.CloudQueue else Icons.Default.WbCloudy
        51, 53, 55, 61, 63, 65, 80, 81 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("weather_now_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Current Weather Main Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = currentTempStr,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    if (feelsLikeStr.isNotEmpty()) {
                        Text(
                            text = feelsLikeStr,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = conditionText,
                            tint = if (isNight) Color(0xFF93C5FD) else Color(0xFFFEF08A),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = conditionText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (cacheLabel != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cacheLabel,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hourly Forecast Horizontal Row (Real Data from Open-Meteo)
            if (weatherData != null && weatherData.hourlyForecast.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(
                        items = weatherData.hourlyForecast,
                        key = { it.timeLabel },
                        contentType = { "hourly_forecast" }
                    ) { item ->
                        val hourNum = try {
                            if (item.timeLabel.contains(":")) {
                                item.timeLabel.split(":")[0].trim().toInt()
                            } else {
                                currentHour
                            }
                        } catch (e: Exception) {
                            currentHour
                        }
                        val isSlotNight = hourNum >= 19 || hourNum < 5

                        val itemIcon = when (item.weatherCode) {
                            0 -> if (isSlotNight) Icons.Default.NightsStay else Icons.Default.WbSunny
                            1, 2, 3 -> if (isSlotNight) Icons.Default.CloudQueue else Icons.Default.WbCloudy
                            51, 53, 55, 61, 63, 80 -> Icons.Default.WaterDrop
                            95, 96 -> Icons.Default.Thunderstorm
                            else -> if (isSlotNight) Icons.Default.NightsStay else Icons.Default.CloudQueue
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.timeLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = itemIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${Math.round(item.temperatureC).toInt()}°",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.apparentTemperatureC > item.temperatureC + 1.5) {
                                Text(
                                    text = "${Math.round(item.apparentTemperatureC).toInt()}°",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state / Request Location Permission
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!hasLocationPermission) "Grant location permission for live weather" else "Updating weather from Open-Meteo...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!hasLocationPermission) {
                        Button(
                            onClick = onRequestLocationPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                        }
                    }
                }
            }

            // Weekly Forecast Row (7-day - One UI 9 Style)
            if (weatherData != null && weatherData.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "7-day forecast",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(weatherData.dailyForecast) { day ->
                        val dayIcon = when (day.weatherCode) {
                            0 -> Icons.Default.WbSunny
                            1, 2, 3 -> Icons.Default.WbCloudy
                            51, 53, 55, 61, 63, 65, 80, 81 -> Icons.Default.WaterDrop
                            95, 96, 99 -> Icons.Default.Thunderstorm
                            else -> Icons.Default.CloudQueue
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = day.dayLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = if (day.dayLabel == "Today") FontWeight.Bold else FontWeight.Normal
                            )
                            Icon(
                                imageVector = dayIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${Math.round(day.tempMaxC).toInt()}°",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${Math.round(day.tempMinC).toInt()}°",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val tempC = weatherData?.currentTempC ?: 30.0
            val feelsC = weatherData?.apparentTempC ?: tempC
            val wCode = weatherData?.weatherCode ?: 0

            val adviceText = when {
                wCode in listOf(95, 96, 99) ->
                    "Thunderstorms expected! Limit outdoor activities and stay away from open areas."
                wCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ->
                    "It's raining. Remember to bring an umbrella or raincoat when heading out."
                wCode in listOf(45, 48) ->
                    "Dense fog with low visibility. Drive carefully."
                feelsC >= 40.0 || tempC >= 38.0 ->
                    "Extreme heat, feels like ${Math.round(feelsC).toInt()}°C! Limit time outdoors and stay hydrated."
                feelsC >= 34.0 || tempC >= 31.0 ->
                    "Hot and muggy, feels like ${Math.round(feelsC).toInt()}°C. Seek shade and drink water regularly."
                tempC >= 28.0 && wCode in listOf(0, 1) ->
                    "Warm and sunny ${Math.round(tempC).toInt()}°C, clear skies. Remember sunscreen if going outside."
                wCode in listOf(2, 3) ->
                    "Mostly cloudy ${Math.round(tempC).toInt()}°C, mild and pleasant weather."
                tempC < 20.0 ->
                    "Chilly ${Math.round(tempC).toInt()}°C. Remember to wear a warm jacket when going out."
                isNight ->
                    "Tonight is ${Math.round(tempC).toInt()}°C with pleasant weather. Have a restful night!"
                else ->
                    "Current temperature is ${Math.round(tempC).toInt()}°C. Great conditions for commuting and activities."
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val adviceIcon = when {
                        wCode in listOf(95, 96, 99) -> Icons.Default.Thunderstorm
                        wCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> Icons.Default.Umbrella
                        wCode in listOf(45, 48) -> Icons.Default.CloudQueue
                        feelsC >= 35.0 -> Icons.Default.WbSunny
                        tempC < 20.0 -> Icons.Default.Air
                        isNight -> Icons.Default.CheckCircle
                        else -> Icons.Default.WbSunny
                    }
                    Icon(
                        imageVector = adviceIcon,
                        contentDescription = "Advice",
                        tint = Color(0xFFFEF08A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = adviceText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
