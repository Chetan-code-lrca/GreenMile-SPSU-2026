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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                color = Color(0xFF2E7D32)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun WeekBar(day: String, value: Float, co2: String) {
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
            color = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = Color(0xFF66BB6A),
            trackColor = Color(0xFFE8F5E9)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = co2,
            fontSize = 12.sp,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Medium
        )
    }
}