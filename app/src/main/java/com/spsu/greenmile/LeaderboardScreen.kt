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
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun LeaderboardScreen(isDark: Boolean = false, onBack: () -> Unit) {
    BackHandler { onBack() }

    data class StudentEntry(val name: String, val dept: String, val points: Int, val carbon: Double)
    data class DeptEntry(val dept: String, val points: Int, val count: Int)

    var selectedTab    by remember { mutableStateOf(0) }
    var students       by remember { mutableStateOf<List<StudentEntry>>(emptyList()) }
    var departments    by remember { mutableStateOf<List<DeptEntry>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }

    val bg      = if (isDark) Color(0xFF121212) else Color(0xFFF1F8E9)
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color.White       else Color(0xFF1B5E20)
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray

    val db = FirebaseFirestore.getInstance()

    // ── Real-time listener — updates instantly when admin deletes logs ──
    DisposableEffect(Unit) {
        var reg: ListenerRegistration? = null

        reg = db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) { isLoading = false; return@addSnapshotListener }

                val list = snapshot.documents.mapNotNull { doc ->
                    val name   = doc.getString("name") ?: return@mapNotNull null
                    val dept   = doc.getString("department") ?: "Unknown"
                    val points = (doc.getLong("totalPoints") ?: 0L).toInt()
                    val carbon = when (val r = doc.get("totalCarbonSaved")) {
                        is Double -> r; is Long -> r.toDouble(); else -> 0.0
                    }
                    StudentEntry(name, dept, points, carbon)
                }.sortedByDescending { it.points }

                students = list

                // ── Build department rankings ──
                val deptMap = mutableMapOf<String, Pair<Int, Int>>() // dept -> (totalPts, count)
                list.forEach { s ->
                    val d = s.dept.trim().uppercase()
                    val cur = deptMap[d] ?: Pair(0, 0)
                    deptMap[d] = Pair(cur.first + s.points, cur.second + 1)
                }
                departments = deptMap.map { (d, v) -> DeptEntry(d, v.first, v.second) }
                    .sortedByDescending { it.points }

                isLoading = false
            }

        onDispose { reg?.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2E7D32)).padding(20.dp)) {
            Column {
                Text("← Back", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
                Text("🏆 Leaderboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Top students by green points", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Tabs
        Row(modifier = Modifier.fillMaxWidth().background(if (isDark) Color(0xFF1E1E1E) else Color.White)) {
            listOf("Students", "Departments").forEachIndexed { i, label ->
                val sel = selectedTab == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (sel) Color(0xFF2E7D32) else Color.Transparent)
                        .clickable { selectedTab = i }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color      = if (sel) Color.White else subClr,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 15.sp)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (selectedTab == 0) {
                    // Students
                    if (students.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No students yet 🌱", color = subClr, fontSize = 15.sp)
                        }
                    } else {
                        students.forEachIndexed { index, s ->
                            val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}." }
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape  = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (index == 0 && !isDark) Color(0xFFFFF9C4)
                                    else cardBg
                                )) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(medal, fontSize = 22.sp, modifier = Modifier.width(42.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.name, fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp, color = textClr)
                                        Text(s.dept, fontSize = 12.sp, color = subClr)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${s.points} pts", fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32), fontSize = 15.sp)
                                        Text("%.1f kg".format(s.carbon), fontSize = 11.sp, color = subClr)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Departments
                    if (departments.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No department data yet 🌱", color = subClr, fontSize = 15.sp)
                        }
                    } else {
                        departments.forEachIndexed { index, d ->
                            val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}." }
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape  = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(medal, fontSize = 22.sp, modifier = Modifier.width(42.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(d.dept, fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp, color = textClr)
                                        Text("${d.count} students", fontSize = 12.sp, color = subClr)
                                    }
                                    Text("${d.points} pts", fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32), fontSize = 15.sp)
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