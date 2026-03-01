package com.spsu.greenmile

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StepCounterScreen(
    userId: String,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val stepManager = remember { StepCounterManager(context) }
    val steps by stepManager.stepsToday.collectAsState()

    DisposableEffect(Unit) {
        stepManager.start()
        onDispose { stepManager.stop() }
    }

    // ✅ START FOREGROUND BACKGROUND TRACKING SERVICE
    LaunchedEffect(Unit) {
        val intent = Intent(context, StepForegroundService::class.java)
        intent.putExtra("userId", userId)
        context.startForegroundService(intent)
    }

    val carbonSaved = steps * 0.0004
    val pointsEarned = steps / 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("← Back")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "👣 Step Counter",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Steps Today", fontSize = 18.sp)

        Text(
            text = "$steps",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Carbon Saved: %.3f kg CO₂".format(carbonSaved),
            fontSize = 16.sp
        )

        Text(
            text = "Points Earned: $pointsEarned",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                saveSteps(userId, steps, carbonSaved, pointsEarned)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Today Activity")
        }
    }
}

/* ---------------- SAVE FUNCTION ---------------- */

fun saveSteps(
    userId: String,
    steps: Int,
    carbon: Double,
    points: Int
) {

    val db = FirebaseFirestore.getInstance()

    val todayDate = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).format(Date())

    val documentId = "${userId}_$todayDate"
    val docRef = db.collection("activities").document(documentId)

    db.runTransaction { transaction ->

        val snapshot = transaction.get(docRef)

        if (snapshot.exists()) {

            val oldCarbon = snapshot.getDouble("carbonKg") ?: 0.0
            val oldPoints = snapshot.getLong("pointsEarned") ?: 0

            transaction.update(
                docRef,
                mapOf(
                    "carbonKg" to carbon,
                    "pointsEarned" to points,
                    "steps" to steps,
                    "timestamp" to System.currentTimeMillis()
                )
            )

            updateLeaderboard(
                userId,
                carbon - oldCarbon,
                (points - oldPoints).toInt()
            )

        } else {

            val activityData = hashMapOf(
                "userId" to userId,
                "date" to todayDate,
                "travel" to "Walk",
                "food" to "",
                "electricityHours" to 0.0,
                "usedPlastic" to false,
                "carbonKg" to carbon,
                "pointsEarned" to points,
                "steps" to steps,
                "timestamp" to System.currentTimeMillis()
            )

            transaction.set(docRef, activityData)

            updateLeaderboard(userId, carbon, points)
        }
    }
}

/* ---------------- LEADERBOARD UPDATE ---------------- */

fun updateLeaderboard(
    userId: String,
    carbon: Double,
    points: Int
) {

    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("leaderboard").document(userId)

    db.runTransaction { transaction ->

        val snapshot = transaction.get(ref)

        val oldPoints = snapshot.getLong("totalPoints") ?: 0
        val oldCarbon = snapshot.getDouble("totalCarbon") ?: 0.0

        transaction.set(
            ref,
            mapOf(
                "totalPoints" to oldPoints + points,
                "totalCarbon" to oldCarbon + carbon
            )
        )
    }
}