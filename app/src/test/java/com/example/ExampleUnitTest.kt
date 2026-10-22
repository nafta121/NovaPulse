package com.example

import com.example.core.ai.ExtractedCategory
import com.example.core.ai.LocalLlmManager
import com.example.core.database.UserRoutineLogEntity
import com.example.core.notification.RegexPreFilter
import com.example.core.repository.RoutineRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun regexPreFilter_classifiesTaskExpenseDigestAndIrrelevant() {
        val taskEval = RegexPreFilter.evaluate("Team Sync", "Please submit the report before tomorrow's deadline")
        assertTrue(taskEval.isTriggered)
        assertEquals(ExtractedCategory.TASK, taskEval.predictedCategory)

        val expenseEval = RegexPreFilter.evaluate("VCB Digibank", "Account debited -250,000 VND for Grab payment")
        assertTrue(expenseEval.isTriggered)
        assertEquals(ExtractedCategory.EXPENSE, expenseEval.predictedCategory)

        val digestEval = RegexPreFilter.evaluate("Security Alert", "Your OTP verification code is 482910")
        assertTrue(digestEval.isTriggered)
        assertEquals(ExtractedCategory.DIGEST, digestEval.predictedCategory)

        val irrelevantEval = RegexPreFilter.evaluate("Weather", "Sunny skies expected all afternoon")
        assertFalse(irrelevantEval.isTriggered)
        assertEquals(ExtractedCategory.IRRELEVANT, irrelevantEval.predictedCategory)
    }

    @Test
    fun parseCurrencyAmount_handlesUsdDecimalsAndVndThousands() {
        assertEquals(45.0, LocalLlmManager.parseCurrencyAmount("Card charged $45.00 at Coffee Bean"), 0.001)
        assertEquals(45.5, LocalLlmManager.parseCurrencyAmount("On 10/22 card 4812 charged $45.50 at Amazon"), 0.001)
        assertEquals(25.0, LocalLlmManager.parseCurrencyAmount("Paid $25 for lunch"), 0.001)
        assertEquals(1250.5, LocalLlmManager.parseCurrencyAmount("Transfer 1,250.50 USD completed"), 0.001)
        assertEquals(50000.0, LocalLlmManager.parseCurrencyAmount("Balance change +50,000VND"), 0.001)
        assertEquals(250000.0, LocalLlmManager.parseCurrencyAmount("14:30 Account 9981 debited -250.000 VND at Shopee"), 0.001)
        assertEquals(1500000.0, LocalLlmManager.parseCurrencyAmount("Transfer 1,500,000 VND"), 0.001)
        assertEquals(75000.0, LocalLlmManager.parseCurrencyAmount("Coffee 75k"), 0.001)
    }

    @Test
    fun calculateSleepWindowFromLogs_computesBedtimeAndWakeTime() {
        val emptyWindow = RoutineRepository.calculateSleepWindowFromLogs(emptyList())
        assertEquals("23:00", emptyWindow.bedtimeFormatted)
        assertEquals("07:00", emptyWindow.wakeTimeFormatted)

        val logs = listOf(
            UserRoutineLogEntity(
                timestamp = 1000L,
                eventType = RoutineRepository.EVENT_SCREEN_OFF,
                hourOfDay = 23,
                minuteOfHour = 30
            ),
            UserRoutineLogEntity(
                timestamp = 2000L,
                eventType = RoutineRepository.EVENT_POWER_CONNECTED,
                hourOfDay = 0,
                minuteOfHour = 30
            ),
            UserRoutineLogEntity(
                timestamp = 3000L,
                eventType = RoutineRepository.EVENT_USER_PRESENT,
                hourOfDay = 6,
                minuteOfHour = 20
            ),
            UserRoutineLogEntity(
                timestamp = 4000L,
                eventType = RoutineRepository.EVENT_POWER_DISCONNECTED,
                hourOfDay = 6,
                minuteOfHour = 40
            )
        )
        val window = RoutineRepository.calculateSleepWindowFromLogs(logs)
        assertEquals("00:00", window.bedtimeFormatted)
        assertEquals("06:30", window.wakeTimeFormatted)
        assertEquals(4, window.sampleCount)
    }

    @Test
    fun briefingContext_toPdeXmlSignals_includesRealDeviceMetricsAndCorrelations() {
        val ctx = com.example.core.ai.BriefingContext(
            hourOfDay = 15,
            weatherTempC = 30.0,
            weatherCondition = "Partly Cloudy",
            energyScore = 88,
            remainingCalendarEventsCount = 2,
            upcomingEventsSummary = "Sprint Review",
            topTaskTitle = "Kirim laporan bulanan",
            sleepHoursLastNight = 7.2,
            screenTimeMinutesToday = 145,
            stepsToday = 4850
        )
        val xml = ctx.toPdeXmlSignals()
        assertTrue(xml.contains("<weather>30°C, Partly Cloudy</weather>"))
        assertTrue(xml.contains("<calendar remaining_events=\"2\">Sprint Review</calendar>"))
        assertTrue(xml.contains("<top_task>Kirim laporan bulanan</top_task>"))
        assertTrue(xml.contains("<activity steps=\"4850\" screen_time_min=\"145\" sleep_hours=\"7.2\" />"))
    }

    @Test
    fun nowBarLiveService_constantsAreConfiguredForSamsungNowBar() {
        assertEquals(2001, com.example.feature.island.NowBarLiveService.NOTIFICATION_ID)
        assertEquals(
            "com.example.feature.island.ACTION_REFRESH_NOWBAR",
            com.example.feature.island.NowBarLiveService.ACTION_REFRESH
        )
    }
}
