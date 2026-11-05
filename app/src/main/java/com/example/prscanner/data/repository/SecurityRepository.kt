package com.example.prscanner.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pr_security_prefs", Context.MODE_PRIVATE)

    private val _isPinSet = MutableStateFlow(!prefs.getString("app_pin", null).isNullOrBlank())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _isLocked = MutableStateFlow(!prefs.getString("app_pin", null).isNullOrBlank())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    fun setPin(pin: String) {
        if (pin.length == 4) {
            prefs.edit().putString("app_pin", pin).apply()
            _isPinSet.value = true
            _isLocked.value = false
        }
    }

    fun removePin() {
        prefs.edit().remove("app_pin").apply()
        _isPinSet.value = false
        _isLocked.value = false
    }

    fun verifyPin(input: String): Boolean {
        val stored = prefs.getString("app_pin", "")
        val match = stored == input
        if (match) {
            _isLocked.value = false
        }
        return match
    }

    fun lockApp() {
        if (_isPinSet.value) {
            _isLocked.value = true
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _biometricEnabled.value = enabled
    }
}
