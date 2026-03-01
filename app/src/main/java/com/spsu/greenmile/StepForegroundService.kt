package com.spsu.greenmile

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StepForegroundService : Service() {

    private lateinit var stepManager: StepCounterManager
    private var userId: String = ""
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        userId = intent?.getStringExtra("userId") ?: ""

        startForeground(1, createNotification())

        stepManager = StepCounterManager(this)
        stepManager.start()

        scope.launch {
            while (true) {
                delay(60000) // Save every 60 seconds

                val steps = stepManager.stepsToday.value
                val carbon = steps * 0.0004
                val points = steps / 100

                saveSteps(userId, steps, carbon, points)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stepManager.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {

        val channelId = "step_channel"

        val channel = NotificationChannel(
            channelId,
            "Step Tracking",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GreenMile Tracking")
            .setContentText("Tracking your steps in background 👣")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }
}