package com.spsu.greenmile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LogActivityScreen(
    userId: String,
    userName: String = "",
    userRoll: String = "",
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTravel by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf("") }
    var electricityHours by remember { mutableStateOf("") }
    var usedPlastic by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

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

    fun calculatePoints(carbon: Double): Int = when {
        carbon < 2.0 -> 50
        carbon < 3.0 -> 35
        carbon < 4.0 -> 20
        carbon < 5.0 -> 10
        else -> 5
    }

    fun saveActivity() {
        // Validate using LogValidator
        val validationError = LogValidator.validate(selectedTravel, selectedFood, electricityHours)
        if (validationError != null) {
            errorMsg = validationError
            return
        }
        if (userId.isEmpty()) {
            errorMsg = "User not logged in properly"
            return
        }

        isLoading = true
        errorMsg = ""

        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        // Check for duplicate log today
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    isLoading = false
                    errorMsg = "You already logged today! Come back tomorrow 🌱"
                    return@addOnSuccessListener
                }

                val carbon = calculateCarbon()
                val points = calculatePoints(carbon)

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

                db.collection("activities").add(activityData)
                    .addOnSuccessListener {
                        db.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "totalPoints" to FieldValue.increment(points.toLong()),
                                    "totalCarbonSaved" to FieldValue.increment(carbon),
                                    "totalActivitiesLogged" to FieldValue.increment(1)
                                )
                            )
                            .addOnSuccessListener {
                                // Update streak using StreakManager
                                StreakManager.updateStreak(userId)
                                isLoading = false
                                successMsg = "Activity logged! +$points pts earned 🎉"
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMsg = "Saved activity but failed to update stats"
                            }
                    }
                    .addOnFailureListener {
                        isLoading = false
                        errorMsg = "Failed to save. Please try again."
                    }
            }
            .addOnFailureListener {
                isLoading = false
                errorMsg = "Network error. Please check connection."
            }
    }

    val carbon = calculateCarbon()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "← Back",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "🌱 Log Today's Activity",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Track your carbon footprint",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ── Travel Section ──
            SectionTitle(emoji = "🚗", title = "How did you travel today?")
            Spacer(modifier = Modifier.height(8.dp))
            OptionGrid(
                options = listOf("Car", "Bike", "Bus", "Cycle", "Walk"),
                selected = selectedTravel,
                onSelect = { selectedTravel = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Food Section ──
            SectionTitle(emoji = "🍽️", title = "What did you eat today?")
            Spacer(modifier = Modifier.height(8.dp))
            OptionGrid(
                options = listOf("Non-Veg", "Veg", "Junk Food"),
                selected = selectedFood,
                onSelect = { selectedFood = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Electricity Section ──
            SectionTitle(emoji = "💡", title = "Hours of AC/Electricity used?")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = electricityHours,
                onValueChange = { electricityHours = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Hours (0–24)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedLabelColor = Color(0xFF2E7D32)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Plastic Section ──
            SectionTitle(emoji = "♻️", title = "Did you use single-use plastic?")
            Spacer(modifier = Modifier.height(8.dp))
            PlasticOption(
                usedPlastic = usedPlastic,
                onToggle = { usedPlastic = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Carbon Preview ──
            if (selectedTravel.isNotEmpty() || selectedFood.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (carbon < 4.0) Color(0xFF2E7D32)
                        else Color(0xFFE65100)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Estimated Carbon Footprint",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "%.2f kg CO₂".format(carbon),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (carbon < 4.0) "🌱 Great choice! You'll earn ${calculatePoints(carbon)} pts"
                            else "⚠️ High carbon day. You'll earn ${calculatePoints(carbon)} pts",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val barProgress = (carbon / 10.0).coerceIn(0.0, 1.0).toFloat()
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Error / Success Messages ──
            if (errorMsg.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (successMsg.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Text(
                        text = successMsg,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    )
                ) {
                    Text(
                        "✅  Go to Home",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                // ── Submit Button ──
                Button(
                    onClick = { saveActivity() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "💾  Save Activity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Helper Composables ──

@Composable
fun SectionTitle(emoji: String, title: String) {
    Text(
        text = "$emoji  $title",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Color(0xFF1B5E20)
    )
}

@Composable
fun OptionGrid(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(option) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF2E7D32)
                    else Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PlasticOption(usedPlastic: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onToggle(true) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (usedPlastic) Color(0xFFE65100) else Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛍️ Used Plastic",
                    fontWeight = if (usedPlastic) FontWeight.Bold else FontWeight.Normal,
                    color = if (usedPlastic) Color.White else Color.Black,
                    fontSize = 13.sp
                )
            }
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onToggle(false) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!usedPlastic) Color(0xFF2E7D32) else Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♻️ Reusable",
                    fontWeight = if (!usedPlastic) FontWeight.Bold else FontWeight.Normal,
                    color = if (!usedPlastic) Color.White else Color.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}