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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    BackHandler { onBack() }

    var selectedTravel by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf("") }
    var electricityHours by remember { mutableStateOf("") }
    var usedPlastic by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    fun calculateCarbon(): Double {
        var total = 0.0
        total += when (selectedTravel) {
            "Car" -> 3.0
            "Bike" -> 1.5
            "Bus" -> 0.8
            "Cycle" -> 0.0
            "Walk" -> 0.0
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
        if (selectedTravel.isEmpty() || selectedFood.isEmpty()) {
            errorMsg = "Please select travel and food options"
            return
        }
        if (userId.isEmpty()) {
            errorMsg = "User not logged in properly"
            return
        }

        val elecHours = electricityHours.toDoubleOrNull()
        if (electricityHours.isNotEmpty() &&
            (elecHours == null || elecHours < 0 || elecHours > 24)
        ) {
            errorMsg = "Electricity hours must be between 0 and 24"
            return
        }

        isLoading = true
        errorMsg = ""

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Check duplicate first
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

                db.collection("activities")
                    .add(activityData)
                    .addOnSuccessListener {
                        // Calculate streak
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val lastActiveDate =
                                    userDoc.getString("lastActiveDate") ?: ""
                                val yesterday = SimpleDateFormat(
                                    "yyyy-MM-dd", Locale.getDefault()
                                ).format(Date(System.currentTimeMillis() - 86400000))

                                val currentStreak =
                                    (userDoc.getLong("currentStreak") ?: 0).toInt()

                                val newStreak = when (lastActiveDate) {
                                    yesterday -> currentStreak + 1
                                    today -> currentStreak
                                    else -> 1
                                }

                                db.collection("users").document(userId)
                                    .update(
                                        mapOf(
                                            "totalPoints" to
                                                    FieldValue.increment(points.toLong()),
                                            "totalCarbonSaved" to
                                                    FieldValue.increment(carbon),
                                            "totalActivitiesLogged" to
                                                    FieldValue.increment(1),
                                            "lastActiveDate" to today,
                                            "currentStreak" to newStreak
                                        )
                                    )
                                    .addOnSuccessListener {
                                        isLoading = false
                                        onSubmit()
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        onSubmit()
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                onSubmit()
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMsg = "Failed to save: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMsg = "Failed to check duplicate: ${e.message}"
            }
    }

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
        Text(
            text = "Track your choices to calculate carbon footprint",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // TRAVEL
        SectionTitle(emoji = "🚗", title = "How did you travel today?")
        Spacer(modifier = Modifier.height(10.dp))
        OptionGrid(
            options = listOf("🚗 Car", "🏍️ Bike", "🚌 Bus", "🚲 Cycle", "🚶 Walk"),
            selected = selectedTravel,
            onSelect = { selectedTravel = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // FOOD
        SectionTitle(emoji = "🍽️", title = "What did you eat today?")
        Spacer(modifier = Modifier.height(10.dp))
        OptionGrid(
            options = listOf("🥩 Non-Veg", "🥗 Veg", "🍔 Junk Food"),
            selected = selectedFood,
            onSelect = { selectedFood = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ELECTRICITY
        SectionTitle(emoji = "💡", title = "Electricity usage (hours)")
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = electricityHours,
            onValueChange = { electricityHours = it },
            label = { Text("Hours of AC/heavy appliance use (0-24)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PLASTIC
        SectionTitle(emoji = "♻️", title = "Plastic usage")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlasticOption(
                label = "Used Plastic ❌",
                selected = usedPlastic,
                modifier = Modifier.weight(1f),
                onClick = { usedPlastic = true }
            )
            PlasticOption(
                label = "Reusable ✅",
                selected = !usedPlastic,
                modifier = Modifier.weight(1f),
                onClick = { usedPlastic = false }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Carbon Preview Card
        val carbonPreview = calculateCarbon()
        val pointsPreview = calculatePoints(carbonPreview)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (carbonPreview < 4.0) Color(0xFF2E7D32)
                else Color(0xFFB71C1C)
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
                    text = "%.1f kg CO₂".format(carbonPreview),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (carbonPreview < 4.0) "🌱 Great effort! +$pointsPreview points"
                    else "⚠️ Try greener choices! +$pointsPreview points",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (errorMsg.contains("already"))
                        Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = if (errorMsg.contains("already")) "✅ $errorMsg"
                    else "⚠️ $errorMsg",
                    color = if (errorMsg.contains("already")) Color(0xFF2E7D32)
                    else Color(0xFFB71C1C),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    text = "✅  Save Activity Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SectionTitle(emoji: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B5E20)
        )
    }
}

@Composable
fun OptionGrid(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val rows = options.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val label = option.substringAfter(" ")
                    val isSelected = selected == label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color(0xFF2E7D32) else Color.White,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(label) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PlasticOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) Color(0xFF2E7D32) else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (selected) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) Color.White else Color.DarkGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}