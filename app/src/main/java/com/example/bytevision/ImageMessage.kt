package com.example.bytevision

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bytevision.ui.theme.GlassBorder
import com.example.bytevision.ui.theme.InputBg
import com.example.bytevision.ui.theme.InputBorder
import com.example.bytevision.ui.theme.SendBtnGradientStart
import com.example.bytevision.ui.theme.StatusBg
import com.example.bytevision.ui.theme.StatusBorder
import com.example.bytevision.ui.theme.StatusText
import com.example.bytevision.ui.theme.TextMuted
import com.example.bytevision.ui.theme.TextSubtitle
import com.example.bytevision.ui.theme.TextTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageMessage() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // UI Local States
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var outputBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var password by remember { mutableStateOf("") }
    var secretMessage by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Waiting") }
    var isLoading by remember { mutableStateOf(false) }
    val savingStates = remember { mutableStateMapOf<Int, Boolean>() }

    // Multiple Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            val loadedBitmaps = mutableListOf<Bitmap>()
            for (uri in uris) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { loadedBitmaps.add(it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            selectedBitmaps = loadedBitmaps
            outputBitmaps = emptyList()
            statusText = "${loadedBitmaps.size} Image(s) Loaded"
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
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 24.dp)
                    ) {

                        // HEADER
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Image Steganography",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTitle
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Hide secret text messages inside your images safely",
                                fontSize = 13.sp,
                                color = TextSubtitle
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 1. INPUT IMAGE BOX
                        SectionLabel(text = "IMAGE INPUT")

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(17.dp))
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
                                    columns = GridCells.Adaptive(minSize = 60.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(selectedBitmaps) { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Selected Image",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_upload_file),
                                        contentDescription = "Upload",
                                        tint = SendBtnGradientStart,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Drop your image here",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextTitle
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "or tap to browse image",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // CLEAR ALL BUTTON FOR INPUT IMAGES
                        if (selectedBitmaps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedUris = emptyList()
                                        selectedBitmaps = emptyList()
                                        outputBitmaps = emptyList()
                                        secretMessage = ""
                                        password = ""
                                        statusText = "Waiting"
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.icon_delete),
                                            contentDescription = "Clear All",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = "Clear All",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. PASSWORD FIELD
                        SectionLabel(text = "PASSWORD")

                        Spacer(modifier = Modifier.height(8.dp))

                        PasswordField(
                            value = password,
                            placeholder = "Enter key password",
                            onValueChange = { password = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. SECRET MESSAGE INPUT FIELD
                        SectionLabel(text = "SECRET MESSAGE")

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = secretMessage,
                            onValueChange = { secretMessage = it },
                            placeholder = {
                                Text(
                                    text = "Enter text message to hide in image...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = InputBg,
                                focusedContainerColor = InputBg,
                                unfocusedBorderColor = InputBorder,
                                focusedBorderColor = SendBtnGradientStart,
                                focusedTextColor = TextTitle,
                                unfocusedTextColor = TextTitle
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4. BUTTONS ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GradientButton(
                                    text = "Hide Message",
                                    iconResId = R.drawable.icon_file_delete,
                                    onClick = {
                                        if (selectedUris.isEmpty()) {
                                            showToast(context, "Please select an image first")
                                            return@GradientButton
                                        }
                                        if (password.isBlank()) {
                                            showToast(context, "Please enter a password")
                                            return@GradientButton
                                        }
                                        if (secretMessage.isBlank()) {
                                            showToast(context, "Please enter a message to hide")
                                            return@GradientButton
                                        }

                                        isLoading = true
                                        statusText = "Encrypting..."

                                        coroutineScope.launch(Dispatchers.Default) {
                                            val results = mutableListOf<Bitmap>()
                                            for (uri in selectedUris) {
                                                val processed = encodeMessageToImage(
                                                    context = context,
                                                    imageUri = uri,
                                                    message = secretMessage,
                                                    password = password
                                                )
                                                if (processed != null) {
                                                    results.add(processed)
                                                }
                                            }

                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                if (results.isNotEmpty()) {
                                                    outputBitmaps = results
                                                    statusText = "Message Hidden!"
                                                    showToast(context, "Message hidden successfully!")
                                                } else {
                                                    statusText = "Failed"
                                                    showToast(context, "Image is too small or processing failed")
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                SecondaryButton(
                                    text = "Extract Message",
                                    iconResId = R.drawable.icon_file_delete,
                                    onClick = {
                                        if (selectedUris.isEmpty()) {
                                            showToast(context, "Please select an image first")
                                            return@SecondaryButton
                                        }
                                        if (password.isBlank()) {
                                            showToast(context, "Please enter password")
                                            return@SecondaryButton
                                        }

                                        isLoading = true
                                        statusText = "Decrypting..."

                                        coroutineScope.launch(Dispatchers.Default) {
                                            val extracted = decodeMessageFromImage(
                                                context = context,
                                                imageUri = selectedUris.first(),
                                                password = password
                                            )

                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                if (extracted != null) {
                                                    secretMessage = extracted
                                                    statusText = "Extracted!"
                                                    showToast(context, "Secret message extracted!")
                                                } else {
                                                    statusText = "Failed"
                                                    showToast(context, "Incorrect password or no hidden text found")
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 5. OUTPUT BOX
                        SectionLabel(text = "OUTPUT")

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .border(
                                    width = 1.dp,
                                    color = InputBorder,
                                    shape = RoundedCornerShape(17.dp)
                                )
                                .background(Color.White.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // STATUS BADGE
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(StatusBg)
                                    .border(
                                        width = 1.dp,
                                        color = StatusBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
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

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Processing image...",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            } else if (outputBitmaps.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    itemsIndexed(outputBitmaps) { index, bmp ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Processed Image $index",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Fit
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val isSavingThis = savingStates[index] ?: false
                                                TextButton(
                                                    onClick = {
                                                        if (isSavingThis) return@TextButton
                                                        savingStates[index] = true
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            val saved = saveBitmapToGallery(context, bmp)
                                                            withContext(Dispatchers.Main) {
                                                                savingStates[index] = false
                                                                if (saved) {
                                                                    showToast(context, "Saved to Pictures/ByteVision!")
                                                                } else {
                                                                    showToast(context, "Failed to save image")
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = !isSavingThis
                                                ) {
                                                    if (isSavingThis) {
                                                        CircularProgressIndicator(
                                                            color = SendBtnGradientStart,
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Saving...",
                                                            fontSize = 13.sp,
                                                            color = TextMuted
                                                        )
                                                    } else {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.icon_upload_file),
                                                                contentDescription = "Save",
                                                                tint = SendBtnGradientStart,
                                                                modifier = Modifier.size(16.dp)
                                                            )

                                                            Spacer(modifier = Modifier.width(6.dp))

                                                            Text(
                                                                text = "Save PNG Image",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = SendBtnGradientStart
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "${bmp.width} × ${bmp.height} px",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextMuted
                                                )
                                            }
                                        }
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
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFF5EAFA))
                                            .border(
                                                width = 1.dp,
                                                color = InputBorder,
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.icon_file),
                                            contentDescription = "Placeholder",
                                            tint = SendBtnGradientStart,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Your processed images",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextTitle
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

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

// ==========================================
// TOAST & GALLERY HELPER UTILS
// ==========================================

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

