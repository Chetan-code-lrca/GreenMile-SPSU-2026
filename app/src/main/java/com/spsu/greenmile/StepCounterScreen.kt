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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepCounterScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var stepsToday by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf(false) }
    var hasSensor by remember { mutableStateOf(false) }
    val stepGoal = 10000

    fun startStepService() {
        try {
            val intent = Intent(context, StepCounterService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            // fails silently — live sensor still works on screen
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) startStepService()
    }

    LaunchedEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hasSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            hasPermission = true
            startStepService()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
                val totalSteps = event.values[0]
                val prefs = context.getSharedPreferences(
                    StepCounterService.PREFS_NAME, Context.MODE_PRIVATE
                )
                val offset = prefs.getFloat(StepCounterService.KEY_STEPS_OFFSET, totalSteps)
                stepsToday = (totalSteps - offset).toInt().coerceAtLeast(0)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepsToday = StepCounterService.getStepsToday(context)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    val km = stepsToday * 0.00075
    val kcal = stepsToday * 0.03
    val progress = (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOut),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16213E))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "← Back",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "👟 Step Counter",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Today's activity",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                !hasSensor -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "😔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Step sensor not available",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Your device doesn't have a built-in step counter sensor.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                !hasPermission -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔒", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Permission Required",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Please allow Activity Recognition to count steps.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        permissionLauncher.launch(
                                            android.Manifest.permission.ACTIVITY_RECOGNITION
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 28.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2f,
                                (size.height - diameter) / 2f
                            )
                            val arcSize = Size(diameter, diameter)
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,d".format(stepsToday),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "steps",
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% of $stepGoal goal",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepStatItem(emoji = "📍", value = "%.2f km".format(km), label = "Distance")
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        StepStatItem(emoji = "🔥", value = "%.0f kcal".format(kcal), label = "Calories")
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✅", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Counting in background",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Steps tracked even when app is closed • No internet needed",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🕛", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Resets automatically every midnight",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepStatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}