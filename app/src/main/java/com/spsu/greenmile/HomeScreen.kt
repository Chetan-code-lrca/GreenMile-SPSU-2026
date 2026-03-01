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
    onViewAdmin: () -> Unit = {},
    onOpenSteps: () -> Unit
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

        // Header
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onViewProfile() }
            ) {
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
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
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {

            // Carbon Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Carbon Footprint",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "%.1f kg CO₂".format(totalCarbon),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1B5E20)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogActivity,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("➕  Log Today's Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
                Text("📋  Activity History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ STEP COUNTER BUTTON ADDED
            Button(
                onClick = onOpenSteps,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("👣  Step Counter", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewLeaderboard,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("🏆  View Leaderboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}