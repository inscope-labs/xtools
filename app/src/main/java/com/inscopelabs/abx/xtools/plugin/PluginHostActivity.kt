package com.inscopelabs.abx.xtools.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.bridge.BridgeContract
import com.inscopelabs.abx.xtools.bridge.BridgeEvent
import com.inscopelabs.abx.xtools.bridge.BridgeMessage
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.bridge.JavaScriptBridge
import com.inscopelabs.abx.xtools.databinding.ActivityPluginHostBinding
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager
import com.inscopelabs.abx.xtools.webview.SecureWebView
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * PluginHostActivity is the main host for loading and displaying
 * HTML/JavaScript plugins in a secure WebView environment.
 */
class PluginHostActivity : AppCompatActivity(), JavaScriptBridge.BridgeCallback {

    private lateinit var binding: ActivityPluginHostBinding
    private lateinit var secureWebView: SecureWebView
    private lateinit var jsBridge: JavaScriptBridge
    private lateinit var pluginManager: PluginManager

    private var currentPluginId: String? = null
    private var pluginManifest: PluginManifest? = null
    private var isLoading = false

    companion object {
        private const val EXTRA_PLUGIN_ID = "plugin_id"
        private const val EXTRA_PLUGIN_ASSET = "plugin_asset"

        fun createIntent(context: Context, pluginId: String): Intent {
            return Intent(context, PluginHostActivity::class.java).apply {
                putExtra(EXTRA_PLUGIN_ID, pluginId)
            }
        }

        fun createIntentFromAsset(context: Context, assetPath: String): Intent {
            return Intent(context, PluginHostActivity::class.java).apply {
                putExtra(EXTRA_PLUGIN_ASSET, assetPath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityPluginHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pluginManager = PluginManager.getInstance(this)

        setupToolbar()
        setupWebView()
        setupBackNavigation()
        setupProgressIndicator()

        // Load the plugin
        loadPlugin()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        secureWebView = SecureWebView(this)

        // Set up navigation interceptor
        secureWebView.navigationInterceptor = object : SecureWebView.NavigationInterceptor {
            override fun shouldInterceptNavigation(url: String): Boolean {
                return !isUrlAllowed(url)
            }

            override fun onSecurityError(error: String) {
                showError("Security Error: $error")
            }
        }

        // Add WebView to container
        binding.webViewContainer.addView(
            secureWebView,
            0,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Set up JavaScript bridge
        jsBridge = JavaScriptBridge(secureWebView)
        jsBridge.setBridgeCallback(this)
        jsBridge.inject()

        // Handle prompt for bridge communication
        secureWebView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                message?.let {
                    android.util.Log.d("xtools:console", "[${it.sourceId()}:${it.lineNumber()}] ${it.message()}")
                }
                return super.onConsoleMessage(message)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                updateProgress(newProgress)
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (secureWebView.canGoBack()) {
                    // Check if plugin wants to handle back button
                    jsBridge.sendEvent(
                        BridgeEvent(
                            event = BridgeContract.Events.BACK_BUTTON,
                            data = emptyMap<String, Any>()
                        )
                    )
                } else {
                    // Allow default back navigation
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupProgressIndicator() {
        binding.progressIndicator.apply {
            max = 100
            progress = 0
            setIndicatorColor(getColor(R.color.primary))
            trackColor = getColor(R.color.surface_container)
        }
    }

    private fun loadPlugin() {
        val pluginId = intent.getStringExtra(EXTRA_PLUGIN_ID)
        val pluginAsset = intent.getStringExtra(EXTRA_PLUGIN_ASSET)

        when {
            !pluginId.isNullOrEmpty() -> loadPluginById(pluginId)
            !pluginAsset.isNullOrEmpty() -> loadPluginFromAsset(pluginAsset)
            else -> {
                showError(getString(R.string.no_plugins))
            }
        }
    }

    private fun loadPluginById(pluginId: String) {
        currentPluginId = pluginId
        isLoading = true

        lifecycleScope.launch {
            try {
                val plugin = pluginManager.getPlugin(pluginId)
                if (plugin != null) {
                    pluginManifest = plugin.manifest
                    supportActionBar?.title = plugin.name

                    val pluginPath = pluginManager.getPluginPath(pluginId)
                    val entryPoint = plugin.entryPoint ?: "index.html"
                    val url = "file://${pluginPath}/${entryPoint}"

                    loadUrl(url)
                } else {
                    showError("Plugin not found: $pluginId")
                }
            } catch (e: Exception) {
                showError("Failed to load plugin: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadPluginFromAsset(assetPath: String) {
        isLoading = true
        currentPluginId = assetPath

        try {
            val htmlContent = loadAssetFile(assetPath)
            val baseUrl = "file:///android_asset/${assetPath.substringBeforeLast("/")}/"

            secureWebView.loadDataWithBaseURL(
                baseUrl,
                htmlContent,
                "text/html",
                "utf-8",
                null
            )

            supportActionBar?.title = assetPath.substringAfterLast("/").substringBefore(".")
        } catch (e: Exception) {
            showError("Failed to load asset: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    private fun loadUrl(url: String) {
        showLoading(true)
        secureWebView.loadUrl(url)
    }

    private fun loadAssetFile(path: String): String {
        return try {
            val inputStream = assets.open(path)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readText()
        } catch (e: Exception) {
            throw IllegalArgumentException("Asset not found: $path")
        }
    }

    private fun isUrlAllowed(url: String): Boolean {
        // Only allow https and file URLs
        val uri = Uri.parse(url)
        return uri.scheme in listOf("https", "file", "data")
    }

    private fun showLoading(show: Boolean) {
        binding.progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressIndicator.progress = 0
    }

    private fun updateProgress(progress: Int) {
        binding.progressIndicator.progress = progress
        if (progress >= 100) {
            binding.progressIndicator.visibility = View.GONE
            jsBridge.sendEvent(
                BridgeEvent(
                    event = BridgeContract.Events.READY,
                    data = mapOf(
                        "pluginId" to (currentPluginId ?: ""),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            )
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.progressIndicator.visibility = View.GONE

        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                        background: #faf9fc;
                        color: #1a1c1e;
                    }
                    .error {
                        text-align: center;
                        padding: 2rem;
                    }
                    h2 { color: #ba1a1a; }
                </style>
            </head>
            <body>
                <div class="error">
                    <h2>Plugin Error</h2>
                    <p>${message.replace("'", "\\'")}</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        secureWebView.loadDataWithBaseURL(null, errorHtml, "text/html", "utf-8", null)
    }

    // JavaScriptBridge.BridgeCallback implementation
    override fun onMessage(message: BridgeMessage): BridgeResponse {
        return when (message.action) {
            BridgeContract.Actions.GET_DEVICE_INFO -> handleGetDeviceInfo()
            BridgeContract.Actions.SHOW_TOAST -> handleShowToast(message)
            BridgeContract.Actions.GET_PREFERENCES -> handleGetPreferences(message)
            BridgeContract.Actions.SET_PREFERENCES -> handleSetPreferences(message)
            BridgeContract.Actions.LOG -> handleLog(message)
            BridgeContract.Actions.CLOSE -> handleClose()
            BridgeContract.Actions.GET_PLUGIN_INFO -> handleGetPluginInfo()
            BridgeContract.Actions.NAVIGATE -> handleNavigate(message)
            else -> BridgeResponse.error(message.id, "Unknown action: ${message.action}")
        }
    }

    override fun onError(error: String) {
        android.util.Log.e("xtools:bridge", error)
    }

    private fun handleGetDeviceInfo(): BridgeResponse {
        val deviceInfo = mapOf(
            "platform" to "android",
            "platformVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "appVersion" to packageManager.getPackageInfo(packageName, 0).versionName,
            "language" to resources.configuration.locales[0].language,
            "timezone" to java.util.TimeZone.getDefault().id
        )
        return BridgeResponse.success("device-info", deviceInfo)
    }

    private fun handleShowToast(message: BridgeMessage): BridgeResponse {
        val text = message.params?.get("message") as? String ?: ""
        val duration = message.params?.get("duration") as? String ?: "short"

        val durationValue = when (duration) {
            "long" -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }

        Toast.makeText(this, text, durationValue).show()
        return BridgeResponse.success(message.id)
    }

    private fun handleGetPreferences(message: BridgeMessage): BridgeResponse {
        val key = message.params?.get("key") as? String
        val value = key?.let { pluginManager.getPreference(it) }
        return BridgeResponse.success(message.id, mapOf("key" to key, "value" to value))
    }

    private fun handleSetPreferences(message: BridgeMessage): BridgeResponse {
        val key = message.params?.get("key") as? String
        val value = message.params?.get("value")
        if (key != null && value != null) {
            pluginManager.setPreference(key, value)
        }
        return BridgeResponse.success(message.id)
    }

    private fun handleLog(message: BridgeMessage): BridgeResponse {
        val level = message.params?.get("level") as? String ?: "info"
        val text = message.params?.get("message") as? String ?: ""
        android.util.Log.println(
            when (level) {
                "error" -> android.util.Log.ERROR
                "warning", "warn" -> android.util.Log.WARN
                "debug" -> android.util.Log.DEBUG
                else -> android.util.Log.INFO
            },
            "xtools:plugin",
            text
        )
        return BridgeResponse.success(message.id)
    }

    private fun handleClose(): BridgeResponse {
        finish()
        return BridgeResponse.success("close")
    }

    private fun handleGetPluginInfo(): BridgeResponse {
        val info = pluginManifest?.let {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "version" to it.version,
                "description" to (it.description ?: ""),
                "author" to (it.author ?: ""),
                "permissions" to (it.permissions)
            )
        } ?: emptyMap<String, Any>()
        return BridgeResponse.success("plugin-info", info)
    }

    private fun handleNavigate(message: BridgeMessage): BridgeResponse {
        val url = message.params?.get("url") as? String
        if (!url.isNullOrEmpty()) {
            loadUrl(url)
        }
        return BridgeResponse.success(message.id)
    }

    override fun onResume() {
        super.onResume()
        secureWebView.onResume()
        jsBridge.sendEvent(BridgeEvent(event = BridgeContract.Events.RESUME))
    }

    override fun onPause() {
        super.onPause()
        secureWebView.onPause()
        jsBridge.sendEvent(BridgeEvent(event = BridgeContract.Events.PAUSE))
    }

    override fun onDestroy() {
        super.onDestroy()
        jsBridge.clearCallbacks()
        binding.webViewContainer.removeAllViews()
        secureWebView.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes
    }
}
