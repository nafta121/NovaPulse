package com.example.core.sensor

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.math.roundToInt

data class StepCounterData(
    val steps: Int = 0,
    val activeMinutes: Int = 0,
    val calories: Int = 0,
    val isSensorActive: Boolean = false
)

/**
 * Manages hardware step counting using both sensors (TYPE_STEP_COUNTER & TYPE_STEP_DETECTOR).
 * Ensures 100% compatibility across Android devices (Samsung Galaxy, Pixel, Xiaomi, etc.).
 */
class StepCounterManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "StepCounterManager"
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_MIDNIGHT_STEPS = "midnight_steps"
        private const val KEY_ACCUMULATED_DETECTOR_STEPS = "accumulated_detector_steps"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var counterTodaySteps = 0
    private var detectorTodaySteps = 0

    private val _stepDataState = MutableStateFlow(StepCounterData())
    val stepDataState: StateFlow<StepCounterData> = _stepDataState.asStateFlow()

    init {
        checkAndResetDailyData()
    }

    private fun checkAndResetDailyData() {
        val todayStr = LocalDate.now().toString()
        val lastSavedDate = prefs.getString(KEY_LAST_DATE, "")

        if (lastSavedDate != todayStr) {
            // New day -> Reset baseline
            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_MIDNIGHT_STEPS, -1)
                .putInt(KEY_ACCUMULATED_DETECTOR_STEPS, 0)
                .apply()
            counterTodaySteps = 0
            detectorTodaySteps = 0
        } else {
            detectorTodaySteps = prefs.getInt(KEY_ACCUMULATED_DETECTOR_STEPS, 0)
        }
    }

    fun startListening() {
        checkAndResetDailyData()
        var registered = false

        if (stepCounterSensor != null) {
            try {
                sensorManager?.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
                registered = true
                Log.d(TAG, "Registered TYPE_STEP_COUNTER successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register TYPE_STEP_COUNTER: ${e.message}")
            }
        }

        if (stepDetectorSensor != null) {
            try {
                sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
                registered = true
                Log.d(TAG, "Registered TYPE_STEP_DETECTOR successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register TYPE_STEP_DETECTOR: ${e.message}")
            }
        }

        recalculateAndEmitState(isSensorActive = registered)
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering step sensors: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        checkAndResetDailyData()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSensorSteps = event.values[0].toInt()
                val todayStr = LocalDate.now().toString()
                var midnightSteps = prefs.getInt(KEY_MIDNIGHT_STEPS, -1)

                if (midnightSteps == -1 || midnightSteps > totalSensorSteps) {
                    midnightSteps = totalSensorSteps
                    prefs.edit()
                        .putString(KEY_LAST_DATE, todayStr)
                        .putInt(KEY_MIDNIGHT_STEPS, midnightSteps)
                        .apply()
                }

                counterTodaySteps = (totalSensorSteps - midnightSteps).coerceAtLeast(0)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    detectorTodaySteps += 1
                    prefs.edit().putInt(KEY_ACCUMULATED_DETECTOR_STEPS, detectorTodaySteps).apply()
                }
            }
        }

        recalculateAndEmitState(isSensorActive = true)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun recalculateAndEmitState(isSensorActive: Boolean) {
        val totalSteps = maxOf(counterTodaySteps, detectorTodaySteps)
        val activeMins = (totalSteps / 100).coerceAtLeast(0)
        val cals = (totalSteps * 0.04f).roundToInt().coerceAtLeast(0)

        _stepDataState.value = StepCounterData(
            steps = totalSteps,
            activeMinutes = activeMins,
            calories = cals,
            isSensorActive = isSensorActive
        )
    }

    fun getTodayStepData(healthConnectSteps: Int = 0, healthConnectCal: Int = 0, healthConnectActive: Int = 0): StepCounterData {
        val currentLocalState = _stepDataState.value
        val steps = maxOf(healthConnectSteps, currentLocalState.steps)
        val activeMins = if (healthConnectActive > 0) healthConnectActive else (steps / 100).coerceAtLeast(0)
        val cals = if (healthConnectCal > 0) healthConnectCal else (steps * 0.04f).roundToInt().coerceAtLeast(0)

        return StepCounterData(
            steps = steps,
            activeMinutes = activeMins,
            calories = cals,
            isSensorActive = stepCounterSensor != null || stepDetectorSensor != null
        )
    }
}
