package com.spsu.greenmile

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepCounterScreen(onBack: () -> Unit, isDark: Boolean = false) {
    BackHandler { onBack() }

    val context    = LocalContext.current
    var stepsToday by remember { mutableStateOf(0) }
    val stepGoal   = 10000

    val bg      = if (isDark) Color(0xFF0D1B2A) else Color(0xFF0D1B2A)
    val cardBg  = if (isDark) Color(0xFF1B2E3C) else Color(0xFF1B2E3C)
    val accent  = Color(0xFF69F0AE)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startStepService(context)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            startStepService(context)
        }
        stepsToday = StepCounterService.getStepsToday(context)
    }

    DisposableEffect(Unit) {
        val sm     = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
                val sensorTotal = event.values[0]
                val prefs       = context.getSharedPreferences(
                    StepCounterService.PREFS_NAME, Context.MODE_PRIVATE
                )
                val today     = StepCounterService.todayString()
                val lastDate  = prefs.getString(StepCounterService.KEY_LAST_DATE, "")
                val offsetSet = prefs.getBoolean(StepCounterService.KEY_OFFSET_SET, false)

                if (lastDate == today && offsetSet) {
                    val offset = prefs.getFloat(StepCounterService.KEY_STEPS_OFFSET, sensorTotal)
                    stepsToday = (sensorTotal - offset).toInt().coerceAtLeast(0)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        stepsToday = StepCounterService.getStepsToday(context)

        onDispose { sm.unregisterListener(listener) }
    }

    val km       = stepsToday * 0.00075
    val kcal     = stepsToday * 0.03
    val progress = (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f)
    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(800, easing = EaseOut),
        label         = "arc"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("← Back", color = accent, fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() }.padding(bottom = 8.dp))
                Text("👟 Step Counter", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Today's walking activity", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Circular Arc
            Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke   = 28.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val tl       = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val sz       = Size(diameter, diameter)
                    drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, tl, sz,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                    if (animProg > 0f) {
                        drawArc(accent, -90f, 360f * animProg, false, tl, sz,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%,d".format(stepsToday), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("steps", fontSize = 16.sp, color = accent)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("${(progress * 100).toInt()}% of $stepGoal daily goal",
                color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(28.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("%.2f".format(km), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("km", fontSize = 12.sp, color = accent)
                        Text("Distance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("%.0f".format(kcal), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("kcal", fontSize = 12.sp, color = Color(0xFFFF7043))
                        Text("Calories", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Counting in background", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Works even when app is closed  •  No internet needed",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🕛", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Resets automatically every midnight",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun startStepService(context: Context) {
    try {
        val intent = Intent(context, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } catch (e: Exception) { /* silent fail */ }
}

@Composable
fun StepStatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
    }
}