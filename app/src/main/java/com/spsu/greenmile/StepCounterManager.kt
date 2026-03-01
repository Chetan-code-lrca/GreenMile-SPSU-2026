package com.spsu.greenmile

import android.content.Context
import android.hardware.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class StepCounterManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialSteps: Int? = null
    private var lastDate: String = getTodayDate()

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday: StateFlow<Int> = _stepsToday

    fun start() {
        stepSensor?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {

            val today = getTodayDate()

            // 🌙 Check if date changed
            if (today != lastDate) {
                initialSteps = null
                _stepsToday.value = 0
                lastDate = today
            }

            val totalSteps = it.values[0].toInt()

            if (initialSteps == null) {
                initialSteps = totalSteps
            }

            _stepsToday.value = totalSteps - (initialSteps ?: totalSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
    }
}