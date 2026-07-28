package com.inscopelabs.abx.xtools.bridge

import com.google.gson.annotations.SerializedName

/**
 * BridgeMessage represents the message structure for communication
 * between JavaScript and Kotlin through the WebView bridge.
 */
data class BridgeMessage(
    @SerializedName("id")
    val id: String,

    @SerializedName("action")
    val action: String,

    @SerializedName("params")
    val params: Map<String, Any>? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_GET_DEVICE_INFO = "getDeviceInfo"
        const val ACTION_SHOW_TOAST = "showToast"
        const val ACTION_GET_PREFERENCES = "getPreferences"
        const val ACTION_SET_PREFERENCES = "setPreferences"
        const val ACTION_PICK_FILE = "pickFile"
        const val ACTION_LOG = "log"
        const val ACTION_NAVIGATE = "navigate"
        const val ACTION_CLOSE = "close"
        const val ACTION_GET_PLUGIN_INFO = "getPluginInfo"
        const val ACTION_REQUEST_PERMISSION = "requestPermission"
        const val ACTION_CHECK_PERMISSION = "checkPermission"
    }
}
