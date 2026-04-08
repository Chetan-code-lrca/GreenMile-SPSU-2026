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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminScreen(userId: String, isDark: Boolean = false, onBack: () -> Unit) {
    BackHandler { onBack() }

    data class UserEntry(val id: String, val name: String, val dept: String,
                         val points: Int, val role: String, val activities: Int)
    data class LogEntry(val id: String, val userId: String, val userName: String,
                        val date: String, val travel: String, val food: String,
                        val carbon: Double, val points: Int, val suspicious: Boolean)

    var selectedTab    by remember { mutableStateOf(0) }
    var users          by remember { mutableStateOf<List<UserEntry>>(emptyList()) }
    var logs           by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var totalStudents  by remember { mutableStateOf(0) }
    var totalLogs      by remember { mutableStateOf(0) }
    var totalCarbon    by remember { mutableStateOf(0.0) }
    var deptBreakdown  by remember { mutableStateOf<Map<String, Pair<Int,Int>>>(emptyMap()) }
    var isLoading      by remember { mutableStateOf(true) }
    var statusMsg      by remember { mutableStateOf("") }

    val bg      = if (isDark) Color(0xFF121212) else Color(0xFFF1F8E9)
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color.White       else Color(0xFF1B5E20)
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray

    val db = FirebaseFirestore.getInstance()

    fun loadData() {
        isLoading = true
        db.collection("users").get().addOnSuccessListener { userSnap ->
            val uList = userSnap.documents.mapNotNull { doc ->
                val name  = doc.getString("name") ?: return@mapNotNull null
                val dept  = doc.getString("department") ?: "Unknown"
                val pts   = (doc.getLong("totalPoints") ?: 0L).toInt()
                val role  = doc.getString("role") ?: "student"
                val acts  = (doc.getLong("totalActivitiesLogged") ?: 0L).toInt()
                UserEntry(doc.id, name, dept, pts, role, acts)
            }
            users         = uList
            totalStudents = uList.count { it.role == "student" }

            // Department breakdown
            val dm = mutableMapOf<String, Pair<Int,Int>>()
            uList.forEach { u ->
                val d = u.dept.trim().uppercase()
                val c = dm[d] ?: Pair(0,0)
                dm[d] = Pair(c.first + u.points, c.second + 1)
            }
            deptBreakdown = dm

            db.collection("activities").get().addOnSuccessListener { logSnap ->
                var carbon = 0.0
                val lList = logSnap.documents.mapNotNull { doc ->
                    val uName   = doc.getString("userName") ?: ""
                    val date    = doc.getString("date") ?: ""
                    val travel  = doc.getString("travel") ?: ""
                    val food    = doc.getString("food") ?: ""
                    val c       = doc.getDouble("carbonKg") ?: 0.0
                    val pts     = (doc.getLong("pointsEarned") ?: 0L).toInt()
                    val uid     = doc.getString("userId") ?: ""
                    carbon     += c
                    // Flag suspicious if carbon > 12 or points > 45
                    val susp    = c > 12.0 || pts > 45
                    LogEntry(doc.id, uid, uName, date, travel, food, c, pts, susp)
                }.sortedByDescending { it.date }

                logs       = lList
                totalLogs  = lList.size
                totalCarbon = carbon
                isLoading   = false
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }

    // ── Delete log AND recalculate user stats so leaderboard updates ──
    fun deleteLog(log: LogEntry) {
        db.collection("activities").document(log.id).delete()
            .addOnSuccessListener {
                // Recalculate user stats from remaining logs
                db.collection("activities")
                    .whereEqualTo("userId", log.userId)
                    .get()
                    .addOnSuccessListener { remaining ->
                        var newCarbon = 0.0
                        var newPoints = 0L
                        val newCount  = remaining.size().toLong()
                        remaining.documents.forEach { doc ->
                            newCarbon += doc.getDouble("carbonKg") ?: 0.0
                            newPoints += doc.getLong("pointsEarned") ?: 0L
                        }
                        val carbonRounded = Math.round(newCarbon * 100.0) / 100.0

                        // Update user document with recalculated stats
                        db.collection("users").document(log.userId)
                            .update(mapOf(
                                "totalPoints"           to newPoints,
                                "totalCarbonSaved"      to carbonRounded,
                                "totalActivitiesLogged" to newCount
                            ))
                            .addOnSuccessListener {
                                statusMsg = "✅ Log deleted and user stats updated"
                                loadData()
                            }
                            .addOnFailureListener {
                                statusMsg = "Log deleted but stats update failed"
                                loadData()
                            }
                    }
                    .addOnFailureListener {
                        statusMsg = "Log deleted but could not recalculate stats"
                        loadData()
                    }
            }
            .addOnFailureListener { statusMsg = "❌ Failed to delete" }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1B5E20)).padding(20.dp)) {
            Column {
                Text("← Back", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
                Text("👑 Admin Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("GreenMile Management Dashboard", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Tabs
        Row(modifier = Modifier.fillMaxWidth().background(if (isDark) Color(0xFF1E1E1E) else Color.White)) {
            listOf("Overview", "Users", "Logs").forEachIndexed { i, label ->
                val sel = selectedTab == i
                Box(modifier = Modifier
                    .weight(1f)
                    .background(if (sel) Color(0xFF2E7D32) else Color.Transparent)
                    .clickable { selectedTab = i }
                    .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color      = if (sel) Color.White else subClr,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 14.sp)
                }
            }
        }

        if (statusMsg.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape  = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusMsg.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )) {
                Text(statusMsg, modifier = Modifier.padding(12.dp), fontSize = 13.sp,
                    color = if (statusMsg.startsWith("✅")) Color(0xFF2E7D32) else Color(0xFFB71C1C))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (selectedTab) {
                    // ── OVERVIEW ──
                    0 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OverviewCard(Modifier.weight(1f), "👥", totalStudents.toString(), "Students", cardBg, textClr)
                            OverviewCard(Modifier.weight(1f), "📋", totalLogs.toString(), "Logs", cardBg, textClr)
                            OverviewCard(Modifier.weight(1f), "🌍", "%.0f".format(totalCarbon), "kg CO₂", cardBg, textClr)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🏛 Department Breakdown", fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp, color = textClr)
                                Spacer(modifier = Modifier.height(10.dp))
                                deptBreakdown.entries.sortedByDescending { it.value.first }.forEach { (dept, v) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(dept, fontSize = 14.sp, color = textClr)
                                        Row {
                                            Text("${v.second} students", fontSize = 12.sp, color = subClr)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("${v.first} pts", fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32), fontSize = 14.sp)
                                        }
                                    }
                                    HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE))
                                }
                            }
                        }
                    }

                    // ── USERS ──
                    1 -> {
                        users.forEach { u ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape  = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textClr)
                                        Text("${u.dept}  •  ${u.role}", fontSize = 12.sp, color = subClr)
                                        Text("${u.activities} activities", fontSize = 11.sp, color = subClr)
                                    }
                                    Text("${u.points} pts", fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32), fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // ── LOGS ──
                    2 -> {
                        if (logs.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No logs found", color = subClr, fontSize = 14.sp)
                            }
                        } else {
                            logs.forEach { log ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape  = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (log.suspicious) Color(0xFFFFEBEE) else cardBg
                                    )) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(log.userName, fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp, color = if (log.suspicious) Color(0xFFB71C1C) else textClr)
                                                Text("${log.date}  •  ${log.travel}  •  ${log.food}",
                                                    fontSize = 12.sp, color = subClr)
                                                Text("%.1f kg CO₂  •  ${log.points} pts".format(log.carbon),
                                                    fontSize = 12.sp, color = subClr)
                                                if (log.suspicious)
                                                    Text("⚠️ Suspicious", fontSize = 11.sp,
                                                        color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = { deleteLog(log) },
                                                colors  = ButtonDefaults.textButtonColors(
                                                    contentColor = Color(0xFFD32F2F)
                                                )
                                            ) { Text("Delete", fontSize = 13.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OverviewCard(modifier: Modifier, emoji: String, value: String, label: String,
                 cardBg: Color, textClr: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
            Text(label, fontSize = 11.sp, color = textClr)
        }
    }
}