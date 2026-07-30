package com.inscopelabs.abx.xtools.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.inscopelabs.abx.xtools.BuildConfig
import com.inscopelabs.abx.xtools.bridge.BridgeContract
import java.io.ByteArrayInputStream

/**
 * SecureWebView provides a hardened WebView configuration
 * with security best practices enabled.
 */
enum class PluginTrustLevel {
    UNTRUSTED,
    SANDBOXED,
    TRUSTED
}

@SuppressLint("SetJavaScriptEnabled")
class SecureWebView(context: Context) : WebView(context) {

    interface NavigationInterceptor {
        fun shouldInterceptNavigation(url: String): Boolean
        fun onSecurityError(error: String)
    }

    var navigationInterceptor: NavigationInterceptor? = null
    var trustLevel: PluginTrustLevel = PluginTrustLevel.SANDBOXED
        private set

    private val debugLogger = DebugConsoleLogger()

    init {
        setupSecureSettings()
        setupWebViewClient()
        setupCookieManager()
        setupWebViewDatabase(context)
        setupSafeBrowsing(context)
    }

    fun setPluginTrustLevel(level: PluginTrustLevel) {
        this.trustLevel = level
        // Gate content:// URL access strictly behind TRUSTED level
        settings.allowContentAccess = (level == PluginTrustLevel.TRUSTED)
    }

    private fun setupSafeBrowsing(context: Context) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(context) { success ->
                if (!success) {
                    debugLogger.logError("Safe Browsing initialization failed")
                }
            }
        }
    }

    private fun setupSecureSettings() {
        settings.apply {
            // JavaScript
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false

            // Security
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)

            // Cache
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true

            // Content
            mediaPlaybackRequiresUserGesture = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false

            // Database
            databaseEnabled = true

            // Mixed content mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            // Safe browsing (Android 8.0+)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                safeBrowsingEnabled = true
            }

            // Disable password saving
            savePassword = false
            saveFormData = false

            // Request focus
            isFocusable = true
            isFocusableInTouchMode = true

            // Disable file URL access by default
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            // Hardware acceleration
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun setupWebViewClient() {
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.url?.toString()?.let { url ->
                    // Log the request
                    debugLogger.logRequest(url)

                    // Check for custom scheme
                    if (request.isForMainFrame) {
                        // Apply CSP headers for main frame requests
                        return createCspResponse()
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.toString()?.let { url ->
                    // Check if we should handle this navigation
                    return if (navigationInterceptor?.shouldInterceptNavigation(url) == true) {
                        true
                    } else {
                        // Only allow https and file URLs
                        val scheme = request.url.scheme
                        if (scheme == "https" || scheme == "file") {
                            debugLogger.logNavigation(url)
                            false
                        } else {
                            navigationInterceptor?.onSecurityError("Blocked navigation to: $url")
                            true
                        }
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { debugLogger.logPageFinished(it) }
            }
        }
    }

    private fun createCspResponse(): WebResourceResponse? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebResourceResponse(
                "text/html",
                "utf-8",
                ByteArrayInputStream(ByteArray(0))
            ).apply {
                responseHeaders = mapOf(
                    "Content-Security-Policy" to BridgeContract.CSP
                )
            }
        } else {
            null
        }
    }

    private fun setupCookieManager() {
        try {
            CookieManager.getInstance().apply {
                setAcceptCookie(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(this@SecureWebView, false)
                }
            }
        } catch (e: Exception) {
            debugLogger.logError("CookieManager setup failed: ${e.message}")
        }
    }

    private fun setupWebViewDatabase(context: Context) {
        try {
            // Clear WebView data on each launch for security
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.deleteDatabase("webview.db")
                context.deleteDatabase("webviewCache.db")
            }
        } catch (e: Exception) {
            debugLogger.logError("WebView database cleanup failed: ${e.message}")
        }
    }

    /**
     * Inject debug console logging script into the WebView.
     */
    fun injectDebugConsole() {
        evaluateJavascript(debugLogger.getConsoleScript(), null)
    }

    /**
     * Inject Content Security Policy meta tag.
     */
    fun injectCSP() {
        val script = """
            (function() {
                var meta = document.createElement('meta');
                meta.httpEquiv = 'Content-Security-Policy';
                meta.content = '${BridgeContract.CSP.replace("'", "\\'")}';
                document.head.appendChild(meta);
            })();
        """.trimIndent()
        evaluateJavascript(script, null)
    }
}
