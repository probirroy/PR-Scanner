package com.example.prscanner.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Nav Tabs
    data object Home : Screen("home")
    data object Documents : Screen("documents")
    data object Tools : Screen("tools")
    data object Cloud : Screen("cloud")
    data object Profile : Screen("profile")

    // Feature Screens
    data object CameraScanner : Screen("camera_scanner/{scanMode}") {
        fun createRoute(scanMode: String = "DOCUMENTS") = "camera_scanner/$scanMode"
    }

    data object CropEditor : Screen("crop_editor/{documentId}/{pageId}") {
        fun createRoute(documentId: Long, pageId: Long) = "crop_editor/$documentId/$pageId"
    }

    data object Enhancement : Screen("enhancement/{documentId}/{pageId}") {
        fun createRoute(documentId: Long, pageId: Long) = "enhancement/$documentId/$pageId"
    }

    data object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: Long) = "document_detail/$documentId"
    }

    data object OcrResult : Screen("ocr_result/{documentId}") {
        fun createRoute(documentId: Long) = "ocr_result/$documentId"
    }

    data object AiAssistant : Screen("ai_assistant/{documentId}") {
        fun createRoute(documentId: Long = 0L) = "ai_assistant/$documentId"
    }

    data object PdfViewer : Screen("pdf_viewer/{documentId}") {
        fun createRoute(documentId: Long) = "pdf_viewer/$documentId"
    }

    data object SignatureManager : Screen("signature_manager")

    // New Image Feature Routes
    data object Measure : Screen("measure")
    data object CountCam : Screen("count_cam")
    data object Scrollshot : Screen("scrollshot")
    data object ImageToText : Screen("image_to_text")
    data object PdfConverter : Screen("pdf_converter/{toolAction}") {
        fun createRoute(toolAction: String = "PDF_TO_WORD") = "pdf_converter/$toolAction"
    }
    data object PdfEditor : Screen("pdf_editor/{toolAction}") {
        fun createRoute(toolAction: String = "MERGE") = "pdf_editor/$toolAction"
    }
}
