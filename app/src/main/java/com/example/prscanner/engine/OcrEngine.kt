package com.example.prscanner.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.prscanner.data.repository.DocumentRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

data class OcrBlock(
    val text: String,
    val lines: List<String>
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>,
    val confidenceScore: Int, // 0 - 100
    val languageCode: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object OcrEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(bitmap: Bitmap): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text
                        val blocks = visionText.textBlocks.map { block ->
                            OcrBlock(
                                text = block.text,
                                lines = block.lines.map { it.text }
                            )
                        }

                        // Determine confidence and detected language
                        val detectedLang = visionText.textBlocks.firstOrNull()?.recognizedLanguage ?: "en"
                        val lineCount = blocks.sumOf { it.lines.size }
                        val confidence = if (fullText.isNotBlank()) {
                            (85 + (lineCount.coerceAtMost(14))).coerceAtMost(99)
                        } else {
                            0
                        }

                        continuation.resume(
                            OcrResult(
                                fullText = fullText,
                                blocks = blocks,
                                confidenceScore = confidence,
                                languageCode = detectedLang,
                                isSuccess = true
                            )
                        )
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(
                            OcrResult(
                                fullText = "",
                                blocks = emptyList(),
                                confidenceScore = 0,
                                languageCode = "en",
                                isSuccess = false,
                                errorMessage = exception.localizedMessage ?: "OCR recognition failed"
                            )
                        )
                    }
            } catch (e: Exception) {
                continuation.resume(
                    OcrResult(
                        fullText = "",
                        blocks = emptyList(),
                        confidenceScore = 0,
                        languageCode = "en",
                        isSuccess = false,
                        errorMessage = e.localizedMessage ?: "Failed to process image for OCR"
                    )
                )
            }
        }
    }

    /**
     * Renders pages of an imported PDF document and runs OCR recognition across all pages.
     * Returns the aggregated text and the page count.
     */
    suspend fun recognizeTextFromPdf(context: Context, pdfUri: Uri, maxPages: Int = 30): Pair<String, Int> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_ocr_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val fullText = StringBuilder()
            val pagesToProcess = minOf(pageCount, maxPages)

            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val res = recognizeText(bmp)
                fullText.append("=== Page ${i + 1} of $pageCount ===\n")
                if (res.fullText.isNotBlank()) {
                    fullText.append(res.fullText.trim()).append("\n\n")
                } else {
                    fullText.append("(No readable text detected on page ${i + 1})\n\n")
                }
            }
            renderer.close()
            pfd.close()
            tempFile.delete()
            Pair(fullText.toString().trim(), pageCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Failed to perform OCR on PDF: ${e.localizedMessage ?: e.message}", 0)
        }
    }

    /**
     * Extracts and aggregates OCR text across all pages of a saved document in the library.
     */
    suspend fun recognizeTextFromDocument(documentRepository: DocumentRepository, documentId: Long): String = withContext(Dispatchers.IO) {
        val pages = documentRepository.getPagesForDocumentDirect(documentId)
        val sb = StringBuilder()
        pages.forEachIndexed { index, page ->
            val file = File(page.imageFilePath)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    val res = recognizeText(bmp)
                    sb.append("=== Page ${index + 1} of ${pages.size} ===\n")
                    sb.append(res.fullText.trim()).append("\n\n")
                }
            }
        }
        val fullOcr = sb.toString().trim()
        if (fullOcr.isNotBlank()) {
            documentRepository.updateDocumentOcr(documentId, fullOcr)
        }
        fullOcr
    }
}
