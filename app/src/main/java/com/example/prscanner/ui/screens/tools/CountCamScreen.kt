package com.example.prscanner.ui.screens.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.CountPin
import com.example.prscanner.engine.ObjectCounterEngine
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CountCamScreen(
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Working image bitmap
    var currentBitmap by remember {
        mutableStateOf<Bitmap?>(generateSampleInventoryBitmap())
    }

    val pins = remember { mutableStateListOf<CountPin>() }
    var autoSensitivity by remember { mutableFloatStateOf(0.6f) }
    var selectedPinColor by remember { mutableStateOf(Color(0xFF2563EB)) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        currentBitmap = bmp
                        pins.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("countcam_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CountCam",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Count objects with your camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Total Tally Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ${pins.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Action Ribbon (Import, Sample, Clear, Undo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        currentBitmap = generateSampleInventoryBitmap()
                        pins.clear()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (pins.isNotEmpty()) pins.removeAt(pins.lastIndex) },
                    enabled = pins.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Pin")
                }

                IconButton(
                    onClick = { pins.clear() },
                    enabled = pins.isNotEmpty()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                }
            }
        }

        // Interactive Counting Canvas with Pinned Markers
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            val bmp = currentBitmap
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()

            if (bmp != null) {
                // Rendered pinned bitmap or base bitmap with pins overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bmp) {
                            detectTapGestures { tapOffset ->
                                val relX = (tapOffset.x / canvasW).coerceIn(0f, 1f)
                                val relY = (tapOffset.y / canvasH).coerceIn(0f, 1f)
                                val nextId = pins.size + 1
                                val colorInt = (selectedPinColor.value shr 32).toInt() or 0xFF000000.toInt()
                                pins.add(
                                    CountPin(
                                        id = nextId,
                                        relX = relX,
                                        relY = relY,
                                        label = "Object #$nextId",
                                        colorArgb = colorInt
                                    )
                                )
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Count source",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // Draw pins overlays
                    pins.forEach { pin ->
                        val pxX = pin.relX * canvasW
                        val pxY = pin.relY * canvasH

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .offsetPx(pxX - 14f, pxY - 14f)
                                .clip(CircleShape)
                                .background(selectedPinColor)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pin.id}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Instruction overlay pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Tap any object to count & tag with a numbered pin",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Bottom Controls: Auto-Detect, Sensitivity, Export
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val bmp = currentBitmap
                            if (bmp != null) {
                                val detected = ObjectCounterEngine.autoDetectObjects(bmp, autoSensitivity)
                                pins.clear()
                                pins.addAll(detected)
                                Toast.makeText(context, "Auto-detected ${detected.size} objects!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("auto_detect_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto Count Objects", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    // Save as Document
                    Button(
                        onClick = {
                            val bmp = currentBitmap
                            if (bmp != null) {
                                scope.launch {
                                    val markedBitmap = ObjectCounterEngine.renderPinnedBitmap(bmp, pins.toList())
                                    val docId = documentRepository.createDocumentWithPages(
                                        title = "CountCam Report (${pins.size} items)",
                                        pageBitmaps = listOf(markedBitmap),
                                        docType = DocType.GENERAL
                                    )
                                    Toast.makeText(context, "Count report saved!", Toast.LENGTH_SHORT).show()
                                    onNavigateToDocument(docId)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_count_report_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Report")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sensitivity slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Detection Sensitivity: ${(autoSensitivity * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = autoSensitivity,
                    onValueChange = { autoSensitivity = it },
                    valueRange = 0.2f..0.95f
                )
            }
        }
    }
}

private fun Modifier.offsetPx(x: Float, y: Float) = this.then(
    Modifier.padding(start = (x / 2.7f).dp.coerceAtLeast(0.dp), top = (y / 2.7f).dp.coerceAtLeast(0.dp))
)

/**
 * Generates a clean synthetic item inventory bitmap for instant emulator testing
 * (e.g. circles representing pills, coins, or screws).
 */
private fun generateSampleInventoryBitmap(): Bitmap {
    val w = 600
    val h = 600
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.rgb(240, 244, 248))

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(220, 226, 235)
        strokeWidth = 2f
    }
    for (i in 0..w step 60) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), h.toFloat(), gridPaint)
        canvas.drawLine(0f, i.toFloat(), w.toFloat(), i.toFloat(), gridPaint)
    }

    val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(40, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // Draw 12 distinct circular objects
    val itemPositions = listOf(
        Pair(120f, 130f), Pair(260f, 110f), Pair(420f, 140f),
        Pair(160f, 260f), Pair(330f, 280f), Pair(480f, 290f),
        Pair(110f, 410f), Pair(250f, 430f), Pair(440f, 420f),
        Pair(190f, 520f), Pair(360f, 510f), Pair(510f, 530f)
    )

    val colors = listOf(
        AndroidColor.rgb(37, 99, 235),
        AndroidColor.rgb(16, 185, 129),
        AndroidColor.rgb(245, 158, 11),
        AndroidColor.rgb(239, 68, 68),
        AndroidColor.rgb(139, 92, 246)
    )

    itemPositions.forEachIndexed { idx, (x, y) ->
        canvas.drawCircle(x + 4f, y + 5f, 32f, shadowPaint)
        itemPaint.color = colors[idx % colors.size]
        canvas.drawCircle(x, y, 32f, itemPaint)

        // Specular highlight
        val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(120, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x - 8f, y - 8f, 10f, hlPaint)
    }

    return bmp
}
