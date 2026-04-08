package com.spsu.greenmile

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userId: String,
    isDark: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current

    var name          by remember { mutableStateOf("") }
    var rollNo        by remember { mutableStateOf("") }
    var department    by remember { mutableStateOf("") }
    var email         by remember { mutableStateOf("") }
    var role          by remember { mutableStateOf("student") }
    var totalPoints   by remember { mutableStateOf(0) }
    var totalCarbon   by remember { mutableStateOf(0.0) }
    var totalActivities by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var joinedDate    by remember { mutableStateOf("") }
    var isLoading     by remember { mutableStateOf(true) }

    // ── Theme colours ──
    val bg       = if (isDark) Color(0xFF121212) else Color(0xFFF1F8E9)
    val cardBg   = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textMain = if (isDark) Color.White       else Color(0xFF1B5E20)
    val textSub  = if (isDark) Color(0xFFBBBBBB) else Color.Gray
    val headerBg = Color(0xFF2E7D32)

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        if (userId.isEmpty()) { isLoading = false; return@LaunchedEffect }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    name            = doc.getString("name") ?: ""
                    rollNo          = doc.getString("rollNo") ?: ""
                    department      = doc.getString("department") ?: ""
                    email           = doc.getString("email") ?: ""
                    role            = doc.getString("role") ?: "student"
                    totalPoints     = (doc.getLong("totalPoints") ?: 0L).toInt()
                    totalCarbon     = when (val r = doc.get("totalCarbonSaved")) {
                        is Double -> r; is Long -> r.toDouble(); else -> 0.0
                    }
                    totalActivities = (doc.getLong("totalActivitiesLogged") ?: 0L).toInt()
                    currentStreak   = (doc.getLong("currentStreak") ?: 0L).toInt()
                    val ts = doc.getLong("joinedAt") ?: 0L
                    if (ts > 0) {
                        joinedDate = java.text.SimpleDateFormat("dd MMM yyyy",
                            java.util.Locale.getDefault()).format(java.util.Date(ts))
                    }
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val badge = when {
        totalActivities >= 30 -> "🏆 Eco Champion"
        totalCarbon >= 10.0   -> "🌍 Carbon Hero"
        currentStreak >= 7    -> "🔥 Streak Master"
        totalActivities >= 1  -> "🌎 New Joiner"
        else                  -> "🌱 Getting Started"
    }
    val badgeDesc = when {
        totalActivities >= 30 -> "Logged 30+ activities. True sustainability champion!"
        totalCarbon >= 10.0   -> "Tracked over 10 kg CO₂. Keep going!"
        currentStreak >= 7    -> "7-day streak achieved. Consistency is key!"
        totalActivities >= 1  -> "Welcome! Keep logging to earn more points!"
        else                  -> "Start logging activities to earn badges!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(20.dp)
        ) {
            Column {
                Text("← Back", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() }.padding(bottom = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        shape  = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (name.isNotEmpty()) name.first().uppercase() else "?",
                                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (role == "admin") "👑 Admin" else "🎓 Student",
                            fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        if (joinedDate.isNotEmpty())
                            Text("Joined $joinedDate", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {

                Spacer(modifier = Modifier.height(12.dp))

                // ── Personal Info ──
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileRow("Full Name",   name,       textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Roll Number", rollNo,     textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Department",  department, textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Email",       email,      textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Role",        if (role == "admin") "Admin" else "Student", textMain, textSub)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Activity Summary ──
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 Activity Summary", fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = textMain)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileRow("Total Activities Logged", totalActivities.toString(), textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Total Carbon Tracked", "%.2f kg CO₂".format(totalCarbon), textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Green Points Earned", "$totalPoints pts", textMain, textSub)
                        HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                        ProfileRow("Current Streak", "$currentStreak days 🔥", textMain, textSub)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Badge ──
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1A2E1A) else Color(0xFFE8F5E9)
                    )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🏅 Your Badge", fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(badge, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(badgeDesc, fontSize = 13.sp, color = textSub)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Settings Card with Dark Mode Toggle ──
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚙️ Settings", fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = textMain)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isDark) "🌙 Dark Mode" else "☀️ Light Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textMain
                                )
                                Text(
                                    text = "Tap to switch theme",
                                    fontSize = 12.sp,
                                    color = textSub
                                )
                            }
                            Switch(
                                checked = isDark,
                                onCheckedChange = { newVal ->
                                    onDarkModeToggle(newVal)
                                    // Save to SharedPreferences
                                    context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                                        .edit().putBoolean("isDarkMode", newVal).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor   = Color.White,
                                    checkedTrackColor   = Color(0xFF2E7D32),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Logout ──
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("🚪  Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String, textMain: Color, textSub: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = textSub)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textMain)
    }
}