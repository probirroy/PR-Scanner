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
}
