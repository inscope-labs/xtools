package com.inscopelabs.abx.xtools.kernel.event

import com.inscopelabs.abx.xtools.kernel.mode.OperatingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * A simple publish‑subscribe event bus for inter‑component communication.
 * Supports typed events and automatic cleanup on plugin session termination.
 *
 * @see §2.1 Step 1.1.2, §6.3.1
 */
class EventBus(private val scope: CoroutineScope) {
    private val _events = MutableSharedFlow<XEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun publish(event: XEvent) {
        _events.emit(event)
    }

    fun subscribe(pluginId: String, onEvent: suspend (XEvent) -> Unit) {
        // In a full implementation, this would manage subscriber coroutines
        // scoped to the plugin session.
        scope.launch {
            events.collect { event ->
                if (event.targetPluginId == null || event.targetPluginId == pluginId) {
                    onEvent(event)
                }
            }
        }
    }
}

sealed class XEvent(
    open val sourcePluginId: String,
    open val targetPluginId: String? = null
) {
    data class ModeTransition(
        override val sourcePluginId: String,
        val newMode: OperatingMode
    ) : XEvent(sourcePluginId)

    data class PermissionChanged(
        override val sourcePluginId: String,
        val pluginId: String,
        val granted: Boolean
    ) : XEvent(sourcePluginId)
}
