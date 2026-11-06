package com.example.prscanner.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.ImageEnhancementEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun CropEditorScreen(
    documentId: Long,
    pageId: Long,
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToEnhance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableIntStateOf(0) }

    // Normalized crop rectangle [0f .. 1f]
    var cropLeft by remember { mutableFloatStateOf(0.06f) }
    var cropTop by remember { mutableFloatStateOf(0.06f) }
    var cropRight by remember { mutableFloatStateOf(0.94f) }
    var cropBottom by remember { mutableFloatStateOf(0.94f) }

    var selectedPreset by remember { mutableStateOf("Free") }

    LaunchedEffect(pageId) {
        withContext(Dispatchers.IO) {
            val db = com.example.prscanner.data.db.PrScannerDatabase.getDatabase(context)
            val page = db.documentPageDao().getPageById(pageId)
            if (page != null) {
                val f = File(page.originalFilePath.ifBlank { page.imageFilePath })
                if (f.exists()) {
                    loadedBitmap = BitmapFactory.decodeFile(f.absolutePath)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Crop & Perspective",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = {
                    rotationAngle = (rotationAngle + 90) % 360
                    loadedBitmap?.let { bmp ->
                        val matrix = Matrix().apply { postRotate(90f) }
                        loadedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White)
            }
        }

        // Interactive Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Drag boundaries smoothly
                        val dx = dragAmount.x / size.width
                        val dy = dragAmount.y / size.height

                        // Move handles based on quadrant
                        val touchX = change.position.x / size.width
                        val touchY = change.position.y / size.height

                        if (touchX < 0.5f) {
                            cropLeft = (cropLeft + dx).coerceIn(0.01f, cropRight - 0.1f)
                        } else {
                            cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 0.99f)
                        }

                        if (touchY < 0.5f) {
                            cropTop = (cropTop + dy).coerceIn(0.01f, cropBottom - 0.1f)
                        } else {
                            cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 0.99f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = loadedBitmap
            if (bmp != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    val scale = minOf(canvasW / bmp.width.toFloat(), canvasH / bmp.height.toFloat())
                    val drawnW = bmp.width * scale
                    val drawnH = bmp.height * scale
                    val offsetX = (canvasW - drawnW) / 2f
                    val offsetY = (canvasH - drawnH) / 2f

                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                        dstSize = IntSize(drawnW.toInt(), drawnH.toInt())
                    )

                    // Draw Crop boundary box
                    val rectL = offsetX + cropLeft * drawnW
                    val rectT = offsetY + cropTop * drawnH
                    val rectR = offsetX + cropRight * drawnW
                    val rectB = offsetY + cropBottom * drawnH

                    // Dim outside areas
                    drawRect(color = Color.Black.copy(alpha = 0.45f), size = size)

                    // Clear inside crop rect
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(rectL, rectT),
                        size = androidx.compose.ui.geometry.Size(rectR - rectL, rectB - rectT),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )

                    // Crop border lines
                    drawRect(
                        color = Color(0xFF06B6D4),
                        topLeft = Offset(rectL, rectT),
                        size = androidx.compose.ui.geometry.Size(rectR - rectL, rectB - rectT),
                        style = Stroke(width = 4f)
                    )

                    // 4 Corner handles
                    val handleRadius = 24f
                    val corners = listOf(
                        Offset(rectL, rectT),
                        Offset(rectR, rectT),
                        Offset(rectR, rectB),
                        Offset(rectL, rectB)
                    )
                    drawPoints(
                        points = corners,
                        pointMode = PointMode.Points,
                        color = Color.White,
                        strokeWidth = handleRadius * 2,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            } else {
                Text("Loading document page...", color = Color.LightGray)
            }
        }

        // Aspect Ratio Preset Bar
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Auto Detect", "Free", "A4", "US Letter", "ID Card", "Reset").forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                            modifier = Modifier.clickable {
                                selectedPreset = preset
                                when (preset) {
                                    "Auto Detect" -> {
                                        loadedBitmap?.let { bmp ->
                                            val bounds = ImageEnhancementEngine.detectDocumentBounds(bmp)
                                            cropLeft = bounds.left
                                            cropTop = bounds.top
                                            cropRight = bounds.right
                                            cropBottom = bounds.bottom
                                        }
                                    }
                                    "A4" -> {
                                        cropLeft = 0.1f; cropRight = 0.9f
                                        cropTop = 0.05f; cropBottom = 0.95f
                                    }
                                    "ID Card" -> {
                                        cropLeft = 0.08f; cropRight = 0.92f
                                        cropTop = 0.28f; cropBottom = 0.72f
                                    }
                                    "Reset" -> {
                                        cropLeft = 0.04f; cropRight = 0.96f
                                        cropTop = 0.04f; cropBottom = 0.96f
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = preset,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val bmp = loadedBitmap
                        if (bmp != null) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val cropped = ImageEnhancementEngine.cropBitmap(
                                        bmp,
                                        RectF(cropLeft, cropTop, cropRight, cropBottom)
                                    )
                                    val db = com.example.prscanner.data.db.PrScannerDatabase.getDatabase(context)
                                    val page = db.documentPageDao().getPageById(pageId)
                                    if (page != null) {
                                        val croppedFile = File(context.filesDir, "docs/crop_${page.id}_${UUID.randomUUID()}.jpg")
                                        FileOutputStream(croppedFile).use { out ->
                                            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                        }
                                        db.documentPageDao().updatePage(
                                            page.copy(
                                                imageFilePath = croppedFile.absolutePath,
                                                originalFilePath = croppedFile.absolutePath
                                            )
                                        )
                                    }
                                }
                                onNavigateToEnhance()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("apply_crop_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Next: Filters", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
