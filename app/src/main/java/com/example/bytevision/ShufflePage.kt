package com.example.bytevision

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bytevision.ui.theme.ActionTextPurple
import com.example.bytevision.ui.theme.GlassBorder
import com.example.bytevision.ui.theme.InputBg
import com.example.bytevision.ui.theme.InputBorder
import com.example.bytevision.ui.theme.SendBtnGradientEnd
import com.example.bytevision.ui.theme.SendBtnGradientStart
import com.example.bytevision.ui.theme.StatusBg
import com.example.bytevision.ui.theme.StatusBorder
import com.example.bytevision.ui.theme.StatusText
import com.example.bytevision.ui.theme.TextBody
import com.example.bytevision.ui.theme.TextMuted
import com.example.bytevision.ui.theme.TextSubtitle
import com.example.bytevision.ui.theme.TextTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper Function to Save Processed Bitmap to MediaStore
fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    val filename = "ByteVision_${System.currentTimeMillis()}.png"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/ByteVision"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    return try {
        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)

            outputStream?.use { stream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(
                    MediaStore.MediaColumns.IS_PENDING,
                    0
                )

                resolver.update(
                    it,
                    contentValues,
                    null,
                    null
                )
            }

            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun Shuffle() {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBitmaps by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    var outputBitmaps by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    var password by remember {
        mutableStateOf("")
    }

    var statusText by remember {
        mutableStateOf("Waiting")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    // Track individual image saving states by index
    val savingStates = remember {
        mutableStateMapOf<Int, Boolean>()
    }

    // Background Thread safe function for Multiple Shuffle
    fun shuffleImages(
        bitmaps: List<Bitmap>,
        pass: String
    ) {
        if (bitmaps.isEmpty()) {
            Toast.makeText(
                context,
                "Pehle images select karein!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (pass.isEmpty()) {
            Toast.makeText(
                context,
                "Kripya password enter karein!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isLoading = true
        statusText = "Shuffling..."

        coroutineScope.launch(Dispatchers.Default) {
            try {
                val results = mutableListOf<Bitmap>()

                for (bmp in bitmaps) {
                    val result = shuffleBitmap(bmp, pass)
                    result?.let {
                        results.add(it)
                    }
                }

                withContext(Dispatchers.Main) {
                    outputBitmaps = results
                    statusText = "Shuffled"
                    isLoading = false
                }

            } catch (e: Exception) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    statusText = "Error"
                    isLoading = false

                    Toast.makeText(
                        context,
                        "Processing failed: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Background Thread safe function for Multiple Unshuffle
    fun unshuffleImages(
        bitmaps: List<Bitmap>,
        pass: String
    ) {
        if (bitmaps.isEmpty()) {
            Toast.makeText(
                context,
                "Pehle images select karein!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (pass.isEmpty()) {
            Toast.makeText(
                context,
                "Kripya password enter karein!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isLoading = true
        statusText = "Unshuffling..."

        coroutineScope.launch(Dispatchers.Default) {
            try {
                val results = mutableListOf<Bitmap>()

                for (bmp in bitmaps) {
                    val result = unshuffleBitmap(bmp, pass)
                    result?.let {
                        results.add(it)
                    }
                }

                withContext(Dispatchers.Main) {
                    outputBitmaps = results
                    statusText = "Unshuffled"
                    isLoading = false
                }

            } catch (e: Exception) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    statusText = "Error"
                    isLoading = false

                    Toast.makeText(
                        context,
                        "Processing failed: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Individual Image Save Helper Function with Loading State
    fun saveSingleImage(
        index: Int,
        bitmap: Bitmap
    ) {
        savingStates[index] = true

        coroutineScope.launch(Dispatchers.IO) {

            val isSuccess = saveBitmapToGallery(
                context,
                bitmap
            )

            withContext(Dispatchers.Main) {

                savingStates[index] = false

                if (isSuccess) {
                    Toast.makeText(
                        context,
                        "Image saved to Gallery!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to save image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Save All Output Images
    fun saveAllImages(
        bitmaps: List<Bitmap>
    ) {
        if (bitmaps.isEmpty()) {
            return
        }

        coroutineScope.launch(Dispatchers.IO) {

            var savedCount = 0

            for (bitmap in bitmaps) {

                val isSuccess = saveBitmapToGallery(
                    context,
                    bitmap
                )

                if (isSuccess) {
                    savedCount++
                }
            }

            withContext(Dispatchers.Main) {

                Toast.makeText(
                    context,
                    "$savedCount image(s) saved to Gallery!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Multiple Image Selection Launcher
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->

            if (uris.isNotEmpty()) {

                val bitmaps = uris.mapNotNull {
                    it.toBitmap(context)
                }

                selectedBitmaps = bitmaps
                outputBitmaps = emptyList()
                statusText = "Waiting"
            }
        }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            com.example.bytevision.ui.theme.BgGradientStart,
                            com.example.bytevision.ui.theme.BgGradientEnd
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        GlassBorder
                    ),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 18.dp,
                                vertical = 24.dp
                            )
                    ) {

                        // HEADER
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Image Shuffle",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTitle
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "Securely transform your images with a password",
                                fontSize = 13.sp,
                                color = TextSubtitle
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        // 1. INPUT IMAGE BOX
                        SectionLabel(
                            text = "IMAGE INPUT"
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(
                                    RoundedCornerShape(17.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = InputBorder,
                                    shape = RoundedCornerShape(17.dp)
                                )
                                .background(InputBg)
                                .clickable {
                                    imagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {

                            if (selectedBitmaps.isNotEmpty()) {

                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(
                                        minSize = 60.dp
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        6.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(
                                        6.dp
                                    )
                                ) {

                                    items(selectedBitmaps) { bmp ->

                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Selected Image",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                            } else {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {

                                    // Upload Icon
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.icon_upload_file
                                        ),
                                        contentDescription = "Upload",
                                        tint = SendBtnGradientStart,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = "Drop your images here",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextTitle
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = "or tap to browse multiple images",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // CLEAR ALL BUTTON FOR INPUT IMAGES
                        if (selectedBitmaps.isNotEmpty()) {

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {

                                TextButton(
                                    onClick = {
                                        selectedBitmaps = emptyList()
                                        outputBitmaps = emptyList()
                                        statusText = "Waiting"
                                    }
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        // Delete Icon
                                        Icon(
                                            painter = painterResource(
                                                id = R.drawable.icon_delete
                                            ),
                                            contentDescription = "Clear All",
                                            tint = Color.Red.copy(
                                                alpha = 0.8f
                                            ),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(
                                            modifier = Modifier.width(4.dp)
                                        )

                                        Text(
                                            text = "Clear All",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Red.copy(
                                                alpha = 0.8f
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // 2. PASSWORD FIELD
                        SectionLabel(
                            text = "PASSWORD"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        PasswordField(
                            value = password,
                            placeholder = "Enter key password",
                            onValueChange = {
                                password = it
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // 3. BUTTONS ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                10.dp
                            )
                        ) {

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {

                                GradientButton(
                                    text = "Shuffle",
                                    iconResId = R.drawable.icon_file_delete,
                                    onClick = {
                                        shuffleImages(
                                            selectedBitmaps,
                                            password
                                        )
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {

                                SecondaryButton(
                                    text = "Unshuffle",
                                    iconResId = R.drawable.icon_file_delete,
                                    onClick = {
                                        unshuffleImages(
                                            selectedBitmaps,
                                            password
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(22.dp)
                        )

                        // 4. OUTPUT BOX
                        SectionLabel(
                            text = "OUTPUT"
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(
                                    RoundedCornerShape(17.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = InputBorder,
                                    shape = RoundedCornerShape(17.dp)
                                )
                                .background(
                                    Color.White.copy(
                                        alpha = 0.72f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            // STATUS
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(
                                        RoundedCornerShape(20.dp)
                                    )
                                    .background(StatusBg)
                                    .border(
                                        width = 1.dp,
                                        color = StatusBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(
                                        horizontal = 11.dp,
                                        vertical = 5.dp
                                    )
                            ) {

                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    color = StatusText
                                )
                            }

                            if (isLoading) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {

                                    CircularProgressIndicator(
                                        color = SendBtnGradientStart,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.height(10.dp)
                                    )

                                    Text(
                                        text = "Processing images...",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }

                            } else if (outputBitmaps.isNotEmpty()) {

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(
                                        16.dp
                                    )
                                ) {

                                    itemsIndexed(
                                        outputBitmaps
                                    ) { index, bmp ->

                                        val isSaving =
                                            savingStates[index] == true

                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            // FULL IMAGE
                                            // Original aspect ratio maintained
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Processed Image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(
                                                        bmp.width.toFloat() /
                                                                bmp.height.toFloat()
                                                    )
                                                    .clip(
                                                        RoundedCornerShape(
                                                            12.dp
                                                        )
                                                    ),
                                                contentScale = ContentScale.Fit
                                            )

                                            Spacer(
                                                modifier = Modifier.height(8.dp)
                                            )

                                            // SAVE IMAGE + PIXEL INFO
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                // SAVE IMAGE BUTTON
                                                TextButton(
                                                    onClick = {
                                                        if (!isSaving) {
                                                            saveSingleImage(
                                                                index,
                                                                bmp
                                                            )
                                                        }
                                                    },
                                                    enabled = !isSaving
                                                ) {

                                                    if (isSaving) {

                                                        CircularProgressIndicator(
                                                            color = SendBtnGradientStart,
                                                            modifier = Modifier.size(
                                                                16.dp
                                                            ),
                                                            strokeWidth = 2.dp
                                                        )

                                                        Spacer(
                                                            modifier = Modifier.width(
                                                                6.dp
                                                            )
                                                        )

                                                        Text(
                                                            text = "Saving...",
                                                            fontSize = 13.sp,
                                                            color = TextMuted
                                                        )

                                                    } else {

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {

                                                            Icon(
                                                                painter = painterResource(
                                                                    id = R.drawable.icon_upload_file
                                                                ),
                                                                contentDescription = "Save",
                                                                tint = SendBtnGradientStart,
                                                                modifier = Modifier.size(
                                                                    16.dp
                                                                )
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.width(
                                                                    6.dp
                                                                )
                                                            )

                                                            Text(
                                                                text = "Save Image",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = SendBtnGradientStart
                                                            )
                                                        }
                                                    }
                                                }

                                                // IMAGE PIXEL INFO
                                                Text(
                                                    text = "${bmp.width} × ${bmp.height} px",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }

                                    // SAVE ALL BUTTON
                                    item {

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        GradientButton(
                                            text = "Save All",
                                            iconResId = R.drawable.icon_upload_file,
                                            onClick = {
                                                saveAllImages(
                                                    outputBitmaps
                                                )
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )
                                    }
                                }

                            } else {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(
                                                RoundedCornerShape(20.dp)
                                            )
                                            .background(
                                                Color(0xFFF5EAFA)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = InputBorder,
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        // Gallery / Placeholder Icon
                                        Icon(
                                            painter = painterResource(
                                                id = R.drawable.icon_file
                                            ),
                                            contentDescription = "Output Placeholder",
                                            tint = SendBtnGradientStart,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )

                                    Text(
                                        text = "Your processed images",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextTitle
                                    )

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Text(
                                        text = "The results will appear here",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
        color = ActionTextPurple
    )
}

@Composable
private fun PasswordField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {

    var isPasswordVisible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {

            Text(
                text = placeholder,
                color = TextMuted,
                fontSize = 13.sp
            )
        },
        leadingIcon = {

            // Lock Icon
            Icon(
                painter = painterResource(
                    id = R.drawable.icon_lock
                ),
                contentDescription = "Password Lock",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        visualTransformation =
            if (isPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        trailingIcon = {

            IconButton(
                onClick = {
                    isPasswordVisible =
                        !isPasswordVisible
                }
            ) {

                // Visibility Toggle Icon
                Icon(
                    painter = painterResource(
                        id =
                            if (isPasswordVisible)
                                R.drawable.icon_lock
                            else
                                R.drawable.icon_lock
                    ),
                    contentDescription =
                        if (isPasswordVisible)
                            "Hide Password"
                        else
                            "Show Password",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors =
            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SendBtnGradientStart,
                unfocusedBorderColor = InputBorder,
                focusedContainerColor = InputBg,
                unfocusedContainerColor = InputBg,
                focusedTextColor = TextBody,
                unfocusedTextColor = TextBody
            )
    )
}

@Composable
private fun GradientButton(
    text: String,
    iconResId: Int,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SendBtnGradientStart,
                        SendBtnGradientEnd
                    )
                )
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(
                    id = iconResId
                ),
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    iconResId: Int,
    onClick: () -> Unit
) {

    Button(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = InputBorder,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = InputBg,
            contentColor = TextBody
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(
                    id = iconResId
                ),
                contentDescription = text,
                tint = TextBody,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}