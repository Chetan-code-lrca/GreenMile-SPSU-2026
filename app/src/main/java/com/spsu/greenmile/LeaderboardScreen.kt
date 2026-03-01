package com.spsu.greenmile

import androidx.compose.foundation.background
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
    val rank: Int,
    val name: String,
    val department: String,
    val points: Int,
    val carbonSaved: Double
)

data class DeptEntry(
    val rank: Int,
    val name: String,
    val totalPoints: Int,
    val members: Int
)

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {

    var selectedTab by remember { mutableStateOf(0) }
    var students by remember { mutableStateOf<List<StudentEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexed { index, doc ->
                    StudentEntry(
                        rank = index + 1,
                        name = doc.getString("name") ?: "Unknown",
                        department = doc.getString("department") ?: "N/A",
                        points = (doc.getLong("totalPoints") ?: 0).toInt(),
                        carbonSaved = doc.getDouble("totalCarbonSaved") ?: 0.0
                    )
                }
                students = list
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val departments = listOf(
        DeptEntry(1, "🖥️ CSE", 1220, 45),
        DeptEntry(2, "⚡ ECE", 1080, 40),
        DeptEntry(3, "⚙️ ME", 960, 38),
        DeptEntry(4, "🏗️ Civil", 820, 35),
        DeptEntry(5, "📊 MBA", 650, 30)
    )

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
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontSize = 14.sp
                )
                Text(
                    text = "🏆 Leaderboard",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Top eco-warriors on campus",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabButton(
                text = "👤 Students",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "🏫 Departments",
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
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
                    Text("Loading real data...", color = Color.Gray)
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
                    if (students.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🌱", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No entries yet!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Be the first to log activity",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        if (students.size >= 3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                PodiumCard(student = students[1], medal = "🥈", height = 80)
                                PodiumCard(student = students[0], medal = "🥇", height = 110)
                                PodiumCard(student = students[2], medal = "🥉", height = 65)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            students.drop(3).forEach { student ->
                                StudentRow(student = student)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            students.forEach { student ->
                                StudentRow(student = student)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    departments.forEach { dept ->
                        DeptRow(dept = dept)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "4,730",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Total Points",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "188",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Students",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "52.1 kg",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "CO₂ Saved",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2E7D32) else Color(0xFFE8F5E9),
            contentColor = if (selected) Color.White else Color(0xFF2E7D32)
        )
    ) {
        Text(text = text, fontSize = 13.sp)
    }
}

@Composable
fun PodiumCard(student: StudentEntry, medal: String, height: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = medal, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.width(100.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👤", fontSize = 28.sp)
                Text(
                    text = student.name.split(" ").first(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "${student.points} pts",
                    fontSize = 11.sp,
                    color = Color(0xFF43A047)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(height.dp)
                .background(
                    Color(0xFF2E7D32),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )
    }
}

@Composable
fun StudentRow(student: StudentEntry) {
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
                text = "#${student.rank}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                fontSize = 16.sp,
                modifier = Modifier.width(36.dp)
            )
            Text(text = "👤", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = student.department,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${student.points} pts",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp
                )
                Text(
                    text = "-${student.carbonSaved}kg CO₂",
                    fontSize = 11.sp,
                    color = Color(0xFF66BB6A)
                )
            }
        }
    }
}

@Composable
fun DeptRow(dept: DeptEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dept.rank == 1) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (dept.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#${dept.rank}"
                },
                fontSize = 24.sp,
                modifier = Modifier.width(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dept.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "${dept.members} students participating",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "${dept.totalPoints} pts",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                fontSize = 16.sp
            )
        }
    }
}