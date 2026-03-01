package com.spsu.greenmile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun LogActivityScreen(onBack: () -> Unit, onSubmit: (Double) -> Unit) {

    var selectedTravel by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf("") }
    var electricityHours by remember { mutableStateOf("") }
    var usedPlastic by remember { mutableStateOf(false) }

    fun calculateCarbon(): Double {
        var total = 0.0
        total += when (selectedTravel) {
            "Car" -> 3.0
            "Bike" -> 1.5
            "Bus" -> 0.8
            "Cycle" -> 0.0
            "Walk" -> 0.0
            else -> 1.0
        }
        total += when (selectedFood) {
            "Non-Veg" -> 3.5
            "Veg" -> 1.0
            "Junk Food" -> 2.0
            else -> 1.5
        }
        val hours = electricityHours.toDoubleOrNull() ?: 0.0
        total += hours * 0.5
        if (usedPlastic) total += 0.3
        return total
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "← Back",
            color = Color(0xFF2E7D32),
            modifier = Modifier.clickable { onBack() },
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Log Today's Activity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "Track your choices to calculate carbon footprint",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // TRAVEL
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🚗", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "How did you travel today?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val travelOptions = listOf("🚗 Car", "🏍️ Bike", "🚌 Bus", "🚲 Cycle", "🚶 Walk")
        val travelRows = travelOptions.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            travelRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        val label = option.substringAfter(" ")
                        val isSelected = selectedTravel == label
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF2E7D32) else Color.White,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTravel = label }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // FOOD
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🍽️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "What did you eat today?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val foodOptions = listOf("🥩 Non-Veg", "🥗 Veg", "🍔 Junk Food")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            foodOptions.forEach { option ->
                val label = option.substringAfter(" ")
                val isSelected = selectedFood == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color(0xFF2E7D32) else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFood = label }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ELECTRICITY
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "💡", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Electricity usage (hours)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = electricityHours,
            onValueChange = { electricityHours = it },
            label = { Text("Hours of AC/heavy appliance use") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PLASTIC
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "♻️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Plastic usage",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B5E20)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (usedPlastic) Color(0xFF2E7D32) else Color.White,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (usedPlastic) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { usedPlastic = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Used Plastic ❌",
                    fontSize = 13.sp,
                    color = if (usedPlastic) Color.White else Color.DarkGray,
                    fontWeight = if (usedPlastic) FontWeight.Bold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (!usedPlastic) Color(0xFF2E7D32) else Color.White,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (!usedPlastic) Color(0xFF2E7D32) else Color(0xFFCCCCCC),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { usedPlastic = false }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reusable ✅",
                    fontSize = 13.sp,
                    color = if (!usedPlastic) Color.White else Color.DarkGray,
                    fontWeight = if (!usedPlastic) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Carbon Preview
        val carbonPreview = calculateCarbon()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (carbonPreview < 4.0) Color(0xFF2E7D32) else Color(0xFFB71C1C)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Estimated Carbon Footprint",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Text(
                    text = "%.1f kg CO₂".format(carbonPreview),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (carbonPreview < 4.0) "🌱 Great effort today!" else "⚠️ Try greener choices!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedTravel.isNotEmpty() && selectedFood.isNotEmpty()) {
                    onSubmit(carbonPreview)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
        ) {
            Text(
                text = "✅  Save Activity Log",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}