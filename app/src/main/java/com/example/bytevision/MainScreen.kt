package com.example.bytevision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.bytevision.ui.theme.GlassBackground
import com.example.bytevision.ui.theme.GlassBorder
import com.example.bytevision.ui.theme.TextMuted
import com.example.bytevision.ui.theme.TextTitle


enum class Screen {
    WEBVIEW,
    CAMERA,
    SHUFFLE,
    IMAGE_MESSAGE
}


@Composable
fun MainAppContainer() {

    var currentScreen by remember {
        mutableStateOf(Screen.WEBVIEW)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),

        bottomBar = {

            TextBottomNavigationBar(
                currentScreen = currentScreen,

                onScreenSelected = {
                    currentScreen = it
                }
            )
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            AnimatedContent(
                targetState = currentScreen,

                transitionSpec = {

                    fadeIn(
                        animationSpec = tween(200)
                    ) togetherWith fadeOut(
                        animationSpec = tween(200)
                    )
                },

                label = "ScreenSwitch"

            ) { targetScreen ->

                when (targetScreen) {

                    Screen.WEBVIEW -> {
                        WebVieew()
                    }

                    Screen.CAMERA -> {
                        CameraPage()
                    }

                    Screen.SHUFFLE -> {
                        Shuffle()
                    }

                    Screen.IMAGE_MESSAGE -> {
                        ImageMessage()
                    }
                }
            }
        }
    }
}


@Composable
fun TextBottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassBackground)
            .border(
                width = 0.5.dp,
                color = GlassBorder
            )
            .padding(
                vertical = 12.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceAround,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // WEBVIEW
            TextNavButton(
                title = "AUDIO",

                isSelected =
                    currentScreen == Screen.WEBVIEW,

                onClick = {
                    onScreenSelected(Screen.WEBVIEW)
                }
            )


            // CAMERA
            TextNavButton(
                title = "CAMERA",

                isSelected =
                    currentScreen == Screen.CAMERA,

                onClick = {
                    onScreenSelected(Screen.CAMERA)
                }
            )


            // SHUFFLE
            TextNavButton(
                title = "SHUFFLE",

                isSelected =
                    currentScreen == Screen.SHUFFLE,

                onClick = {
                    onScreenSelected(Screen.SHUFFLE)
                }
            )


            // IMAGE MESSAGE
            TextNavButton(
                title = "MESSAGE",

                isSelected =
                    currentScreen == Screen.IMAGE_MESSAGE,

                onClick = {
                    onScreenSelected(Screen.IMAGE_MESSAGE)
                }
            )
        }
    }
}


@Composable
fun TextNavButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = title,

                fontSize = 11.sp,

                fontWeight =
                    if (isSelected) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Medium
                    },

                color =
                    if (isSelected) {
                        TextTitle
                    } else {
                        TextMuted
                    },

                letterSpacing = 0.5.sp
            )


            // Selected indicator
            if (isSelected) {

                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(TextTitle)
                )
            }
        }
    }
}