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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ActivityLog(
    val id: String,
    val date: String,
    val travel: String,
    val food: String,
    val electricityHours: Double,
    val usedPlastic: Boolean,
    val carbonKg: Double,
    val pointsEarned: Int,
    val timestamp: Long
)

@Composable
fun ActivityHistoryScreen(
    userId: String,
    isDark: Boolean = false,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var activityList by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCarbon by remember { mutableStateOf(0.0) }
    var totalPoints by remember { mutableStateOf(0) }

    val bg      = if (isDark) Color(0xFF121212) else Color(0xFFF1F8E9)
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color.White       else Color(0xFF1B5E20)
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("activities")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    val logs = result.documents.mapNotNull { doc ->
                        ActivityLog(
                            id = doc.id,
                            date = doc.getString("date") ?: "",
                            travel = doc.getString("travel") ?: "",
                            food = doc.getString("food") ?: "",
                            electricityHours = doc.getDouble("electricityHours") ?: 0.0,
                            usedPlastic = doc.getBoolean("usedPlastic") ?: false,
                            carbonKg = doc.getDouble("carbonKg") ?: 0.0,
                            pointsEarned = (doc.getLong("pointsEarned") ?: 0).toInt(),
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.sortedByDescending { it.timestamp } // sort in memory instead
                    activityList = logs
                    totalCarbon = logs.sumOf { it.carbonKg }
                    totalPoints = logs.sumOf { it.pointsEarned }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
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
                    text = "📋 Activity History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "All your logged activities",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading history...", color = subClr)
                }
            }
        } else if (activityList.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(text = "🌱", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No activities yet!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start logging your daily activities to track your carbon footprint and earn green points!",
                        fontSize = 14.sp,
                        color = subClr,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onBack() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text(
                            "➕ Log First Activity",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📋",
                        value = activityList.size.toString(),
                        label = "Total Logs",
                        isDark = isDark
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🌍",
                        value = "%.1f".format(totalCarbon),
                        label = "kg CO₂",
                        isDark = isDark
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "⭐",
                        value = totalPoints.toString(),
                        label = "Points",
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Average per day card
                val avgCarbon = if (activityList.isNotEmpty())
                    totalCarbon / activityList.size else 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (avgCarbon < 4.0) Color(0xFF2E7D32)
                        else Color(0xFFE65100)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📊", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Average per Day",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "%.2f kg CO₂".format(avgCarbon),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (avgCarbon < 4.0) "🌱 Below average — great job!"
                                else "⚠️ Above average — try greener choices!",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Recent Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textClr
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Activity log cards
                activityList.forEach { log ->
                    ActivityCard(log = log, isDark = isDark)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ActivityCard(log: ActivityLog, isDark: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color.White       else Color.Black
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row — date and carbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDate(log.date),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textClr
                    )
                    Text(
                        text = buildActivitySummary(log),
                        fontSize = 12.sp,
                        color = subClr
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f kg".format(log.carbonKg),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (log.carbonKg < 4.0) Color(0xFF2E7D32)
                        else Color(0xFFE65100)
                    )
                    Text(
                        text = "+${log.pointsEarned} pts",
                        fontSize = 12.sp,
                        color = Color(0xFF43A047),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Carbon bar
            Spacer(modifier = Modifier.height(10.dp))
            val progress = (log.carbonKg / 10.0).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (log.carbonKg < 4.0) Color(0xFF66BB6A)
                else Color(0xFFEF5350),
                trackColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE8F5E9)
            )

            // Expanded details
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFE8F5E9))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Activity Details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF66BB6A) else Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip(
                        modifier = Modifier.weight(1f),
                        emoji = travelEmoji(log.travel),
                        label = log.travel.ifEmpty { "Not set" },
                        isDark = isDark
                    )
                    DetailChip(
                        modifier = Modifier.weight(1f),
                        emoji = foodEmoji(log.food),
                        label = log.food.ifEmpty { "Not set" },
                        isDark = isDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip(
                        modifier = Modifier.weight(1f),
                        emoji = "💡",
                        label = "${log.electricityHours}h AC",
                        isDark = isDark
                    )
                    DetailChip(
                        modifier = Modifier.weight(1f),
                        emoji = if (log.usedPlastic) "♻️" else "✅",
                        label = if (log.usedPlastic) "Used Plastic" else "No Plastic",
                        isDark = isDark
                    )
                }
            }

            // Expand hint
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (expanded) "▲ Show less" else "▼ Show details",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DetailChip(modifier: Modifier, emoji: String, label: String, isDark: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F8E9))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF66BB6A) else Color(0xFF1B5E20),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("EEE, dd MMM yyyy", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}

fun buildActivitySummary(log: ActivityLog): String {
    val parts = mutableListOf<String>()
    if (log.travel.isNotEmpty()) parts.add(travelEmoji(log.travel) + " ${log.travel}")
    if (log.food.isNotEmpty()) parts.add(foodEmoji(log.food) + " ${log.food}")
    return parts.joinToString(" • ")
}

fun travelEmoji(travel: String): String = when (travel) {
    "Car" -> "🚗"
    "Bike" -> "🏍️"
    "Bus" -> "🚌"
    "Cycle" -> "🚲"
    "Walk" -> "🚶"
    else -> "🚗"
}

fun foodEmoji(food: String): String = when (food) {
    "Non-Veg" -> "🥩"
    "Veg" -> "🥗"
    "Junk Food" -> "🍔"
    else -> "🍽️"
}