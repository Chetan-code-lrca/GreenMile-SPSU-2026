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

data class UserReport(
    val uid: String,
    val name: String,
    val rollNo: String,
    val department: String,
    val totalPoints: Int,
    val totalActivities: Int,
    val totalCarbon: Double,
    val role: String
)

data class ActivityReport(
    val id: String,
    val userName: String,
    val rollNo: String,
    val date: String,
    val travel: String,
    val food: String,
    val electricityHours: Double,
    val usedPlastic: Boolean,
    val carbonKg: Double,
    val pointsEarned: Int,
    val flagged: Boolean
)

@Composable
fun AdminScreen(
    userId: String,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableStateOf(0) }
    var userList by remember { mutableStateOf<List<UserReport>>(emptyList()) }
    var activityList by remember { mutableStateOf<List<ActivityReport>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(true) }
    var isLoadingActivities by remember { mutableStateOf(true) }
    var totalUsersCount by remember { mutableStateOf(0) }
    var totalActivitiesCount by remember { mutableStateOf(0) }
    var totalCampusCarbon by remember { mutableStateOf(0.0) }
    var isAdmin by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Check if user is admin
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                isAdmin = doc.getString("role") == "admin"
            }
    }

    // Load all users
    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val users = result.documents.mapNotNull { doc ->
                    UserReport(
                        uid = doc.id,
                        name = doc.getString("name") ?: "",
                        rollNo = doc.getString("rollNo") ?: "",
                        department = doc.getString("department") ?: "",
                        totalPoints = (doc.getLong("totalPoints") ?: 0).toInt(),
                        totalActivities = (doc.getLong("totalActivitiesLogged") ?: 0).toInt(),
                        totalCarbon = doc.getDouble("totalCarbonSaved") ?: 0.0,
                        role = doc.getString("role") ?: "student"
                    )
                }
                userList = users
                totalUsersCount = users.size
                totalCampusCarbon = users.sumOf { it.totalCarbon }
                isLoadingUsers = false
            }
            .addOnFailureListener { isLoadingUsers = false }
    }

    // Load all activities
    LaunchedEffect(Unit) {
        db.collection("activities")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                val activities = result.documents.mapNotNull { doc ->
                    val carbon = doc.getDouble("carbonKg") ?: 0.0
                    // Auto flag suspicious entries
                    val isFlagged = carbon > 15.0 ||
                            (doc.getDouble("electricityHours") ?: 0.0) > 20.0

                    ActivityReport(
                        id = doc.id,
                        userName = doc.getString("userName") ?: "Unknown",
                        rollNo = doc.getString("rollNo") ?: "",
                        date = doc.getString("date") ?: "",
                        travel = doc.getString("travel") ?: "",
                        food = doc.getString("food") ?: "",
                        electricityHours = doc.getDouble("electricityHours") ?: 0.0,
                        usedPlastic = doc.getBoolean("usedPlastic") ?: false,
                        carbonKg = carbon,
                        pointsEarned = (doc.getLong("pointsEarned") ?: 0).toInt(),
                        flagged = isFlagged
                    )
                }
                activityList = activities
                totalActivitiesCount = activities.size
                isLoadingActivities = false
            }
            .addOnFailureListener { isLoadingActivities = false }
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
                .background(Color(0xFF1B5E20))
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
                    text = "👑 Admin Panel",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "GreenMile Management Dashboard",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (!isAdmin) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🚫", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Access Denied",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                    Text(
                        text = "Only admins can access this panel",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            return@Column
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                title = "📊 Overview",
                selected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 0 }
            )
            TabButton(
                title = "👥 Users",
                selected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 1 }
            )
            TabButton(
                title = "📋 Logs",
                selected = selectedTab == 2,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 2 }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (selectedTab) {

                // ── OVERVIEW TAB ──
                0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "👥",
                            value = totalUsersCount.toString(),
                            label = "Students"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "📋",
                            value = totalActivitiesCount.toString(),
                            label = "Logs"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            emoji = "🌍",
                            value = "%.0f".format(totalCampusCarbon),
                            label = "kg CO₂"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Flagged activities warning
                    val flaggedCount = activityList.count { it.flagged }
                    if (flaggedCount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⚠️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$flaggedCount Suspicious Activities",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Unusually high carbon values detected. Check Logs tab.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB71C1C).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Department breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏛️ Department Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val deptStats = userList
                                .groupBy { it.department }
                                .mapValues { (_, users) ->
                                    Triple(
                                        users.size,
                                        users.sumOf { it.totalPoints },
                                        users.sumOf { it.totalCarbon }
                                    )
                                }
                                .entries
                                .sortedByDescending { it.value.second }

                            deptStats.forEach { (dept, stats) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dept,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "${stats.first} students",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${stats.second} pts",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE8F5E9))
                            }
                        }
                    }
                }

                // ── USERS TAB ──
                1 -> {
                    if (isLoadingUsers) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2E7D32))
                        }
                    } else {
                        Text(
                            text = "All Registered Students",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        userList.forEachIndexed { index, user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (user.role == "admin")
                                        Color(0xFFE8F5E9) else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank circle
                                    Card(
                                        shape = RoundedCornerShape(50),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF2E7D32)
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (user.role == "admin") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "ADMIN",
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .background(
                                                            Color(0xFF1B5E20),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(
                                                            horizontal = 4.dp,
                                                            vertical = 2.dp
                                                        )
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${user.rollNo} • ${user.department}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "${user.totalActivities} logs • %.1f kg CO₂".format(user.totalCarbon),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Text(
                                        text = "${user.totalPoints}\npts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2E7D32),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // ── LOGS TAB ──
                2 -> {
                    if (isLoadingActivities) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2E7D32))
                        }
                    } else {
                        val flaggedActivities = activityList.filter { it.flagged }
                        val normalActivities = activityList.filter { !it.flagged }

                        // Flagged section
                        if (flaggedActivities.isNotEmpty()) {
                            Text(
                                text = "⚠️ Suspicious Entries (${flaggedActivities.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            flaggedActivities.forEach { activity ->
                                AdminActivityCard(activity = activity, db = db) {
                                    activityList = activityList.filter { it.id != activity.id }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Normal section
                        Text(
                            text = "✅ Recent Logs (${normalActivities.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        normalActivities.forEach { activity ->
                            AdminActivityCard(activity = activity, db = db) {
                                activityList = activityList.filter { it.id != activity.id }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdminActivityCard(
    activity: ActivityReport,
    db: FirebaseFirestore,
    onDeleted: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Activity?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C)
                )
            },
            text = {
                Text(
                    "This will permanently delete this activity log. " +
                            "The user's points will NOT be automatically adjusted.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("activities").document(activity.id)
                            .delete()
                            .addOnSuccessListener {
                                showDeleteDialog = false
                                onDeleted()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.flagged) Color(0xFFFFEBEE) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activity.flagged) {
                            Text(text = "⚠️ ", fontSize = 14.sp)
                        }
                        Text(
                            text = activity.userName.ifEmpty { "Unknown User" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (activity.flagged) Color(0xFFB71C1C) else Color.Black
                        )
                    }
                    Text(
                        text = "${activity.rollNo} • ${activity.date}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "🚗 ${activity.travel.ifEmpty{"—"}} • 🍽️ ${activity.food.ifEmpty{"—"}} • 💡 ${activity.electricityHours}h",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f kg".format(activity.carbonKg),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (activity.flagged) Color(0xFFB71C1C)
                        else Color(0xFF2E7D32)
                    )
                    Text(
                        text = "+${activity.pointsEarned} pts",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🗑️ Delete",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showDeleteDialog = true }
                    )
                }
            }
        }
    }
}