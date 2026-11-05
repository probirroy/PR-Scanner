package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SecurityRepository
import com.example.prscanner.data.repository.SubscriptionRepository
import com.example.prscanner.ui.PrScannerApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val documentRepository by lazy { DocumentRepository(applicationContext) }
    private val subscriptionRepository by lazy { SubscriptionRepository(applicationContext) }
    private val securityRepository by lazy { SecurityRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PrScannerApp(
                        documentRepository = documentRepository,
                        subscriptionRepository = subscriptionRepository,
                        securityRepository = securityRepository
                    )
                }
            }
        }
    }
}
