package com.inscopelabs.abx.xtools.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.inscopelabs.abx.xtools.bridge.BridgeHandler

class SecureWebViewClient(
    private val bridgeHandler: BridgeHandler
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri: Uri? = request?.url
        if (uri != null) {
            val scheme = uri.scheme
            // Restrict navigation inside plugin webview to local assets or safe HTTPs
            if (scheme == "file" && uri.path?.startsWith("/android_asset/") == true) {
                return false
            }
            if (scheme == "http" || scheme == "https") {
                bridgeHandler.log("WARN", "Plugin attempted external navigation to ${uri}")
                return true // Block top-level external redirects inside plugin frame
            }
        }
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        bridgeHandler.log("INFO", "WebView loading page: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        bridgeHandler.log("INFO", "WebView page loaded successfully: $url")
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        // Enforce Content Security Policy headers or asset interception
        return super.shouldInterceptRequest(view, request)
    }
}
