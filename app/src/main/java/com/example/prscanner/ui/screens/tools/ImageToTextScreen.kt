package com.example.prscanner.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.DocxEngine
import com.example.prscanner.engine.OcrEngine
import com.example.prscanner.engine.PdfEngine
import com.example.prscanner.util.SampleDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun ImageToTextScreen(
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    onNavigateToCamera: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())

    // 0: Image, 1: PDF, 2: Document Library
    var selectedSourceTab by remember { mutableIntStateOf(0) }

    // Image Source State
    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE))
    }

    // PDF Source State
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableIntStateOf(0) }

    // Document Source State
    var selectedDocument by remember { mutableStateOf<DocumentEntity?>(null) }

    var extractedText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisStatusMessage by remember { mutableStateOf("Processing...") }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        textToSpeech.language = Locale.getDefault()
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // Initial analysis for sample image on launch
    LaunchedEffect(Unit) {
        val bmp = selectedBitmap
        if (bmp != null && extractedText.isBlank()) {
            isAnalyzing = true
            analysisStatusMessage = "Analyzing sample document image with ML Kit..."
            val res = OcrEngine.recognizeText(bmp)
            extractedText = res.fullText.ifBlank { "No readable text detected in this image." }
            isAnalyzing = false
        }
    }

    // Photo picker for Image OCR
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                analysisStatusMessage = "Loading and running OCR on image..."
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            selectedBitmap = bmp
                            val ocrResult = OcrEngine.recognizeText(bmp)
                            extractedText = ocrResult.fullText.ifBlank { "No readable text detected in this image." }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Could not open image", Toast.LENGTH_SHORT).show()
                }
                isAnalyzing = false
            }
        }
    }

    // PDF file picker for PDF OCR
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            scope.launch {
                isAnalyzing = true
                analysisStatusMessage = "Rendering PDF pages & running multi-page OCR..."
                val (text, pages) = OcrEngine.recognizeTextFromPdf(context, uri)
                extractedText = text
                pdfPageCount = pages
                isAnalyzing = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ocr_tool_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OCR Text Converter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Convert Image, PDF, or Document into editable text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ML Kit Engine",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Source Type Tabs (Image, PDF, Document)
        TabRow(
            selectedTabIndex = selectedSourceTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSourceTab == 0,
                onClick = { selectedSourceTab = 0 },
                text = { Text("Image OCR", fontWeight = if (selectedSourceTab == 0) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSourceTab == 1,
                onClick = { selectedSourceTab = 1 },
                text = { Text("PDF OCR", fontWeight = if (selectedSourceTab == 1) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSourceTab == 2,
                onClick = { selectedSourceTab = 2 },
                text = { Text("Document OCR", fontWeight = if (selectedSourceTab == 2) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Sub-ribbon based on selected tab
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (selectedSourceTab) {
                    0 -> {
                        // Image OCR options
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { photoPicker.launch("image/*") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Photo", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToCamera("DOCUMENTS") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                                selectedBitmap = bmp
                                scope.launch {
                                    isAnalyzing = true
                                    analysisStatusMessage = "Analyzing sample document image with ML Kit..."
                                    val res = OcrEngine.recognizeText(bmp)
                                    extractedText = res.fullText.ifBlank { "No readable text detected in this image." }
                                    isAnalyzing = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample", fontSize = 12.sp)
                        }
                    }

                    1 -> {
                        // PDF OCR options
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPicker.launch("application/pdf") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select PDF File", fontSize = 12.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    isAnalyzing = true
                                    analysisStatusMessage = "Generating & running OCR on sample multi-page PDF..."
                                    val bmp1 = SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                                    val bmp2 = SampleDocumentGenerator.generateSampleBitmap(DocType.RECEIPT)
                                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                    val samplePdf = File(exportDir, "sample_invoice_receipt.pdf")
                                    PdfEngine.generatePdf(listOf(bmp1, bmp2), samplePdf)
                                    val (text, pages) = OcrEngine.recognizeTextFromPdf(context, Uri.fromFile(samplePdf))
                                    extractedText = text
                                    pdfPageCount = pages
                                    isAnalyzing = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample PDF", fontSize = 12.sp)
                        }
                    }

                    2 -> {
                        // Document Library OCR options
                        var showDocPicker by remember { mutableStateOf(false) }

                        Box {
                            OutlinedButton(
                                onClick = { showDocPicker = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedDocument?.title?.take(18) ?: "Choose Document", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = showDocPicker,
                                onDismissRequest = { showDocPicker = false }
                            ) {
                                if (allDocs.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No documents in library") },
                                        onClick = { showDocPicker = false }
                                    )
                                } else {
                                    allDocs.forEach { doc ->
                                        DropdownMenuItem(
                                            text = { Text("${doc.title} (${doc.pageCount}p)") },
                                            onClick = {
                                                selectedDocument = doc
                                                showDocPicker = false
                                                scope.launch {
                                                    isAnalyzing = true
                                                    analysisStatusMessage = "Performing multi-page OCR on ${doc.title}..."
                                                    val text = OcrEngine.recognizeTextFromDocument(documentRepository, doc.id)
                                                    extractedText = text.ifBlank { "No text recognized in document pages." }
                                                    isAnalyzing = false
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${allDocs.size} documents in library",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Source Image Preview (When on Image Tab)
            if (selectedSourceTab == 0 && selectedBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Source image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Extracted Text Card & Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Extracted Editable Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val wordCount = extractedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                            Text("$wordCount words  •  ${extractedText.length} characters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // TTS Play
                            IconButton(
                                onClick = {
                                    if (isTtsReady && extractedText.isNotBlank()) {
                                        tts?.speak(extractedText, TextToSpeech.QUEUE_FLUSH, null, "ocr_speak")
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Copy
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Extracted Text", extractedText))
                                    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }

                            // Share
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, extractedText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Extracted Text"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAnalyzing) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(analysisStatusMessage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Extracted OCR text will appear here...") }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Conversion & Export Actions
                    Text("Convert Extracted Text To:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Convert to Word (.docx)
                        OutlinedButton(
                            onClick = {
                                if (extractedText.isNotBlank()) {
                                    scope.launch {
                                        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val wordFile = File(exportDir, "OCR_Extracted_${System.currentTimeMillis() % 10000}.docx")
                                        val ok = DocxEngine.createDocxFile(wordFile, "OCR Converted Document", listOf(extractedText))
                                        if (ok) {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", wordFile)
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Converted Word Document"))
                                        } else {
                                            Toast.makeText(context, "Failed to create Word file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Word (.docx)", fontSize = 11.sp)
                        }

                        // Export as Text (.txt)
                        OutlinedButton(
                            onClick = {
                                if (extractedText.isNotBlank()) {
                                    scope.launch {
                                        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val txtFile = File(exportDir, "OCR_Text_${System.currentTimeMillis() % 10000}.txt")
                                        txtFile.writeText(extractedText)
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", txtFile)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Text File"))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Text (.txt)", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save as New Searchable Document in Library
                    Button(
                        onClick = {
                            val bmp = selectedBitmap ?: SampleDocumentGenerator.generateSampleBitmap(DocType.GENERAL)
                            scope.launch {
                                val id = documentRepository.createDocumentWithPages(
                                    title = "OCR Converted Document",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.GENERAL
                                )
                                documentRepository.updateDocumentOcr(id, extractedText)
                                Toast.makeText(context, "Document created with searchable OCR text!", Toast.LENGTH_SHORT).show()
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Article, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save as Searchable Document", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
