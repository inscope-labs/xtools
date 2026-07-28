package com.inscopelabs.abx.xtools.bridge

/**
 * Bridge contract definitions for the Kotlin-JavaScript bridge.
 * This object defines the interface contract for all bridge communications.
 */
object BridgeContract {

    /**
     * Available actions that can be invoked from JavaScript.
     */
    object Actions {
        const val GET_DEVICE_INFO = "getDeviceInfo"
        const val SHOW_TOAST = "showToast"
        const val GET_PREFERENCES = "getPreferences"
        const val SET_PREFERENCES = "setPreferences"
        const val PICK_FILE = "pickFile"
        const val LOG = "log"
        const val NAVIGATE = "navigate"
        const val CLOSE = "close"
        const val GET_PLUGIN_INFO = "getPluginInfo"
        const val REQUEST_PERMISSION = "requestPermission"
        const val CHECK_PERMISSION = "checkPermission"
        const val OPEN_URL = "openUrl"
        const val SHARE = "share"
        const val GET_STORAGE = "getStorage"
        const val SET_STORAGE = "setStorage"
        const val CLEAR_STORAGE = "clearStorage"
    }

    /**
     * Events that can be sent from Kotlin to JavaScript.
     */
    object Events {
        const val READY = "ready"
        const val RESUME = "resume"
        const val PAUSE = "pause"
        const val BACK_BUTTON = "backButton"
        const val THEME_CHANGED = "themeChanged"
        const val PERMISSION_RESULT = "permissionResult"
        const val FILE_PICKED = "filePicked"
        const val LIFECYCLE = "lifecycle"
    }

    /**
     * Log levels for bridge logging.
     */
    object LogLevel {
        const val DEBUG = "debug"
        const val INFO = "info"
        const val WARNING = "warning"
        const val ERROR = "error"
    }

    /**
     * Content Security Policy for the WebView.
     */
    val CSP = """
        default-src 'self';
        script-src 'self' 'unsafe-inline' 'unsafe-eval';
        style-src 'self' 'unsafe-inline';
        img-src 'self' data: blob: https:;
        font-src 'self' data:;
        connect-src 'self' https:;
        frame-src 'self';
        object-src 'none';
        base-uri 'self';
        form-action 'self';
        frame-ancestors 'none';
    """.trimIndent()
}
