package com.spsu.greenmile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    // Splash just shows UI — navigation is handled by MainActivity LaunchedEffect
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🌍", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GreenMile",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Campus Sustainability Platform",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "SPSU • Udaipur",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}