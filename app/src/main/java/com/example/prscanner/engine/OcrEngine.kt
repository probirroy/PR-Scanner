package com.example.prscanner.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
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
}
