package com.example.prscanner.ui.screens.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import android.content.Intent
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.model.FilterType
import com.example.prscanner.data.model.PageSizePreset
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.ImageEnhancementEngine
import com.example.prscanner.engine.OcrEngine
import com.example.prscanner.engine.PdfEngine
import com.example.prscanner.engine.PdfExportOptions
import com.example.prscanner.util.SampleDocumentGenerator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraScannerScreen(
    initialScanMode: String = "SINGLE",
    documentRepository: DocumentRepository,
    onNavigateBack: () -> Unit,
    onScanFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera and Scan Configuration
    var scanMode by remember { mutableStateOf(initialScanMode) } // SINGLE, BATCH, ID_CARD, BOOK
    var isTorchOn by remember { mutableStateOf(false) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val capturedPages = remember { mutableStateListOf<Bitmap>() }
    var isCapturing by remember { mutableStateOf(false) }

    // Real-time ML Kit OCR Detection State
    var detectedSnippet by remember { mutableStateOf("") }
    var isTextDetected by remember { mutableStateOf(false) }
    var detectedBlockCount by remember { mutableIntStateOf(0) }
    var lastAnalysisTime by remember { mutableStateOf(0L) }

    // Shutter animation and focus indicator
    var showFocusRing by remember { mutableStateOf(false) }

    // Scanning laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Post-Scan Auto-Crop & Processing Mode state
    var reviewRawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var reviewCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAutoCropEnabled by remember { mutableStateOf(true) }
    var reviewCropRect by remember { mutableStateOf(RectF(0f, 0f, 1f, 1f)) }
    var selectedFilterMode by remember { mutableStateOf(FilterType.ORIGINAL) }
    var reviewRotationDegrees by remember { mutableIntStateOf(0) }
    var isSavingAction by remember { mutableStateOf(false) }

    fun processCapturedImage(rawBitmap: Bitmap) {
        val (cropped, bounds) = ImageEnhancementEngine.autoCropBitmap(rawBitmap)
        reviewRawBitmap = rawBitmap
        reviewCroppedBitmap = cropped
        reviewCropRect = bounds
        isAutoCropEnabled = true
        selectedFilterMode = FilterType.ORIGINAL
        reviewRotationDegrees = 0
    }

    val currentReviewDisplayBitmap = remember(reviewRawBitmap, reviewCroppedBitmap, isAutoCropEnabled, selectedFilterMode, reviewRotationDegrees) {
        val base = if (isAutoCropEnabled) (reviewCroppedBitmap ?: reviewRawBitmap) else reviewRawBitmap
        base?.let { bmp ->
            ImageEnhancementEngine.applyAdjustmentsAndFilter(
                source = bmp,
                filter = selectedFilterMode,
                rotationDegrees = reviewRotationDegrees
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            textRecognizer.close()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        processCapturedImage(bmp)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_scanner_screen")
    ) {
        // Camera Preview with Real-Time ML Kit Analysis
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        // Real-time ML Kit Text Recognition ImageAnalysis
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            // Rate-limit analysis to ~3 times per second to maximize smoothness
                            if (now - lastAnalysisTime > 300) {
                                lastAnalysisTime = now
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val text = visionText.text
                                            if (text.isNotBlank()) {
                                                val cleanSnippet = text.lines()
                                                    .map { it.trim() }
                                                    .firstOrNull { it.length > 4 }
                                                    ?.take(36) ?: ""
                                                detectedSnippet = cleanSnippet
                                                isTextDetected = true
                                                detectedBlockCount = visionText.textBlocks.size
                                            } else {
                                                isTextDetected = false
                                                detectedSnippet = ""
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                                analysis
                            )
                            camera = cam
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    // Tap to Focus
                    previewView.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val factory = SurfaceOrientedMeteringPointFactory(
                                previewView.width.toFloat(),
                                previewView.height.toFloat()
                            )
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                            showFocusRing = true
                        }
                        true
                    }

                    previewView
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission is required to scan", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Viewfinder Guidelines Box (Turns vibrant green when ML Kit detects document text!)
        val guideBorderColor = if (isTextDetected) Color(0xFF10B981) else Color.White.copy(alpha = 0.65f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 115.dp)
                .border(2.5.dp, guideBorderColor, RoundedCornerShape(18.dp))
        ) {
            // Animated Scanning Laser Beam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = (laserPosition * 400).dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (isTextDetected) Color(0xFF10B981) else Color(0xFF06B6D4),
                                Color.White,
                                if (isTextDetected) Color(0xFF10B981) else Color(0xFF06B6D4),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Top Status Pill (Real-Time ML Kit Feedback)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isTextDetected) Color(0xFF064E3B).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTextDetected) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (detectedSnippet.isNotBlank()) "Text: $detectedSnippet..." else "Document Text Detected",
                            color = Color(0xFFE6FFFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = when (scanMode) {
                                "ID_CARD" -> "Align ID Card within the guide frame"
                                "BOOK" -> "Position open book across the center spine"
                                "TEST_PAPERS" -> "Test paper: Auto-cleans handwriting & enhances print"
                                "WHITEBOARDS" -> "Whiteboard: Anti-glare reflection removal & ink boost"
                                "SLIDES" -> "Presentation slide: Keystone projection alignment"
                                "BATCH" -> "Batch mode: Snap multiple pages"
                                else -> "Align document within the frame"
                            },
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Top Control Bar (Back, Scan Mode Pills, Flash, Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Mode Selector Pills
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        "DOCUMENTS" to "Docs",
                        "ID_CARD" to "IDs",
                        "BOOK" to "Books",
                        "TEST_PAPERS" to "Test Papers",
                        "WHITEBOARDS" to "Whiteboard",
                        "SLIDES" to "Slides",
                        "BATCH" to "Batch"
                    )
                    items(modes) { (mode, label) ->
                        val isSelected = scanMode == mode || (mode == "DOCUMENTS" && scanMode == "SINGLE")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { scanMode = mode }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Zoom 1x / 2x Toggle
                IconButton(
                    onClick = {
                        val newZoom = if (currentZoomRatio == 1f) 2f else 1f
                        currentZoomRatio = newZoom
                        camera?.cameraControl?.setZoomRatio(newZoom)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${currentZoomRatio.toInt()}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Flash / Torch Toggle
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) Color(0xFFF59E0B) else Color.White
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(bottom = 32.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Captured Pages Thumbnail Tray (for batch)
            if (capturedPages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedPages) { pageBmp ->
                        Box(
                            modifier = Modifier
                                .size(50.dp, 68.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(
                                model = pageBmp,
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Quick Demo Document generator button for emulators
            Button(
                onClick = {
                    val sample = when (scanMode) {
                        "ID_CARD" -> SampleDocumentGenerator.generateSampleBitmap(DocType.ID_CARD)
                        "BOOK" -> SampleDocumentGenerator.generateSampleBitmap(DocType.BOOK)
                        "TEST_PAPERS" -> SampleDocumentGenerator.generateSampleBitmap(DocType.TEST_PAPER)
                        "WHITEBOARDS" -> SampleDocumentGenerator.generateSampleBitmap(DocType.WHITEBOARD)
                        "SLIDES" -> SampleDocumentGenerator.generateSampleBitmap(DocType.SLIDES)
                        else -> SampleDocumentGenerator.generateSampleBitmap(DocType.INVOICE)
                    }
                    processCapturedImage(sample)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("camera_demo_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Document Capture", color = Color.White, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import from gallery button
                IconButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Image",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, if (isTextDetected) Color(0xFF10B981) else Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.LightGray else Color.White)
                        .clickable(enabled = !isCapturing) {
                            val capture = imageCapture
                            if (capture != null) {
                                isCapturing = true
                                capture.takePicture(
                                    Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val buffer = imageProxy.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            val rotation = imageProxy.imageInfo.rotationDegrees

                                             val rotatedBitmap = if (rotation != 0 && rawBitmap != null) {
                                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                            } else {
                                                rawBitmap
                                            }
                                            imageProxy.close()

                                            scope.launch(Dispatchers.Main) {
                                                isCapturing = false
                                                if (rotatedBitmap != null) {
                                                    processCapturedImage(rotatedBitmap)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            scope.launch(Dispatchers.Main) { isCapturing = false }
                                        }
                                    }
                                )
                            }
                        }
                        .testTag("shutter_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Finish / Save button (active if batch has pages)
                if (capturedPages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val id = documentRepository.createDocumentWithPages(
                                    title = "PR Batch Scan (${capturedPages.size} pages)",
                                    pageBitmaps = capturedPages.toList(),
                                    docType = DocType.GENERAL
                                )
                                onScanFinished(id)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("finish_batch_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Finish Scan",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }

        // ====================================================================
        // Post-Scan Review, Auto-Crop & Processing Modes Overlay
        // ====================================================================
        if (currentReviewDisplayBitmap != null) {
            val displayBmp = currentReviewDisplayBitmap
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("scan_review_overlay"),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Review Top App Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    reviewRawBitmap = null
                                    reviewCroppedBitmap = null
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retake")
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan Review",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAutoCropEnabled) "Auto-cropped to document" else "Full image capture",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAutoCropEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Rotate 90 degrees
                            IconButton(
                                onClick = {
                                    reviewRotationDegrees = (reviewRotationDegrees + 90) % 360
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate")
                            }

                            // Toggle Auto-Crop / Full image
                            IconButton(
                                onClick = {
                                    isAutoCropEnabled = !isAutoCropEnabled
                                }
                            ) {
                                Icon(
                                    Icons.Default.Crop,
                                    contentDescription = "Toggle Crop",
                                    tint = if (isAutoCropEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Document Preview Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = displayBmp.asImageBitmap(),
                                    contentDescription = "Processed scan",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = selectedFilterMode.displayName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Enhancement Modes Strip (Original, Black & White, Vivid, Document, Magic Color)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Choose Enhancement Mode:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            val modes = listOf(
                                FilterType.ORIGINAL to "Original",
                                FilterType.BW to "Black & White",
                                FilterType.VIVID to "Vivid",
                                FilterType.DOCUMENT to "Document",
                                FilterType.MAGIC_COLOR to "Magic Color"
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(modes) { (mode, label) ->
                                    val isSelected = selectedFilterMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clickable { selectedFilterMode = mode }
                                            .testTag("mode_${mode.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Save or Share Options Section
                        Text(
                            text = "Save or Share:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // PDF Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save as PDF
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingAction = true
                                        val allBitmaps = capturedPages.toList() + displayBmp
                                        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val tempPdf = File(exportDir, "Scan_${System.currentTimeMillis() % 10000}.pdf")
                                        val success = PdfEngine.generatePdf(
                                            bitmaps = allBitmaps,
                                            outputFile = tempPdf,
                                            options = PdfExportOptions(preset = PageSizePreset.A4)
                                        )
                                        if (success) {
                                            val savedPath = documentRepository.savePdfToDownloads(tempPdf, tempPdf.name)
                                            Toast.makeText(
                                                context,
                                                "PDF saved to ${savedPath ?: "Downloads/PR_Scanner"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                        isSavingAction = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_as_pdf_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save as PDF", fontWeight = FontWeight.Bold)
                            }

                            // Share as PDF
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isSavingAction = true
                                        val allBitmaps = capturedPages.toList() + displayBmp
                                        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val tempPdf = File(exportDir, "Scan_${System.currentTimeMillis() % 10000}.pdf")
                                        val success = PdfEngine.generatePdf(allBitmaps, tempPdf)
                                        if (success) {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempPdf)
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Document PDF"))
                                        }
                                        isSavingAction = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_as_pdf_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PDF")
                            }
                        }

                        // Image Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save as Image
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingAction = true
                                        val imgName = "PR_Scan_${System.currentTimeMillis() % 10000}.jpg"
                                        val savedPath = documentRepository.saveImageToGallery(displayBmp, imgName)
                                        Toast.makeText(
                                            context,
                                            "Image saved to ${savedPath ?: "Pictures/PR_Scanner"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        isSavingAction = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_as_image_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save as Image", fontWeight = FontWeight.Bold)
                            }

                            // Share as Image
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isSavingAction = true
                                        val imgFile = documentRepository.exportImageFile(displayBmp, "Scan_${System.currentTimeMillis() % 10000}")
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imgFile)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Scanned Image"))
                                        isSavingAction = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_as_image_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Image")
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Navigation & Multi-page Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Add Next Page
                            OutlinedButton(
                                onClick = {
                                    capturedPages.add(displayBmp)
                                    reviewRawBitmap = null
                                    reviewCroppedBitmap = null
                                    Toast.makeText(context, "Page added! Snap next page.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("add_next_page_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Page (${capturedPages.size + 1})")
                            }

                            // Save to Library & Open
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingAction = true
                                        val allBitmaps = capturedPages.toList() + displayBmp
                                        val title = when (scanMode) {
                                            "ID_CARD" -> "Scanned ID Card"
                                            "BOOK" -> "Scanned Book Pages"
                                            "TEST_PAPERS" -> "Scanned Test Paper"
                                            "WHITEBOARDS" -> "Captured Whiteboard"
                                            "SLIDES" -> "Captured Presentation Slide"
                                            else -> "PR Scan ${System.currentTimeMillis() % 10000}"
                                        }
                                        val docType = when (scanMode) {
                                            "ID_CARD" -> DocType.ID_CARD
                                            "BOOK" -> DocType.BOOK
                                            "TEST_PAPERS" -> DocType.TEST_PAPER
                                            "WHITEBOARDS" -> DocType.WHITEBOARD
                                            "SLIDES" -> DocType.SLIDES
                                            else -> DocType.GENERAL
                                        }
                                        val id = documentRepository.createDocumentWithPages(
                                            title = title,
                                            pageBitmaps = allBitmaps,
                                            docType = docType
                                        )
                                        isSavingAction = false
                                        onScanFinished(id)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp)
                                    .testTag("save_to_library_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Library", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
