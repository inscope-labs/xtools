package com.inscopelabs.abx.xtools.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.inscopelabs.abx.xtools.ui.MainActivity

/**
 * Handles deep linking, custom URL scheme support, and back navigation.
 * Supports schemes like `xtools://plugin/{pluginId}`.
 *
 * @see §3.2 Step 2.2.1, §3.2 Step 2.2.2
 */
class NavigationRouter(private val activity: AppCompatActivity) {

    companion object {
        const val SCHEME = "xtools"
        const val HOST_PLUGIN = "plugin"
        const val HOST_SETTINGS = "settings"
    }

    /**
     * Parses an incoming intent to determine if it's a deep link we handle.
     * Called from MainActivity.onNewIntent().
     */
    fun handleDeepLink(intent: Intent): Boolean {
        val data: Uri? = intent.data
        if (data == null || data.scheme != SCHEME) return false

        return when (data.host) {
            HOST_PLUGIN -> {
                val pluginId = data.pathSegments.firstOrNull()
                if (pluginId != null) {
                    // Navigate to PluginDetailFragment via MainActivity.
                    (activity as? MainActivity)?.navigateToPluginDetail(pluginId)
                    true
                } else false
            }
            HOST_SETTINGS -> {
                (activity as? MainActivity)?.navigateToSettings()
                true
            }
            else -> false
        }
    }

    /**
     * Handles WebView navigation requests for custom URLs intercepted by the WebViewClient.
     * Stub: validates the URL against registered plugin patterns.
     */
    fun validateCustomUrl(url: String): Boolean {
        // Placeholder security check.
        return url.startsWith("$SCHEME://") && !url.contains("..")
    }

    /**
     * Manages the back stack: if the WebView has history, navigate back there;
     * otherwise fallback to native fragment back stack.
     */
    fun handleBackPressed(webViewCanGoBack: Boolean): Boolean {
        // In the real implementation, this coordinates with the WebView.
        return if (webViewCanGoBack) {
            // webView.goBack(); // handled by the WebView component
            true
        } else {
            // Let the fragment manager handle it.
            false
        }
    }
}
