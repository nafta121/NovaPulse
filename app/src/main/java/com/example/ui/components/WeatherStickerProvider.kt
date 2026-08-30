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
 * Trả về ID drawable tương ứng với mã thời tiết từ Open-Meteo API
 * - weather_sunny.png (Mã 0, 1 ban ngày)
 * - weather_night.png (Mã 0, 1 ban đêm)
 * - weather_cloudy.png (Mã 2, 3, 45, 48 - Sương mù tự động dùng cloudy)
 * - weather_rainy.png (Mã 51..67, 80..82)
 * - weather_thunder.png (Mã 95..99)
 */
fun getWeatherStickerRes(weatherCode: Int, isNight: Boolean = false): Int {
    return when (weatherCode) {
        0, 1 -> if (isNight) R.drawable.weather_night else R.drawable.weather_sunny
        2, 3 -> R.drawable.weather_cloudy
        45, 48 -> R.drawable.weather_cloudy // Sương mù tự động dùng sticker mây
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.weather_rainy
        95, 96, 99 -> R.drawable.weather_thunder
        else -> if (isNight) R.drawable.weather_night else R.drawable.weather_sunny
    }
}

/**
 * Weather Sticker Component hiển thị ảnh PNG sắc nét kèm hiệu ứng nhún nhảy (floating animation) mượt mà
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
