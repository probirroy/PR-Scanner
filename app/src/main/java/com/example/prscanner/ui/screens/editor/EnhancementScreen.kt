package com.example.prscanner.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.prscanner.data.model.FilterType
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.ImageEnhancementEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EnhancementScreen(
    documentId: Long,
    pageId: Long,
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedFilter by remember { mutableStateOf(FilterType.MAGIC_COLOR) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pageId) {
        withContext(Dispatchers.IO) {
            val db = com.example.prscanner.data.db.PrScannerDatabase.getDatabase(context)
            val page = db.documentPageDao().getPageById(pageId)
            if (page != null) {
                val f = File(page.originalFilePath.ifBlank { page.imageFilePath })
                if (f.exists()) {
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    baseBitmap = bmp
                    if (bmp != null) {
                        previewBitmap = ImageEnhancementEngine.applyAdjustmentsAndFilter(
                            source = bmp,
                            filter = selectedFilter,
                            brightness = brightness,
                            contrast = contrast
                        )
                    }
                }
            }
        }
    }

    fun updatePreview() {
        val base = baseBitmap ?: return
        previewBitmap = ImageEnhancementEngine.applyAdjustmentsAndFilter(
            source = base,
            filter = selectedFilter,
            brightness = brightness,
            contrast = contrast
        )
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
                text = "Document Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = {
                    scope.launch {
                        documentRepository.updatePageFilter(
                            pageId = pageId,
                            filter = selectedFilter,
                            brightness = brightness,
                            contrast = contrast,
                            rotationDegrees = 0
                        )
                        onFinish()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("save_filter_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
            }
        }

        // Live Preview Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = previewBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Enhanced Page Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Rendering enhancement...", color = Color.LightGray)
            }
        }

        // Filters Bottom Controls Sheet
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
                // Filter selection horizontal chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FilterType.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                            modifier = Modifier
                                .clickable {
                                    selectedFilter = filter
                                    updatePreview()
                                }
                                .testTag("filter_chip_${filter.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (filter == FilterType.MAGIC_COLOR || filter == FilterType.AUTO_ENHANCE) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF06B6D4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = filter.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brightness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brightness",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.width(70.dp)
                    )
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            updatePreview()
                        },
                        valueRange = -60f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Contrast Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contrast",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.width(70.dp)
                    )
                    Slider(
                        value = contrast,
                        onValueChange = {
                            contrast = it
                            updatePreview()
                        },
                        valueRange = 0.6f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            documentRepository.updatePageFilter(
                                pageId = pageId,
                                filter = selectedFilter,
                                brightness = brightness,
                                contrast = contrast,
                                rotationDegrees = 0
                            )
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_filter_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Page Enhancement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
