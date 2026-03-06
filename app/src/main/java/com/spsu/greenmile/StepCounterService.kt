package com.spsu.greenmile

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import java.util.Calendar

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1f

    // ── BroadcastReceiver to reset steps at midnight reliably ──
    private val midnightReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MIDNIGHT_RESET) {
                resetStepsForNewDay()
            }
        }
    }

    companion object {
        const val PREFS_NAME = "StepPrefs"
        const val KEY_STEPS_TODAY = "steps_today"
        const val KEY_STEPS_OFFSET = "steps_offset"
        const val KEY_LAST_DATE = "last_date"
        const val ACTION_MIDNIGHT_RESET = "com.spsu.greenmile.MIDNIGHT_RESET"

        fun getStepsToday(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // ── Also check date here as backup reset ──
            val today = java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val lastDate = prefs.getString(KEY_LAST_DATE, "")

            if (lastDate != today && lastDate!!.isNotEmpty()) {
                // Day changed — reset
                prefs.edit()
                    .putInt(KEY_STEPS_TODAY, 0)
                    .putString(KEY_LAST_DATE, today)
                    .apply()
                return 0
            }

            return prefs.getInt(KEY_STEPS_TODAY, 0)
        }

        // ── Schedule AlarmManager to fire at midnight ──
        fun scheduleMidnightReset(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ACTION_MIDNIGHT_RESET)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate next midnight
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }

            // Use setRepeating for every 24 hours
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                midnight.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun resetStepsForNewDay() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.getDefault()
        ).format(java.util.Date())

        // Get current sensor value to use as new offset
        val currentOffset = prefs.getFloat(KEY_STEPS_OFFSET, 0f) +
                prefs.getInt(KEY_STEPS_TODAY, 0)

        prefs.edit()
            .putInt(KEY_STEPS_TODAY, 0)
            .putFloat(KEY_STEPS_OFFSET, currentOffset)
            .putString(KEY_LAST_DATE, today)
            .apply()

        initialSteps = -1f  // Force recalculate from sensor
    }

    override fun onCreate() {
        super.onCreate()

        // ── Register midnight reset receiver ──
        val filter = IntentFilter(ACTION_MIDNIGHT_RESET)
        registerReceiver(midnightReceiver, filter, RECEIVER_NOT_EXPORTED)

        // ── Schedule alarm for midnight reset ──
        scheduleMidnightReset(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalSteps = event.values[0]
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.getDefault()
        ).format(java.util.Date())

        val lastDate = prefs.getString(KEY_LAST_DATE, "")

        // ── Backup date check inside sensor too ──
        if (lastDate != today) {
            resetStepsForNewDay()
            return
        }

        if (initialSteps < 0) {
            initialSteps = prefs.getFloat(KEY_STEPS_OFFSET, totalSteps)
        }

        val stepsToday = (totalSteps - initialSteps).toInt().coerceAtLeast(0)

        prefs.edit()
            .putInt(KEY_STEPS_TODAY, stepsToday)
            .putString(KEY_LAST_DATE, today)
            .apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        try { unregisterReceiver(midnightReceiver) } catch (e: Exception) { }
    }
}