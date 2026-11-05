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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
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
    var activeFilterTab by remember { mutableStateOf("ALL") } // ALL, FAVORITES, INVOICES, RECEIPTS, ID_CARDS, TRASH
    var sortBy by remember { mutableStateOf("DATE_DESC") }
    var showSortMenu by remember { mutableStateOf(false) }

    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())
    val favDocs by documentRepository.favoriteDocuments.collectAsState(initial = emptyList())
    val trashedDocs by documentRepository.trashedDocuments.collectAsState(initial = emptyList())
    val searchResults by documentRepository.searchDocuments(searchQuery).collectAsState(initial = emptyList())

    val displayedList = remember(searchQuery, activeFilterTab, sortBy, allDocs, favDocs, trashedDocs, searchResults) {
        val base = when {
            searchQuery.isNotBlank() -> searchResults
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
                    Text(
                        text = "Document Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_btn")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
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

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, tag, or OCR content...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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

        // Documents List
        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching documents found" else "No documents in this view",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
