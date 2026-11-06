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
            DocType.BOOK -> {
                // Dual-page book spread with center gutter
                canvas.drawText("CHAPTER 4: MODERN SOFTWARE ARCHITECTURE", 60f, 120f, headerPaint)
                canvas.drawLine(width / 2f, 150f, width / 2f, height - 100f, borderPaint)

                // Left page text
                canvas.drawText("Page 48", 80f, 180f, subHeaderPaint)
                canvas.drawText("4.1 System Modularity", 80f, 240f, accentPaint)
                canvas.drawText("Building scalable Android applications", 80f, 300f, textPaint)
                canvas.drawText("requires strict separation of concerns.", 80f, 340f, textPaint)
                canvas.drawText("Jetpack Compose declarative UI pairs", 80f, 380f, textPaint)
                canvas.drawText("naturally with Unidirectional Data Flow.", 80f, 420f, textPaint)
                canvas.drawText("State hoisting ensures testability and", 80f, 460f, textPaint)
                canvas.drawText("predictable rendering pipelines.", 80f, 500f, textPaint)

                // Right page text
                val rx = width / 2f + 40f
                canvas.drawText("Page 49", rx, 180f, subHeaderPaint)
                canvas.drawText("4.2 On-Device ML & Privacy", rx, 240f, accentPaint)
                canvas.drawText("Processing optical text recognition", rx, 300f, textPaint)
                canvas.drawText("directly on device safeguards user", rx, 340f, textPaint)
                canvas.drawText("privacy while guaranteeing zero-latency", rx, 380f, textPaint)
                canvas.drawText("document analysis without internet.", rx, 420f, textPaint)
                canvas.drawText("Curvature correction removes page gutters.", rx, 460f, textPaint)
            }
            DocType.TEST_PAPER -> {
                canvas.drawText("COMPUTER SCIENCE MIDTERM EXAM", 80f, 130f, headerPaint)
                canvas.drawText("Course: CS-402  •  Time Allowed: 90 Minutes", 80f, 180f, subHeaderPaint)
                canvas.drawText("Student Name: _________________  ID: ___________", 80f, 230f, textPaint)
                canvas.drawLine(80f, 270f, width - 80f, 270f, borderPaint)

                canvas.drawText("SECTION A: MULTIPLE CHOICE (Answer all questions)", 80f, 330f, accentPaint)
                canvas.drawText("1. Which data structure provides O(1) average lookup?", 80f, 390f, textPaint)
                canvas.drawText("[ A ] Linked List    [ B ] Binary Search Tree", 100f, 440f, subHeaderPaint)
                canvas.drawText("[ C ] Hash Map      [ D ] ArrayDeque", 100f, 480f, subHeaderPaint)

                canvas.drawText("2. What does ACID stand for in database transactions?", 80f, 560f, textPaint)
                canvas.drawText("[ A ] Atomicity, Consistency, Isolation, Durability", 100f, 610f, subHeaderPaint)
                canvas.drawText("[ B ] Access, Control, Identity, Data", 100f, 650f, subHeaderPaint)

                canvas.drawText("SECTION B: CODE ANALYSIS", 80f, 740f, accentPaint)
                canvas.drawText("fun computeFactorial(n: Int): Long = ...", 80f, 800f, textPaint)
                canvas.drawText("Calculate the time complexity of tail recursive factorial.", 80f, 850f, textPaint)
                canvas.drawText("Answer Space: _____________________________________", 80f, 920f, subHeaderPaint)
            }
            DocType.WHITEBOARD -> {
                canvas.drawColor(0xFFF8FAFC.toInt())
                canvas.drawText("WHITEBOARD: SPRINT 24 ARCHITECTURE", 60f, 120f, headerPaint)
                canvas.drawText("Date: Nov 05, 2026 • Lead Architect: Tech Team", 60f, 170f, subHeaderPaint)

                // Whiteboard Boxes
                val boxPaint1 = Paint().apply { color = 0xFF2563EB.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f }
                val boxPaint2 = Paint().apply { color = 0xFF10B981.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f }
                val boxPaint3 = Paint().apply { color = 0xFFF59E0B.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f }

                canvas.drawRoundRect(RectF(80f, 240f, 380f, 420f), 12f, 12f, boxPaint1)
                canvas.drawText("MOBILE CLIENT", 120f, 300f, accentPaint)
                canvas.drawText("CameraX + ML Kit", 120f, 350f, textPaint)

                canvas.drawRoundRect(RectF(500f, 240f, 820f, 420f), 12f, 12f, boxPaint2)
                canvas.drawText("EDGE ENGINE", 530f, 300f, accentPaint)
                canvas.drawText("PDF & DOCX Render", 530f, 350f, textPaint)

                canvas.drawLine(380f, 330f, 500f, 330f, boxPaint3)
                canvas.drawText("--> gRPC / REST -->", 390f, 310f, subHeaderPaint)

                canvas.drawText("• Anti-Glare Optimization Enabled", 80f, 520f, textPaint)
                canvas.drawText("• High Contrast Marker Ink Boosted", 80f, 570f, textPaint)
                canvas.drawText("• Action Items: Export to PDF & Share with Team", 80f, 620f, accentPaint)
            }
            DocType.SLIDES -> {
                canvas.drawColor(0xFF0F172A.toInt())
                val whiteHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 46f; isFakeBoldText = true }
                val whiteSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt(); textSize = 26f }
                val whiteBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2E8F0.toInt(); textSize = 30f }
                val blueAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF38BDF8.toInt(); textSize = 32f; isFakeBoldText = true }

                canvas.drawText("PR SCANNER: ENTERPRISE SUITE", 80f, 160f, whiteHeader)
                canvas.drawText("Q4 2026 Executive Overview & Architecture", 80f, 220f, whiteSub)
                canvas.drawLine(80f, 260f, width - 80f, 260f, whiteSub)

                canvas.drawText("Key Pillars of the Platform:", 80f, 340f, blueAccent)
                canvas.drawText("1. Universal Document Scanner with 7 specialized modes", 80f, 420f, whiteBody)
                canvas.drawText("2. Complete 8-in-1 PDF Converter (Word, Excel, PPT, JPG)", 80f, 490f, whiteBody)
                canvas.drawText("3. Full PDF Editing Studio (Merge, Split, Rotate, Sign)", 80f, 560f, whiteBody)
                canvas.drawText("4. App-Only Toolkit: AR Measure, Ask AI, Scrollshot, CountCam", 80f, 630f, whiteBody)

                canvas.drawText("Confidential • Internal Strategy Deck", 80f, 850f, whiteSub)
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

    fun getSampleDocumentText(type: DocType): String {
        return when (type) {
            DocType.INVOICE -> """
                TAX INVOICE
                Invoice #: INV-2026-8891
                Date: 2026-11-04 • Due Date: 2026-11-18
                Billed To: Horizon Global Technologies Ltd.
                Email: accounts@horizonglobal.com
                Item Description: Enterprise Software Subscription $ 2,450.00
                Item Description: Cloud Security & OCR Architecture $ 1,800.00
                Item Description: AI Document Understanding Suite $ 750.00
                SUBTOTAL: $ 5,000.00
                TAX (10%): $ 500.00
                TOTAL AMOUNT DUE: $ 5,500.00
                Payment Terms: Net 14 Days. Direct Bank Wire Transfer.
            """.trimIndent()

            DocType.RECEIPT -> """
                SUPERMART EXPRESS
                Store #1042 • Tel: (555) 019-2831
                Date: Nov 04, 2026 14:22 PM
                Cashier: Sarah M.
                Organic Milk (1 Gallon) $ 4.89
                Whole Grain Bread $ 3.49
                Premium Roast Coffee Beans $ 14.99
                Fresh Blueberries (2pk) $ 7.50
                TOTAL: $ 30.87
                Paid by Visa Card ending in 4109
                Thank you for shopping with us!
            """.trimIndent()

            DocType.ID_CARD -> """
                REPUBLIC IDENTITY CARD
                National Identity Card
                ID Number: A-9482-1082-US
                Full Name: ALEXANDER J. MORGAN
                Date of Birth: 14 AUG 1988
                Issue Date: 12 JAN 2022 • Expiry: 12 JAN 2032
                Sex: M • Height: 5'11" • Blood: O+ • Class: C
                Holder Signature: Alex Morgan
            """.trimIndent()

            DocType.BOOK -> """
                CHAPTER 4: THE ARCHITECTURE OF KNOWLEDGE
                Page 128
                The nature of written information has transformed civilization across epochs.
                From papyrus scrolls to distributed neural indexing, documents represent
                the collective memory of humanity. Every recorded thought, transaction,
                and discovery forms a persistent thread connecting historical inquiry
                with modern analytical insight.
            """.trimIndent()

            DocType.TEST_PAPER -> """
                ACADEMIC ASSESSMENT EXAMINATION
                Course: Advanced Mathematics & Computer Science
                Total Points: 100 • Time Allowed: 120 Minutes
                Student Name: Jordan Lee • ID: STU-8921-99
                Section A: Computational Complexity & Optimization
                Question 1: Derive the asymptotical lower bound for matrix decomposition.
                Question 2: Explain memory allocation in multi-threaded runtime environments.
            """.trimIndent()

            DocType.WHITEBOARD -> """
                SYSTEM ARCHITECTURE DESIGN REVIEW Q4
                Pipeline: CameraX High-Res Frame Capture
                Edge Detection & Automatic Polygon Perspective Rectification
                Google ML Kit Multilingual OCR Text Extraction
                OpenXML Docx & Native Android PdfRenderer Export Engine
                Zero-Leakage Local Vault with AES Encrypted Storage
            """.trimIndent()

            DocType.SLIDES -> """
                PRESENTATION: EXECUTIVE STRATEGY 2026
                Title: The Future of Autonomous Document Intelligence
                Key Milestones:
                1. 100% Free Pro Scanning Suite for all enterprise users
                2. On-device OCR text search across gallery scans
                3. High-definition multi-page PDF generation & word export
                4. Local secure vault & biometric credential locking
            """.trimIndent()

            DocType.CONTRACT -> """
                MUTUAL NON-DISCLOSURE & DATA PRIVACY AGREEMENT
                Effective Date: November 4, 2026
                Between: PR Scanner Systems Inc. and Horizon Global Partner
                Confidential Information Definition: All proprietary OCR algorithms,
                document artifacts, encrypted schemas, and application source code.
                Term: 3 years from the date of mutual signature execution.
            """.trimIndent()

            else -> """
                PR Scanner General Document Record
                Optical Character Recognition content extracted from scanned pages.
                Searchable text indexing active for quick gallery retrieval.
            """.trimIndent()
        }
    }
}
