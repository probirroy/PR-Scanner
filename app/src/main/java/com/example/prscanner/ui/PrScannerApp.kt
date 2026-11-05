package com.example.prscanner.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.rememberCoroutineScope
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.data.repository.SecurityRepository
import com.example.prscanner.data.repository.SubscriptionRepository
import com.example.prscanner.ui.components.CameraPermissionHandler
import com.example.prscanner.ui.components.PinLockScreen
import com.example.prscanner.ui.navigation.Screen
import com.example.prscanner.ui.screens.ai.AiAssistantScreen
import com.example.prscanner.util.SampleDocumentGenerator
import kotlinx.coroutines.launch
import com.example.prscanner.ui.screens.cloud.CloudScreen
import com.example.prscanner.ui.screens.detail.DocumentDetailScreen
import com.example.prscanner.ui.screens.documents.DocumentsScreen
import com.example.prscanner.ui.screens.editor.CropEditorScreen
import com.example.prscanner.ui.screens.editor.EnhancementScreen
import com.example.prscanner.ui.screens.home.HomeScreen
import com.example.prscanner.ui.screens.ocr.OcrResultScreen
import com.example.prscanner.ui.screens.profile.ProfileScreen
import com.example.prscanner.ui.screens.scanner.CameraScannerScreen
import com.example.prscanner.ui.screens.subscription.SubscriptionScreen
import com.example.prscanner.ui.screens.tools.CountCamScreen
import com.example.prscanner.ui.screens.tools.ImageToTextScreen
import com.example.prscanner.ui.screens.tools.MeasureScreen
import com.example.prscanner.ui.screens.tools.PdfConverterScreen
import com.example.prscanner.ui.screens.tools.PdfEditorScreen
import com.example.prscanner.ui.screens.tools.ScrollshotScreen
import com.example.prscanner.ui.screens.tools.ToolsScreen

@Composable
fun PrScannerApp(
    documentRepository: DocumentRepository,
    subscriptionRepository: SubscriptionRepository,
    securityRepository: SecurityRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLocked by securityRepository.isLocked.collectAsState()

    // If app is PIN-locked, gate entire screen with PinLockScreen
    if (isLocked) {
        PinLockScreen(
            onPinEntered = { pin ->
                securityRepository.verifyPin(pin)
            }
        )
        return
    }

    val bottomBarScreens = listOf(
        Screen.Home,
        Screen.Documents,
        Screen.Tools,
        Screen.Cloud,
        Screen.Profile
    )

    val showBottomBar = currentRoute in bottomBarScreens.map { it.route }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav")
                ) {
                    bottomBarScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val (icon, label) = when (screen) {
                            Screen.Home -> (if (selected) Icons.Filled.Home else Icons.Outlined.Home) to "Home"
                            Screen.Documents -> (if (selected) Icons.Filled.Description else Icons.Outlined.Description) to "Documents"
                            Screen.Tools -> (if (selected) Icons.Filled.Build else Icons.Outlined.Build) to "Tools"
                            Screen.Cloud -> (if (selected) Icons.Filled.Cloud else Icons.Outlined.Cloud) to "Cloud"
                            Screen.Profile -> (if (selected) Icons.Filled.Person else Icons.Outlined.Person) to "Profile"
                            else -> Icons.Default.Home to ""
                        }

                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    documentRepository = documentRepository,
                    subscriptionRepository = subscriptionRepository,
                    onNavigateToCamera = { mode ->
                        navController.navigate(Screen.CameraScanner.createRoute(mode))
                    },
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId))
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Documents.route)
                    },
                    onNavigateToConverter = { action ->
                        navController.navigate(Screen.PdfConverter.createRoute(action))
                    },
                    onNavigateToEditor = { action ->
                        navController.navigate(Screen.PdfEditor.createRoute(action))
                    },
                    onNavigateToMeasure = {
                        navController.navigate(Screen.Measure.route)
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.AiAssistant.createRoute(0L))
                    },
                    onNavigateToScrollshot = {
                        navController.navigate(Screen.Scrollshot.route)
                    },
                    onNavigateToCountCam = {
                        navController.navigate(Screen.CountCam.route)
                    },
                    onNavigateToImageToText = {
                        navController.navigate(Screen.ImageToText.route)
                    },
                    onNavigateToToolsTab = {
                        navController.navigate(Screen.Tools.route)
                    }
                )
            }

            // Documents
            composable(Screen.Documents.route) {
                DocumentsScreen(
                    documentRepository = documentRepository,
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId))
                    }
                )
            }

            // Tools
            composable(Screen.Tools.route) {
                ToolsScreen(
                    documentRepository = documentRepository,
                    securityRepository = securityRepository,
                    onNavigateToCamera = { mode ->
                        navController.navigate(Screen.CameraScanner.createRoute(mode))
                    },
                    onNavigateToDocument = { docId ->
                        navController.navigate(Screen.DocumentDetail.createRoute(docId))
                    },
                    onNavigateToConverter = { action ->
                        navController.navigate(Screen.PdfConverter.createRoute(action))
                    },
                    onNavigateToEditor = { action ->
                        navController.navigate(Screen.PdfEditor.createRoute(action))
                    },
                    onNavigateToMeasure = {
                        navController.navigate(Screen.Measure.route)
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.AiAssistant.createRoute(0L))
                    },
                    onNavigateToScrollshot = {
                        navController.navigate(Screen.Scrollshot.route)
                    },
                    onNavigateToCountCam = {
                        navController.navigate(Screen.CountCam.route)
                    },
                    onNavigateToImageToText = {
                        navController.navigate(Screen.ImageToText.route)
                    }
                )
            }

            // Cloud
            composable(Screen.Cloud.route) {
                CloudScreen(
                    documentRepository = documentRepository,
                    subscriptionRepository = subscriptionRepository
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    documentRepository = documentRepository,
                    subscriptionRepository = subscriptionRepository,
                    securityRepository = securityRepository,
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    }
                )
            }

            // Camera Scanner with CameraPermissionHandler
            composable(
                route = Screen.CameraScanner.route,
                arguments = listOf(navArgument("scanMode") { type = NavType.StringType; defaultValue = "SINGLE" })
            ) { backStackEntry ->
                val scanMode = backStackEntry.arguments?.getString("scanMode") ?: "SINGLE"
                val scope = rememberCoroutineScope()
                CameraPermissionHandler(
                    onNavigateBack = { navController.popBackStack() },
                    onSimulateCapture = {
                        val sampleDocType = when (scanMode) {
                            "ID_CARD" -> DocType.ID_CARD
                            "BOOK" -> DocType.BOOK
                            "TEST_PAPERS" -> DocType.TEST_PAPER
                            "WHITEBOARDS" -> DocType.WHITEBOARD
                            "SLIDES" -> DocType.SLIDES
                            else -> DocType.INVOICE
                        }
                        val sampleTitle = when (scanMode) {
                            "ID_CARD" -> "Scanned ID Card"
                            "BOOK" -> "Scanned Book Pages"
                            "TEST_PAPERS" -> "Scanned Test Paper"
                            "WHITEBOARDS" -> "Captured Whiteboard"
                            "SLIDES" -> "Captured Presentation Slide"
                            else -> "Simulated Document Scan"
                        }
                        val sample = SampleDocumentGenerator.generateSampleBitmap(sampleDocType)
                        scope.launch {
                            val id = documentRepository.createDocumentWithPages(
                                title = sampleTitle,
                                pageBitmaps = listOf(sample),
                                docType = sampleDocType
                            )
                            navController.navigate(Screen.DocumentDetail.createRoute(id)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                ) {
                    CameraScannerScreen(
                        initialScanMode = scanMode,
                        documentRepository = documentRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onScanFinished = { docId ->
                            navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }

            // Crop Editor
            composable(
                route = Screen.CropEditor.route,
                arguments = listOf(
                    navArgument("documentId") { type = NavType.LongType },
                    navArgument("pageId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
                CropEditorScreen(
                    documentId = docId,
                    pageId = pageId,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEnhance = {
                        navController.navigate(Screen.Enhancement.createRoute(docId, pageId)) {
                            popUpTo(Screen.CropEditor.createRoute(docId, pageId)) { inclusive = true }
                        }
                    }
                )
            }

            // Enhancement
            composable(
                route = Screen.Enhancement.route,
                arguments = listOf(
                    navArgument("documentId") { type = NavType.LongType },
                    navArgument("pageId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
                EnhancementScreen(
                    documentId = docId,
                    pageId = pageId,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onFinish = { navController.popBackStack() }
                )
            }

            // Document Detail
            composable(
                route = Screen.DocumentDetail.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                DocumentDetailScreen(
                    documentId = docId,
                    documentRepository = documentRepository,
                    subscriptionRepository = subscriptionRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCrop = { dId, pId ->
                        navController.navigate(Screen.CropEditor.createRoute(dId, pId))
                    },
                    onNavigateToEnhance = { dId, pId ->
                        navController.navigate(Screen.Enhancement.createRoute(dId, pId))
                    },
                    onNavigateToOcr = { dId ->
                        navController.navigate(Screen.OcrResult.createRoute(dId))
                    },
                    onNavigateToAi = { dId ->
                        navController.navigate(Screen.AiAssistant.createRoute(dId))
                    },
                    onAddPageCamera = {
                        navController.navigate(Screen.CameraScanner.createRoute("BATCH"))
                    }
                )
            }

            // OCR Result
            composable(
                route = Screen.OcrResult.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                OcrResultScreen(
                    documentId = docId,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAi = { dId ->
                        navController.navigate(Screen.AiAssistant.createRoute(dId))
                    }
                )
            }

            // AI Assistant
            composable(
                route = Screen.AiAssistant.route,
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                AiAssistantScreen(
                    documentId = docId,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Smart Measure
            composable(Screen.Measure.route) {
                MeasureScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // CountCam
            composable(Screen.CountCam.route) {
                CountCamScreen(
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) }
                )
            }

            // Scrollshot
            composable(Screen.Scrollshot.route) {
                ScrollshotScreen(
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) }
                )
            }

            // Image to Text (OCR)
            composable(Screen.ImageToText.route) {
                ImageToTextScreen(
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) },
                    onNavigateToCamera = { mode -> navController.navigate(Screen.CameraScanner.createRoute(mode)) }
                )
            }

            // PDF & Office Converter (8 tools)
            composable(
                route = Screen.PdfConverter.route,
                arguments = listOf(navArgument("toolAction") { type = NavType.StringType; defaultValue = "PDF_TO_WORD" })
            ) { backStackEntry ->
                val action = backStackEntry.arguments?.getString("toolAction") ?: "PDF_TO_WORD"
                PdfConverterScreen(
                    initialAction = action,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) }
                )
            }

            // PDF Editor Studio (8 tools)
            composable(
                route = Screen.PdfEditor.route,
                arguments = listOf(navArgument("toolAction") { type = NavType.StringType; defaultValue = "MERGE" })
            ) { backStackEntry ->
                val action = backStackEntry.arguments?.getString("toolAction") ?: "MERGE"
                PdfEditorScreen(
                    initialAction = action,
                    documentRepository = documentRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Screen.DocumentDetail.createRoute(id)) }
                )
            }

            // Subscription
            composable(Screen.Subscription.route) {
                SubscriptionScreen(
                    subscriptionRepository = subscriptionRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
