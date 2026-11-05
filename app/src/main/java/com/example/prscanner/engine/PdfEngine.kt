package com.example.prscanner.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.prscanner.data.model.PageSizePreset
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

data class PdfExportOptions(
    val preset: PageSizePreset = PageSizePreset.A4,
    val isLandscape: Boolean = false,
    val marginPt: Int = 20,
    val addPageNumbers: Boolean = true,
    val watermarkText: String? = null,
    val signatureBitmap: Bitmap? = null,
    val compressionQuality: Int = 90
)

object PdfEngine {

    fun generatePdf(
        bitmaps: List<Bitmap>,
        outputFile: File,
        options: PdfExportOptions = PdfExportOptions()
    ): Boolean {
        if (bitmaps.isEmpty()) return false

        val document = PdfDocument()

        val pageWidth = if (options.isLandscape) options.preset.heightPt else options.preset.widthPt
        val pageHeight = if (options.isLandscape) options.preset.widthPt else options.preset.heightPt

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 100, 100, 100)
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        try {
            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Background white
                canvas.drawColor(Color.WHITE)

                // Usable printable area
                val printableLeft = options.marginPt.toFloat()
                val printableTop = options.marginPt.toFloat()
                val printableRight = (pageWidth - options.marginPt).toFloat()
                val printableBottom = if (options.addPageNumbers) (pageHeight - options.marginPt - 20).toFloat() else (pageHeight - options.marginPt).toFloat()

                val availableWidth = printableRight - printableLeft
                val availableHeight = printableBottom - printableTop

                // Calculate aspect ratio preserving fit
                val scale = minOf(
                    availableWidth / bitmap.width.toFloat(),
                    availableHeight / bitmap.height.toFloat()
                )

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val destLeft = printableLeft + (availableWidth - scaledWidth) / 2f
                val destTop = printableTop + (availableHeight - scaledHeight) / 2f
                val destRect = RectF(destLeft, destTop, destLeft + scaledWidth, destTop + scaledHeight)

                canvas.drawBitmap(bitmap, null, destRect, paint)

                // Draw Watermark if specified
                options.watermarkText?.let { wmText ->
                    if (wmText.isNotBlank()) {
                        canvas.save()
                        canvas.rotate(-45f, pageWidth / 2f, pageHeight / 2f)
                        canvas.drawText(wmText, pageWidth / 2f, pageHeight / 2f, watermarkPaint)
                        canvas.restore()
                    }
                }

                // Draw E-Signature if requested on final page
                if (index == bitmaps.size - 1 && options.signatureBitmap != null) {
                    val sig = options.signatureBitmap
                    val sigScale = minOf(140f / sig.width, 60f / sig.height)
                    val sigW = sig.width * sigScale
                    val sigH = sig.height * sigScale
                    val sigLeft = printableRight - sigW - 10f
                    val sigTop = printableBottom - sigH - 10f
                    canvas.drawBitmap(sig, null, RectF(sigLeft, sigTop, sigLeft + sigW, sigTop + sigH), paint)
                }

                // Page numbering footer
                if (options.addPageNumbers) {
                    val footerText = "Page ${index + 1} of ${bitmaps.size}  •  PR Scanner"
                    canvas.drawText(footerText, pageWidth / 2f, pageHeight - 12f, textPaint)
                }

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            document.close()
        }
    }

    fun renderPdfPagesToBitmaps(pdfFile: File, maxPages: Int = 20): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        if (!pdfFile.exists()) return bitmaps

        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = minOf(renderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val width = page.width * 2 // 2x density for crisp preview
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String = "PR_Scanner_Document") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Destination is null")
                    return
                }

                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: IOException) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
