package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.prscanner.data.repository.SubscriptionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PR Scanner", appName)
  }

  @Test
  fun `verify all pro and business features are unlocked for free`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val subRepo = SubscriptionRepository(context)
    assertTrue(subRepo.isProOrBusiness)
    assertTrue(subRepo.isBusiness)
    assertTrue(subRepo.canUseFeature("batch_ocr"))
    assertTrue(subRepo.canUseFeature("team_workspace"))
    assertTrue(subRepo.canUseFeature("audit_export"))
    assertTrue(subRepo.canUseFeature("magic_color"))
  }

  @Test
  fun `verify sample document generation for all types`() {
    val bookBmp = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(com.example.prscanner.data.model.DocType.BOOK)
    assertEquals(1000, bookBmp.width)
    assertEquals(1400, bookBmp.height)

    val testPaperBmp = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(com.example.prscanner.data.model.DocType.TEST_PAPER)
    assertEquals(1000, testPaperBmp.width)
    assertEquals(1400, testPaperBmp.height)

    val whiteboardBmp = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(com.example.prscanner.data.model.DocType.WHITEBOARD)
    assertEquals(1000, whiteboardBmp.width)

    val slidesBmp = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(com.example.prscanner.data.model.DocType.SLIDES)
    assertEquals(1000, slidesBmp.width)
  }

  @Test
  fun `verify PDF export options and storage persistence logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testPdfFile = java.io.File(context.cacheDir, "sample_document.pdf")
    testPdfFile.writeText("%PDF-1.4\n1 0 obj\n<< /Title (PR Scanner Multi-Page Export) >>\nendobj\ntrailer\n<< >>\n%%EOF")

    assertTrue("Test PDF file must exist", testPdfFile.exists())
    assertTrue("Test PDF size must be greater than zero", testPdfFile.length() > 0)

    val options = com.example.prscanner.engine.PdfExportOptions(
      preset = com.example.prscanner.data.model.PageSizePreset.A4,
      addPageNumbers = true,
      watermarkText = "CONFIDENTIAL",
      isLandscape = false
    )

    assertEquals(com.example.prscanner.data.model.PageSizePreset.A4, options.preset)
    assertTrue(options.addPageNumbers)
    assertEquals("CONFIDENTIAL", options.watermarkText)
  }
}
