package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Structured event log the SDK publishes. The debug package's
 * [com.inscopelabs.abx.xtools.plugins.debug.ConsoleView] subscribes to
 * this; production releases can opt in via the same path to surface logs
 * in the in-app bug-report tool.
 *
 * Implementations are push-only — consumers don't get an acknowledgement.
 * The buffer is generous (1024) so a slow UI doesn't drop live previews.
 */
class LogBridge {

    private val _events = MutableSharedFlow<PluginEvent>(extraBufferCapacity = 1024)

    val events: SharedFlow<PluginEvent> = _events.asSharedFlow()

    fun publish(event: PluginEvent) {
        _events.tryEmit(event)
    }

    companion object {
        /** Process-wide default; tests should construct their own instance. */
        val shared: LogBridge = LogBridge()
    }
}
