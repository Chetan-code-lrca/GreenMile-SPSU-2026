package com.spsu.greenmile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    companion object {
        const val PREFS_NAME = "StepPrefs"
        const val KEY_STEPS_TODAY = "steps_today"
        const val KEY_STEPS_OFFSET = "steps_offset"
        const val KEY_LAST_DATE = "last_date"

        fun getStepsToday(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today = todayString()
            val lastDate = prefs.getString(KEY_LAST_DATE, "")
            // If date changed, return 0 immediately
            if (lastDate != today) return 0
            return prefs.getInt(KEY_STEPS_TODAY, 0)
        }

        // Schedule is not needed — keeping this stub so MainActivity compiles
        fun scheduleMidnightReset(context: Context) { /* not needed */ }

        private fun todayString(): String =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsFromSensor = event.values[0]
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")

        if (lastDate != today) {
            // ── Midnight crossed — reset for new day ──
            // Save current sensor value as the new baseline offset
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putFloat(KEY_STEPS_OFFSET, totalStepsFromSensor)
                .putInt(KEY_STEPS_TODAY, 0)
                .apply()
            return
        }

        // ── Normal case — calculate today's steps from baseline ──
        val offset = prefs.getFloat(KEY_STEPS_OFFSET, totalStepsFromSensor)
        val stepsToday = (totalStepsFromSensor - offset).toInt().coerceAtLeast(0)

        prefs.edit()
            .putInt(KEY_STEPS_TODAY, stepsToday)
            .apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}