package com.example.bytevision

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.bytevision.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.atan2

enum class CameraRatio(
    val title: String,
    val cameraAspectRatio: Int,
    val previewRatio: Float
)
{
    RATIO_4_3(title = "4:3", cameraAspectRatio = AspectRatio.RATIO_4_3, previewRatio = 4f / 3f),
    RATIO_1_1(title = "1:1", cameraAspectRatio = AspectRatio.RATIO_4_3, previewRatio = 1f),
    RATIO_9_16(title = "9:16", cameraAspectRatio = AspectRatio.RATIO_16_9, previewRatio = 9f / 16f)
}

enum class CameraResolution(
    val label: String,
    val targetSize: Size?
)
{
    HIGH("High (4K / Max)", Size(2160, 3840)),
    MEDIUM("Medium (1080p)", Size(1080, 1920)),
    LOW("Low (720p)", Size(720, 1280))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPage() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val sharedPreferences = remember { context.getSharedPreferences("ByteVisionPrefs", Context.MODE_PRIVATE) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var selectedRatio by remember { mutableStateOf(CameraRatio.RATIO_4_3) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }
    var showGrid by remember { mutableStateOf(false) }
    var showLeveler by remember { mutableStateOf(false) }

    var selectedResolution by remember {
        val savedRes = sharedPreferences.getString("CAMERA_RESOLUTION", CameraResolution.HIGH.name)
        mutableStateOf(
            try {
                CameraResolution.valueOf(savedRes ?: CameraResolution.HIGH.name)
            } catch (e: Exception) {
                CameraResolution.HIGH
            }
        )
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var savedPassword by remember { mutableStateOf(sharedPreferences.getString("CAMERA_PASSWORD", "123") ?: "123") }
    var tempPasswordText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showPhotoPreviewDialog by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var deviceTiltAngle by remember { mutableFloatStateOf(0f) }

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(showLeveler) {
        if (!showLeveler) {
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        val accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val ax = it.values[0]
                    val ay = it.values[1]

                    val angle = Math.toDegrees(
                        atan2(
                            ax.toDouble(),
                            ay.toDouble()
                        )
                    ).toFloat()

                    deviceTiltAngle = -angle
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }


    fun saveBitmapToGallery(
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        val resolver = context.contentResolver

        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "$filename.png"
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/png"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/ByteVision"
                )
                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }
        }

        val imageUri = resolver.insert(
            imageCollection,
            contentValues
        )

        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                }

                resolver.update(
                    uri,
                    updateValues,
                    null,
                    null
                )
            }
        }

        return imageUri
    }

    fun executeCapture() {
        val capture = imageCapture ?: return

        val tempCacheFile = File(
            context.cacheDir,
            "temp_capture_${System.currentTimeMillis()}.png"
        )

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(tempCacheFile)
            .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inMutable = true
                        }

                        val originalBitmap = BitmapFactory.decodeFile(
                            tempCacheFile.absolutePath,
                            options
                        )

                        if (originalBitmap != null) {
                            val password = savedPassword

                            val shuffledBitmap = shuffleBitmap(
                                originalBitmap,
                                password
                            )

                            val filename = "shuffled_${System.currentTimeMillis()}"

                            val savedUri = saveBitmapToGallery(
                                shuffledBitmap,
                                filename
                            )

                            if (savedUri != null) {
                                lastPhoto = savedUri

                                Toast.makeText(
                                    context,
                                    "Saved to Gallery (Pictures/ByteVision)!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to save image to Gallery",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to decode cached image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        Toast.makeText(
                            context,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        if (tempCacheFile.exists()) {
                            tempCacheFile.delete()
                        }
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    Toast.makeText(
                        context,
                        "Capture Failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    fun onShutterClick() {
        if (isTimerRunning) {
            return
        }

        if (timerSeconds > 0) {
            isTimerRunning = true
            countdownValue = timerSeconds

            coroutineScope.launch {
                while (countdownValue > 0) {
                    delay(1000)
                    countdownValue--
                }

                isTimerRunning = false
                executeCapture()
            }
        } else {
            executeCapture()
        }
    }

    fun onFlipCameraClick() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        zoomRatio = 1f
    }

    fun onTimerToggleClick() {
        timerSeconds = when (timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
    }

    fun onGridToggleClick() {
        showGrid = !showGrid
    }

    fun onLevelerToggleClick() {
        showLeveler = !showLeveler
    }

    fun onRatioSelectClick(ratio: CameraRatio) {
        selectedRatio = ratio
        zoomRatio = 1f
        camera?.cameraControl?.setZoomRatio(1f)
    }

    fun onGalleryThumbnailClick() {
        if (lastPhoto != null) {
            showPhotoPreviewDialog = true
        } else {
            Toast.makeText(
                context,
                "No photo captured yet",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(
        lensFacing,
        selectedResolution,
        selectedRatio
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()


            val previewBuilder = Preview.Builder()

            // 1:1 ke liye CameraX ka direct ratio nahi hota.
            // 4:3 aur 9:16 directly set honge.
            if (selectedRatio == CameraRatio.RATIO_4_3) {
                previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            } else if (selectedRatio == CameraRatio.RATIO_9_16) {
                previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            } else {
                previewBuilder.setTargetResolution(Size(1080, 1080))
            }

            val preview = previewBuilder.build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            val captureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

            if (selectedRatio == CameraRatio.RATIO_4_3) {
                captureBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            } else if (selectedRatio == CameraRatio.RATIO_9_16) {
                captureBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            } else {
                captureBuilder.setTargetResolution(Size(1080, 1080))
            }

            selectedResolution.targetSize?.let { size ->
                /*
                 * Resolution ko ratio ke saath use karne ke liye
                 * CameraX ko target resolution diya ja raha hai.
                 * Ratio selection ko priority dene ke liye 1:1 mein
                 * square resolution use hota hai.
                 */
                if (selectedRatio == CameraRatio.RATIO_1_1) {
                    val squareSize = when (selectedResolution) {
                        CameraResolution.HIGH -> Size(2160, 2160)
                        CameraResolution.MEDIUM -> Size(1080, 1080)
                        CameraResolution.LOW -> Size(720, 720)
                    }

                    captureBuilder.setTargetResolution(squareSize)
                }
            }

            imageCapture = captureBuilder.build()


            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                camera?.cameraControl?.setZoomRatio(zoomRatio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.statusBars,

        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TIMER
                    TextButton(
                        onClick = { onTimerToggleClick() }
                    ) {
                        Text(
                            text = if (timerSeconds == 0) "TIMER OFF" else "TIMER ${timerSeconds}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // GRID
                    TextButton(
                        onClick = { onGridToggleClick() }
                    ) {
                        Text(
                            text = if (showGrid) "GRID ON" else "GRID OFF",
                            color = if (showGrid) Color.Yellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // LEVELER
                    TextButton(
                        onClick = { onLevelerToggleClick() }
                    ) {
                        Text(
                            text = if (showLeveler) "SENSOR ON" else "SENSOR OFF",
                            color = if (showLeveler) Color.Green else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // SETTINGS
                    TextButton(
                        onClick = {
                            tempPasswordText = savedPassword
                            isPasswordVisible = false
                            showSettingsDialog = true
                        }
                    ) {
                        Text(
                            text = "SETTINGS",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },

        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(CameraRatio.entries.toTypedArray()) { ratio ->
                        Text(
                            text = ratio.title,
                            color = if (selectedRatio == ratio) ActionTextPink else TextMuted,
                            fontWeight = if (selectedRatio == ratio) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .clickable { onRatioSelectClick(ratio) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GALLERY
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ListenBtnBg)
                            .border(1.dp, ListenBtnBorder, RoundedCornerShape(14.dp))
                            .clickable { onGalleryThumbnailClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        lastPhoto?.let { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Gallery Preview Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Text(
                            text = "NONE",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }

                    // SHUTTER
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        SendBtnGradientStart,
                                        SendBtnGradientEnd
                                    )
                                )
                            )
                            .clickable { onShutterClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // FLIP
                    Button(
                        onClick = { onFlipCameraClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ListenBtnBg,
                            contentColor = ListenBtnText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "FLIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ListenBtnText
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when (selectedRatio) {
                            CameraRatio.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
                            CameraRatio.RATIO_1_1 -> Modifier.aspectRatio(1f)
                            CameraRatio.RATIO_9_16 -> Modifier.aspectRatio(9f / 16f)
                        }
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomRatio = (zoomRatio * zoom).coerceIn(1f, 5f)
                            camera?.cameraControl?.setZoomRatio(zoomRatio)
                        }
                    }
            ) {

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.first().position

                                    focusPoint = position

                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(position.x, position.y)
                                    val action = FocusMeteringAction.Builder(point).build()

                                    camera?.cameraControl?.startFocusAndMetering(action)
                                }
                            }
                        }
                )

                focusPoint?.let { point ->
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = 40f,
                            center = point,
                            style = Stroke(width = 3f)
                        )
                    }
                }

                if (showGrid) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val w = size.width
                        val h = size.height

                        drawLine(
                            Color.White.copy(alpha = 0.4f),
                            start = Offset(w / 3, 0f),
                            end = Offset(w / 3, h),
                            strokeWidth = 1f
                        )

                        drawLine(
                            Color.White.copy(alpha = 0.4f),
                            start = Offset((2 * w) / 3, 0f),
                            end = Offset((2 * w) / 3, h),
                            strokeWidth = 1f
                        )

                        drawLine(
                            Color.White.copy(alpha = 0.4f),
                            start = Offset(0f, h / 3),
                            end = Offset(w, h / 3),
                            strokeWidth = 1f
                        )

                        drawLine(
                            Color.White.copy(alpha = 0.4f),
                            start = Offset(0f, (2 * h) / 3),
                            end = Offset(w, (2 * h) / 3),
                            strokeWidth = 1f
                        )
                    }
                }

                if (showLeveler) {
                    val isBalanced = kotlin.math.abs(deviceTiltAngle) < 2f
                    val levelerColor = if (isBalanced) Color.Green else Color.Yellow

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(160.dp)
                            .height(2.dp)
                            .rotate(deviceTiltAngle)
                            .background(levelerColor)
                    )
                }

                if (isTimerRunning) {
                    Text(
                        text = "$countdownValue",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Slider(
                value = zoomRatio,
                onValueChange = {
                    zoomRatio = it
                    camera?.cameraControl?.setZoomRatio(it)
                },
                valueRange = 1f..5f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(0.6f)
            )
        }
    }


    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
            },
            title = {
                Text(
                    text = "Camera & Security Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Photo Resolution / Quality",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = {
                            dropdownExpanded = !dropdownExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedResolution.label,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Quality")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = dropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            }
                        ) {
                            CameraResolution.entries.forEach { resolution ->
                                DropdownMenuItem(
                                    text = {
                                        Text(resolution.label)
                                    },
                                    onClick = {
                                        selectedResolution = resolution
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enter password for image encryption:",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = tempPasswordText,
                        onValueChange = {
                            tempPasswordText = it
                        },
                        label = {
                            Text("Password")
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    isPasswordVisible = !isPasswordVisible
                                }
                            ) {
                                Text(
                                    text = if (isPasswordVisible) "HIDE" else "SHOW",
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempPasswordText.isNotEmpty()) {
                            savedPassword = tempPasswordText

                            sharedPreferences
                                .edit()
                                .putString("CAMERA_PASSWORD", tempPasswordText)
                                .putString("CAMERA_RESOLUTION", selectedResolution.name)
                                .apply()

                            Toast.makeText(
                                context,
                                "Settings saved!",
                                Toast.LENGTH_SHORT
                            ).show()

                            showSettingsDialog = false
                        } else {
                            Toast.makeText(
                                context,
                                "Password cannot be empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showPhotoPreviewDialog && lastPhoto != null) {
        Dialog(
            onDismissRequest = {
                showPhotoPreviewDialog = false
            }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Captured Photo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Image(
                        painter = rememberAsyncImagePainter(lastPhoto),
                        contentDescription = "Preview Captured Photo Dialog",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(selectedRatio.previewRatio)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showPhotoPreviewDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActionTextPink
                        )
                    ) {
                        Text(
                            text = "Close",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}