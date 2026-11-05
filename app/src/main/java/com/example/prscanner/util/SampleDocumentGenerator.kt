package com.example.prscanner.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.prscanner.data.model.DocType

object SampleDocumentGenerator {

    fun generateSampleBitmap(type: DocType): Bitmap {
        val width = 1000
        val height = 1400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paper white background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(RectF(30f, 30f, width - 30f, height - 30f), borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 28f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E3A8A.toInt()
            textSize = 42f
            isFakeBoldText = true
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 24f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2563EB.toInt()
            textSize = 32f
            isFakeBoldText = true
        }

        when (type) {
            DocType.INVOICE -> {
                canvas.drawText("TAX INVOICE", 80f, 130f, headerPaint)
                canvas.drawText("Invoice #: INV-2026-8891", 80f, 180f, textPaint)
                canvas.drawText("Date: 2026-11-04  •  Due Date: 2026-11-18", 80f, 220f, subHeaderPaint)
                canvas.drawText("Billed To: Horizon Global Technologies Ltd.", 80f, 280f, textPaint)
                canvas.drawText("Email: accounts@horizonglobal.com", 80f, 320f, subHeaderPaint)

                // Divider
                canvas.drawLine(80f, 370f, width - 80f, 370f, borderPaint)
                canvas.drawText("Item Description", 80f, 420f, subHeaderPaint)
                canvas.drawText("Amount (USD)", width - 300f, 420f, subHeaderPaint)

                canvas.drawText("1. Enterprise Software Subscription", 80f, 480f, textPaint)
                canvas.drawText("$ 2,450.00", width - 300f, 480f, textPaint)

                canvas.drawText("2. Cloud Security & OCR Architecture", 80f, 540f, textPaint)
                canvas.drawText("$ 1,800.00", width - 300f, 540f, textPaint)

                canvas.drawText("3. AI Document Understanding Suite", 80f, 600f, textPaint)
                canvas.drawText("$ 750.00", width - 300f, 600f, textPaint)

                canvas.drawLine(80f, 660f, width - 80f, 660f, borderPaint)

                canvas.drawText("SUBTOTAL:  $ 5,000.00", width - 420f, 720f, textPaint)
                canvas.drawText("TAX (10%):  $ 500.00", width - 420f, 770f, textPaint)
                canvas.drawText("TOTAL AMOUNT DUE:  $ 5,500.00", 80f, 850f, accentPaint)

                canvas.drawText("Payment Terms: Net 14 Days. Direct Bank Wire Transfer.", 80f, 960f, subHeaderPaint)
                canvas.drawText("Authorized Signature: _______________________", 80f, 1150f, textPaint)
            }
            DocType.RECEIPT -> {
                canvas.drawText("SUPERMART EXPRESS", width / 2f - 200f, 140f, headerPaint)
                canvas.drawText("Store #1042  •  Tel: (555) 019-2831", width / 2f - 180f, 190f, subHeaderPaint)
                canvas.drawText("Date: Nov 04, 2026 14:22 PM", 120f, 260f, textPaint)
                canvas.drawText("Cashier: Sarah M.", 120f, 300f, subHeaderPaint)
                canvas.drawLine(120f, 340f, width - 120f, 340f, borderPaint)

                canvas.drawText("Organic Milk (1 Gallon)", 120f, 400f, textPaint)
                canvas.drawText("$ 4.89", width - 260f, 400f, textPaint)

                canvas.drawText("Whole Grain Bread", 120f, 460f, textPaint)
                canvas.drawText("$ 3.49", width - 260f, 460f, textPaint)

                canvas.drawText("Premium Roast Coffee Beans", 120f, 520f, textPaint)
                canvas.drawText("$ 14.99", width - 260f, 520f, textPaint)

                canvas.drawText("Fresh Blueberries (2pk)", 120f, 580f, textPaint)
                canvas.drawText("$ 7.50", width - 260f, 580f, textPaint)

                canvas.drawLine(120f, 640f, width - 120f, 640f, borderPaint)
                canvas.drawText("TOTAL: $ 30.87", width - 360f, 720f, accentPaint)
                canvas.drawText("Paid by Visa Card ending in 4109", 120f, 790f, subHeaderPaint)
                canvas.drawText("Thank you for shopping with us!", width / 2f - 190f, 920f, subHeaderPaint)
            }
            DocType.ID_CARD -> {
                canvas.drawText("INTERNATIONAL IDENTITY CARD", 100f, 150f, headerPaint)
                canvas.drawText("Republic Authority of Identification", 100f, 200f, subHeaderPaint)

                // Photo placeholder box
                val photoRect = RectF(100f, 260f, 350f, 580f)
                canvas.drawRoundRect(photoRect, 16f, 16f, borderPaint)
                canvas.drawText("PHOTO", 175f, 430f, subHeaderPaint)

                canvas.drawText("Surname: DOE", 400f, 310f, textPaint)
                canvas.drawText("Given Names: JANE ALEXANDRA", 400f, 370f, textPaint)
                canvas.drawText("Nationality: CITIZEN", 400f, 430f, subHeaderPaint)
                canvas.drawText("Date of Birth: 15 AUG 1992", 400f, 490f, textPaint)
                canvas.drawText("ID Number: A-84920491-X", 400f, 550f, accentPaint)
                canvas.drawText("Expiry Date: 2032-08-15", 400f, 610f, textPaint)

                canvas.drawLine(100f, 700f, width - 100f, 700f, borderPaint)
                canvas.drawText("MACHINE READABLE ZONE (MRZ):", 100f, 760f, subHeaderPaint)
                canvas.drawText("IDUTO<<DOE<JANE<<<<<<<<<<<<<<<<<<<<<<", 100f, 820f, textPaint)
                canvas.drawText("A849204919UTO9208154F3208155<<<<<<<<<<", 100f, 870f, textPaint)
            }
            else -> {
                canvas.drawText("NON-DISCLOSURE AGREEMENT", 80f, 140f, headerPaint)
                canvas.drawText("Confidentiality & Intellectual Property Protocol", 80f, 190f, subHeaderPaint)
                canvas.drawText("Effective Date: November 04, 2026", 80f, 240f, textPaint)

                canvas.drawText("This Agreement is entered into between:", 80f, 320f, textPaint)
                canvas.drawText("Disclosing Party: PR Scanner Technologies Corp.", 80f, 370f, textPaint)
                canvas.drawText("Receiving Party: Authorized Corporate Client.", 80f, 420f, textPaint)

                canvas.drawText("1. Confidential Information Definition:", 80f, 490f, accentPaint)
                canvas.drawText("All proprietary code, scanned artifacts, database models,", 80f, 540f, textPaint)
                canvas.drawText("and OCR telemetry shared under this engagement.", 80f, 580f, textPaint)

                canvas.drawText("2. Term & Termination:", 80f, 650f, accentPaint)
                canvas.drawText("This obligation remains active for 3 years from effective date.", 80f, 700f, textPaint)

                canvas.drawText("Executed by Authorized Representatives:", 80f, 850f, textPaint)
                canvas.drawText("Sign: _________________________  Date: 2026-11-04", 80f, 950f, subHeaderPaint)
            }
        }

        return bitmap
    }
}
