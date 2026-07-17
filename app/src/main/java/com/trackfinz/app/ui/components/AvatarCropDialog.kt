package com.trackfinz.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Full-screen crop dialog.
 * User can pinch-zoom and drag to position the image inside the circular crop window.
 * Tapping "Done" crops the bitmap, saves it to internal storage, and returns the file path.
 */
@Composable
fun AvatarCropDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (filePath: String) -> Unit
) {
    val context = LocalContext.current

    // Load bitmap on a background thread
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sourceUri) {
        sourceBitmap = withContext(Dispatchers.IO) { loadBitmap(context, sourceUri) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (sourceBitmap == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                CropContent(
                    bitmap = sourceBitmap!!,
                    context = context,
                    onDismiss = onDismiss,
                    onCropped = onCropped
                )
            }
        }
    }
}

@Composable
private fun CropContent(
    bitmap: Bitmap,
    context: Context,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit
) {
    // Canvas size
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Transform state
    var scale  by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Derived: circle radius = 80% of the shorter canvas side / 2
    val circleRadius by remember(canvasSize) {
        derivedStateOf { min(canvasSize.width, canvasSize.height) * 0.40f }
    }
    val circleCenter by remember(canvasSize) {
        derivedStateOf { Offset(canvasSize.width / 2f, canvasSize.height / 2f) }
    }

    // Fit bitmap into canvas initially
    val fitScale by remember(canvasSize, bitmap) {
        derivedStateOf {
            if (canvasSize.width == 0 || canvasSize.height == 0) 1f
            else {
                val sw = canvasSize.width.toFloat() / bitmap.width
                val sh = canvasSize.height.toFloat() / bitmap.height
                maxOf(sw, sh)          // fill so circle is always covered
            }
        }
    }

    // Reset when bitmap/canvas changes
    LaunchedEffect(fitScale) {
        scale = fitScale
        offset = Offset.Zero
    }

    var isSaving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Crop Photo",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp).padding(6.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = {
                    isSaving = true
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                }
            }
        }

        // ── Crop canvas ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(fitScale * 0.8f, fitScale * 5f)
                        offset += pan
                    }
                }
        ) {
            if (canvasSize.width > 0) {
                val imagePainter = remember(bitmap) { bitmap.asImageBitmap() }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw the image centered + transformed
                    val imgW = bitmap.width * scale
                    val imgH = bitmap.height * scale
                    val imgLeft = size.width / 2f - imgW / 2f + offset.x
                    val imgTop  = size.height / 2f - imgH / 2f + offset.y

                    drawImage(
                        image = imagePainter,
                        dstOffset = androidx.compose.ui.unit.IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(imgW.roundToInt(), imgH.roundToInt())
                    )

                    // Dark overlay with circular hole
                    drawCropOverlay(circleCenter, circleRadius)
                }
            }
        }

        // ── Hint ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Pinch to zoom · Drag to reposition",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }

    // Trigger save when isSaving flips
    if (isSaving && canvasSize.width > 0) {
        LaunchedEffect(Unit) {
            val path = withContext(Dispatchers.IO) {
                cropAndSave(
                    bitmap = bitmap,
                    canvasWidth = canvasSize.width,
                    canvasHeight = canvasSize.height,
                    scale = scale,
                    offset = offset,
                    circleCenter = circleCenter,
                    circleRadius = circleRadius,
                    context = context
                )
            }
            isSaving = false
            onCropped(path)
        }
    }
}

// ── Drawing ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawCropOverlay(center: Offset, radius: Float) {
    // Semi-transparent dark overlay
    val path = Path().apply {
        addRect(Rect(0f, 0f, size.width, size.height))
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        fillType = PathFillType.EvenOdd
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.55f))

    // White circle border
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

// ── Bitmap helpers ────────────────────────────────────────────────────────────

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) { null }
}

private fun cropAndSave(
    bitmap: Bitmap,
    canvasWidth: Int,
    canvasHeight: Int,
    scale: Float,
    offset: Offset,
    circleCenter: Offset,
    circleRadius: Float,
    context: Context
): String {
    // Map circle bounds back to bitmap coordinates
    val imgW = bitmap.width * scale
    val imgH = bitmap.height * scale
    val imgLeft = canvasWidth / 2f - imgW / 2f + offset.x
    val imgTop  = canvasHeight / 2f - imgH / 2f + offset.y

    // Circle bounds in canvas space
    val cropLeft   = circleCenter.x - circleRadius
    val cropTop    = circleCenter.y - circleRadius
    val cropSize   = circleRadius * 2f

    // Convert to bitmap pixel coordinates
    val bmpX = ((cropLeft - imgLeft) / scale).roundToInt().coerceAtLeast(0)
    val bmpY = ((cropTop  - imgTop)  / scale).roundToInt().coerceAtLeast(0)
    val bmpSize = (cropSize / scale).roundToInt()
        .coerceAtMost(bitmap.width - bmpX)
        .coerceAtMost(bitmap.height - bmpY)
        .coerceAtLeast(1)

    // Crop square from source
    val cropped = Bitmap.createBitmap(bitmap, bmpX, bmpY, bmpSize, bmpSize)

    // Scale to 256×256 output
    val output = 256
    val scaled = Bitmap.createScaledBitmap(cropped, output, output, true)

    // Apply circular mask
    val result = Bitmap.createBitmap(output, output, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    val paint  = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    canvas.drawCircle(output / 2f, output / 2f, output / 2f, paint)
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, 0f, 0f, paint)

    // Save to internal storage
    val file = File(context.filesDir, "avatar.png")
    FileOutputStream(file).use { out ->
        result.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    cropped.recycle()
    scaled.recycle()

    return file.absolutePath
}
