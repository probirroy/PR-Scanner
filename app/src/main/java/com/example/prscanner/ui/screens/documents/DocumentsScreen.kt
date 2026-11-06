package com.example.prscanner.ui.screens.documents

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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.ui.components.DocumentCard
import kotlinx.coroutines.launch

@Composable
fun DocumentsScreen(
    documentRepository: DocumentRepository,
    onNavigateToDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf("ALL") } // ALL, OCR_ONLY, TITLE_ONLY
    var activeFilterTab by remember { mutableStateOf("ALL") } // ALL, FAVORITES, INVOICES, RECEIPTS, ID_CARDS, TRASH
    var sortBy by remember { mutableStateOf("DATE_DESC") }
    var showSortMenu by remember { mutableStateOf(false) }

    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())
    val favDocs by documentRepository.favoriteDocuments.collectAsState(initial = emptyList())
    val trashedDocs by documentRepository.trashedDocuments.collectAsState(initial = emptyList())
    val allSearchResults by documentRepository.searchDocuments(searchQuery).collectAsState(initial = emptyList())
    val ocrSearchResults by documentRepository.searchDocumentsByOcrOnly(searchQuery).collectAsState(initial = emptyList())

    val displayedList = remember(
        searchQuery, searchScope, activeFilterTab, sortBy,
        allDocs, favDocs, trashedDocs, allSearchResults, ocrSearchResults
    ) {
        val base = when {
            searchQuery.isNotBlank() -> {
                val matches = when (searchScope) {
                    "OCR_ONLY" -> ocrSearchResults
                    "TITLE_ONLY" -> allDocs.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    else -> allSearchResults
                }
                when (activeFilterTab) {
                    "FAVORITES" -> matches.filter { it.isFavorite }
                    "INVOICES" -> matches.filter { it.docType == DocType.INVOICE.name }
                    "RECEIPTS" -> matches.filter { it.docType == DocType.RECEIPT.name }
                    "ID_CARDS" -> matches.filter { it.docType == DocType.ID_CARD.name }
                    "TRASH" -> trashedDocs.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.fullOcrText.contains(searchQuery, ignoreCase = true)
                    }
                    else -> matches
                }
            }
            activeFilterTab == "FAVORITES" -> favDocs
            activeFilterTab == "TRASH" -> trashedDocs
            activeFilterTab == "INVOICES" -> allDocs.filter { it.docType == DocType.INVOICE.name }
            activeFilterTab == "RECEIPTS" -> allDocs.filter { it.docType == DocType.RECEIPT.name }
            activeFilterTab == "ID_CARDS" -> allDocs.filter { it.docType == DocType.ID_CARD.name }
            else -> allDocs
        }

        when (sortBy) {
            "NAME_ASC" -> base.sortedBy { it.title.lowercase() }
            "PAGES_DESC" -> base.sortedByDescending { it.pageCount }
            else -> base.sortedByDescending { it.updatedAt }
        }
    }

    val quickOcrKeywords = listOf(
        "Invoice", "Receipt", "Total", "Tax", "Horizon", "Morgan", "Payment", "Chapter", "Algorithm"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Document Gallery",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Full-text OCR search across all scans & pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)") },
                                onClick = { sortBy = "DATE_DESC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (A to Z)") },
                                onClick = { sortBy = "NAME_ASC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Most Pages") },
                                onClick = { sortBy = "PAGES_DESC"; showSortMenu = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar with Clear and OCR Indicators
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title or OCR text content...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_field")
                )

                // Search Scope Pills (All, OCR Text, Title)
                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scope:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        listOf("ALL" to "All Fields", "OCR_ONLY" to "OCR Content Only", "TITLE_ONLY" to "Titles Only").forEach { (key, label) ->
                            val isSelected = searchScope == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { searchScope = key }
                                    .testTag("scope_btn_$key")
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Result count banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displayedList.size} scan(s) matched \"$searchQuery\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { searchQuery = "" },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Quick OCR Search Suggestions Chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick OCR Search:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickOcrKeywords) { keyword ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable { searchQuery = keyword }
                                    .testTag("quick_ocr_chip_$keyword")
                            ) {
                                Text(
                                    text = keyword,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs (All, Favorites, Invoices, Receipts, IDs, Trash)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "All (${allDocs.size})",
                        "FAVORITES" to "Favorites (${favDocs.size})",
                        "INVOICES" to "Invoices",
                        "RECEIPTS" to "Receipts",
                        "ID_CARDS" to "ID Cards",
                        "TRASH" to "Trash (${trashedDocs.size})"
                    )

                    items(tabs) { (key, label) ->
                        val isSelected = activeFilterTab == key
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { activeFilterTab = key }
                                .testTag("tab_$key")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Documents List or Empty State
        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isNotBlank()) Icons.Default.FindInPage else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "No scans found matching \"$searchQuery\" in titles or OCR text"
                        } else {
                            "No documents in this view"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "Check spelling or try a different keyword like 'Invoice', 'Total', or 'Tax'"
                        } else {
                            "Tap the camera or scan studio to capture new documents"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { searchQuery = "" },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Clear Search Query")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedList, key = { it.id }) { doc ->
                    if (activeFilterTab == "TRASH") {
                        // Trashed Item View
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, fontWeight = FontWeight.Bold)
                                    Text("${doc.pageCount} pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch { documentRepository.restoreFromTrash(doc.id) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch { documentRepository.deletePermanently(doc.id) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Permanent Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    } else {
                        DocumentCard(
                            document = doc,
                            onClick = { onNavigateToDocument(doc.id) },
                            searchQuery = searchQuery,
                            onFavoriteToggle = {
                                scope.launch { documentRepository.toggleFavorite(doc.id) }
                            },
                            onRename = { /* Handle rename inline or in detail */ },
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
                            onSavePdf = {
                                scope.launch {
                                    val file = documentRepository.exportToPdfFile(doc.id)
                                    if (file != null) {
                                        val path = documentRepository.savePdfToDownloads(file, "${doc.title}.pdf")
                                        android.widget.Toast.makeText(
                                            context,
                                            "Saved multi-page PDF to ${path ?: "Downloads/PR_Scanner"}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Could not convert to PDF", android.widget.Toast.LENGTH_SHORT).show()
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
        }
    }
}
