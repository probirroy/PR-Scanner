package com.example.prscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CoPresent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-fidelity StudioFeatureCatalog matching the exact 4-category layout
 * from the CamScanner feature banner image:
 * 1. Scan (Scan Documents, Scan IDs, Scan Books, Scan Test Papers, Capture Whiteboards, Capture Slides, Image to Text)
 * 2. Convert PDF (PDF to Word, Word to PDF, PDF to Excel, Excel to PDF, PDF to PPT, PPT to PDF, PDF to JPG, JPG to PDF)
 * 3. Edit PDF (Merge PDF, Split PDF, Organize PDF, Rotate PDF Pages, Delete PDF Pages, Add Watermark, Protect PDF, Sign PDF)
 * 4. App-Only (Measure, Ask AI, Scrollshot, CountCam)
 */
@Composable
fun StudioFeatureCatalog(
    onScan: (String) -> Unit,
    onConvert: (String) -> Unit,
    onEdit: (String) -> Unit,
    onMeasure: () -> Unit,
    onAskAi: () -> Unit,
    onScrollshot: () -> Unit,
    onCountCam: () -> Unit,
    onImageToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterTab by remember { mutableStateOf("ALL") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_feature_catalog"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with interactive Category Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Document Studio Suite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All 27 tools • CamScanner pro catalog",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Filter pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "All Tools",
                        "SCAN" to "Scan",
                        "CONVERT" to "Convert PDF",
                        "EDIT" to "Edit PDF",
                        "APP_ONLY" to "App-Only"
                    )
                    items(tabs.size) { idx ->
                        val (key, label) = tabs[idx]
                        val isSelected = selectedFilterTab == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFilterTab = key }
                                .testTag("catalog_tab_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: SCAN
            if (selectedFilterTab == "ALL" || selectedFilterTab == "SCAN") {
                CategorySectionHeader(title = "Scan", count = "7 modes")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogItemRow(
                            icon = Icons.Default.CameraAlt,
                            iconColor = Color(0xFF0D9488), // Teal
                            title = "Scan Documents",
                            onClick = { onScan("DOCUMENTS") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.AutoStories,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Scan Books",
                            onClick = { onScan("BOOK") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.CoPresent,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Capture Whiteboards",
                            onClick = { onScan("WHITEBOARDS") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.TextFields,
                            iconColor = Color(0xFF10B981), // Green
                            title = "Image to Text",
                            onClick = onImageToText
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogItemRow(
                            icon = Icons.Default.Badge,
                            iconColor = Color(0xFF0D9488), // Teal
                            title = "Scan IDs",
                            onClick = { onScan("ID_CARD") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.AssignmentTurnedIn,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Scan Test Papers",
                            onClick = { onScan("TEST_PAPERS") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.CoPresent,
                            iconColor = Color(0xFFF97316), // Orange
                            title = "Capture Slides",
                            onClick = { onScan("SLIDES") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 2: CONVERT PDF
            if (selectedFilterTab == "ALL" || selectedFilterTab == "CONVERT") {
                CategorySectionHeader(title = "Convert PDF", count = "8 formats")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogBadgeRow(
                            badgeText = "W",
                            badgeBg = Color(0xFF2563EB), // Word Blue
                            title = "PDF to Word",
                            onClick = { onConvert("PDF_TO_WORD") }
                        )
                        CatalogBadgeRow(
                            badgeText = "X",
                            badgeBg = Color(0xFF10B981), // Excel Green
                            title = "PDF to Excel",
                            onClick = { onConvert("PDF_TO_EXCEL") }
                        )
                        CatalogBadgeRow(
                            badgeText = "P",
                            badgeBg = Color(0xFFF97316), // PPT Orange
                            title = "PDF to PPT",
                            onClick = { onConvert("PDF_TO_PPT") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.Image,
                            iconColor = Color(0xFF2563EB),
                            title = "PDF to JPG",
                            onClick = { onConvert("PDF_TO_JPG") }
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogBadgeRow(
                            badgeText = "PDF",
                            badgeBg = Color(0xFFEF4444), // PDF Red
                            title = "Word to PDF",
                            onClick = { onConvert("WORD_TO_PDF") }
                        )
                        CatalogBadgeRow(
                            badgeText = "PDF",
                            badgeBg = Color(0xFF10B981), // Excel to PDF
                            title = "Excel to PDF",
                            onClick = { onConvert("EXCEL_TO_PDF") }
                        )
                        CatalogBadgeRow(
                            badgeText = "PDF",
                            badgeBg = Color(0xFFF97316), // PPT to PDF
                            title = "PPT to PDF",
                            onClick = { onConvert("PPT_TO_PDF") }
                        )
                        CatalogBadgeRow(
                            badgeText = "PDF",
                            badgeBg = Color(0xFF2563EB), // JPG to PDF
                            title = "JPG to PDF",
                            onClick = { onConvert("JPG_TO_PDF") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 3: EDIT PDF
            if (selectedFilterTab == "ALL" || selectedFilterTab == "EDIT") {
                CategorySectionHeader(title = "Edit PDF", count = "8 tools")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogItemRow(
                            icon = Icons.Default.Merge,
                            iconColor = Color(0xFFEF4444), // Red
                            title = "Merge PDF",
                            onClick = { onEdit("MERGE") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.Reorder,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Organize PDF",
                            onClick = { onEdit("ORGANIZE") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.Delete,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Delete PDF Pages",
                            onClick = { onEdit("DELETE_PAGES") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.Lock,
                            iconColor = Color(0xFF10B981), // Green
                            title = "Protect PDF",
                            onClick = { onEdit("PROTECT") }
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CatalogItemRow(
                            icon = Icons.Default.Splitscreen,
                            iconColor = Color(0xFFEF4444), // Red
                            title = "Split PDF",
                            onClick = { onEdit("SPLIT") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.ScreenRotation,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Rotate PDF Pages",
                            onClick = { onEdit("ROTATE") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.WaterDamage,
                            iconColor = Color(0xFF2563EB), // Blue
                            title = "Add Watermark",
                            onClick = { onEdit("WATERMARK") }
                        )
                        CatalogItemRow(
                            icon = Icons.Default.Draw,
                            iconColor = Color(0xFF0D9488), // Teal
                            title = "Sign PDF",
                            onClick = { onEdit("SIGN") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 4: APP-ONLY
            if (selectedFilterTab == "ALL" || selectedFilterTab == "APP_ONLY") {
                CategorySectionHeader(title = "App-Only", count = "4 utilities")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOnlyCatalogCard(
                            icon = Icons.Default.Straighten,
                            iconColor = Color(0xFF2563EB),
                            title = "Measure",
                            subtitle = "Measure distances with your phone.",
                            onClick = onMeasure,
                            testTag = "catalog_measure_btn"
                        )
                        AppOnlyCatalogCard(
                            icon = Icons.Default.VerticalAlignBottom,
                            iconColor = Color(0xFF2563EB),
                            title = "Scrollshot",
                            subtitle = "Capture full-page scrolling screenshots.",
                            onClick = onScrollshot,
                            testTag = "catalog_scrollshot_btn"
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOnlyCatalogCard(
                            icon = Icons.Default.Psychology,
                            iconColor = Color(0xFF2563EB),
                            title = "Ask AI",
                            subtitle = "Ask AI questions and get answers on any topic.",
                            onClick = onAskAi,
                            testTag = "catalog_ask_ai_btn"
                        )
                        AppOnlyCatalogCard(
                            icon = Icons.Default.FilterCenterFocus,
                            iconColor = Color(0xFF2563EB),
                            title = "CountCam",
                            subtitle = "Count objects with your camera.",
                            onClick = onCountCam,
                            testTag = "catalog_count_cam_btn"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = count,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CatalogItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("catalog_item_${title.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CatalogBadgeRow(
    badgeText: String,
    badgeBg: Color,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("catalog_item_${title.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = if (badgeText.length > 2) 7.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppOnlyCatalogCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
