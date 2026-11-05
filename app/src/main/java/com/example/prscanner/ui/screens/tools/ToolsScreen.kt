package com.example.prscanner.ui.screens.tools

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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SecurityRepository

data class StudioTool(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val category: String, // "Scan", "Convert PDF", "Edit PDF", "App-Only"
    val icon: ImageVector,
    val iconColor: Color,
    val badge: String? = null
)

@Composable
fun ToolsScreen(
    documentRepository: DocumentRepository,
    securityRepository: SecurityRepository,
    onNavigateToCamera: (String) -> Unit,
    onNavigateToDocument: (Long) -> Unit,
    onNavigateToConverter: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToMeasure: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToScrollshot: () -> Unit,
    onNavigateToCountCam: () -> Unit,
    onNavigateToImageToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Scan", "Convert PDF", "Edit PDF", "App-Only")

    val allStudioTools = listOf(
        // SECTION 1: SCAN
        StudioTool(
            id = "SCAN_DOCS",
            title = "Scan Documents",
            subtitle = "Camera edge-detect & crop",
            category = "Scan",
            icon = Icons.Default.CameraAlt,
            iconColor = Color(0xFF0D9488) // Teal
        ),
        StudioTool(
            id = "SCAN_IDS",
            title = "Scan IDs",
            subtitle = "Front & back 2-sided stitch",
            category = "Scan",
            icon = Icons.Default.Badge,
            iconColor = Color(0xFF0D9488)
        ),
        StudioTool(
            id = "SCAN_BOOKS",
            title = "Scan Books",
            subtitle = "Two-page curvature gutter",
            category = "Scan",
            icon = Icons.Default.AutoStories,
            iconColor = Color(0xFF2563EB) // Blue
        ),
        StudioTool(
            id = "SCAN_TEST_PAPERS",
            title = "Scan Test Papers",
            subtitle = "Exam & answer sheet optimize",
            category = "Scan",
            icon = Icons.Default.AssignmentTurnedIn,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "CAPTURE_WHITEBOARDS",
            title = "Capture Whiteboards",
            subtitle = "Anti-glare & ink boost",
            category = "Scan",
            icon = Icons.Default.CoPresent,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "CAPTURE_SLIDES",
            title = "Capture Slides",
            subtitle = "Projector keystone correction",
            category = "Scan",
            icon = Icons.Default.CoPresent,
            iconColor = Color(0xFFF97316) // Orange
        ),
        StudioTool(
            id = "IMAGE_TO_TEXT",
            title = "Image to Text",
            subtitle = "Instant ML Kit text OCR",
            category = "Scan",
            icon = Icons.Default.TextFields,
            iconColor = Color(0xFF10B981) // Green
        ),

        // SECTION 2: CONVERT PDF
        StudioTool(
            id = "PDF_TO_WORD",
            title = "PDF to Word",
            subtitle = "Export editable .docx file",
            category = "Convert PDF",
            icon = Icons.Default.Description,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "WORD_TO_PDF",
            title = "Word to PDF",
            subtitle = "Convert text & docs to PDF",
            category = "Convert PDF",
            icon = Icons.Default.PictureAsPdf,
            iconColor = Color(0xFFEF4444)
        ),
        StudioTool(
            id = "PDF_TO_EXCEL",
            title = "PDF to Excel",
            subtitle = "Extract tables to CSV/.xlsx",
            category = "Convert PDF",
            icon = Icons.Default.TableChart,
            iconColor = Color(0xFF10B981)
        ),
        StudioTool(
            id = "EXCEL_TO_PDF",
            title = "Excel to PDF",
            subtitle = "Format tables into PDF report",
            category = "Convert PDF",
            icon = Icons.Default.TableChart,
            iconColor = Color(0xFF10B981)
        ),
        StudioTool(
            id = "PDF_TO_PPT",
            title = "PDF to PPT",
            subtitle = "Generate presentation slides",
            category = "Convert PDF",
            icon = Icons.Default.CoPresent,
            iconColor = Color(0xFFF97316)
        ),
        StudioTool(
            id = "PPT_TO_PDF",
            title = "PPT to PDF",
            subtitle = "Landscape presentation PDF",
            category = "Convert PDF",
            icon = Icons.Default.PictureAsPdf,
            iconColor = Color(0xFFF97316)
        ),
        StudioTool(
            id = "PDF_TO_JPG",
            title = "PDF to JPG",
            subtitle = "Render pages as high-res images",
            category = "Convert PDF",
            icon = Icons.Default.Image,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "JPG_TO_PDF",
            title = "JPG to PDF",
            subtitle = "Combine gallery photos to PDF",
            category = "Convert PDF",
            icon = Icons.Default.PictureAsPdf,
            iconColor = Color(0xFF2563EB)
        ),

        // SECTION 3: EDIT PDF
        StudioTool(
            id = "MERGE_PDF",
            title = "Merge PDF",
            subtitle = "Combine multi-PDFs into one",
            category = "Edit PDF",
            icon = Icons.Default.Merge,
            iconColor = Color(0xFFEF4444)
        ),
        StudioTool(
            id = "SPLIT_PDF",
            title = "Split PDF",
            subtitle = "Extract page ranges to new file",
            category = "Edit PDF",
            icon = Icons.Default.Splitscreen,
            iconColor = Color(0xFFEF4444)
        ),
        StudioTool(
            id = "ORGANIZE_PDF",
            title = "Organize PDF",
            subtitle = "Reorder & duplicate pages",
            category = "Edit PDF",
            icon = Icons.Default.Reorder,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "ROTATE_PDF",
            title = "Rotate PDF Pages",
            subtitle = "Rotate 90°, 180°, 270°",
            category = "Edit PDF",
            icon = Icons.Default.ScreenRotation,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "DELETE_PDF_PAGES",
            title = "Delete PDF Pages",
            subtitle = "Remove unwanted pages",
            category = "Edit PDF",
            icon = Icons.Default.Delete,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "ADD_WATERMARK",
            title = "Add Watermark",
            subtitle = "Stamp confidential text",
            category = "Edit PDF",
            icon = Icons.Default.WaterDamage,
            iconColor = Color(0xFF2563EB)
        ),
        StudioTool(
            id = "PROTECT_PDF",
            title = "Protect PDF",
            subtitle = "Password security encryption",
            category = "Edit PDF",
            icon = Icons.Default.Lock,
            iconColor = Color(0xFF10B981)
        ),
        StudioTool(
            id = "SIGN_PDF",
            title = "Sign PDF",
            subtitle = "Draw & stamp digital signature",
            category = "Edit PDF",
            icon = Icons.Default.Draw,
            iconColor = Color(0xFF0D9488)
        ),

        // SECTION 4: APP-ONLY
        StudioTool(
            id = "MEASURE",
            title = "Measure",
            subtitle = "Measure distances with your phone.",
            category = "App-Only",
            icon = Icons.Default.Straighten,
            iconColor = Color(0xFF2563EB),
            badge = "AR CAM"
        ),
        StudioTool(
            id = "ASK_AI",
            title = "Ask AI",
            subtitle = "Ask AI questions and get answers on any topic.",
            category = "App-Only",
            icon = Icons.Default.Psychology,
            iconColor = Color(0xFF2563EB),
            badge = "SMART AI"
        ),
        StudioTool(
            id = "SCROLLSHOT",
            title = "Scrollshot",
            subtitle = "Capture full-page scrolling screenshots.",
            category = "App-Only",
            icon = Icons.Default.VerticalAlignBottom,
            iconColor = Color(0xFF2563EB),
            badge = "STITCH"
        ),
        StudioTool(
            id = "COUNT_CAM",
            title = "CountCam",
            subtitle = "Count objects with your camera.",
            category = "App-Only",
            icon = Icons.Default.FilterCenterFocus,
            iconColor = Color(0xFF2563EB),
            badge = "1-2-3"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
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
                            text = "PR Scanner Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Scan  •  Convert PDF  •  Edit PDF  •  App-Only Tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ALL FREE",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategoryFilter == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable { selectedCategoryFilter = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Tools List Grouped by Category
        val displayedCategories = if (selectedCategoryFilter == "All") {
            listOf("Scan", "Convert PDF", "Edit PDF", "App-Only")
        } else {
            listOf(selectedCategoryFilter)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            displayedCategories.forEach { categoryName ->
                val toolsInCategory = allStudioTools.filter { it.category == categoryName }
                if (toolsInCategory.isNotEmpty()) {
                    // Category Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${toolsInCategory.size} tools",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 2-Column Grid or Card Items for tools
                    items(toolsInCategory.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { tool ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            when (tool.id) {
                                                // Scan
                                                "SCAN_DOCS" -> onNavigateToCamera("DOCUMENTS")
                                                "SCAN_IDS" -> onNavigateToCamera("ID_CARD")
                                                "SCAN_BOOKS" -> onNavigateToCamera("BOOK")
                                                "SCAN_TEST_PAPERS" -> onNavigateToCamera("TEST_PAPERS")
                                                "CAPTURE_WHITEBOARDS" -> onNavigateToCamera("WHITEBOARDS")
                                                "CAPTURE_SLIDES" -> onNavigateToCamera("SLIDES")
                                                "IMAGE_TO_TEXT" -> onNavigateToImageToText()

                                                // Convert PDF
                                                "PDF_TO_WORD" -> onNavigateToConverter("PDF_TO_WORD")
                                                "WORD_TO_PDF" -> onNavigateToConverter("WORD_TO_PDF")
                                                "PDF_TO_EXCEL" -> onNavigateToConverter("PDF_TO_EXCEL")
                                                "EXCEL_TO_PDF" -> onNavigateToConverter("EXCEL_TO_PDF")
                                                "PDF_TO_PPT" -> onNavigateToConverter("PDF_TO_PPT")
                                                "PPT_TO_PDF" -> onNavigateToConverter("PPT_TO_PDF")
                                                "PDF_TO_JPG" -> onNavigateToConverter("PDF_TO_JPG")
                                                "JPG_TO_PDF" -> onNavigateToConverter("JPG_TO_PDF")

                                                // Edit PDF
                                                "MERGE_PDF" -> onNavigateToEditor("MERGE")
                                                "SPLIT_PDF" -> onNavigateToEditor("SPLIT")
                                                "ORGANIZE_PDF" -> onNavigateToEditor("ORGANIZE")
                                                "ROTATE_PDF" -> onNavigateToEditor("ROTATE")
                                                "DELETE_PDF_PAGES" -> onNavigateToEditor("DELETE_PAGES")
                                                "ADD_WATERMARK" -> onNavigateToEditor("WATERMARK")
                                                "PROTECT_PDF" -> onNavigateToEditor("PROTECT")
                                                "SIGN_PDF" -> onNavigateToEditor("SIGN")

                                                // App-Only
                                                "MEASURE" -> onNavigateToMeasure()
                                                "ASK_AI" -> onNavigateToAi()
                                                "SCROLLSHOT" -> onNavigateToScrollshot()
                                                "COUNT_CAM" -> onNavigateToCountCam()
                                            }
                                        }
                                        .testTag("tool_${tool.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(tool.iconColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = tool.icon,
                                                    contentDescription = null,
                                                    tint = tool.iconColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            tool.badge?.let { b ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = tool.iconColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = b,
                                                        color = tool.iconColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = tool.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        tool.subtitle?.let { sub ->
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }

                            // If single item in pair, add empty spacer
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
