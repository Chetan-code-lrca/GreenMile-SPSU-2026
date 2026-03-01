package com.spsu.greenmile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.spsu.greenmile.utils.LogValidator
import com.spsu.greenmile.utils.StreakManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogActivityScreen(
    userId: String,
    userName: String = "",
    userRoll: String = "",
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {

    var selectedTravel by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf("") }
    var electricityHours by remember { mutableStateOf("") }
    var usedPlastic by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun calculateCarbon(): Double {
        var total = 0.0

        total += when (selectedTravel) {
            "Car" -> 3.0
            "Bike" -> 1.5
            "Bus" -> 0.8
            "Cycle", "Walk" -> 0.0
            else -> 1.0
        }

        total += when (selectedFood) {
            "Non-Veg" -> 3.5
            "Veg" -> 1.0
            "Junk Food" -> 2.0
            else -> 1.5
        }

        val hours = electricityHours.toDoubleOrNull() ?: 0.0
        total += hours * 0.5

        if (usedPlastic) total += 0.3

        return total
    }

    fun calculatePoints(carbon: Double): Int {
        return when {
            carbon < 2.0 -> 50
            carbon < 3.0 -> 35
            carbon < 4.0 -> 20
            carbon < 5.0 -> 10
            else -> 5
        }
    }

    fun saveActivity() {

        scope.launch {

            val canLog = LogValidator.canLogToday(userId)

            if (!canLog) {
                errorMsg = "You already logged today!"
                return@launch
            }

            if (selectedTravel.isEmpty() || selectedFood.isEmpty()) {
                errorMsg = "Please select travel and food options"
                return@launch
            }

            val elecHours = electricityHours.toDoubleOrNull()
            if (electricityHours.isNotEmpty() &&
                (elecHours == null || elecHours < 0 || elecHours > 24)
            ) {
                errorMsg = "Electricity hours must be between 0 and 24"
                return@launch
            }

            isLoading = true
            errorMsg = ""

            val carbon = calculateCarbon()
            val points = calculatePoints(carbon)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val activityData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "rollNo" to userRoll,
                "date" to today,
                "timestamp" to System.currentTimeMillis(),
                "travel" to selectedTravel,
                "food" to selectedFood,
                "electricityHours" to (electricityHours.toDoubleOrNull() ?: 0.0),
                "usedPlastic" to usedPlastic,
                "carbonKg" to carbon,
                "pointsEarned" to points
            )

            db.collection("activities")
                .add(activityData)
                .addOnSuccessListener {

                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "totalPoints" to FieldValue.increment(points.toLong()),
                                "totalCarbonSaved" to FieldValue.increment(carbon),
                                "totalActivitiesLogged" to FieldValue.increment(1),
                                "lastActiveDate" to today
                            )
                        )
                        .addOnSuccessListener {

                            scope.launch {
                                StreakManager.updateStreak(userId)
                            }

                            isLoading = false
                            successMsg = "Saved! +$points points earned 🌱"
                            onSubmit()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            successMsg = "Activity saved! Points update pending."
                            onSubmit()
                        }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    errorMsg = "Failed to save: ${e.message}"
                }
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "← Back",
            color = Color(0xFF2E7D32),
            modifier = Modifier.clickable { onBack() },
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Log Today's Activity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { saveActivity() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF43A047)
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "✅ Save Activity Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMsg,
                color = Color.Red,
                fontSize = 13.sp
            )
        }

        if (successMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = successMsg,
                color = Color(0xFF2E7D32),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}