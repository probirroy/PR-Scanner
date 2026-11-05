package com.example.prscanner.data.repository

import android.content.Context
import com.example.prscanner.data.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pr_subscription_prefs", Context.MODE_PRIVATE)

    // All Pro & Business features are fully unlocked for all users at 100% free!
    private val _currentTier = MutableStateFlow(SubscriptionTier.BUSINESS)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _isAnnualBilling = MutableStateFlow(true)
    val isAnnualBilling: StateFlow<Boolean> = _isAnnualBilling.asStateFlow()

    val isProOrBusiness: Boolean
        get() = true

    val isBusiness: Boolean
        get() = true

    fun upgradeTier(tier: SubscriptionTier, annual: Boolean = false) {
        prefs.edit()
            .putString("sub_tier", tier.name)
            .putBoolean("is_annual", annual)
            .apply()
        _currentTier.value = tier
    }

    fun restorePurchases(): Boolean {
        _currentTier.value = SubscriptionTier.BUSINESS
        return true
    }

    fun canUseFeature(featureKey: String): Boolean {
        // Full free access to all features (Pro, Business, AI, OCR, signatures, collage, export)
        return true
    }
}
