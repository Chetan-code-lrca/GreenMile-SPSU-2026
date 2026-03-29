package com.spsu.greenmile

import android.content.Context
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
fun StepCounterScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context    = LocalContext.current
    var stepsToday by remember { mutableStateOf(0) }
    var permissionGranted by remember { mutableStateOf(false) }
    val stepGoal = 10000

    // ── Permission launcher ──
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    // ── Request permission on first open ──
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            permissionGranted = true
        }
    }

    // ── Read sensor directly — no service needed ──
    DisposableEffect(permissionGranted) {
        if (!permissionGranted) return@DisposableEffect onDispose { }

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

                if (lastDate != today) {
                    // ── New day — reset offset ──
                    prefs.edit()
                        .putFloat(StepCounterService.KEY_STEPS_OFFSET, sensorTotal)
                        .putString(StepCounterService.KEY_LAST_DATE, today)
                        .putInt(StepCounterService.KEY_STEPS_TODAY, 0)
                        .putBoolean(StepCounterService.KEY_OFFSET_SET, true)
                        .apply()
                    stepsToday = 0
                    return
                }

                if (!offsetSet) {
                    // ── First reading today — lock offset ──
                    prefs.edit()
                        .putFloat(StepCounterService.KEY_STEPS_OFFSET, sensorTotal)
                        .putString(StepCounterService.KEY_LAST_DATE, today)
                        .putInt(StepCounterService.KEY_STEPS_TODAY, 0)
                        .putBoolean(StepCounterService.KEY_OFFSET_SET, true)
                        .apply()
                    stepsToday = 0
                    return
                }

                // ── Normal: subtract offset from total ──
                val offset = prefs.getFloat(StepCounterService.KEY_STEPS_OFFSET, sensorTotal)
                val steps  = (sensorTotal - offset).toInt().coerceAtLeast(0)

                // Save to prefs
                prefs.edit().putInt(StepCounterService.KEY_STEPS_TODAY, steps).apply()
                stepsToday = steps
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Load saved value immediately
        stepsToday = StepCounterService.getStepsToday(context)

        onDispose {
            sm.unregisterListener(listener)
        }
    }

    val km       = stepsToday * 0.00075
    val kcal     = stepsToday * 0.03
    val progress = (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 800, easing = EaseOut),
        label         = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B2E3C))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text(
                    text     = "← Back",
                    color    = Color(0xFF69F0AE),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(bottom = 8.dp)
                )
                Text(
                    text       = "👟 Step Counter",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text     = "Today's walking activity",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Permission warning ──
            if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF4A148C))
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text       = "🔒 Permission Required",
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text     = "Please grant Activity Recognition permission to count your steps.",
                            color    = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    android.Manifest.permission.ACTIVITY_RECOGNITION
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B1FA2)
                            )
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Circular Progress ──
            Box(
                modifier        = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke   = 28.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val tl       = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val sz = Size(diameter, diameter)

                    // Track ring
                    drawArc(
                        color      = Color.White.copy(alpha = 0.08f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Progress ring
                    if (animatedProgress > 0f) {
                        drawArc(
                            color      = Color(0xFF69F0AE),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter  = false,
                            topLeft    = tl,
                            size       = sz,
                            style      = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "%,d".format(stepsToday),
                        fontSize   = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        text     = "steps",
                        fontSize = 16.sp,
                        color    = Color(0xFF69F0AE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text     = "${(progress * 100).toInt()}% of $stepGoal daily goal",
                color    = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ── Distance + Calories ──
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF1B2E3C))
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📍", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text       = "%.2f".format(km),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(text = "km",       fontSize = 12.sp, color = Color(0xFF69F0AE))
                        Text(text = "Distance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF1B2E3C))
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔥", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text       = "%.0f".format(kcal),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(text = "kcal",     fontSize = 12.sp, color = Color(0xFFFF7043))
                        Text(text = "Calories", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF1B2E3C))
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🕛", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text     = "Keep this screen open to count steps in real time. Resets at midnight.",
                        color    = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepStatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
    }
}