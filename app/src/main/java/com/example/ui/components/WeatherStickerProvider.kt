package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Returns the drawable resource ID corresponding to the Open-Meteo API weather code
 * - weather_sunny.png (Codes 0, 1 during daytime)
 * - weather_night.png (Codes 0, 1 during nighttime)
 * - weather_cloudy.png (Codes 2, 3, 45, 48 - Fog automatically uses cloudy)
 * - weather_rainy.png (Codes 51..67, 80..82)
 * - weather_thunder.png (Codes 95..99)
 */
fun getWeatherStickerRes(weatherCode: Int, isNight: Boolean = false): Int {
    return when (weatherCode) {
        0, 1 -> if (isNight) R.drawable.weather_night else R.drawable.weather_sunny
        2, 3 -> R.drawable.weather_cloudy
        45, 48 -> R.drawable.weather_cloudy // Fog automatically uses the cloudy sticker
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.weather_rainy
        95, 96, 99 -> R.drawable.weather_thunder
        else -> if (isNight) R.drawable.weather_night else R.drawable.weather_sunny
    }
}

/**
 * Weather Sticker Component displaying a crisp PNG image with a smooth floating animation
 */
@Composable
fun WeatherStickerImage(
    weatherCode: Int,
    isNight: Boolean = false,
    size: Dp = 64.dp,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val stickerRes = getWeatherStickerRes(weatherCode, isNight)

    val animModifier = if (animate) {
        val transition = rememberInfiniteTransition(label = "WeatherStickerFloat")
        val offsetY by transition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OffsetY"
        )
        val rotation by transition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Rotation"
        )
        Modifier.graphicsLayer {
            translationY = offsetY.dp.toPx()
            rotationZ = rotation
        }
    } else {
        Modifier
    }

    Image(
        painter = painterResource(id = stickerRes),
        contentDescription = "Weather Sticker",
        modifier = modifier
            .size(size)
            .then(animModifier)
    )
}
