package com.example.prscanner.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

/**
 * Scrollshot stitching engine for combining multiple vertical screenshots or scanned pages
 * into one continuous long screenshot / long document.
 */
object ScrollshotEngine {

    /**
     * Stitches multiple bitmaps vertically into one continuous long image.
     * @param bitmaps ordered list of sequential screenshots
     * @param overlapPx pixels of overlap to trim from subsequent images
     */
    fun stitchVerticalScreenshots(
        bitmaps: List<Bitmap>,
        overlapPx: Int = 0
    ): Bitmap? {
        if (bitmaps.isEmpty()) return null
        if (bitmaps.size == 1) return bitmaps[0]

        // Normalize all bitmaps to the width of the first bitmap
        val targetWidth = bitmaps[0].width
        val scaledBitmaps = bitmaps.map { bmp ->
            if (bmp.width == targetWidth) bmp
            else {
                val scale = targetWidth.toFloat() / bmp.width.toFloat()
                val targetHeight = (bmp.height * scale).toInt()
                Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
            }
        }

        // Calculate total combined height considering overlap
        var totalHeight = scaledBitmaps[0].height
        for (i in 1 until scaledBitmaps.size) {
            val netHeight = maxOf(20, scaledBitmaps[i].height - overlapPx)
            totalHeight += netHeight
        }

        val stitchedBitmap = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stitchedBitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        var currentY = 0
        scaledBitmaps.forEachIndexed { index, bmp ->
            if (index == 0) {
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                currentY += bmp.height
            } else {
                val srcTop = minOf(overlapPx, bmp.height - 10)
                val srcRect = Rect(0, srcTop, bmp.width, bmp.height)
                val destRect = Rect(0, currentY, targetWidth, currentY + (bmp.height - srcTop))
                canvas.drawBitmap(bmp, srcRect, destRect, paint)
                currentY += (bmp.height - srcTop)
            }
        }

        return stitchedBitmap
    }

    /**
     * Saves the stitched long screenshot to a JPEG file.
     */
    fun saveStitchedImage(bitmap: Bitmap, outputFile: File, quality: Int = 92): Boolean {
        return try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
