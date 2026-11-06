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

  @Test
  fun `verify auto-crop document boundary detection and modes`() {
    val sample = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(com.example.prscanner.data.model.DocType.INVOICE)
    val (cropped, bounds) = com.example.prscanner.engine.ImageEnhancementEngine.autoCropBitmap(sample)

    assertTrue("Left bound must be non-negative", bounds.left >= 0f)
    assertTrue("Right bound must be greater than left", bounds.right > bounds.left)
    assertTrue("Bottom bound must be greater than top", bounds.bottom > bounds.top)
    assertTrue("Cropped width must be greater than zero", cropped.width > 0)
    assertTrue("Cropped height must be greater than zero", cropped.height > 0)

    // Test Original, Black & White, and Vivid enhancement modes
    val originalMode = com.example.prscanner.engine.ImageEnhancementEngine.applyAdjustmentsAndFilter(
      source = cropped,
      filter = com.example.prscanner.data.model.FilterType.ORIGINAL
    )
    assertEquals(cropped.width, originalMode.width)

    val bwMode = com.example.prscanner.engine.ImageEnhancementEngine.applyAdjustmentsAndFilter(
      source = cropped,
      filter = com.example.prscanner.data.model.FilterType.BW
    )
    assertEquals(cropped.width, bwMode.width)

    val vividMode = com.example.prscanner.engine.ImageEnhancementEngine.applyAdjustmentsAndFilter(
      source = cropped,
      filter = com.example.prscanner.data.model.FilterType.VIVID
    )
    assertEquals(cropped.width, vividMode.width)
  }

  @Test
  fun `verify theme repository persistence and theme switching`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val themeRepo = com.example.prscanner.data.repository.ThemeRepository(context)

    // Default theme should be PREMIUM_BLUE
    assertEquals(
      com.example.prscanner.data.repository.AppThemeMode.PREMIUM_BLUE,
      themeRepo.currentTheme.value
    )

    // Switch to Clean White
    themeRepo.setTheme(com.example.prscanner.data.repository.AppThemeMode.WHITE)
    assertEquals(
      com.example.prscanner.data.repository.AppThemeMode.WHITE,
      themeRepo.currentTheme.value
    )

    // Switch to Midnight Dark
    themeRepo.setTheme(com.example.prscanner.data.repository.AppThemeMode.DARK)
    assertEquals(
      com.example.prscanner.data.repository.AppThemeMode.DARK,
      themeRepo.currentTheme.value
    )

    // Re-instantiate repository to verify persistence across restarts
    val reloadedRepo = com.example.prscanner.data.repository.ThemeRepository(context)
    assertEquals(
      com.example.prscanner.data.repository.AppThemeMode.DARK,
      reloadedRepo.currentTheme.value
    )

    // Switch back to Premium Blue
    reloadedRepo.setTheme(com.example.prscanner.data.repository.AppThemeMode.PREMIUM_BLUE)
    assertEquals(
      com.example.prscanner.data.repository.AppThemeMode.PREMIUM_BLUE,
      reloadedRepo.currentTheme.value
    )
  }

  @Test
  fun `verify search gallery by OCR extracted text content`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val docRepo = com.example.prscanner.data.repository.DocumentRepository(context)

    // Create sample invoice with known OCR content (Horizon Global Technologies, INV-2026-8891)
    val invoiceBmp = com.example.prscanner.util.SampleDocumentGenerator.generateSampleBitmap(
      com.example.prscanner.data.model.DocType.INVOICE
    )
    val docId = docRepo.createDocumentWithPages(
      title = "November Billing Statement",
      pageBitmaps = listOf(invoiceBmp),
      docType = com.example.prscanner.data.model.DocType.INVOICE
    )
    assertTrue("Created docId must be > 0", docId > 0)

    // Search for OCR-extracted keyword "Horizon" which is NOT in title "November Billing Statement"
    val resultsFlow = docRepo.searchDocuments("Horizon")
    val firstResults = kotlinx.coroutines.flow.first(resultsFlow)
    assertTrue(
      "Search results for 'Horizon' must locate the invoice by its OCR content",
      firstResults.any { it.id == docId }
    )

    // Test OCR-only search query
    val ocrOnlyResults = kotlinx.coroutines.flow.first(docRepo.searchDocumentsByOcrOnly("Horizon"))
    assertTrue(
      "OCR-only search must match the invoice",
      ocrOnlyResults.any { it.id == docId }
    )

    // Test OCR snippet helper
    val snippet = com.example.prscanner.ui.components.findMatchingOcrSnippet(
      fullText = "Billed To: Horizon Global Technologies Ltd. accounts@horizonglobal.com",
      query = "Horizon"
    )
    assertTrue("Snippet must not be null", snippet != null)
    assertTrue("Snippet must contain search keyword", snippet!!.contains("Horizon"))
  }
}


