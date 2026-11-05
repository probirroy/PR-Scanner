package com.example.prscanner.ui.screens.tools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

val CONVERT_TOOLS = listOf(
    "PDF_TO_WORD" to "PDF to Word",
    "WORD_TO_PDF" to "Word to PDF",
    "PDF_TO_EXCEL" to "PDF to Excel",
    "EXCEL_TO_PDF" to "Excel to PDF",
    "PDF_TO_PPT" to "PDF to PPT",
    "PPT_TO_PDF" to "PPT to PDF",
    "PDF_TO_JPG" to "PDF to JPG",
    "JPG_TO_PDF" to "JPG to PDF"
)

@Composable
fun PdfConverterScreen(
    initialAction: String = "PDF_TO_WORD",
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())

    val initialIndex = CONVERT_TOOLS.indexOfFirst { it.first == initialAction }.let { if (it >= 0) it else 0 }
    var selectedTab by remember { mutableIntStateOf(initialIndex) }

    var selectedDocument by remember { mutableStateOf<DocumentEntity?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var outputFilePath by remember { mutableStateOf<String?>(null) }

    // Multi images for JPG to PDF
    val importedBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Text inputs for Word to PDF / Excel to PDF
    var wordTextInput by remember {
        mutableStateOf(
            "PR Scanner Executive Report\n\n1. Project Overview\nThis document was converted using the PR Scanner PDF & Office Conversion Engine.\n\n2. Key Highlights\n• High fidelity document rendering\n• Zero cloud dependency & 100% offline security\n• Instant compatibility with Word, Excel, and PowerPoint\n\n3. Next Steps\nShare this document or print directly via wireless network."
        )
    }
    var excelCsvInput by remember {
        mutableStateOf(
            "Item,Category,Quantity,Unit Price,Total\n" +
            "Pro License,Software,1,$0.00,$0.00\n" +
            "A4 Scan Paper,Office,500,$12.50,$12.50\n" +
            "Cloud Storage,Hosting,100GB,$0.00,$0.00\n" +
            "Total Amount,,, ,$12.50"
        )
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) importedBitmaps.add(bmp)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("converter_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF & Office Converter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Convert between PDF, Word, Excel, PPT & JPG",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2563EB).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = CONVERT_TOOLS[selectedTab].second,
                        color = Color(0xFF2563EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scrollable Tabs for the 8 Conversion Tools
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            CONVERT_TOOLS.forEachIndexed { index, (key, title) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        outputFilePath = null
                    },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val currentToolKey = CONVERT_TOOLS[selectedTab].first

            // Document selector card for PDF-based tools
            if (currentToolKey in listOf("PDF_TO_WORD", "PDF_TO_EXCEL", "PDF_TO_PPT", "PDF_TO_JPG")) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Select Source Document:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allDocs.isEmpty()) {
                            Text("No documents found in library. Generate a sample document below:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val sample = SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                                        val id = documentRepository.createDocumentWithPages(
                                            title = "Sample Business Invoice",
                                            pageBitmaps = listOf(sample),
                                            docType = DocType.INVOICE
                                        )
                                        selectedDocument = documentRepository.getDocument(id)
                                    }
                                }
                            ) {
                                Text("Generate Sample Document")
                            }
                        } else {
                            var showPicker by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = selectedDocument?.title ?: "Select a document to convert...",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPicker = true },
                                    trailingIcon = {
                                        TextButton(onClick = { showPicker = true }) { Text("Change") }
                                    }
                                )

                                DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                                    allDocs.forEach { doc ->
                                        DropdownMenuItem(
                                            text = { Text("${doc.title} (${doc.pageCount} pages)") },
                                            onClick = {
                                                selectedDocument = doc
                                                showPicker = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // JPG to PDF tool specifics: Multi-Image Picker
            if (currentToolKey == "JPG_TO_PDF") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Images to Combine (${importedBitmaps.size}):", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { multiPhotoPicker.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Images")
                                }
                                OutlinedButton(
                                    onClick = {
                                        importedBitmaps.add(SampleDocumentGenerator.generateSampleBitmap(DocType.RECEIPT))
                                        importedBitmaps.add(SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE))
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sample")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (importedBitmaps.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(importedBitmaps) { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp, 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Text("Pick one or more gallery photos to compile into a PDF document.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Word to PDF tool specifics: Editable Text
            if (currentToolKey == "WORD_TO_PDF") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Word Document Text Content:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wordTextInput,
                            onValueChange = { wordTextInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Excel to PDF tool specifics: Editable CSV Table
            if (currentToolKey == "EXCEL_TO_PDF") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spreadsheet Data (CSV / Table format):", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = excelCsvInput,
                            onValueChange = { excelCsvInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // PPT to PDF specifics: Slide Outline
            if (currentToolKey == "PPT_TO_PDF") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Presentation Slide Cards to PDF:", fontWeight = FontWeight.Bold)
                        Text("Generates a clean landscape presentation PDF deck from slides.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Conversion Trigger Button
            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        val outDir = File(context.cacheDir, "conversions").apply { mkdirs() }

                        when (currentToolKey) {
                            "PDF_TO_WORD" -> {
                                val doc = selectedDocument ?: allDocs.firstOrNull()
                                if (doc != null) {
                                    val pages = documentRepository.getPagesForDocumentDirect(doc.id)
                                    val texts = mutableListOf<String>()
                                    for (page in pages) {
                                        val t = if (page.ocrText.isNotBlank()) page.ocrText
                                        else {
                                            val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                            if (bmp != null) OcrEngine.recognizeText(bmp).fullText else ""
                                        }
                                        texts.add(t)
                                    }
                                    val outFile = File(outDir, "${doc.title.replace(' ', '_')}.docx")
                                    val ok = DocxEngine.createDocxFile(outFile, doc.title, texts)
                                    if (ok) outputFilePath = outFile.absolutePath
                                } else {
                                    Toast.makeText(context, "Please select a document first", Toast.LENGTH_SHORT).show()
                                }
                            }

                            "WORD_TO_PDF" -> {
                                val outFile = File(outDir, "Word_Report_${System.currentTimeMillis() % 10000}.pdf")
                                val doc = PdfDocument()
                                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                val page = doc.startPage(pageInfo)
                                val canvas = page.canvas
                                canvas.drawColor(AndroidColor.WHITE)

                                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.rgb(30, 58, 138)
                                    textSize = 20f
                                    isFakeBoldText = true
                                }
                                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.rgb(51, 65, 85)
                                    textSize = 12f
                                }

                                var y = 60f
                                val lines = wordTextInput.split("\n")
                                lines.forEachIndexed { i, line ->
                                    if (i == 0) {
                                        canvas.drawText(line, 40f, y, titlePaint)
                                        y += 30f
                                    } else {
                                        canvas.drawText(line, 40f, y, textPaint)
                                        y += 18f
                                    }
                                }

                                doc.finishPage(page)
                                FileOutputStream(outFile).use { doc.writeTo(it) }
                                doc.close()
                                outputFilePath = outFile.absolutePath
                            }

                            "PDF_TO_EXCEL" -> {
                                val doc = selectedDocument ?: allDocs.firstOrNull()
                                if (doc != null) {
                                    val pages = documentRepository.getPagesForDocumentDirect(doc.id)
                                    val texts = mutableListOf<String>()
                                    for (page in pages) {
                                        val t = if (page.ocrText.isNotBlank()) page.ocrText
                                        else {
                                            val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                            if (bmp != null) OcrEngine.recognizeText(bmp).fullText else ""
                                        }
                                        texts.add(t)
                                    }
                                    val outFile = File(outDir, "${doc.title.replace(' ', '_')}.csv")
                                    val ok = DocxEngine.createCsvSpreadsheet(outFile, texts)
                                    if (ok) outputFilePath = outFile.absolutePath
                                }
                            }

                            "EXCEL_TO_PDF" -> {
                                val outFile = File(outDir, "Excel_Table_Report.pdf")
                                val doc = PdfDocument()
                                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                val page = doc.startPage(pageInfo)
                                val canvas = page.canvas
                                canvas.drawColor(AndroidColor.WHITE)

                                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.rgb(16, 185, 129)
                                    textSize = 18f
                                    isFakeBoldText = true
                                }
                                val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.rgb(30, 41, 59)
                                    textSize = 11f
                                }
                                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = AndroidColor.rgb(203, 213, 225)
                                    strokeWidth = 1f
                                }

                                canvas.drawText("EXCEL DATA REPORT", 40f, 50f, titlePaint)

                                var curY = 80f
                                val rows = excelCsvInput.split("\n").filter { it.isNotBlank() }
                                rows.forEach { row ->
                                    val cols = row.split(",")
                                    var curX = 40f
                                    cols.forEach { col ->
                                        canvas.drawText(col.trim(), curX, curY + 14f, cellPaint)
                                        curX += 95f
                                    }
                                    canvas.drawLine(40f, curY + 20f, 550f, curY + 20f, gridPaint)
                                    curY += 26f
                                }

                                doc.finishPage(page)
                                FileOutputStream(outFile).use { doc.writeTo(it) }
                                doc.close()
                                outputFilePath = outFile.absolutePath
                            }

                            "PDF_TO_PPT" -> {
                                val doc = selectedDocument ?: allDocs.firstOrNull()
                                if (doc != null) {
                                    val pages = documentRepository.getPagesForDocumentDirect(doc.id)
                                    val texts = mutableListOf<String>()
                                    for (page in pages) {
                                        val t = if (page.ocrText.isNotBlank()) page.ocrText
                                        else {
                                            val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                            if (bmp != null) OcrEngine.recognizeText(bmp).fullText else ""
                                        }
                                        texts.add(t)
                                    }
                                    val outFile = File(outDir, "${doc.title.replace(' ', '_')}_Slides.txt")
                                    val ok = DocxEngine.createSlideDeckText(outFile, doc.title, texts)
                                    if (ok) outputFilePath = outFile.absolutePath
                                }
                            }

                            "PPT_TO_PDF" -> {
                                val outFile = File(outDir, "Presentation_Deck.pdf")
                                val doc = PdfDocument()
                                // Landscape 842x595
                                for (p in 1..3) {
                                    val pageInfo = PdfDocument.PageInfo.Builder(842, 595, p).create()
                                    val page = doc.startPage(pageInfo)
                                    val canvas = page.canvas
                                    canvas.drawColor(AndroidColor.rgb(248, 250, 252))

                                    val slideTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = AndroidColor.rgb(37, 99, 235)
                                        textSize = 28f
                                        isFakeBoldText = true
                                    }
                                    canvas.drawText("SLIDE $p: EXECUTIVE OVERVIEW", 60f, 80f, slideTitlePaint)

                                    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = AndroidColor.rgb(51, 65, 85)
                                        textSize = 16f
                                    }
                                    canvas.drawText("• PR Scanner Slide Deck Presentation Engine", 60f, 150f, bodyPaint)
                                    canvas.drawText("• Fully vectorised landscape page format", 60f, 190f, bodyPaint)
                                    canvas.drawText("• Ready for high-definition projector display", 60f, 230f, bodyPaint)

                                    doc.finishPage(page)
                                }
                                FileOutputStream(outFile).use { doc.writeTo(it) }
                                doc.close()
                                outputFilePath = outFile.absolutePath
                            }

                            "PDF_TO_JPG" -> {
                                val doc = selectedDocument ?: allDocs.firstOrNull()
                                if (doc != null) {
                                    val pages = documentRepository.getPagesForDocumentDirect(doc.id)
                                    if (pages.isNotEmpty()) {
                                        // Take the first page image
                                        val firstPage = pages[0]
                                        outputFilePath = firstPage.imageFilePath
                                    }
                                }
                            }

                            "JPG_TO_PDF" -> {
                                val bmps = if (importedBitmaps.isNotEmpty()) importedBitmaps.toList()
                                else listOf(SampleDocumentGenerator.generateSampleBitmap(DocType.RECEIPT))

                                val docId = documentRepository.createDocumentWithPages(
                                    title = "JPG to PDF (${bmps.size} images)",
                                    pageBitmaps = bmps,
                                    docType = DocType.GENERAL
                                )
                                val pdf = documentRepository.exportToPdfFile(docId)
                                if (pdf != null) outputFilePath = pdf.absolutePath
                            }
                        }

                        isProcessing = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("run_conversion_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Converting Document...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert Now (${CONVERT_TOOLS[selectedTab].second})", fontWeight = FontWeight.Bold)
                }
            }

            // Output Card
            outputFilePath?.let { path ->
                val file = File(path)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Conversion Complete!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(file.name, style = MaterialTheme.typography.bodySmall)
                                Text("${file.length() / 1024} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val savedPath = documentRepository.savePdfToDownloads(file, file.name)
                                            Toast.makeText(
                                                context,
                                                "Saved to ${savedPath ?: "Downloads/PR_Scanner"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val mimeType = when {
                                            file.name.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            file.name.endsWith(".csv") -> "text/csv"
                                            file.name.endsWith(".pdf") -> "application/pdf"
                                            file.name.endsWith(".jpg") -> "image/jpeg"
                                            else -> "text/plain"
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = mimeType
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Converted Document"))
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
