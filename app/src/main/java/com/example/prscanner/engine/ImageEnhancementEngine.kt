package com.example.prscanner.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.example.prscanner.data.model.FilterType
import kotlin.math.max
import kotlin.math.min

object ImageEnhancementEngine {

    fun applyAdjustmentsAndFilter(
        source: Bitmap,
        filter: FilterType,
        brightness: Float = 0f, // -100f to +100f
        contrast: Float = 1f,   // 0.5f to 2.0f
        rotationDegrees: Int = 0
    ): Bitmap {
        var result = source

        // 1. Rotation if needed
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        }

        // 2. Base Filter Transformation
        result = when (filter) {
            FilterType.ORIGINAL -> result.copy(Bitmap.Config.ARGB_8888, true)
            FilterType.BW -> applyBlackAndWhite(result)
            FilterType.VIVID -> applyVivid(result)
            FilterType.MAGIC_COLOR -> applyMagicColor(result)
            FilterType.DOCUMENT -> applyDocumentMode(result)
            FilterType.AUTO_ENHANCE -> applyAutoEnhance(result)
            FilterType.GRAYSCALE -> applyGrayscale(result)
            FilterType.LIGHTEN -> applyBrightnessOffset(result, 35f)
            FilterType.DARKEN -> applyBrightnessOffset(result, -35f)
            FilterType.SHARPEN -> applySharpen(result)
            FilterType.HIGH_CONTRAST -> applyHighContrast(result)
        }

        // 3. User Brightness & Contrast fine tuning
        if (brightness != 0f || contrast != 1f) {
            result = applyBrightnessContrast(result, brightness, contrast)
        }

        return result
    }

    /**
     * Automatically analyzes document luminance and edge contrast to detect the relevant
     * document area, returning the cropped bitmap and the detected boundary RectF.
     */
    fun autoCropBitmap(source: Bitmap): Pair<Bitmap, RectF> {
        val bounds = detectDocumentBounds(source)
        val cropped = cropBitmap(source, bounds)
        return Pair(cropped, bounds)
    }

    /**
     * Detects document boundaries by sampling luminance gradients and paper contrast.
     * Returns a normalized RectF with left, top, right, bottom coordinates in [0..1].
     */
    fun detectDocumentBounds(source: Bitmap): RectF {
        val sampleW = 100
        val sampleH = 100
        val scaled = Bitmap.createScaledBitmap(source, sampleW, sampleH, false)
        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        // Calculate average luminance across center vs borders
        val lums = DoubleArray(sampleW * sampleH) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            0.299 * r + 0.587 * g + 0.114 * b
        }

        // Measure row-by-row and col-by-col luminance variance
        var top = 5
        var bottom = 95
        var left = 5
        var right = 95

        // Scan from top down
        for (y in 0 until 35) {
            var rowAvg = 0.0
            for (x in 20 until 80) rowAvg += lums[y * sampleW + x]
            rowAvg /= 60.0
            if (rowAvg > 65.0) { // Found document content/paper threshold
                top = max(4, y)
                break
            }
        }

        // Scan from bottom up
        for (y in 99 downTo 65) {
            var rowAvg = 0.0
            for (x in 20 until 80) rowAvg += lums[y * sampleW + x]
            rowAvg /= 60.0
            if (rowAvg > 65.0) {
                bottom = min(96, y)
                break
            }
        }

        // Scan from left to right
        for (x in 0 until 35) {
            var colAvg = 0.0
            for (y in 20 until 80) colAvg += lums[y * sampleW + x]
            colAvg /= 60.0
            if (colAvg > 65.0) {
                left = max(4, x)
                break
            }
        }

        // Scan from right to left
        for (x in 99 downTo 65) {
            var colAvg = 0.0
            for (y in 20 until 80) colAvg += lums[y * sampleW + x]
            colAvg /= 60.0
            if (colAvg > 65.0) {
                right = min(96, x)
                break
            }
        }

        // Safety fallback bounds: ensure at least 40% width and 40% height
        val normLeft = (left / 100f).coerceIn(0.02f, 0.25f)
        val normTop = (top / 100f).coerceIn(0.02f, 0.25f)
        val normRight = (right / 100f).coerceIn(0.75f, 0.98f)
        val normBottom = (bottom / 100f).coerceIn(0.75f, 0.98f)

        return RectF(normLeft, normTop, normRight, normBottom)
    }

    fun cropBitmap(source: Bitmap, cropRect: RectF): Bitmap {
        val left = max(0, (cropRect.left * source.width).toInt())
        val top = max(0, (cropRect.top * source.height).toInt())
        val width = min(source.width - left, ((cropRect.right - cropRect.left) * source.width).toInt())
        val height = min(source.height - top, ((cropRect.bottom - cropRect.top) * source.height).toInt())

        if (width <= 0 || height <= 0) return source
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    private fun applyVivid(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vivid: boosts vibrancy, saturation (1.4x), and high-contrast clean paper whites
        val cm = ColorMatrix()
        cm.setSaturation(1.4f)

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                1.35f, 0f, 0f, 0f, -25f,
                0f, 1.35f, 0f, 0f, -25f,
                0f, 0f, 1.35f, 0f, -25f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyMagicColor(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Magic color increases saturation, contrast, and clarifies paper whites
        val cm = ColorMatrix()
        cm.setSaturation(1.35f)

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                1.25f, 0f, 0f, 0f, -20f,
                0f, 1.25f, 0f, 0f, -20f,
                0f, 0f, 1.25f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyDocumentMode(src: Bitmap): Bitmap {
        // High clarity document mode: clean white background with punchy dark text
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                1.4f, 0f, 0f, 0f, -40f,
                0f, 1.4f, 0f, 0f, -40f,
                0f, 0f, 1.4f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyGrayscale(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyBlackAndWhite(src: Bitmap): Bitmap {
        // Crisp binary thresholding
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalLum = 0L
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalLum += (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        val avgLum = (totalLum / (width * height)).toInt()
        val threshold = min(200, max(80, (avgLum * 0.95).toInt()))

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            pixels[i] = if (lum > threshold) Color.WHITE else Color.BLACK
        }

        dest.setPixels(pixels, 0, width, 0, 0, width, height)
        return dest
    }

    private fun applyAutoEnhance(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 10f,
                0f, 1.15f, 0f, 0f, 10f,
                0f, 0f, 1.15f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyHighContrast(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                1.6f, 0f, 0f, 0f, -60f,
                0f, 1.6f, 0f, 0f, -60f,
                0f, 0f, 1.6f, 0f, -60f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applySharpen(src: Bitmap): Bitmap {
        // Fast 3x3 sharpening convolution
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(width * height)
        val destPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = srcPixels[idx]
                val top = srcPixels[idx - width]
                val bottom = srcPixels[idx + width]
                val left = srcPixels[idx - 1]
                val right = srcPixels[idx + 1]

                val r = (Color.red(center) * 5 - Color.red(top) - Color.red(bottom) - Color.red(left) - Color.red(right)).coerceIn(0, 255)
                val g = (Color.green(center) * 5 - Color.green(top) - Color.green(bottom) - Color.green(left) - Color.green(right)).coerceIn(0, 255)
                val b = (Color.blue(center) * 5 - Color.blue(top) - Color.blue(bottom) - Color.blue(left) - Color.blue(right)).coerceIn(0, 255)

                destPixels[idx] = Color.argb(Color.alpha(center), r, g, b)
            }
        }
        dest.setPixels(destPixels, 0, width, 0, 0, width, height)
        return dest
    }

    private fun applyBrightnessOffset(src: Bitmap, offset: Float): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, offset,
                0f, 1f, 0f, 0f, offset,
                0f, 0f, 1f, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyBrightnessContrast(src: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}
