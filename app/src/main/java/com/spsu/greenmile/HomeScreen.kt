package com.spsu.greenmile

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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    userName: String = "Student",
    userId: String = "",
    userRole: String = "student",
    onLogActivity: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onViewProfile: () -> Unit,
    onViewHistory: () -> Unit,
    onViewSteps: () -> Unit,
    onViewAdmin: () -> Unit = {}
) {
    var totalCarbon by remember { mutableStateOf(0.0) }
    var totalPoints by remember { mutableStateOf(0) }
    var totalActivities by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var department by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(userRole) }
    var isLoading by remember { mutableStateOf(true) }
    var ecoTip by remember { mutableStateOf("Walk or cycle to campus to save up to 1.5 kg CO₂ daily!") }

    val db = FirebaseFirestore.getInstance()

    val ecoTips = listOf(
        "🚶 Walk or cycle to campus to save up to 1.5 kg CO₂ daily!",
        "🥗 Choosing a vegetarian meal saves ~2.5 kg CO₂ vs non-veg!",
        "💡 Turn off AC when not in room — saves 0.5 kg CO₂ per hour!",
        "♻️ Using a reusable bottle saves 0.3 kg CO₂ per day!",
        "🚌 Taking the college bus instead of bike saves ~0.7 kg CO₂!",
        "🌱 Every small green choice adds up to a big campus impact!"
    )

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        totalCarbon = doc.getDouble("totalCarbonSaved") ?: 0.0
                        totalPoints = (doc.getLong("totalPoints") ?: 0).toInt()
                        totalActivities = (doc.getLong("totalActivitiesLogged") ?: 0).toInt()
                        currentStreak = (doc.getLong("currentStreak") ?: 0).toInt()
                        department = doc.getString("department") ?: ""
                        role = doc.getString("role") ?: "student"
                    }
                    isLoading = false
                    ecoTip = ecoTips.random()
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, $userName 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = if (department.isNotEmpty()) "$department • SPSU"
                    else "SPSU GreenMile",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            // ── Clickable Profile Circle ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onViewProfile() }
            ) {
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.first().uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Profile",
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {

            // ── Carbon Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Carbon Footprint",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "%.1f kg CO₂".format(totalCarbon),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (totalActivities == 0) "Log your first activity below! 👇"
                        else "Across $totalActivities activities logged 🌱",
                        color = Color(0xFFA5D6A7),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val progress = (totalCarbon / 50.0).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF69F0AE),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% of monthly average",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Stats Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "⭐",
                    value = totalPoints.toString(),
                    label = "Green Points"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📋",
                    value = totalActivities.toString(),
                    label = "Activities"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    value = currentStreak.toString(),
                    label = "Day Streak"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Weekly Chart ──
            WeeklyChartCard(userId = userId)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1B5E20)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Log Activity ──
            Button(
                onClick = onLogActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF43A047)
                )
            ) {
                Text(
                    text = "➕  Log Today's Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Step Counter ──
            Button(
                onClick = onViewSteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00838F)
                )
            ) {
                Text(
                    text = "👟  Step Counter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Activity History ──
            Button(
                onClick = onViewHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00796B)
                )
            ) {
                Text(
                    text = "📋  Activity History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Leaderboard ──
            Button(
                onClick = onViewLeaderboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {
                Text(
                    text = "🏆  View Leaderboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // ── Admin Panel — only visible to admins ──
            if (role == "admin") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onViewAdmin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A148C)
                    )
                ) {
                    Text(
                        text = "👑  Admin Panel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Eco Tip ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Eco Tip of the Day",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp
                        )
                        Text(
                            text = ecoTip,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── First time welcome card ──
            if (totalActivities == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🌟 Welcome to GreenMile!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start by logging today's activity to track your carbon footprint and earn your first green points!",
                            color = Color(0xFFBF360C),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}