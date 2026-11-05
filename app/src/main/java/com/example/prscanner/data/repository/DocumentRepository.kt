package com.example.prscanner.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.FileInputStream
import com.example.prscanner.data.db.PrScannerDatabase
import com.example.prscanner.data.model.AuditLogEntity
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.DocumentEntity
import com.example.prscanner.data.model.DocumentPageEntity
import com.example.prscanner.data.model.FilterType
import com.example.prscanner.engine.AiDocumentEngine
import com.example.prscanner.engine.ImageEnhancementEngine
import com.example.prscanner.engine.OcrEngine
import com.example.prscanner.engine.PdfEngine
import com.example.prscanner.engine.PdfExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentRepository(private val context: Context) {
    private val db = PrScannerDatabase.getDatabase(context)
    private val documentDao = db.documentDao()
    private val pageDao = db.documentPageDao()
    private val auditLogDao = db.auditLogDao()

    val allActiveDocuments: Flow<List<DocumentEntity>> = documentDao.getAllActiveDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()
    val trashedDocuments: Flow<List<DocumentEntity>> = documentDao.getTrashedDocuments()
    val activeDocumentCount: Flow<Int> = documentDao.countActiveDocuments()
    val totalStorageUsed: Flow<Long?> = documentDao.getTotalStorageUsedBytes()
    val totalPageCount: Flow<Int> = pageDao.getTotalPageCount()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentAuditLogs()

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = documentDao.searchDocuments(query)

    fun getDocumentsByFolder(folderId: Long): Flow<List<DocumentEntity>> = documentDao.getDocumentsByFolder(folderId)

    fun observeDocument(id: Long): Flow<DocumentEntity?> = documentDao.observeDocumentById(id)

    fun getPagesForDocument(documentId: Long): Flow<List<DocumentPageEntity>> = pageDao.getPagesForDocument(documentId)

    suspend fun getDocumentById(id: Long): DocumentEntity? = documentDao.getDocumentById(id)
    suspend fun getDocument(id: Long): DocumentEntity? = documentDao.getDocumentById(id)
    suspend fun getPagesForDocumentDirect(documentId: Long): List<DocumentPageEntity> = pageDao.getPagesListForDocument(documentId)

    suspend fun createDocumentWithPages(
        title: String,
        pageBitmaps: List<Bitmap>,
        docType: DocType = DocType.GENERAL,
        folderId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val docDir = File(context.filesDir, "docs").apply { mkdirs() }
        val finalTitle = title.ifBlank { "Scan ${System.currentTimeMillis() % 10000}" }

        // Save thumbnail from page 1
        val thumbFile = File(docDir, "thumb_${UUID.randomUUID()}.jpg")
        if (pageBitmaps.isNotEmpty()) {
            FileOutputStream(thumbFile).use { out ->
                pageBitmaps[0].compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
        }

        val docEntity = DocumentEntity(
            title = finalTitle,
            docType = docType.name,
            folderId = folderId,
            pageCount = pageBitmaps.size,
            thumbnailUri = thumbFile.absolutePath,
            fileSizeEstimateBytes = thumbFile.length() * pageBitmaps.size
        )

        val docId = documentDao.insertDocument(docEntity)

        // Save each page to storage
        var accumulatedOcr = StringBuilder()
        pageBitmaps.forEachIndexed { index, bitmap ->
            val pageFile = File(docDir, "page_${docId}_${index + 1}_${UUID.randomUUID()}.jpg")
            FileOutputStream(pageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Quick OCR on page 1 for title/search indexing
            val ocrText = if (index == 0) {
                val ocr = OcrEngine.recognizeText(bitmap)
                ocr.fullText
            } else ""

            if (ocrText.isNotBlank()) {
                accumulatedOcr.append(ocrText).append("\n")
            }

            val pageEntity = DocumentPageEntity(
                documentId = docId,
                pageNumber = index + 1,
                imageFilePath = pageFile.absolutePath,
                originalFilePath = pageFile.absolutePath,
                ocrText = ocrText
            )
            pageDao.insertPage(pageEntity)
        }

        // Update doc summary & full OCR if found
        if (accumulatedOcr.isNotBlank()) {
            val text = accumulatedOcr.toString()
            val summary = AiDocumentEngine.extractInformation(text).summary
            documentDao.updateDocument(
                docEntity.copy(
                    id = docId,
                    fullOcrText = text,
                    ocrSummary = summary
                )
            )
        }

        auditLogDao.logAction(
            AuditLogEntity(
                action = "CREATE_DOCUMENT",
                targetName = finalTitle,
                details = "Scanned ${pageBitmaps.size} pages"
            )
        )

        docId
    }

    suspend fun addPage(documentId: Long, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        val docDir = File(context.filesDir, "docs").apply { mkdirs() }
        val newPageNum = doc.pageCount + 1
        val pageFile = File(docDir, "page_${documentId}_${newPageNum}_${UUID.randomUUID()}.jpg")

        FileOutputStream(pageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        val ocrResult = OcrEngine.recognizeText(bitmap)

        pageDao.insertPage(
            DocumentPageEntity(
                documentId = documentId,
                pageNumber = newPageNum,
                imageFilePath = pageFile.absolutePath,
                originalFilePath = pageFile.absolutePath,
                ocrText = ocrResult.fullText
            )
        )

        val updatedOcr = (doc.fullOcrText + "\n" + ocrResult.fullText).trim()
        documentDao.updateDocument(
            doc.copy(
                pageCount = newPageNum,
                updatedAt = System.currentTimeMillis(),
                fullOcrText = updatedOcr
            )
        )
    }

    suspend fun updatePageFilter(
        pageId: Long,
        filter: FilterType,
        brightness: Float,
        contrast: Float,
        rotationDegrees: Int
    ) = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext
        val baseFile = File(page.originalFilePath.ifBlank { page.imageFilePath })
        if (!baseFile.exists()) return@withContext

        val baseBitmap = BitmapFactory.decodeFile(baseFile.absolutePath) ?: return@withContext
        val adjustedBitmap = ImageEnhancementEngine.applyAdjustmentsAndFilter(
            source = baseBitmap,
            filter = filter,
            brightness = brightness,
            contrast = contrast,
            rotationDegrees = rotationDegrees
        )

        // Save adjusted version
        val updatedFile = File(context.filesDir, "docs/page_adj_${page.id}_${UUID.randomUUID()}.jpg")
        FileOutputStream(updatedFile).use { out ->
            adjustedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        pageDao.updatePage(
            page.copy(
                imageFilePath = updatedFile.absolutePath,
                filterType = filter.name,
                brightness = brightness,
                contrast = contrast,
                rotationDegrees = rotationDegrees
            )
        )
    }

    suspend fun deletePage(pageId: Long, documentId: Long) = withContext(Dispatchers.IO) {
        pageDao.deletePage(pageId)
        val remaining = pageDao.getPagesListForDocument(documentId)
        val doc = documentDao.getDocumentById(documentId)
        if (doc != null) {
            if (remaining.isEmpty()) {
                documentDao.deletePermanently(documentId)
            } else {
                // Re-index page numbers
                remaining.forEachIndexed { index, p ->
                    pageDao.updatePage(p.copy(pageNumber = index + 1))
                }
                documentDao.updateDocument(
                    doc.copy(
                        pageCount = remaining.size,
                        updatedAt = System.currentTimeMillis(),
                        thumbnailUri = remaining.first().imageFilePath
                    )
                )
            }
        }
    }

    suspend fun updateDocumentTitle(documentId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(doc.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        auditLogDao.logAction(AuditLogEntity(action = "RENAME", targetName = newTitle))
    }

    suspend fun toggleFavorite(documentId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(doc.copy(isFavorite = !doc.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(documentId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId)
        documentDao.moveToTrash(documentId)
        auditLogDao.logAction(AuditLogEntity(action = "TRASH", targetName = doc?.title ?: "Document #$documentId"))
    }

    suspend fun restoreFromTrash(documentId: Long) = withContext(Dispatchers.IO) {
        documentDao.restoreFromTrash(documentId)
    }

    suspend fun deletePermanently(documentId: Long) = withContext(Dispatchers.IO) {
        documentDao.deletePermanently(documentId)
        pageDao.deletePagesForDocument(documentId)
    }

    suspend fun updateDocumentOcr(documentId: Long, fullOcrText: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        val summary = AiDocumentEngine.extractInformation(fullOcrText).summary
        documentDao.updateDocument(
            doc.copy(
                fullOcrText = fullOcrText,
                ocrSummary = summary,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun exportToPdfFile(
        documentId: Long,
        options: PdfExportOptions = PdfExportOptions()
    ): File? = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext null
        val pages = pageDao.getPagesListForDocument(documentId)
        if (pages.isEmpty()) return@withContext null

        val bitmaps = pages.mapNotNull { p ->
            val file = File(p.imageFilePath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
        if (bitmaps.isEmpty()) return@withContext null

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanName = doc.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(exportDir, "${cleanName}.pdf")

        val success = PdfEngine.generatePdf(bitmaps, pdfFile, options)
        if (success) {
            auditLogDao.logAction(
                AuditLogEntity(
                    action = "EXPORT_PDF",
                    targetName = doc.title,
                    details = "Size: ${pdfFile.length() / 1024} KB"
                )
            )
            pdfFile
        } else null
    }

    suspend fun mergeDocuments(documentIds: List<Long>, mergedTitle: String): Long = withContext(Dispatchers.IO) {
        val allBitmaps = mutableListOf<Bitmap>()
        for (id in documentIds) {
            val pages = pageDao.getPagesListForDocument(id)
            for (p in pages) {
                val f = File(p.imageFilePath)
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath)?.let { allBitmaps.add(it) }
                }
            }
        }
        if (allBitmaps.isEmpty()) return@withContext -1L

        createDocumentWithPages(
            title = mergedTitle,
            pageBitmaps = allBitmaps,
            docType = DocType.GENERAL
        )
    }

    suspend fun savePdfToUri(sourcePdfFile: File, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(sourcePdfFile).use { input ->
                    input.copyTo(out)
                }
            }
            auditLogDao.logAction(
                AuditLogEntity(
                    action = "SAVE_PDF_TO_STORAGE",
                    targetName = sourcePdfFile.name,
                    details = "Saved to storage location: $destinationUri"
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun savePdfToDownloads(sourcePdfFile: File, desiredFileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = desiredFileName.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            val finalName = if (cleanTitle.endsWith(".pdf", ignoreCase = true)) cleanTitle else "$cleanTitle.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PR_Scanner")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourcePdfFile).use { input ->
                        input.copyTo(out)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                auditLogDao.logAction(
                    AuditLogEntity(
                        action = "SAVE_PDF_DOWNLOADS",
                        targetName = finalName,
                        details = "Saved to Downloads/PR_Scanner"
                    )
                )
                "Downloads/PR_Scanner/$finalName"
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PR_Scanner").apply { mkdirs() }
                val targetFile = File(downloadsDir, finalName)
                FileInputStream(sourcePdfFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
