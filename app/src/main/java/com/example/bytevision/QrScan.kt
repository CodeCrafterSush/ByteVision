package com.example.bytevision

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

// Regex Pattern: ^\[(\d+)/(\d+)\]
private val HEADER_REGEX = Regex("""^\[(\d+)/(\d+)\]""")

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScan() {
    val context = LocalContext.current

    // Session State
    var totalChunks by remember { mutableIntStateOf(0) }
    val byteChunkBuffer = remember { mutableStateMapOf<Int, ByteArray>() }
    var isScanningActive by remember { mutableStateOf(true) }
    var reconstructedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf("Point camera at raw byte QR stream") }
    var detectedExtension by remember { mutableStateOf("bin") }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Reset Scanner Session
    fun resetScannerSession() {
        totalChunks = 0
        byteChunkBuffer.clear()
        reconstructedBytes = null
        previewBitmap = null
        isScanningActive = true
        statusMessage = "Point camera at raw byte QR stream"
        detectedExtension = "bin"
    }

    // Save Assembled ByteArray to Storage (Download / Export)
    fun saveReconstructedFileToStorage(data: ByteArray, ext: String) {
        try {
            val fileName = "Reconstructed_File_${System.currentTimeMillis()}.$ext"
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, if (ext == "png" || ext == "jpg") "image/$ext" else "application/octet-stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { it.write(data) }
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create download file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Process Complete Chunks Reassembly
    fun assembleByteArrayChunks() {
        try {
            statusMessage = "Merging ByteArray Chunks..."
            val outputStream = ByteArrayOutputStream()

            for (i in 1..totalChunks) {
                val chunk = byteChunkBuffer[i] ?: ByteArray(0)
                outputStream.write(chunk)
            }

            val finalByteArray = outputStream.toByteArray()
            reconstructedBytes = finalByteArray
            isScanningActive = false

            // Auto Magic Bytes Check (Image vs Document)
            if (finalByteArray.size >= 4) {
                if (finalByteArray[0] == 0x89.toByte() && finalByteArray[1] == 0x50.toByte()) {
                    detectedExtension = "png"
                } else if (finalByteArray[0] == 0xFF.toByte() && finalByteArray[1] == 0xD8.toByte()) {
                    detectedExtension = "jpg"
                } else if (finalByteArray[0] == 0x25.toByte() && finalByteArray[1] == 0x50.toByte()) {
                    detectedExtension = "pdf"
                }
            }

            // Render Preview if Image
            val bmp = BitmapFactory.decodeByteArray(finalByteArray, 0, finalByteArray.size)
            if (bmp != null) {
                previewBitmap = bmp
            }

            statusMessage = "Reconstructed Successfully! (${finalByteArray.size} bytes)"
        } catch (e: Exception) {
            e.printStackTrace()
            statusMessage = "Error assembling bytes: ${e.localizedMessage}"
        }
    }

    // Parse String Raw Byte Character Payload
    fun processRawByteQrPayload(rawPayload: String) {
        if (!isScanningActive) return

        val matchResult = HEADER_REGEX.find(rawPayload)
        if (matchResult != null) {
            val (currIdxStr, totalChunksStr) = matchResult.destructured
            val currentIndex = currIdxStr.toIntOrNull() ?: return
            val scannedTotalChunks = totalChunksStr.toIntOrNull() ?: return

            if (currentIndex <= 0 || scannedTotalChunks <= 0 || currentIndex > scannedTotalChunks) return

            // Extract content AFTER header bracket ']'
            val headerLength = matchResult.value.length
            val rawByteString = rawPayload.substring(headerLength)

            // Convert string characters to Raw ByteArray (ISO-8859-1 preserves 8-bit bytes)
            val byteData = rawByteString.toByteArray(StandardCharsets.ISO_8859_1)

            // Rule 1: Reset buffer on totalChunks mismatch
            if (totalChunks != 0 && totalChunks != scannedTotalChunks) {
                byteChunkBuffer.clear()
                totalChunks = scannedTotalChunks
            } else if (totalChunks == 0) {
                totalChunks = scannedTotalChunks
            }

            // Rule 2: Ignore duplicates
            if (!byteChunkBuffer.containsKey(currentIndex)) {
                byteChunkBuffer[currentIndex] = byteData
                statusMessage = "Scanning stream..."

                // Check Completion
                if (byteChunkBuffer.size == totalChunks) {
                    assembleByteArrayChunks()
                }
            }
        }
    }

    val surfaceDark = Color(0xFF121212)
    val cardBackground = Color(0xFF1E1E2C)
    val accentNeon = Color(0xFF00E676)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFFA0A0B0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ByteVision ByteArray Reconstructor",
                color = textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Reset",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                    .clickable { resetScannerSession() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // TOP SECTION: Status / Preview / Download Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (reconstructedBytes != null) {
                    // FILE RECONSTRUCTED COMPLETELY
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Image Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF181824), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Binary File Assembled\n(${reconstructedBytes!!.size} Bytes)",
                                    color = textPrimary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                saveReconstructedFileToStorage(
                                    reconstructedBytes!!,
                                    detectedExtension
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentNeon),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Download / Save File (.$detectedExtension)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // LIVE STREAMING & CHUNK PROGRESS
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = statusMessage,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (totalChunks > 0) "Scanned ${byteChunkBuffer.size} / $totalChunks Chunks" else "Awaiting stream...",
                                color = accentNeon,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val progress = if (totalChunks > 0) byteChunkBuffer.size.toFloat() / totalChunks else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentNeon,
                            trackColor = Color(0xFF2A2A3D),
                        )

                        if (totalChunks > 0) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed((1..totalChunks).toList()) { _, chunkIndex ->
                                    val isCaptured = byteChunkBuffer.containsKey(chunkIndex)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isCaptured) accentNeon else Color(0xFF2A2A3D))
                                            .border(
                                                1.dp,
                                                if (isCaptured) accentNeon else Color.Gray.copy(alpha = 0.3f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$chunkIndex",
                                            color = if (isCaptured) Color.Black else textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTTOM CAMERA SCANNER BOX
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333344))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    if (isScanningActive) {
                        CameraPreviewView(onQrCodeScanned = { rawPayload ->
                            processRawByteQrPayload(rawPayload)
                        })

                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .border(2.dp, accentNeon.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text(
                            text = "Assembly Complete.\nTap Reset to scan new stream.",
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Camera permission required to scan stream",
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewView(onQrCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { qrValue ->
                                        onQrCodeScanned(qrValue)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}