package com.inscopelabs.abx.xtools.bridge

import com.google.gson.annotations.SerializedName

/**
 * BridgeEvent represents events sent from Kotlin to JavaScript
 * without requiring a response (fire-and-forget pattern).
 */
data class BridgeEvent(
    @SerializedName("event")
    val event: String,

    @SerializedName("data")
    val data: Any? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val EVENT_READY = "ready"
        const val EVENT_RESUME = "resume"
        const val EVENT_PAUSE = "pause"
        const val EVENT_BACK_BUTTON = "backButton"
        const val EVENT_THEME_CHANGED = "themeChanged"
        const val EVENT_PERMISSION_RESULT = "permissionResult"
        const val EVENT_FILE_PICKED = "filePicked"
        const val EVENT_LIFECYCLE = "lifecycle"
    }
}
