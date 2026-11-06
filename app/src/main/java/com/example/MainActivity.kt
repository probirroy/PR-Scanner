package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SecurityRepository
import com.example.prscanner.data.repository.SubscriptionRepository
import com.example.prscanner.data.repository.ThemeRepository
import com.example.prscanner.ui.PrScannerApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val documentRepository by lazy { DocumentRepository(applicationContext) }
    private val subscriptionRepository by lazy { SubscriptionRepository(applicationContext) }
    private val securityRepository by lazy { SecurityRepository(applicationContext) }
    private val themeRepository by lazy { ThemeRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by themeRepository.currentTheme.collectAsState()

            MyApplicationTheme(themeMode = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PrScannerApp(
                        documentRepository = documentRepository,
                        subscriptionRepository = subscriptionRepository,
                        securityRepository = securityRepository,
                        themeRepository = themeRepository
                    )
                }
            }
        }
    }
}
