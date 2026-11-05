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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.ScrollshotEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScrollshotScreen(
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val segments = remember {
        mutableStateListOf<Bitmap>().apply {
            addAll(generateSampleScrollSegments())
        }
    }

    var overlapPx by remember { mutableFloatStateOf(40f) }
    var stitchedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isStitching by remember { mutableStateOf(false) }

    // Multi-photo picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) segments.add(bmp)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Re-stitch when segments or overlap change
    LaunchedEffect(segments.size, overlapPx) {
        if (segments.isNotEmpty()) {
            isStitching = true
            withContext(Dispatchers.Default) {
                val result = ScrollshotEngine.stitchVerticalScreenshots(
                    segments.toList(),
                    overlapPx.toInt()
                )
                stitchedBitmap = result
                isStitching = false
            }
        } else {
            stitchedBitmap = null
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
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("scrollshot_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scrollshot Stitcher",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Capture full-page scrolling screenshots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${segments.size} Shots",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Segments Thumbnail Strip & Import Buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Screenshot Segments (Top to Bottom):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { photoPicker.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Photos")
                        }
                        TextButton(onClick = {
                            segments.clear()
                            segments.addAll(generateSampleScrollSegments())
                        }) {
                            Text("Reset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(segments) { index, segment ->
                        Box(
                            modifier = Modifier
                                .size(56.dp, 80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = segment.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Index badge
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { if (segments.size > 1) segments.removeAt(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Overlap Adjustment Slider
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Seam Overlap: ${overlapPx.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = overlapPx,
                    onValueChange = { overlapPx = it },
                    valueRange = 0f..150f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Full Long Stitched Preview Canvas (Scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = stitchedBitmap
            if (isStitching) {
                CircularProgressIndicator(color = Color.White)
            } else if (bmp != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Stitched long screenshot",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            } else {
                Text("Add screenshot segments above to stitch", color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Bottom Actions (Save as Document / Save as Long PDF)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val bmp = stitchedBitmap
                        if (bmp != null) {
                            scope.launch {
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Scrollshot (${segments.size} parts)",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.GENERAL
                                )
                                Toast.makeText(context, "Long screenshot saved to documents!", Toast.LENGTH_SHORT).show()
                                onNavigateToDocument(id)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_scrollshot_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Long Image")
                }

                Button(
                    onClick = {
                        val bmp = stitchedBitmap
                        if (bmp != null) {
                            scope.launch {
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Scrollshot Long PDF",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.GENERAL
                                )
                                val pdfFile = documentRepository.exportToPdfFile(id)
                                if (pdfFile != null) {
                                    Toast.makeText(context, "Long PDF generated successfully!", Toast.LENGTH_SHORT).show()
                                    onNavigateToDocument(id)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Long PDF")
                }
            }
        }
    }
}

/**
 * Generates 3 synthetic sequential web/article screenshot segments with slight overlap
 * for instant testing of vertical stitching.
 */
private fun generateSampleScrollSegments(): List<Bitmap> {
    val w = 500
    val h = 400

    val segments = mutableListOf<Bitmap>()
    val colors = listOf(
        AndroidColor.rgb(255, 255, 255),
        AndroidColor.rgb(248, 250, 252),
        AndroidColor.rgb(241, 245, 249)
    )

    val titles = listOf(
        "PR Scanner Article Part 1: Architecture",
        "Part 2: Advanced OCR & Neural Detection",
        "Part 3: Enterprise PDF Management & Security"
    )

    for (i in 0..2) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(colors[i])

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(30, 58, 138)
            textSize = 20f
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(71, 85, 105)
            textSize = 14f
        }
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(37, 99, 235)
        }

        // Top bar
        canvas.drawRect(0f, 0f, w.toFloat(), 12f, barPaint)

        canvas.drawText(titles[i], 24f, 50f, headerPaint)

        for (line in 0..7) {
            val y = 90f + line * 34f
            val widthLine = (w - 80f - (line * 15 % 70))
            canvas.drawRect(24f, y, 24f + widthLine, y + 14f, Paint().apply {
                color = AndroidColor.rgb(226, 232, 240)
            })
        }

        // Section badge
        canvas.drawText("Section ${i + 1} of 3  •  Full Page Scrollshot", 24f, 380f, textPaint)

        segments.add(bmp)
    }

    return segments
}
