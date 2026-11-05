package com.example.prscanner.ui.screens.tools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.tan

@Composable
fun MeasureScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Screen Caliper Ruler, 1: Camera Distance Estimator
    var selectedUnit by remember { mutableStateOf("cm") } // cm, in, mm

    // Sensor state for tilt
    var pitchAngleDeg by remember { mutableFloatStateOf(45f) }
    var userHeightM by remember { mutableFloatStateOf(1.5f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val y = event.values[1]
                    val z = event.values[2]
                    val pitchRad = atan2(y.toDouble(), z.toDouble())
                    val deg = Math.toDegrees(pitchRad).toFloat()
                    pitchAngleDeg = abs(deg).coerceIn(5f, 85f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("measure_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Measure",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Measure distances with your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Unit Chips
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        listOf("cm", "mm", "in").forEach { unit ->
                            val isSel = selectedUnit == unit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedUnit = unit }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = unit,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabs (Ruler vs Camera AR)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Digital Caliper Ruler") },
                icon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Camera Distance AR") },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTab == 0) {
            // Screen Caliper Ruler
            ScreenRulerView(unit = selectedUnit)
        } else {
            // Camera AR Distance Estimator
            CameraDistanceView(
                pitchAngleDeg = pitchAngleDeg,
                userHeightM = userHeightM,
                onHeightChange = { userHeightM = it },
                unit = selectedUnit
            )
        }
    }
}

@Composable
fun ScreenRulerView(unit: String) {
    val density = LocalDensity.current
    var topCaliperY by remember { mutableFloatStateOf(120f) }
    var bottomCaliperY by remember { mutableFloatStateOf(420f) }

    val rawDiffPx = abs(bottomCaliperY - topCaliperY)
    val diffDp = with(density) { rawDiffPx.toDp() }

    // Conversion factor based on ~160 dpi baseline
    // 1 inch = 160 dp approx
    val lengthCm = (diffDp.value / 160f) * 2.54f
    val lengthMm = lengthCm * 10f
    val lengthInches = diffDp.value / 160f

    val formattedReading = when (unit) {
        "mm" -> "%.1f mm".format(lengthMm)
        "in" -> "%.2f in".format(lengthInches)
        else -> "%.2f cm".format(lengthCm)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHeightPx = constraints.maxHeight.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Measurement Readout Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Measured Distance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = formattedReading,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Drag calipers to object",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Ruler Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                // Ruler tick marks canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw vertical ruler scale along left side
                    val pxPerCm = (160f / 2.54f) * (density.density)
                    val pxPerMm = pxPerCm / 10f

                    var y = 0f
                    var mmIndex = 0
                    while (y < h) {
                        val isCm = mmIndex % 10 == 0
                        val isHalfCm = mmIndex % 5 == 0
                        val tickLen = if (isCm) 48f else if (isHalfCm) 32f else 18f

                        drawLine(
                            color = if (isCm) Color.DarkGray else Color.LightGray,
                            start = Offset(0f, y),
                            end = Offset(tickLen, y),
                            strokeWidth = if (isCm) 2.5f else 1.2f
                        )

                        // Draw right side tick marks too
                        drawLine(
                            color = if (isCm) Color.DarkGray else Color.LightGray,
                            start = Offset(w - tickLen, y),
                            end = Offset(w, y),
                            strokeWidth = if (isCm) 2.5f else 1.2f
                        )

                        y += pxPerMm
                        mmIndex++
                    }

                    // Draw connection dotted line between calipers
                    drawLine(
                        color = Color(0xFF2563EB),
                        start = Offset(w / 2f, topCaliperY),
                        end = Offset(w / 2f, bottomCaliperY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Top Caliper Handle
                CaliperBar(
                    y = topCaliperY,
                    label = "Start Pin",
                    color = Color(0xFF10B981),
                    onDrag = { dy ->
                        topCaliperY = (topCaliperY + dy).coerceIn(20f, bottomCaliperY - 15f)
                    }
                )

                // Bottom Caliper Handle
                CaliperBar(
                    y = bottomCaliperY,
                    label = "End Pin",
                    color = Color(0xFFEF4444),
                    onDrag = { dy ->
                        bottomCaliperY = (bottomCaliperY + dy).coerceIn(topCaliperY + 15f, maxHeightPx - 60f)
                    }
                )
            }
        }
    }
}

@Composable
fun CaliperBar(
    y: Float,
    label: String,
    color: Color,
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp)
            .offset(y = with(LocalDensity.current) { (y - 22f).toDp() })
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            }
    ) {
        // Horizontal line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(color)
                .align(Alignment.Center)
        )

        // Center Grip Handle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraDistanceView(
    pitchAngleDeg: Float,
    userHeightM: Float,
    onHeightChange: (Float) -> Unit,
    unit: String
) {
    // Distance formula: D = H * tan(pitch)
    val pitchRad = Math.toRadians(pitchAngleDeg.toDouble())
    val distanceM = (userHeightM * tan(pitchRad)).toFloat().coerceIn(0.1f, 50f)

    val distanceDisplay = when (unit) {
        "in" -> "%.1f in (%.1f ft)".format(distanceM * 39.37f, distanceM * 3.28f)
        "mm" -> "%.0f mm".format(distanceM * 1000f)
        else -> "%.2f m (%.1f cm)".format(distanceM, distanceM * 100f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simulated Camera Reticle Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            // Reticle overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Crosshair circles
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 60f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(color = Color(0xFF10B981), radius = 10f, center = Offset(cx, cy))

                // Crosshair lines
                drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(cx - 100f, cy), end = Offset(cx - 20f, cy), strokeWidth = 2f)
                drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(cx + 20f, cy), end = Offset(cx + 100f, cy), strokeWidth = 2f)
                drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(cx, cy - 100f), end = Offset(cx, cy - 20f), strokeWidth = 2f)
                drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(cx, cy + 20f), end = Offset(cx, cy + 100f), strokeWidth = 2f)
            }

            // Tilt Level Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phone Pitch: ${pitchAngleDeg.toInt()}°",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Target Guide Instruction
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Aim reticle at base of object on the floor",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Distance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated Distance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = distanceDisplay,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "AR Gyro Trig",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calibration: Device Height Slider
                Text(
                    text = "Device Height from Floor: %.2f m".format(userHeightM),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userHeightM,
                    onValueChange = onHeightChange,
                    valueRange = 0.5f..2.2f,
                    steps = 17
                )
            }
        }
    }
}

// Helper offset extension
private fun Modifier.offset(y: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.padding(top = y)
)
