package com.example.bytevision

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bytevision.ui.theme.ActionTextPurple
import com.example.bytevision.ui.theme.BgGradientCircle1
import com.example.bytevision.ui.theme.BgGradientCircle2
import com.example.bytevision.ui.theme.BgGradientEnd
import com.example.bytevision.ui.theme.BgGradientStart
import com.example.bytevision.ui.theme.GlassBackground
import com.example.bytevision.ui.theme.GlassBorder
import com.example.bytevision.ui.theme.InputBg
import com.example.bytevision.ui.theme.InputBorder
import com.example.bytevision.ui.theme.ListenBtnBg
import com.example.bytevision.ui.theme.ListenBtnText
import com.example.bytevision.ui.theme.SendBtnGradientEnd
import com.example.bytevision.ui.theme.SendBtnGradientStart
import com.example.bytevision.ui.theme.StatusBg
import com.example.bytevision.ui.theme.StatusBorder
import com.example.bytevision.ui.theme.StatusText
import com.example.bytevision.ui.theme.TextBody
import com.example.bytevision.ui.theme.TextMuted
import com.example.bytevision.ui.theme.TextSubtitle
import com.example.bytevision.ui.theme.TextTitle


@Composable
fun Shuffle() {

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var shufflePassword by remember {
        mutableStateOf("")
    }

    var unshufflePassword by remember {
        mutableStateOf("")
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            selectedImageUri = uri
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
                            BgGradientStart,
                            BgGradientEnd
                        )
                    )
                )
                .padding(innerPadding)
        ) {

            // Background Circle 1
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopStart)
                    .background(
                        color = BgGradientCircle1,
                        shape = RoundedCornerShape(200.dp)
                    )
            )

            // Background Circle 2
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        color = BgGradientCircle2,
                        shape = RoundedCornerShape(200.dp)
                    )
            )


            // Main Scroll Content
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // MAIN CONTAINER
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = GlassBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        GlassBorder
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 18.dp,
                                vertical = 28.dp
                            )
                    ) {

                        // HEADER
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Image Shuffle",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTitle
                            )

                            Spacer(
                                modifier = Modifier.height(7.dp)
                            )

                            Text(
                                text = "Securely transform your images with a password",
                                fontSize = 13.sp,
                                color = TextSubtitle
                            )
                        }


                        Spacer(
                            modifier = Modifier.height(28.dp)
                        )


                        // CONTENT
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {

                            // LEFT CARD
                            ShuffleInputCard(
                                selectedImageUri = selectedImageUri,
                                shufflePassword = shufflePassword,
                                unshufflePassword = unshufflePassword,

                                onImageClick = {
                                    imagePickerLauncher.launch("image/*")
                                },

                                onShufflePasswordChange = {
                                    shufflePassword = it
                                },

                                onUnshufflePasswordChange = {
                                    unshufflePassword = it
                                },

                                onShuffleClick = {
                                    shuffleImage()
                                },

                                onUnshuffleClick = {
                                    unshuffleImage()
                                }
                            )


                            // RIGHT CARD
                            ShuffleOutputCard(
                                selectedImageUri = selectedImageUri
                            )
                        }
                    }
                }
            }
        }
    }
}


// =========================================================
// INPUT CARD
// =========================================================

@Composable
private fun ShuffleInputCard(
    selectedImageUri: Uri?,
    shufflePassword: String,
    unshufflePassword: String,
    onImageClick: () -> Unit,
    onShufflePasswordChange: (String) -> Unit,
    onUnshufflePasswordChange: (String) -> Unit,
    onShuffleClick: () -> Unit,
    onUnshuffleClick: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.52f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.8f)
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // IMAGE INPUT LABEL
            SectionLabel(
                text = "IMAGE INPUT"
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // IMAGE UPLOAD BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
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
                        onImageClick()
                    },
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // UPLOAD SYMBOL
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(18.dp)
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SendBtnGradientStart,
                                        SendBtnGradientEnd
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "↑",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(15.dp)
                    )


                    Text(
                        text = if (selectedImageUri == null) {
                            "Drop your image here"
                        } else {
                            "Image selected"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTitle
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    Text(
                        text = if (selectedImageUri == null) {
                            "or tap to browse from your device"
                        } else {
                            "Tap to select another image"
                        },
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // SHUFFLE PASSWORD
            SectionLabel(
                text = "SHUFFLE PASSWORD"
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            PasswordField(
                value = shufflePassword,
                placeholder = "Enter password for shuffling",
                onValueChange = onShufflePasswordChange
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // SHUFFLE BUTTON
            GradientButton(
                text = "↻  Shuffle Image",
                onClick = onShuffleClick
            )


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // UNSHUFFLE PASSWORD
            SectionLabel(
                text = "UNSHUFFLE PASSWORD"
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            PasswordField(
                value = unshufflePassword,
                placeholder = "Enter password for unshuffling",
                onValueChange = onUnshufflePasswordChange
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // UNSHUFFLE BUTTON
            SecondaryButton(
                text = "↺  Unshuffle Image",
                onClick = onUnshuffleClick
            )
        }
    }
}


// =========================================================
// OUTPUT CARD
// =========================================================

@Composable
private fun ShuffleOutputCard(
    selectedImageUri: Uri?
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.52f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.8f)
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // OUTPUT LABEL
            SectionLabel(
                text = "OUTPUT"
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // OUTPUT BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(
                        RoundedCornerShape(17.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = InputBorder,
                        shape = RoundedCornerShape(17.dp)
                    )
                    .background(
                        Color.White.copy(alpha = 0.72f)
                    ),
                contentAlignment = Alignment.Center
            ) {


                // STATUS
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
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
                            vertical = 6.dp
                        )
                ) {

                    Text(
                        text = "Waiting",
                        fontSize = 10.sp,
                        color = StatusText
                    )
                }


                // OUTPUT CONTENT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // OUTPUT SYMBOL
                    Box(
                        modifier = Modifier
                            .size(70.dp)
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

                        Text(
                            text = "◇",
                            color = SendBtnGradientStart,
                            fontSize = 30.sp
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(15.dp)
                    )


                    Text(
                        text = "Your processed image",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTitle
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    Text(
                        text = "The result will appear here",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}


// =========================================================
// SECTION LABEL
// =========================================================

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


// =========================================================
// PASSWORD FIELD
// =========================================================

@Composable
private fun PasswordField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {

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

        singleLine = true,

        visualTransformation =
            PasswordVisualTransformation(),

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),

        shape = RoundedCornerShape(14.dp),

        colors =
            androidx.compose.material3
                .OutlinedTextFieldDefaults
                .colors(
                    focusedBorderColor =
                        SendBtnGradientStart,

                    unfocusedBorderColor =
                        InputBorder,

                    focusedContainerColor =
                        InputBg,

                    unfocusedContainerColor =
                        InputBg,

                    focusedTextColor =
                        TextBody,

                    unfocusedTextColor =
                        TextBody
                )
    )
}


// =========================================================
// GRADIENT BUTTON
// =========================================================

@Composable
private fun GradientButton(
    text: String,
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

        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// =========================================================
// SECONDARY BUTTON
// =========================================================

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit
) {

    Button(
        onClick = {
            onClick()
        },

        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),

        shape = RoundedCornerShape(14.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = ListenBtnBg,
            contentColor = ListenBtnText
        ),

        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp
        )
    ) {

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// =========================================================
// SHUFFLE FUNCTION
// =========================================================

private fun shuffleImage() {

    // Empty for now
}


// =========================================================
// UNSHUFFLE FUNCTION
// =========================================================

private fun unshuffleImage() {

    // Empty for now
}