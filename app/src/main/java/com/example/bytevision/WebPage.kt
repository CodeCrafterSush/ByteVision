package com.example.bytevision

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

@Composable
fun WebVieew() {
    val context = LocalContext.current
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Android permission milne ke baad WebRTC ko grant karein
            pendingPermissionRequest?.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
            pendingPermissionRequest?.deny()
        }
        pendingPermissionRequest = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 30.dp),
            factory = { webViewContext ->
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/assets/",
                        WebViewAssetLoader.AssetsPathHandler(webViewContext)
                    )
                    .build()

                WebView(webViewContext).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false

                        // WebRTC support settings
                        allowFileAccess = true
                        allowContentAccess = true
                        databaseEnabled = true
                    }

                    WebView.setWebContentsDebuggingEnabled(true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            val audioResources = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                            if (audioResources) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    // Direct Grant if permission already exists
                                    post { request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) }
                                } else {
                                    // Request runtime permission first
                                    pendingPermissionRequest = request
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                request.deny()
                            }
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/index.html")
                }
            }
        )
    }
}