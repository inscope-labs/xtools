package com.inscopelabs.abx.xtools.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.inscopelabs.abx.xtools.bridge.BridgeHandler
import com.inscopelabs.abx.xtools.bridge.JsBridge

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XToolsWebView(
    pluginAssetPath: String, // e.g. "plugins/sample"
    entryFile: String,       // e.g. "index.html"
    jsBridge: JsBridge,
    bridgeHandler: BridgeHandler,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current

    val webView = remember(context) {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = false
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val level = when (it.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                            ConsoleMessage.MessageLevel.WARNING -> "WARN"
                            else -> "LOG"
                        }
                        bridgeHandler.log(level, "[JS Console] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                    }
                    return true
                }
            }

            webViewClient = SecureWebViewClient(bridgeHandler)

            addJavascriptInterface(jsBridge, "XToolsNativeBridge")
            jsBridge.attachWebView(this)
            onWebViewCreated(this)
        }
    }

    DisposableEffect(pluginAssetPath, entryFile) {
        val fullAssetUrl = "file:///android_asset/$pluginAssetPath/$entryFile"
        webView.loadUrl(fullAssetUrl)

        onDispose {
            jsBridge.detachWebView()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
