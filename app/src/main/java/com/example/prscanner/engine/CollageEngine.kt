package com.example.prscanner.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object CollageEngine {

    /**
     * Stitches Front and Back of an ID card onto a single clean portrait sheet (A4/Letter ratio)
     */
    fun createIdCardCollage(frontBitmap: Bitmap, backBitmap: Bitmap): Bitmap {
        val width = 1200
        val height = 1600
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Background paper white
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E3A8A.toInt()
            style = Paint.Style.FILL
        }
        val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Title at top
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 38f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("IDENTITY DOCUMENT SCAN", width / 2f, 100f, titlePaint)

        // Card 1 (Front)
        val cardMargin = 80f
        val cardWidth = width - 2 * cardMargin
        val cardHeight = 550f

        // Front Header
        val frontLabelRect = RectF(cardMargin, 160f, cardMargin + 200f, 210f)
        canvas.drawRoundRect(frontLabelRect, 12f, 12f, labelBgPaint)
        canvas.drawText("FRONT SIDE", frontLabelRect.centerX(), 196f, labelTextPaint)

        // Front Image
        val frontRect = RectF(cardMargin, 230f, cardMargin + cardWidth, 230f + cardHeight)
        canvas.drawRoundRect(frontRect, 16f, 16f, borderPaint)
        drawScaledFit(canvas, frontBitmap, frontRect, bitmapPaint)

        // Card 2 (Back)
        val backTop = 870f
        val backLabelRect = RectF(cardMargin, backTop, cardMargin + 200f, backTop + 50f)
        canvas.drawRoundRect(backLabelRect, 12f, 12f, labelBgPaint)
        canvas.drawText("BACK SIDE", backLabelRect.centerX(), backTop + 36f, labelTextPaint)

        // Back Image
        val backRect = RectF(cardMargin, backTop + 70f, cardMargin + cardWidth, backTop + 70f + cardHeight)
        canvas.drawRoundRect(backRect, 16f, 16f, borderPaint)
        drawScaledFit(canvas, backBitmap, backRect, bitmapPaint)

        return result
    }

    /**
     * Combines 2 pages side-by-side or 4 pages in a 2x2 grid
     */
    fun createMultiPageCollage(bitmaps: List<Bitmap>): Bitmap {
        if (bitmaps.isEmpty()) return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        if (bitmaps.size == 1) return bitmaps[0]

        val width = 1600
        val height = 2200
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val margin = 40f

        if (bitmaps.size == 2) {
            // 2 pages top and bottom
            val itemHeight = (height - 3 * margin) / 2f
            val itemWidth = width - 2 * margin
            val rect1 = RectF(margin, margin, margin + itemWidth, margin + itemHeight)
            val rect2 = RectF(margin, margin * 2 + itemHeight, margin + itemWidth, height - margin)
            drawScaledFit(canvas, bitmaps[0], rect1, paint)
            drawScaledFit(canvas, bitmaps[1], rect2, paint)
        } else {
            // 4 pages (2x2 grid)
            val itemW = (width - 3 * margin) / 2f
            val itemH = (height - 3 * margin) / 2f

            val rects = listOf(
                RectF(margin, margin, margin + itemW, margin + itemH),
                RectF(margin * 2 + itemW, margin, width - margin, margin + itemH),
                RectF(margin, margin * 2 + itemH, margin + itemW, height - margin),
                RectF(margin * 2 + itemW, margin * 2 + itemH, width - margin, height - margin)
            )

            for (i in 0 until minOf(4, bitmaps.size)) {
                drawScaledFit(canvas, bitmaps[i], rects[i], paint)
            }
        }

        return result
    }

    private fun drawScaledFit(canvas: Canvas, bitmap: Bitmap, target: RectF, paint: Paint) {
        val scale = minOf(target.width() / bitmap.width.toFloat(), target.height() / bitmap.height.toFloat())
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val left = target.left + (target.width() - scaledW) / 2f
        val top = target.top + (target.height() - scaledH) / 2f
        val dest = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(bitmap, null, dest, paint)
    }
}
