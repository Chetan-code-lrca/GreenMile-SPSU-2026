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
        const val PREFS_NAME       = "StepPrefs"
        const val KEY_STEPS_TODAY  = "steps_today"
        const val KEY_STEPS_OFFSET = "steps_offset"
        const val KEY_LAST_DATE    = "last_date"
        const val KEY_OFFSET_SET   = "offset_set"

        // ── public so StepCounterScreen can access it ──
        fun todayString(): String =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())

        fun getStepsToday(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today = todayString()
            val lastDate = prefs.getString(KEY_LAST_DATE, "")
            if (lastDate != today) return 0
            return prefs.getInt(KEY_STEPS_TODAY, 0)
        }

        fun scheduleMidnightReset(context: Context) {
            // no-op for mini project
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val sensorTotal = event.values[0]
        val prefs       = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today       = todayString()
        val lastDate    = prefs.getString(KEY_LAST_DATE, "")
        val offsetSet   = prefs.getBoolean(KEY_OFFSET_SET, false)

        if (lastDate != today) {
            // ── New day — lock fresh offset ──
            prefs.edit()
                .putFloat(KEY_STEPS_OFFSET, sensorTotal)
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_STEPS_TODAY, 0)
                .putBoolean(KEY_OFFSET_SET, true)
                .apply()
            return
        }

        if (!offsetSet) {
            // ── First event today, offset not set yet ──
            prefs.edit()
                .putFloat(KEY_STEPS_OFFSET, sensorTotal)
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_STEPS_TODAY, 0)
                .putBoolean(KEY_OFFSET_SET, true)
                .apply()
            return
        }

        // ── Normal counting — offset locked, never touch it again ──
        val offset     = prefs.getFloat(KEY_STEPS_OFFSET, sensorTotal)
        val stepsToday = (sensorTotal - offset).toInt().coerceAtLeast(0)

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