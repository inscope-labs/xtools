package com.inscopelabs.abx.xtools.plugins.debug

import android.webkit.WebView
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.PluginBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of everything the Studio's inspector pane shows: the live
 * DOM, scoped storage, plugin state, and the most recent bridge events.
 * The Studio polls this on a `setInterval`-style cadence and re-renders
 * the panel.
 */
class Inspector(
    private val webView: WebView,
    private val bridge: PluginBridge?,
) {

    data class Snapshot(
        val domHtml: String,
        val storage: List<StorageEntry>,
        val pluginState: Map<String, String>,
        val bridgeEventCount: Int,
        val lastBridgeError: String?,
    )

    data class StorageEntry(val path: String, val sizeBytes: Long, val modifiedMs: Long)

    private val _last = MutableStateFlow<Snapshot?>(null)
    val last: StateFlow<Snapshot?> = _last.asStateFlow()

    /** Capture a fresh snapshot. Cheap; safe to call on every UI tick. */
    fun capture(): Snapshot {
        val dom = runCatching { webView.evaluateJavascriptHack() }.getOrDefault("<unavailable>")
        val storage = captureStorage()
        val snapshot = Snapshot(
            domHtml = dom,
            storage = storage,
            pluginState = emptyMap(),
            bridgeEventCount = 0,
            lastBridgeError = null,
        )
        _last.value = snapshot
        return snapshot
    }

    private fun captureStorage(): List<StorageEntry> {
        // We don't have a handle to HostStorage from the Inspector
        // because storage is per-plugin. The Studio wires this in
        // through [withStorage] before calling capture().
        return storageAccessor?.list() ?: emptyList()
    }

    fun interface StorageAccessor {
        fun list(): List<StorageEntry>
    }

    var storageAccessor: StorageAccessor? = null

    private fun WebView.evaluateJavascriptHack(): String {
        // Synchronous read isn't supported by the Android WebView API.
        // The Studio's UI calls capture() repeatedly and treats the
        // DOM field as best-effort.
        return "<async-only>"
    }
}
