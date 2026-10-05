package com.example.bytevision

import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.bytevision.ui.theme.BgGradientCircle1
import com.example.bytevision.ui.theme.BgGradientCircle2
import com.example.bytevision.ui.theme.BgGradientEnd
import com.example.bytevision.ui.theme.BgGradientStart
import com.example.bytevision.ui.theme.GlassBackground
import com.example.bytevision.ui.theme.GlassBorder
import com.example.bytevision.ui.theme.ListenBtnBg
import com.example.bytevision.ui.theme.ListenBtnBorder
import com.example.bytevision.ui.theme.ListenBtnText
import com.example.bytevision.ui.theme.SendBtnGradientEnd
import com.example.bytevision.ui.theme.SendBtnGradientStart
import com.example.bytevision.ui.theme.TextMuted
import com.example.bytevision.ui.theme.TextTitle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// =========================================================
// MAIN CAMERA SCREEN (Production Grade)
// =========================================================
@Composable
fun CameraPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    // Camera Lifecycle Setup
    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

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
    }

    fun takePhoto() {
        val capture = imageCapture ?: return

        val photoFile = File(
            context.externalCacheDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    lastCapturedUri = Uri.fromFile(photoFile)
                    Toast.makeText(context, "Photo Captured!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(BgGradientStart, BgGradientEnd)
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundBrush)
        ) {
            // Ambient Decorative Elements
            CameraBackgroundDecorations()

            // Main Preview View Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 110.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, GlassBorder, RoundedCornerShape(28.dp))
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Glassmorphic Control Bar
            CameraControlPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                lastCapturedUri = lastCapturedUri,
                onThumbnailClick = { showPhotoDialog = true },
                onShutterClick = { takePhoto() },
                onFlipClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            )

            // Fullscreen Preview Overlay Modal
            if (showPhotoDialog && lastCapturedUri != null) {
                PhotoPreviewDialog(
                    imageUri = lastCapturedUri!!,
                    onDismiss = { showPhotoDialog = false }
                )
            }
        }
    }
}

// =========================================================
// HELPER COMPOSABLES
// =========================================================

@Composable
private fun CameraBackgroundDecorations() {
    Box(
        modifier = Modifier
            .size(280.dp)
            .background(BgGradientCircle1, CircleShape)
    )
    Box(
        modifier = Modifier
            .size(260.dp)
            .background(BgGradientCircle2, CircleShape)
    )
}

@Composable
private fun CameraControlPanel(
    modifier: Modifier = Modifier,
    lastCapturedUri: Uri?,
    onThumbnailClick: () -> Unit,
    onShutterClick: () -> Unit,
    onFlipClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp, horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gallery Thumbnail Slot
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ListenBtnBg)
                    .border(1.5.dp, ListenBtnBorder, CircleShape)
                    .clickable(enabled = lastCapturedUri != null, onClick = onThumbnailClick),
                contentAlignment = Alignment.Center
            ) {
                if (lastCapturedUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(lastCapturedUri),
                        contentDescription = "Last captured photo thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "PIC",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center: Gradient Shutter Action
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .border(3.dp, ListenBtnBorder, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SendBtnGradientStart, SendBtnGradientEnd)
                        )
                    )
                    .clickable(onClick = onShutterClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }

            // Right: Flip Lens Switcher
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ListenBtnBg)
                    .border(1.5.dp, ListenBtnBorder, CircleShape)
                    .clickable(onClick = onFlipClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Flip",
                    color = ListenBtnText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = GlassBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.5.dp, GlassBorder, RoundedCornerShape(28.dp))
                    .padding(16.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Captured Photo Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .padding(top = 40.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photo Preview",
                        color = TextTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ListenBtnBg)
                            .border(1.dp, ListenBtnBorder, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = ListenBtnText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}