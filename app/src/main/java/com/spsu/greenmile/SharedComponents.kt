package com.spsu.greenmile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

// ── Stat Card ──────────────────────────────────────────────
@Composable
fun StatCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    isDark: Boolean = false
) {
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textClr
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = subClr,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Week Bar (single row bar) ───────────────────────────────
@Composable
fun WeekBar(day: String, value: Float, co2: String, isDark: Boolean = false) {
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray
    val textClr = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val track   = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE8F5E9)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day,
            modifier = Modifier.width(40.dp),
            fontSize = 12.sp,
            color = subClr
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = Color(0xFF66BB6A),
            trackColor = track
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = co2,
            fontSize = 12.sp,
            color = textClr,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Weekly Chart Card (fetches last 7 days from Firestore) ──
@Composable
fun WeeklyChartCard(userId: String, isDark: Boolean = false) {
    data class DayData(val day: String, val carbon: Double)

    var weekData by remember { mutableStateOf<List<DayData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseFirestore.getInstance()
    
    val cardBg  = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textClr = if (isDark) Color.White       else Color(0xFF1B5E20)
    val subClr  = if (isDark) Color(0xFFAAAAAA) else Color.Gray
    val mainClr = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            // Get last 7 days date strings
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()

            val last7Days = (6 downTo 0).map { daysAgo ->
                val cal = calendar.clone() as java.util.Calendar
                cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                Pair(
                    dateFormat.format(cal.time),   // "2026-03-01"
                    dayFormat.format(cal.time)      // "Sun"
                )
            }

            db.collection("activities")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    // Build a map of date → total carbon
                    val carbonByDate = mutableMapOf<String, Double>()
                    result.documents.forEach { doc ->
                        val date = doc.getString("date") ?: return@forEach
                        val carbon = doc.getDouble("carbonKg") ?: 0.0
                        carbonByDate[date] = (carbonByDate[date] ?: 0.0) + carbon
                    }

                    weekData = last7Days.map { (dateStr, dayLabel) ->
                        DayData(dayLabel, carbonByDate[dateStr] ?: 0.0)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    // Show empty chart on failure
                    weekData = last7Days.map { (_, dayLabel) -> DayData(dayLabel, 0.0) }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Last 7 Days",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textClr
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily carbon footprint (kg CO₂)",
                fontSize = 12.sp,
                color = subClr
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                val maxCarbon = weekData.maxOfOrNull { it.carbon }?.takeIf { it > 0 } ?: 1.0

                weekData.forEach { dayData ->
                    val barProgress = (dayData.carbon / maxCarbon).toFloat().coerceIn(0f, 1f)
                    WeekBar(
                        day = dayData.day,
                        value = barProgress,
                        co2 = if (dayData.carbon > 0) "%.1f kg".format(dayData.carbon) else "—",
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekly total
                val weekTotal = weekData.sumOf { it.carbon }
                if (weekTotal > 0) {
                    HorizontalDivider(color = if (isDark) Color(0xFF333333) else Color(0xFFE8F5E9))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Weekly Total",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = subClr
                        )
                        Text(
                            text = "%.1f kg CO₂".format(weekTotal),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainClr
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities logged this week yet 🌱",
                            fontSize = 12.sp,
                            color = subClr,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}