package com.example.prscanner.ui.screens.ai

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.AiDocumentEngine
import com.example.prscanner.engine.ExtractedDocumentInfo
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    documentId: Long,
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val document by documentRepository.observeDocument(documentId).collectAsState(initial = null)

    val allDocs by documentRepository.allActiveDocuments.collectAsState(initial = emptyList())
    var selectedDocOverride by remember { mutableStateOf<com.example.prscanner.data.model.DocumentEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(if (documentId == 0L) 1 else 0) } // 0: Insights, 1: Q&A, 2: Translate
    var extractedInfo by remember { mutableStateOf<ExtractedDocumentInfo?>(null) }
    var chatQuery by remember { mutableStateOf("") }
    var chatAnswer by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }

    val activeDoc = selectedDocOverride ?: document
    val docText = activeDoc?.fullOcrText ?: ""

    LaunchedEffect(docText) {
        if (docText.isNotBlank()) {
            extractedInfo = AiDocumentEngine.extractInformation(docText)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Ask AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Ask AI questions and get answers on any topic.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Insights") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ask Q&A") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Translate") }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Insights & Structured Entities
                    val info = extractedInfo
                    if (info != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Classification: ${info.detectedType}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Entities: Amounts
                        if (info.financialAmounts.isNotEmpty()) {
                            EntitySectionCard(
                                icon = Icons.Default.MonetizationOn,
                                title = "Financial Figures",
                                items = info.financialAmounts,
                                accentColor = Color(0xFF10B981)
                            )
                        }

                        // Entities: Dates & Deadlines
                        val allDates = (info.dates + info.deadlines).distinct()
                        if (allDates.isNotEmpty()) {
                            EntitySectionCard(
                                icon = Icons.Default.CalendarToday,
                                title = "Dates & Deadlines",
                                items = allDates,
                                accentColor = Color(0xFF2563EB)
                            )
                        }

                        // Entities: Contacts (Emails & Phones)
                        val contacts = (info.emails + info.phoneNumbers).distinct()
                        if (contacts.isNotEmpty()) {
                            EntitySectionCard(
                                icon = Icons.Default.Email,
                                title = "Contact Points",
                                items = contacts,
                                accentColor = Color(0xFF8B5CF6)
                            )
                        }

                        // Key Highlights
                        if (info.keyPoints.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Key Points", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    info.keyPoints.forEach { pt ->
                                        Text(pt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Text("No text available for analysis. Make sure OCR has scanned the document.")
                    }
                }

                1 -> {
                    // Q&A Mode: Ask AI questions and get answers on any topic
                    if (activeDoc != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Analyzing: ${activeDoc.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                if (allDocs.size > 1) {
                                    TextButton(onClick = { selectedDocOverride = null }) {
                                        Text("Ask Any Topic", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else if (allDocs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ask about any topic, or select a document:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allDocs) { doc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { selectedDocOverride = doc }
                                ) {
                                    Text("📄 ${doc.title.take(16)}", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    Text(
                        text = if (activeDoc != null) "Ask any question about this document:" else "Ask AI questions & get answers on any topic:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Suggested Questions Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeDoc != null) {
                            item {
                                SuggestedChip("What is the total amount?") {
                                    chatQuery = "What is the total amount?"
                                }
                            }
                            item {
                                SuggestedChip("When is the due date?") {
                                    chatQuery = "When is the due date?"
                                }
                            }
                            item {
                                SuggestedChip("Who are the parties?") {
                                    chatQuery = "Who are the parties involved?"
                                }
                            }
                        } else {
                            item {
                                SuggestedChip("What is an NDA?") {
                                    chatQuery = "What is a Non-Disclosure Agreement (NDA)?"
                                }
                            }
                            item {
                                SuggestedChip("Draft invoice email") {
                                    chatQuery = "Draft a professional email for sending an invoice."
                                }
                            }
                            item {
                                SuggestedChip("What is Force Majeure?") {
                                    chatQuery = "Explain Force Majeure contract clause."
                                }
                            }
                            item {
                                SuggestedChip("Receipt vs Invoice") {
                                    chatQuery = "What is the difference between a receipt and an invoice?"
                                }
                            }
                            item {
                                SuggestedChip("How does OCR work?") {
                                    chatQuery = "How does optical character recognition work?"
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatQuery,
                            onValueChange = { chatQuery = it },
                            placeholder = { Text("Ask something...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_query_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (chatQuery.isNotBlank()) {
                                    isThinking = true
                                    scope.launch {
                                        val ans = AiDocumentEngine.queryDocument(chatQuery, docText, "QA")
                                        chatAnswer = ans
                                        isThinking = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("ai_send_query_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }

                    if (isThinking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing document...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    chatAnswer?.let { answer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Answer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(answer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                2 -> {
                    // Translation Mode
                    Text("Translate document into another language:", fontWeight = FontWeight.Medium)

                    var targetLanguage by remember { mutableStateOf("Spanish") }
                    var translatedText by remember { mutableStateOf<String?>(null) }
                    var isTranslating by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Spanish", "French", "German", "Bengali", "Arabic", "Hindi").forEach { lang ->
                            val isSel = targetLanguage == lang
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { targetLanguage = lang }
                            ) {
                                Text(
                                    text = lang,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isTranslating = true
                            scope.launch {
                                val res = AiDocumentEngine.queryDocument(targetLanguage, docText, "TRANSLATE")
                                translatedText = res
                                isTranslating = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Translate to $targetLanguage")
                    }

                    if (isTranslating) {
                        CircularProgressIndicator()
                    }

                    translatedText?.let { tr ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(tr, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntitySectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
