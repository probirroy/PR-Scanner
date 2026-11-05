package com.example.prscanner.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

data class CountPin(
    val id: Int,
    val relX: Float, // 0f..1f
    val relY: Float, // 0f..1f
    val label: String = "Item",
    val colorArgb: Int = 0xFF2563EB.toInt()
)

/**
 * Engine for CountCam object counting tool.
 * Supports manual pin tagging and automated contrast spot detection.
 */
object ObjectCounterEngine {

    /**
     * Detects distinct contrast objects / spots in a bitmap automatically.
     * Uses downsampled grid luminance analysis to locate local intensity variance peaks.
     */
    fun autoDetectObjects(
        bitmap: Bitmap,
        sensitivity: Float = 0.5f,
        maxItems: Int = 100
    ): List<CountPin> {
        val pins = mutableListOf<CountPin>()
        val width = bitmap.width
        val height = bitmap.height

        // Downsample for fast scanning
        val scanWidth = 120
        val scanHeight = (height * (scanWidth.toFloat() / width.toFloat())).toInt().coerceAtLeast(10)
        val scaled = Bitmap.createScaledBitmap(bitmap, scanWidth, scanHeight, true)

        val pixels = IntArray(scanWidth * scanHeight)
        scaled.getPixels(pixels, 0, scanWidth, 0, 0, scanWidth, scanHeight)

        // Compute average luminance
        var totalLum = 0.0
        val lumArray = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            lumArray[i] = lum
            totalLum += lum
        }
        val avgLum = (totalLum / pixels.size).toFloat()

        // Threshold based on sensitivity (0.1 to 0.9)
        val diffThreshold = 35f * (1.2f - sensitivity)
        val minDistance = (8f * (1.2f - sensitivity)).toInt().coerceAtLeast(4)

        var pinIndex = 1
        for (y in 5 until scanHeight - 5 step 3) {
            for (x in 5 until scanWidth - 5 step 3) {
                val idx = y * scanWidth + x
                val diff = Math.abs(lumArray[idx] - avgLum)

                if (diff > diffThreshold) {
                    // Check local maximum
                    var isLocalPeak = true
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val nIdx = (y + dy) * scanWidth + (x + dx)
                            if (nIdx in lumArray.indices && Math.abs(lumArray[nIdx] - avgLum) > diff) {
                                isLocalPeak = false
                                break
                            }
                        }
                        if (!isLocalPeak) break
                    }

                    if (isLocalPeak) {
                        // Check distance from existing pins
                        val relX = x.toFloat() / scanWidth.toFloat()
                        val relY = y.toFloat() / scanHeight.toFloat()
                        val tooClose = pins.any { pin ->
                            val dxPx = (pin.relX - relX) * scanWidth
                            val dyPx = (pin.relY - relY) * scanHeight
                            Math.hypot(dxPx.toDouble(), dyPx.toDouble()) < minDistance
                        }

                        if (!tooClose) {
                            pins.add(
                                CountPin(
                                    id = pinIndex++,
                                    relX = relX,
                                    relY = relY,
                                    label = "Obj #$pinIndex"
                                )
                            )
                            if (pins.size >= maxItems) break
                        }
                    }
                }
            }
            if (pins.size >= maxItems) break
        }

        return pins
    }

    /**
     * Renders numbered circular pin markers onto a copy of the source bitmap.
     */
    fun renderPinnedBitmap(
        sourceBitmap: Bitmap,
        pins: List<CountPin>
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val pinRadius = (minOf(sourceBitmap.width, sourceBitmap.height) * 0.028f).coerceIn(16f, 44f)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = pinRadius * 0.18f
            color = Color.WHITE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = pinRadius * 1.05f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        pins.forEach { pin ->
            val cx = pin.relX * sourceBitmap.width
            val cy = pin.relY * sourceBitmap.height

            circlePaint.color = pin.colorArgb
            canvas.drawCircle(cx, cy, pinRadius, circlePaint)
            canvas.drawCircle(cx, cy, pinRadius, borderPaint)

            val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText("${pin.id}", cx, textY, textPaint)
        }

        return result
    }

    /**
     * Saves the pinned count report image to disk.
     */
    fun savePinnedImage(bitmap: Bitmap, outputFile: File): Boolean {
        return try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
