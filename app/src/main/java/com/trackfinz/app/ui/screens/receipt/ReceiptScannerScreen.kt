package com.trackfinz.app.ui.screens.receipt

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.utils.ReceiptData
import com.trackfinz.app.utils.ReceiptScanner
import com.trackfinz.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onBack: () -> Unit,
    onReceiptSaved: () -> Unit,
    vm: TransactionViewModel = hiltViewModel()
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var receiptData by remember { mutableStateOf<ReceiptData?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.RECEIPT_SCANNER)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s(Str.BACK))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !hasCameraPermission -> {
                    PermissionDeniedContent(s)
                }
                isProcessing -> {
                    ProcessingContent(s)
                }
                capturedImage != null && receiptData != null -> {
                    ConfirmReceiptContent(
                        bitmap = capturedImage!!,
                        receiptData = receiptData!!,
                        s = s,
                        onRetake = {
                            capturedImage = null
                            receiptData = null
                        },
                        onConfirm = {
                            showConfirmDialog = true
                        }
                    )
                }
                else -> {
                    CameraPreviewContent(
                        context = context,
                        s = s,
                        onImageCaptured = { bitmap ->
                            capturedImage = bitmap
                            isProcessing = true
                            scope.launch {
                                // Save temp image
                                val tempUri = saveTempImage(context, bitmap)
                                tempUri?.let { uri ->
                                    val data = ReceiptScanner.scanReceipt(context, uri)
                                    receiptData = data
                                    isProcessing = false
                                    
                                    if (data == null) {
                                        // Show error and reset
                                        capturedImage = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Confirm dialog
    if (showConfirmDialog && receiptData != null) {
        SaveReceiptDialog(
            receiptData = receiptData!!,
            capturedImage = capturedImage,
            s = s,
            onDismiss = { showConfirmDialog = false },
            onSave = { title, amount, category, note ->
                scope.launch {
                    val imagePath = capturedImage?.let { 
                        ReceiptScanner.saveReceiptImage(context, it) 
                    }
                    
                    val transaction = TransactionEntity(
                        title = title,
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        category = category,
                        note = note,
                        date = receiptData!!.date,
                        receiptImagePath = imagePath,
                        merchantName = receiptData!!.merchantName,
                        scannedItems = receiptData!!.items.joinToString(", ")
                    )
                    
                    vm.addTransaction(transaction)
                    showConfirmDialog = false
                    onReceiptSaved()
                }
            }
        )
    }
}

@Composable
private fun PermissionDeniedContent(s: (String) -> String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            s(Str.CAMERA_PERMISSION),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            s(Str.CAMERA_PERMISSION_NEEDED),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProcessingContent(s: (String) -> String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(s(Str.PROCESSING_RECEIPT), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            s(Str.EXTRACTING_DATA),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CameraPreviewContent(
    context: Context,
    s: (String) -> String,
    onImageCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture button
        FloatingActionButton(
            onClick = {
                imageCapture?.let { capture ->
                    val file = File(context.cacheDir, "temp_receipt_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                onImageCaptured(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
            containerColor = Teal500,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = s(Str.CAPTURE_RECEIPT),
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ConfirmReceiptContent(
    bitmap: Bitmap,
    receiptData: ReceiptData,
    s: (String) -> String,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Receipt image preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Receipt",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Extracted data
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    s(Str.CONFIRM_DETAILS),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                DataRow(s(Str.MERCHANT_NAME), receiptData.merchantName)
                DataRow(s(Str.AMOUNT), "$${String.format("%.2f", receiptData.totalAmount)}")
                DataRow(s(Str.SUGGESTED_CATEGORY), receiptData.suggestedCategory.emoji + " " + receiptData.suggestedCategory.name)
                
                if (receiptData.items.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s(Str.RECEIPT_ITEMS),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        receiptData.items.take(5).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(s(Str.RETAKE_PHOTO))
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(s(Str.SAVE))
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SaveReceiptDialog(
    receiptData: ReceiptData,
    capturedImage: Bitmap?,
    s: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String, Double, TransactionCategory, String) -> Unit
) {
    var title by remember { mutableStateOf(receiptData.merchantName) }
    var amount by remember { mutableStateOf(receiptData.totalAmount.toString()) }
    var category by remember { mutableStateOf(receiptData.suggestedCategory) }
    var note by remember { mutableStateOf(receiptData.items.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.CONFIRM_DETAILS)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s(Str.DESCRIPTION)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(s(Str.AMOUNT)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amount.toDoubleOrNull() ?: receiptData.totalAmount
                    onSave(title, finalAmount, category, note)
                }
            ) {
                Text(s(Str.SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s(Str.CANCEL))
            }
        }
    )
}

private suspend fun saveTempImage(context: Context, bitmap: Bitmap): Uri? {
    return suspendCoroutine { continuation ->
        try {
            val file = File(context.cacheDir, "temp_receipt_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            continuation.resume(Uri.fromFile(file))
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }
}
