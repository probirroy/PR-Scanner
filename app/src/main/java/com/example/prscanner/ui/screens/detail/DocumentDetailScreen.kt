package com.example.prscanner.ui.screens.detail

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.model.DocumentPageEntity
import com.example.prscanner.data.model.PageSizePreset
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SubscriptionRepository
import com.example.prscanner.engine.PdfEngine
import com.example.prscanner.engine.PdfExportOptions
import com.example.prscanner.ui.components.WatermarkDialog
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DocumentDetailScreen(
    documentId: Long,
    documentRepository: DocumentRepository,
    subscriptionRepository: SubscriptionRepository,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: (Long, Long) -> Unit,
    onNavigateToEnhance: (Long, Long) -> Unit,
    onNavigateToOcr: (Long) -> Unit,
    onNavigateToAi: (Long) -> Unit,
    onAddPageCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val document by documentRepository.observeDocument(documentId).collectAsState(initial = null)
    val pages by documentRepository.getPagesForDocument(documentId).collectAsState(initial = emptyList())

    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var showExportPdfDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        documentRepository.addPage(documentId, bmp)
                    }
                }
            }
        }
    }

    val activePage = pages.getOrNull(selectedPageIndex) ?: pages.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document?.title ?: "Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${pages.size} pages • ${document?.docType ?: "General"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val pdf = documentRepository.exportToPdfFile(documentId)
                            isExporting = false
                            if (pdf != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Document PDF"))
                            }
                        }
                    },
                    modifier = Modifier.testTag("detail_share_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Big Page Viewport
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (activePage != null && File(activePage.imageFilePath).exists()) {
                        AsyncImage(
                            model = File(activePage.imageFilePath),
                            contentDescription = "Page ${activePage.pageNumber}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator()
                    }

                    // Page Indicator Chip
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Page ${selectedPageIndex + 1} of ${pages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page Editing Actions Bar
            if (activePage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToCrop(documentId, activePage.id) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("page_crop_btn")
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crop")
                    }

                    OutlinedButton(
                        onClick = { onNavigateToEnhance(documentId, activePage.id) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("page_filter_btn")
                    ) {
                        Icon(Icons.Default.Filter, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filters")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                documentRepository.deletePage(activePage.id, documentId)
                                if (selectedPageIndex > 0) selectedPageIndex--
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-Page Thumbnail Strip & Add Page
            Text(
                text = "Pages (${pages.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    val isSelected = index == selectedPageIndex
                    Box(
                        modifier = Modifier
                            .size(64.dp, 88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPageIndex = index }
                    ) {
                        AsyncImage(
                            model = File(page.imageFilePath),
                            contentDescription = "Thumb $index",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Add Page Button
                item {
                    var showAddMenu by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(64.dp, 88.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .clickable { showAddMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add page", tint = MaterialTheme.colorScheme.primary)
                                Text("Add", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Camera Capture") },
                                onClick = {
                                    showAddMenu = false
                                    onAddPageCamera()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import from Gallery") },
                                onClick = {
                                    showAddMenu = false
                                    photoPicker.launch("image/*")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI & OCR Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigateToOcr(documentId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ocr_action_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OCR Text", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onNavigateToAi(documentId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ai_assistant_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF06B6D4))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Assistant", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export & Print Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showExportPdfDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val pdf = documentRepository.exportToPdfFile(documentId)
                            if (pdf != null) {
                                PdfEngine.printPdf(context, pdf, document?.title ?: "Document")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Export PDF Dialog with Presets
    if (showExportPdfDialog) {
        var selectedSize by remember { mutableStateOf(PageSizePreset.A4) }
        var addPageNumbers by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            title = { Text("Export Document to PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select PDF Page Size:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(PageSizePreset.A4, PageSizePreset.US_LETTER, PageSizePreset.LEGAL).forEach { preset ->
                            val isSelected = selectedSize == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedSize = preset }
                            ) {
                                Text(
                                    text = preset.name.replace('_', ' '),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Watermark setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Watermark: ${if (watermarkText.isNotBlank()) watermarkText else "None"}")
                        TextButton(onClick = { showWatermarkDialog = true }) {
                            Text("Configure")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportPdfDialog = false
                        scope.launch {
                            isExporting = true
                            val options = PdfExportOptions(
                                preset = selectedSize,
                                addPageNumbers = addPageNumbers,
                                watermarkText = watermarkText.ifBlank { null }
                            )
                            val file = documentRepository.exportToPdfFile(documentId, options)
                            isExporting = false
                            if (file != null) {
                                exportedPdfFile = file
                                showSuccessDialog = true
                            }
                        }
                    }
                ) {
                    Text("Generate PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPdfDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showWatermarkDialog) {
        WatermarkDialog(
            initialText = watermarkText.ifBlank { "CONFIDENTIAL" },
            onConfirm = {
                watermarkText = it
                showWatermarkDialog = false
            },
            onDismiss = { showWatermarkDialog = false }
        )
    }

    if (showSuccessDialog && exportedPdfFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("PDF Ready") },
            text = { Text("Document PDF generated successfully!\nFile: ${exportedPdfFile!!.name}\nSize: ${exportedPdfFile!!.length() / 1024} KB") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportedPdfFile!!)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Document PDF"))
                    }
                ) {
                    Text("Share Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) { Text("Done") }
            }
        )
    }
}
