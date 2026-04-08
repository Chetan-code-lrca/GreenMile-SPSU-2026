package com.spsu.greenmile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    companion object {
        const val CHANNEL_ID      = "greenmile_steps"
        const val NOTIFICATION_ID = 101
        const val PREFS_NAME      = "StepPrefs"
        const val KEY_STEPS_TODAY  = "steps_today"
        const val KEY_STEPS_OFFSET = "steps_offset"
        const val KEY_LAST_DATE    = "last_date"
        const val KEY_OFFSET_SET   = "offset_set"

        fun todayString(): String =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())

        fun getStepsToday(context: Context): Int {
            val prefs    = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today    = todayString()
            val lastDate = prefs.getString(KEY_LAST_DATE, "")
            if (lastDate != today) return 0
            return prefs.getInt(KEY_STEPS_TODAY, 0)
        }

        fun scheduleMidnightReset(context: Context) { /* no-op */ }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart service if killed by system
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val sensorTotal = event.values[0]
        val prefs       = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today       = todayString()
        val lastDate    = prefs.getString(KEY_LAST_DATE, "")
        val offsetSet   = prefs.getBoolean(KEY_OFFSET_SET, false)

        when {
            lastDate != today -> {
                // ── New day: lock fresh offset ──
                prefs.edit()
                    .putFloat(KEY_STEPS_OFFSET, sensorTotal)
                    .putString(KEY_LAST_DATE, today)
                    .putInt(KEY_STEPS_TODAY, 0)
                    .putBoolean(KEY_OFFSET_SET, true)
                    .apply()
            }
            !offsetSet -> {
                // ── First reading of today ──
                prefs.edit()
                    .putFloat(KEY_STEPS_OFFSET, sensorTotal)
                    .putString(KEY_LAST_DATE, today)
                    .putInt(KEY_STEPS_TODAY, 0)
                    .putBoolean(KEY_OFFSET_SET, true)
                    .apply()
            }
            else -> {
                // ── Normal: offset locked, just subtract ──
                val offset     = prefs.getFloat(KEY_STEPS_OFFSET, sensorTotal)
                val stepsToday = (sensorTotal - offset).toInt().coerceAtLeast(0)
                prefs.edit().putInt(KEY_STEPS_TODAY, stepsToday).apply()

                // Update notification
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(stepsToday))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun buildNotification(steps: Int): Notification {
        val km   = steps * 0.00075
        val kcal = steps * 0.03
        val pi   = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GreenMile — Step Counter")
            .setContentText("$steps steps  •  %.2f km  •  %.0f kcal".format(km, kcal))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "Step Counter",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your daily steps in background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}