package com.example.prscanner.ui.screens.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.model.SubscriptionTier
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SubscriptionRepository
import com.example.prscanner.ui.components.DocumentCard
import com.example.prscanner.ui.components.StudioFeatureCatalog
import com.example.prscanner.util.SampleDocumentGenerator
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    documentRepository: DocumentRepository,
    subscriptionRepository: SubscriptionRepository,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToConverter: (String) -> Unit = {},
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToMeasure: () -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onNavigateToScrollshot: () -> Unit = {},
    onNavigateToCountCam: () -> Unit = {},
    onNavigateToImageToText: () -> Unit = {},
    onNavigateToToolsTab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documents by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())
    val docCount by documentRepository.activeDocumentCount.collectAsState(initial = 0)
    val pageCount by documentRepository.totalPageCount.collectAsState(initial = 0)
    val storageBytes by documentRepository.totalStorageUsed.collectAsState(initial = 0L)
    val currentTier by subscriptionRepository.currentTier.collectAsState()

    var showRenameDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showDemoPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val id = documentRepository.createDocumentWithPages(
                                title = "Imported Image",
                                pageBitmaps = listOf(bitmap),
                                docType = DocType.GENERAL
                            )
                            onNavigateToDocument(id)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PR Scanner",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "FULL ACCESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "AI Document Scanner & PDF Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("home_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search documents",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Scan Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCamera("SINGLE") }
                        .testTag("hero_quick_scan_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF4F46E5))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Smart Document Scan",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Auto-detect edges, Magic Color & OCR text recognition",
                                        color = Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToCamera("SINGLE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("scan_now_btn")
                                ) {
                                    Icon(
                                        Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF1E3A8A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Scan Now",
                                        color = Color(0xFF1E3A8A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { showDemoPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("load_demo_btn")
                                ) {
                                    Text("Load Demo Doc", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        icon = Icons.Default.AddAPhoto,
                        title = "Import Image",
                        onClick = { photoPicker.launch("image/*") }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Badge,
                        title = "ID Card Mode",
                        onClick = { onNavigateToCamera("ID_CARD") }
                    )
                    QuickActionItem(
                        icon = Icons.Default.AutoStories,
                        title = "Book Scan",
                        onClick = { onNavigateToCamera("BOOK") }
                    )
                    QuickActionItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Batch Scan",
                        onClick = { onNavigateToCamera("BATCH") }
                    )
                }
            }

            // Statistics & Storage Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scan Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val mb = (storageBytes ?: 0L) / (1024f * 1024f)
                            Text(
                                text = "%.1f MB Used".format(mb),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(count = docCount.toString(), label = "Documents")
                            StatBox(count = pageCount.toString(), label = "Total Pages")
                            StatBox(count = "Unlimited", label = "Cloud Quota")
                        }
                    }
                }
            }

            // Complete 4-Category Studio Feature Catalog (Directly from user's CamScanner image)
            item {
                StudioFeatureCatalog(
                    onScan = { mode -> onNavigateToCamera(mode) },
                    onConvert = { action -> onNavigateToConverter(action) },
                    onEdit = { action -> onNavigateToEditor(action) },
                    onMeasure = onNavigateToMeasure,
                    onAskAi = onNavigateToAi,
                    onScrollshot = onNavigateToScrollshot,
                    onCountCam = onNavigateToCountCam,
                    onImageToText = onNavigateToImageToText
                )
            }

            // Recent Documents Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${documents.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (documents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No documents scanned yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the camera button to scan your first receipt, contract, or ID card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showDemoPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Generate Sample Doc")
                            }
                        }
                    }
                }
            } else {
                items(documents, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick = { onNavigateToDocument(doc.id) },
                        onFavoriteToggle = {
                            scope.launch { documentRepository.toggleFavorite(doc.id) }
                        },
                        onRename = {
                            showRenameDialog = doc
                            renameText = doc.title
                        },
                        onShare = {
                            scope.launch {
                                documentRepository.exportToPdfFile(doc.id)?.let { pdfFile ->
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Document PDF"))
                                }
                            }
                        },
                        onDelete = {
                            scope.launch { documentRepository.moveToTrash(doc.id) }
                        }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { onNavigateToCamera("SINGLE") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_scan")
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Scan Document",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Rename Dialog
    if (showRenameDialog != null) {
        val target = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            documentRepository.updateDocumentTitle(target.id, renameText.trim())
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Demo Document Generator Dialog
    if (showDemoPicker) {
        AlertDialog(
            onDismissRequest = { showDemoPicker = false },
            title = { Text("Create Sample Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Generate high-fidelity synthetic documents to immediately test ML Kit OCR, Magic Color filter, PDF export, and AI summary:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Tech Solutions Invoice #8891",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.INVOICE
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📄 Sample Tax Invoice ($ 5,500)")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.RECEIPT)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "SuperMart Grocery Receipt",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.RECEIPT
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🧾 Sample Grocery Receipt ($ 30.87)")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.ID_CARD)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Identity Document (Jane Doe)",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.ID_CARD
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🪪 Sample ID Card (MRZ Zone)")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.CONTRACT)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Non-Disclosure Agreement (NDA)",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.CONTRACT
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📜 Sample Legal Contract / NDA")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.BOOK)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Software Architecture Textbook",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.BOOK
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📖 Sample Book Pages (Ch. 4 Dual-Page)")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.TEST_PAPER)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Computer Science Midterm Exam",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.TEST_PAPER
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📝 Sample Exam / Test Paper")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.WHITEBOARD)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Architecture Sprint Whiteboard",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.WHITEBOARD
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📊 Sample Whiteboard Sprint Diagram")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = SampleDocumentGenerator.generateSampleBitmap(DocType.SLIDES)
                                val id = documentRepository.createDocumentWithPages(
                                    title = "Product Strategy Keynote Slides",
                                    pageBitmaps = listOf(bmp),
                                    docType = DocType.SLIDES
                                )
                                showDemoPicker = false
                                onNavigateToDocument(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📽️ Sample Presentation Slide Deck")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDemoPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatBox(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppOnlyFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}

