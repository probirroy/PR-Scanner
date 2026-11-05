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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.prscanner.data.model.DocType
import com.example.prscanner.data.repository.DocumentRepository
import com.example.prscanner.engine.OcrEngine
import com.example.prscanner.util.SampleDocumentGenerator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        capturedPages.add(bmp)
                        if (scanMode == "SINGLE") {
                            val id = documentRepository.createDocumentWithPages(
                                title = "Imported Document",
                                pageBitmaps = listOf(bmp),
                                docType = DocType.GENERAL
                            )
                            onScanFinished(id)
                        }
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
                    capturedPages.add(sample)
                    if (scanMode != "BATCH") {
                        scope.launch {
                            val title = when (scanMode) {
                                "ID_CARD" -> "Scanned ID Card"
                                "BOOK" -> "Scanned Book Pages"
                                "TEST_PAPERS" -> "Scanned Test Paper"
                                "WHITEBOARDS" -> "Captured Whiteboard"
                                "SLIDES" -> "Captured Presentation Slide"
                                else -> "Scanned Document"
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
                                pageBitmaps = listOf(sample),
                                docType = docType
                            )
                            onScanFinished(id)
                        }
                    }
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
                                                    capturedPages.add(rotatedBitmap)
                                                    if (scanMode != "BATCH") {
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
                                                            pageBitmaps = listOf(rotatedBitmap),
                                                            docType = docType
                                                        )
                                                        onScanFinished(id)
                                                    }
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
    }
}
