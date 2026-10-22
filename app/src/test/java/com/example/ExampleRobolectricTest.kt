package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.repository.CalendarRepository
import com.example.core.repository.HealthConnectRepository
import com.example.core.repository.ScreenTimeRepository
import com.example.core.repository.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NovaPulse", appName)
  }

  @Test
  fun `verify real repository initializations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val calRepo = CalendarRepository(context)
    val weatherRepo = WeatherRepository(context)
    val screenRepo = ScreenTimeRepository(context)
    val healthRepo = HealthConnectRepository(context)

    assertNotNull(calRepo)
    assertNotNull(weatherRepo)
    assertNotNull(screenRepo)
    assertNotNull(healthRepo)
  }

  @Test
  fun `verify legacy Ho Chi Minh cache purge and real media history deduplication`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val origTz = java.util.TimeZone.getDefault()
    try {
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Jakarta"))

      val legacyWeather = com.example.core.repository.RealWeatherData(
        currentTempC = 32.0,
        apparentTempC = 35.0,
        weatherCode = 1,
        conditionDescription = "Mainly clear",
        hourlyForecast = emptyList(),
        locationName = "Ho Chi Minh City"
      )
      com.example.core.repository.WeatherCacheManager.save(context, legacyWeather)
      val loaded = com.example.core.repository.WeatherCacheManager.load(context)
      assertEquals(null, loaded)

      com.example.core.music.RecentMediaTracker.updatePlayback(
        context = context,
        title = "Hati-Hati di Jalan",
        artist = "Tulus",
        packageName = "com.google.android.apps.youtube.music",
        isPlaying = true
      )
      com.example.core.music.RecentMediaTracker.updatePlayback(
        context = context,
        title = "Hati-Hati di Jalan",
        artist = "Tulus",
        packageName = "com.google.android.apps.youtube.music",
        isPlaying = false
      )
      val history = com.example.core.music.RecentMediaTracker.recentHistoryState.value
      assertEquals(1, history.size)
      assertEquals("YouTube Music", history.first().appName)
    } finally {
      java.util.TimeZone.setDefault(origTz)
    }
  }
}
