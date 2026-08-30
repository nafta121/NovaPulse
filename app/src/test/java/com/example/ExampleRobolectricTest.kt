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
}
