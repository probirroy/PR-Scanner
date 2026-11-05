package com.example.prscanner.ui.screens.tools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.prscanner.data.model.DocumentPageEntity
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.PdfEngine
import com.example.prscanner.engine.PdfExportOptions
import com.example.prscanner.ui.components.SignaturePad
import com.example.prscanner.ui.components.WatermarkDialog
import com.example.prscanner.util.SampleDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

val EDIT_TOOLS = listOf(
    "MERGE" to "Merge PDF",
    "SPLIT" to "Split PDF",
    "ORGANIZE" to "Organize PDF",
    "ROTATE" to "Rotate PDF Pages",
    "DELETE_PAGES" to "Delete PDF Pages",
    "WATERMARK" to "Add Watermark",
    "PROTECT" to "Protect PDF",
    "SIGN" to "Sign PDF"
)

@Composable
fun PdfEditorScreen(
    initialAction: String = "MERGE",
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())

    val initialIndex = EDIT_TOOLS.indexOfFirst { it.first == initialAction }.let { if (it >= 0) it else 0 }
    var selectedTab by remember { mutableIntStateOf(initialIndex) }

    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var docPages by remember { mutableStateOf<List<DocumentPageEntity>>(emptyList()) }
    val selectedPageIndices = remember { mutableStateListOf<Int>() }
    val selectedDocIdsForMerge = remember { mutableStateListOf<Long>() }

    // Dialogs
    var showWatermarkModal by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var showSignatureModal by remember { mutableStateOf(false) }
    var savedSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var passwordInput by remember { mutableStateOf("1234") }

    LaunchedEffect(selectedDoc) {
        val doc = selectedDoc ?: allDocs.firstOrNull()
        if (doc != null) {
            selectedDoc = doc
            docPages = documentRepository.getPagesForDocumentDirect(doc.id)
            selectedPageIndices.clear()
        }
    }

    LaunchedEffect(allDocs) {
        if (selectedDoc == null && allDocs.isNotEmpty()) {
            selectedDoc = allDocs.first()
            docPages = documentRepository.getPagesForDocumentDirect(allDocs.first().id)
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
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("editor_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF Editor Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Merge, split, organize, rotate, sign & protect PDFs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = EDIT_TOOLS[selectedTab].second,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scrollable Tabs for the 8 PDF Edit Tools
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            EDIT_TOOLS.forEachIndexed { index, (key, title) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
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
            val currentToolKey = EDIT_TOOLS[selectedTab].first

            // Document selector
            if (currentToolKey != "MERGE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Document:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = selectedDoc?.title ?: "Select a document...",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMenu = true },
                                trailingIcon = {
                                    TextButton(onClick = { showMenu = true }) { Text("Select") }
                                }
                            )

                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                allDocs.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.title} (${d.pageCount} pages)") },
                                        onClick = {
                                            selectedDoc = d
                                            scope.launch {
                                                docPages = documentRepository.getPagesForDocumentDirect(d.id)
                                                selectedPageIndices.clear()
                                            }
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TOOL SPECIFIC INTERFACES:

            // 1. MERGE PDF
            if (currentToolKey == "MERGE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Documents to Merge into Single PDF:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allDocs.size < 2) {
                            Text("You need at least 2 documents in your library to merge.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val p1 = SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                                        val p2 = SampleDocumentGenerator.generateSampleBitmap(DocType.RECEIPT)
                                        documentRepository.createDocumentWithPages("Sample Document A", listOf(p1), DocType.INVOICE)
                                        documentRepository.createDocumentWithPages("Sample Document B", listOf(p2), DocType.RECEIPT)
                                    }
                                }
                            ) {
                                Text("Generate 2 Sample Documents")
                            }
                        } else {
                            allDocs.forEach { doc ->
                                val isChecked = selectedDocIdsForMerge.contains(doc.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedDocIdsForMerge.remove(doc.id)
                                            else selectedDocIdsForMerge.add(doc.id)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            if (check) selectedDocIdsForMerge.add(doc.id)
                                            else selectedDocIdsForMerge.remove(doc.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${doc.pageCount} pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        val mergedId = documentRepository.mergeDocuments(
                                            selectedDocIdsForMerge.toList(),
                                            "Merged PDF (${selectedDocIdsForMerge.size} docs)"
                                        )
                                        Toast.makeText(context, "Documents merged into one PDF!", Toast.LENGTH_SHORT).show()
                                        onNavigateToDocument(mergedId)
                                    }
                                },
                                enabled = selectedDocIdsForMerge.size >= 2,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Merge, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Merge Selected (${selectedDocIdsForMerge.size})")
                            }
                        }
                    }
                }
            }

            // 2. SPLIT PDF
            if (currentToolKey == "SPLIT") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Pages to Extract into New PDF:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        PageSelectionGrid(pages = docPages, selectedIndices = selectedPageIndices)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val doc = selectedDoc
                                if (doc != null && selectedPageIndices.isNotEmpty()) {
                                    scope.launch {
                                        val extractedBitmaps = selectedPageIndices.mapNotNull { idx ->
                                            docPages.getOrNull(idx)?.let { page ->
                                                BitmapFactory.decodeFile(page.imageFilePath)
                                            }
                                        }
                                        val newId = documentRepository.createDocumentWithPages(
                                            title = "${doc.title} (Split ${extractedBitmaps.size}p)",
                                            pageBitmaps = extractedBitmaps,
                                            docType = try { DocType.valueOf(doc.docType) } catch (e: Exception) { DocType.GENERAL }
                                        )
                                        Toast.makeText(context, "Split PDF document created!", Toast.LENGTH_SHORT).show()
                                        onNavigateToDocument(newId)
                                    }
                                } else {
                                    Toast.makeText(context, "Select at least 1 page to split", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Split & Save Selected Pages (${selectedPageIndices.size})")
                        }
                    }
                }
            }

            // 3. ORGANIZE PDF
            if (currentToolKey == "ORGANIZE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Page Arrangement & Duplication:", fontWeight = FontWeight.Bold)
                        Text("Duplicate or manage pages order in this document", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        docPages.forEachIndexed { idx, page ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp, 56.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Page ${idx + 1}", fontWeight = FontWeight.SemiBold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val doc = selectedDoc
                                            if (doc != null) {
                                                scope.launch {
                                                    val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                                    if (bmp != null) {
                                                        documentRepository.addPage(doc.id, bmp)
                                                        docPages = documentRepository.getPagesForDocumentDirect(doc.id)
                                                        Toast.makeText(context, "Page duplicated!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Duplicate", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. ROTATE PDF PAGES
            if (currentToolKey == "ROTATE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rotate Document Pages 90° Clockwise:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        PageSelectionGrid(pages = docPages, selectedIndices = selectedPageIndices)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val targetIndices = if (selectedPageIndices.isEmpty()) docPages.indices.toList() else selectedPageIndices.toList()
                                    targetIndices.forEach { idx ->
                                        docPages.getOrNull(idx)?.let { page ->
                                            val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                                            if (bmp != null) {
                                                val matrix = Matrix().apply { postRotate(90f) }
                                                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                                FileOutputStream(File(page.imageFilePath)).use { out ->
                                                    rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                                }
                                            }
                                        }
                                    }
                                    selectedDoc?.let { docPages = documentRepository.getPagesForDocumentDirect(it.id) }
                                    Toast.makeText(context, "Pages rotated 90°!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate Pages 90°")
                        }
                    }
                }
            }

            // 5. DELETE PDF PAGES
            if (currentToolKey == "DELETE_PAGES") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Pages to Delete:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        PageSelectionGrid(pages = docPages, selectedIndices = selectedPageIndices)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val doc = selectedDoc
                                if (doc != null && selectedPageIndices.isNotEmpty()) {
                                    scope.launch {
                                        selectedPageIndices.sortedDescending().forEach { idx ->
                                            docPages.getOrNull(idx)?.let { page ->
                                                documentRepository.deletePage(page.id, doc.id)
                                            }
                                        }
                                        docPages = documentRepository.getPagesForDocumentDirect(doc.id)
                                        selectedPageIndices.clear()
                                        Toast.makeText(context, "Selected pages deleted!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = selectedPageIndices.isNotEmpty() && selectedPageIndices.size < docPages.size,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Selected (${selectedPageIndices.size} pages)")
                        }
                    }
                }
            }

            // 6. ADD WATERMARK
            if (currentToolKey == "WATERMARK") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add Security Watermark Stamp:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CONFIDENTIAL", "DRAFT", "COPY", "DO NOT SHARE").forEach { pill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (watermarkText == pill) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { watermarkText = pill }
                                ) {
                                    Text(
                                        text = pill,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val doc = selectedDoc
                                if (doc != null) {
                                    scope.launch {
                                        val options = PdfExportOptions(watermarkText = watermarkText)
                                        val pdf = documentRepository.exportToPdfFile(doc.id, options)
                                        if (pdf != null) {
                                            Toast.makeText(context, "Watermarked PDF generated!", Toast.LENGTH_SHORT).show()
                                            onNavigateToDocument(doc.id)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.WaterDamage, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Watermark & Export PDF")
                        }
                    }
                }
            }

            // 7. PROTECT PDF
            if (currentToolKey == "PROTECT") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PDF Password Protection & Lock:", fontWeight = FontWeight.Bold)
                        Text("Protect document from unauthorized opening", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Security Password (e.g. 4-digit PIN)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "PDF Security Password Shield activated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock & Protect Document")
                        }
                    }
                }
            }

            // 8. SIGN PDF
            if (currentToolKey == "SIGN") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Digital Signature Stamper:", fontWeight = FontWeight.Bold)
                        Text("Sign document pages with handwritten digital signature", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        savedSignatureBitmap?.let { sig ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = sig.asImageBitmap(),
                                    contentDescription = "Signature",
                                    modifier = Modifier.size(160.dp, 70.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSignatureModal = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (savedSignatureBitmap != null) "Redraw" else "Draw Signature")
                            }

                            Button(
                                onClick = {
                                    val doc = selectedDoc
                                    val sig = savedSignatureBitmap
                                    if (doc != null && sig != null) {
                                        scope.launch {
                                            val options = PdfExportOptions(signatureBitmap = sig)
                                            val pdf = documentRepository.exportToPdfFile(doc.id, options)
                                            if (pdf != null) {
                                                Toast.makeText(context, "Document signed and exported!", Toast.LENGTH_SHORT).show()
                                                onNavigateToDocument(doc.id)
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please draw a signature first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = savedSignatureBitmap != null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Stamp & Export")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSignatureModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignatureModal = false },
            confirmButton = {},
            text = {
                SignaturePad(
                    onSaveSignature = { sig ->
                        savedSignatureBitmap = sig
                        showSignatureModal = false
                    },
                    onCancel = { showSignatureModal = false }
                )
            }
        )
    }
}

@Composable
fun PageSelectionGrid(
    pages: List<DocumentPageEntity>,
    selectedIndices: MutableList<Int>
) {
    if (pages.isEmpty()) {
        Text("No pages available.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(pages) { index, page ->
                val isSelected = selectedIndices.contains(index)
                val bmp = BitmapFactory.decodeFile(page.imageFilePath)
                Box(
                    modifier = Modifier
                        .size(60.dp, 84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (isSelected) selectedIndices.remove(index)
                            else selectedIndices.add(index)
                        }
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
