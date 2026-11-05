package com.example.prscanner.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class DrawingPath(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 6f
)

@Composable
fun SignaturePad(
    onSaveSignature: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Draw Digital Signature",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints = currentPoints + change.position
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                paths.add(DrawingPath(currentPoints))
                                currentPoints = emptyList()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Baseline guide
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.6f),
                    start = Offset(40f, size.height * 0.75f),
                    end = Offset(size.width - 40f, size.height * 0.75f),
                    strokeWidth = 2f
                )

                for (p in paths) {
                    if (p.points.size > 1) {
                        val path = Path().apply {
                            moveTo(p.points.first().x, p.points.first().y)
                            for (i in 1 until p.points.size) {
                                lineTo(p.points[i].x, p.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = p.color,
                            style = Stroke(
                                width = p.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 6f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Sign with finger here",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPoints = emptyList()
                },
                modifier = Modifier.testTag("clear_signature_btn")
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Text("Clear", modifier = Modifier.padding(start = 6.dp))
            }

            Row {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val width = 600
                        val height = 280
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = AndroidCanvas(bitmap)
                        canvas.drawColor(AndroidColor.TRANSPARENT)

                        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.BLACK
                            strokeWidth = 10f
                            style = AndroidPaint.Style.STROKE
                            strokeCap = AndroidPaint.Cap.ROUND
                            strokeJoin = AndroidPaint.Join.ROUND
                        }

                        // Scale points to bitmap dimensions
                        for (p in paths) {
                            if (p.points.size > 1) {
                                val androidPath = android.graphics.Path()
                                val first = p.points.first()
                                androidPath.moveTo(first.x * 1.5f, first.y * 1.4f)
                                for (i in 1 until p.points.size) {
                                    val pt = p.points[i]
                                    androidPath.lineTo(pt.x * 1.5f, pt.y * 1.4f)
                                }
                                canvas.drawPath(androidPath, paint)
                            }
                        }

                        onSaveSignature(bitmap)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("save_signature_btn"),
                    enabled = paths.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
