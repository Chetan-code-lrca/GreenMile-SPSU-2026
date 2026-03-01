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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var totalPoints by remember { mutableStateOf(0) }
    var totalCarbon by remember { mutableStateOf(0.0) }
    var totalActivities by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var joinedAt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        name = doc.getString("name") ?: ""
                        email = doc.getString("email") ?: ""
                        rollNo = doc.getString("rollNo") ?: ""
                        department = doc.getString("department") ?: ""
                        role = doc.getString("role") ?: "student"
                        totalPoints = (doc.getLong("totalPoints") ?: 0).toInt()
                        totalCarbon = doc.getDouble("totalCarbonSaved") ?: 0.0
                        totalActivities = (doc.getLong("totalActivitiesLogged") ?: 0).toInt()
                        currentStreak = (doc.getLong("currentStreak") ?: 0).toInt()
                        val ts = doc.getLong("joinedAt") ?: 0L
                        if (ts > 0) {
                            val date = java.util.Date(ts)
                            joinedAt = java.text.SimpleDateFormat(
                                "dd MMM yyyy",
                                java.util.Locale.getDefault()
                            ).format(date)
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout from GreenMile?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    )
                ) {
                    Text("Yes, Logout", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
    ) {
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
                        .padding(bottom = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (name.isNotEmpty()) name.first().uppercase() else "?",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = name.ifEmpty { "Loading..." },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (role == "admin") "👑 Admin" else "🎓 Student",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (joinedAt.isNotEmpty()) {
                            Text(
                                text = "Joined $joinedAt",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
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
                    Text("Loading profile...", color = Color.Gray)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "⭐",
                        value = totalPoints.toString(),
                        label = "Points"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🌍",
                        value = "%.1f".format(totalCarbon),
                        label = "kg CO₂"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🔥",
                        value = currentStreak.toString(),
                        label = "Streak"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "👤 Personal Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileRow(label = "Full Name", value = name)
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(label = "Roll Number", value = rollNo)
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(label = "Department", value = department)
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(label = "Email", value = email)
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(
                            label = "Role",
                            value = role.replaceFirstChar { it.uppercase() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 Activity Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileRow(
                            label = "Total Activities Logged",
                            value = totalActivities.toString()
                        )
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(
                            label = "Total Carbon Tracked",
                            value = "%.2f kg CO₂".format(totalCarbon)
                        )
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(
                            label = "Green Points Earned",
                            value = "$totalPoints pts"
                        )
                        HorizontalDivider(color = Color(0xFFE8F5E9))
                        ProfileRow(
                            label = "Current Streak",
                            value = "$currentStreak days 🔥"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏅 Your Badge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val badge = when {
                            totalPoints >= 500 -> "🌟 Eco Champion"
                            totalPoints >= 300 -> "🌿 Green Warrior"
                            totalPoints >= 100 -> "🌱 Eco Starter"
                            totalActivities > 0 -> "🌍 New Joiner"
                            else -> "👣 Just Getting Started"
                        }
                        Text(
                            text = badge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                totalPoints >= 500 -> "Amazing! You're a campus sustainability leader!"
                                totalPoints >= 300 -> "Great work! Keep logging to reach Champion status!"
                                totalPoints >= 100 -> "Good start! You're making a real difference!"
                                totalActivities > 0 -> "Welcome! Keep logging to earn more points!"
                                else -> "Log your first activity to start your eco journey!"
                            },
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    )
                ) {
                    Text(
                        text = "🚪  Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifEmpty { "—" },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}