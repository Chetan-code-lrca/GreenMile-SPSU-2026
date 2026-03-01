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
import com.google.firebase.firestore.Query

data class StudentEntry(
    val name: String,
    val rollNo: String,
    val department: String,
    val points: Int,
    val carbonSaved: Double
)

data class DeptEntry(
    val name: String,
    val totalPoints: Int,
    val memberCount: Int
)

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {

    BackHandler { onBack() }

    var selectedTab by remember { mutableStateOf(0) }
    var studentList by remember { mutableStateOf<List<StudentEntry>>(emptyList()) }
    var deptList by remember { mutableStateOf<List<DeptEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val students = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    StudentEntry(
                        name = name,
                        rollNo = doc.getString("rollNo") ?: "",
                        department = doc.getString("department") ?: "",
                        points = (doc.getLong("totalPoints") ?: 0).toInt(),
                        carbonSaved = doc.getDouble("totalCarbonSaved") ?: 0.0
                    )
                }
                studentList = students

                // Build department leaderboard from student data
                val deptMap = mutableMapOf<String, Pair<Int, Int>>()
                result.documents.forEach { doc ->
                    val dept = doc.getString("department") ?: "Other"
                    val pts = (doc.getLong("totalPoints") ?: 0).toInt()
                    val current = deptMap[dept] ?: Pair(0, 0)
                    deptMap[dept] = Pair(current.first + pts, current.second + 1)
                }
                deptList = deptMap.map { (dept, data) ->
                    DeptEntry(dept, data.first, data.second)
                }.sortedByDescending { it.totalPoints }

                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

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
                    text = "🏆 Leaderboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "SPSU Campus Rankings",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabButton(
                title = "🎓 Students",
                selected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 0 }
            )
            TabButton(
                title = "🏛️ Department",
                selected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 1 }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading rankings...", color = Color.Gray)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    if (studentList.isEmpty()) {
                        EmptyLeaderboard()
                    } else {
                        // Top 3 podium
                        if (studentList.size >= 3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // 2nd place
                                PodiumCard(
                                    modifier = Modifier.weight(1f),
                                    entry = studentList[1],
                                    rank = 2,
                                    height = 80.dp,
                                    color = Color(0xFF90A4AE)
                                )
                                // 1st place
                                PodiumCard(
                                    modifier = Modifier.weight(1f),
                                    entry = studentList[0],
                                    rank = 1,
                                    height = 110.dp,
                                    color = Color(0xFFFFD700)
                                )
                                // 3rd place
                                PodiumCard(
                                    modifier = Modifier.weight(1f),
                                    entry = studentList[2],
                                    rank = 3,
                                    height = 60.dp,
                                    color = Color(0xFFCD7F32)
                                )
                            }
                        }

                        // Rest of the list
                        studentList.drop(3).forEachIndexed { index, entry ->
                            StudentRow(rank = index + 4, entry = entry)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    if (deptList.isEmpty()) {
                        EmptyLeaderboard()
                    } else {
                        deptList.forEachIndexed { index, dept ->
                            DeptRow(rank = index + 1, entry = dept)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campus impact card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌍 Campus Total Impact",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%.1f kg CO₂".format(
                                studentList.sumOf { it.carbonSaved }
                            ),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "tracked by ${studentList.size} students",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2E7D32) else Color(0xFFE8F5E9),
            contentColor = if (selected) Color.White else Color.Gray
        )
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PodiumCard(
    modifier: Modifier,
    entry: StudentEntry,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = entry.name.split(" ").firstOrNull() ?: entry.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "${entry.points} pts",
            fontSize = 11.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" },
                    fontSize = 28.sp
                )
            }
        }
    }
}

@Composable
fun StudentRow(rank: Int, entry: StudentEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.width(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "${entry.rollNo} • ${entry.department}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.points} pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "%.1f kg".format(entry.carbonSaved),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DeptRow(rank: Int, entry: DeptEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" },
                fontSize = 20.sp,
                modifier = Modifier.width(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "${entry.memberCount} students",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "${entry.totalPoints} pts",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun EmptyLeaderboard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🌱", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No entries yet!",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Be the first to log an activity",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}