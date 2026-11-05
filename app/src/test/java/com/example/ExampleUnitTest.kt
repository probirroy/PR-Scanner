package com.example

import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.PageSizePreset
import com.example.prscanner.data.model.SubscriptionTier
import com.example.prscanner.engine.AiDocumentEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAiInformationExtraction_Invoice() {
        val invoiceText = """
            TAX INVOICE
            Invoice #: INV-2026-8891
            Date: 2026-11-04
            Due Date: 2026-11-18
            Billed To: Horizon Global Technologies Ltd.
            Email: accounts@horizonglobal.com
            Tel: 555-019-2831
            TOTAL AMOUNT DUE: $ 5,500.00
        """.trimIndent()

        val info = AiDocumentEngine.extractInformation(invoiceText)
        assertEquals("Invoice", info.detectedType)
        assertTrue(info.dates.isNotEmpty())
        assertTrue(info.deadlines.isNotEmpty())
        assertTrue(info.financialAmounts.any { it.contains("5,500.00") })
        assertTrue(info.emails.contains("accounts@horizonglobal.com"))
        assertTrue(info.phoneNumbers.contains("555-019-2831"))
    }

    @Test
    fun testAiInformationExtraction_Receipt() {
        val receiptText = """
            SUPERMART EXPRESS
            Store #1042
            Date: Nov 04, 2026
            Subtotal: $ 28.50
            Tax: $ 2.37
            Total: $ 30.87
            Cashier: Sarah M.
        """.trimIndent()

        val info = AiDocumentEngine.extractInformation(receiptText)
        assertEquals("Receipt", info.detectedType)
        assertTrue(info.financialAmounts.any { it.contains("30.87") })
    }

    @Test
    fun testAiInformationExtraction_Contract() {
        val contractText = """
            NON-DISCLOSURE AGREEMENT
            This Agreement is entered into between parties.
            Terms and conditions apply.
            Effective Date: 2026-11-04
        """.trimIndent()

        val info = AiDocumentEngine.extractInformation(contractText)
        assertEquals("Legal Contract", info.detectedType)
    }

    @Test
    fun testPageSizePresetDimensions() {
        assertEquals(595, PageSizePreset.A4.widthPt)
        assertEquals(842, PageSizePreset.A4.heightPt)
        assertEquals(612, PageSizePreset.US_LETTER.widthPt)
        assertEquals(792, PageSizePreset.US_LETTER.heightPt)
    }

    @Test
    fun testSubscriptionTiers() {
        assertEquals("FREE", SubscriptionTier.FREE.badge)
        assertEquals("PRO", SubscriptionTier.PRO.badge)
        assertEquals("BUSINESS", SubscriptionTier.BUSINESS.badge)
    }
}
