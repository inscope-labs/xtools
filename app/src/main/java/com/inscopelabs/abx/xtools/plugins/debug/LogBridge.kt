package com.inscopelabs.abx.xtools.plugins.debug

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeCallEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * The console-side counterpart to the SDK's [com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge].
 * Plugins and the bridge push events into the SDK-side publisher; this
 * class subscribes and re-emits them through a single [SharedFlow] the
 * [ConsoleView] consumes.
 *
 * It's deliberately a thin fan-out so additional consumers (a future
 * "telemetry export" toggle, a recording test harness, etc.) can be
 * added without touching the publisher.
 */
class LogBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _out = MutableSharedFlow<Any>(extraBufferCapacity = 2048)
    val out: SharedFlow<Any> = _out.asSharedFlow()

    /**
     * Subscribe to the SDK's event log and forward every event to [out].
     * Call this once from the host (typically from a `Service.onCreate`).
     */
    fun start(sdkLog: com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge) {
        scope.launch {
            sdkLog.events.collect { _out.tryEmit(it) }
        }
    }

    /** Forward bridge-call events from a specific bridge instance. */
    fun attachBridge(events: SharedFlow<BridgeCallEvent>) {
        scope.launch {
            events.collect { _out.tryEmit(it) }
        }
    }

    fun log(pluginId: PluginId, level: PluginEvent.Log.Level, message: String) {
        _out.tryEmit(
            PluginEvent.Log(
                pluginId = pluginId,
                timestampMs = System.currentTimeMillis(),
                level = level,
                message = message,
            )
        )
    }

    fun error(pluginId: PluginId, message: String, stack: String? = null) {
        _out.tryEmit(
            PluginEvent.Error(
                pluginId = pluginId,
                timestampMs = System.currentTimeMillis(),
                message = message,
                stack = stack,
            )
        )
    }
}
