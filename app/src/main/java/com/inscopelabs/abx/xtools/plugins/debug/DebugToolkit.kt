package com.inscopelabs.abx.xtools.plugins.debug

import android.content.Context
import android.webkit.WebView
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge as SdkLogBridge
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.PluginBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The four debug components are useless in isolation. [DebugToolkit] is
 * the one-stop wiring the host creates once and then exposes through a
 * single accessor:
 *
 *     val toolkit = DebugToolkit.attach(context, webView, bridge)
 *     val console: ConsoleView = toolkit.consoleView
 *     val inspector: Inspector = toolkit.inspector
 *     val performance: PerformanceMonitor = toolkit.performance
 *     val log: LogBridge = toolkit.logBridge
 *
 * Every component shares the same [CoroutineScope] so cancellation
 * propagates cleanly when the host tears down the WebView.
 */
class DebugToolkit private constructor(
    val consoleView: ConsoleView,
    val inspector: Inspector,
    val performance: PerformanceMonitor,
    val logBridge: LogBridge,
    private val scope: CoroutineScope,
) {

    fun start() {
        scope.launch {
            logBridge.out.collect { event ->
                when (event) {
                    is com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeCallEvent ->
                        performance.record(event)
                    is com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent ->
                        consoleView.submit(
                            ConsoleView.Line(
                                level = when (event) {
                                    is com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.Error -> "ERROR"
                                    is com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.Log -> event.level.name
                                    is com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.BridgeCall -> "BRIDGE"
                                    is com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.Lifecycle -> "LIFECYCLE"
                                },
                                message = event.toString(),
                                tag = event.pluginId.value,
                            )
                        )
                    else -> Unit
                }
            }
        }
    }

    fun stop() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    companion object {
        /**
         * Convenience constructor. Pass `null` for [bridge] when
         * attaching the toolkit to a plain WebView (e.g. the code
         * editor) that doesn't carry a [PluginBridge].
         */
        fun attach(
            context: Context,
            webView: WebView,
            bridge: PluginBridge?,
            sdkLog: SdkLogBridge = SdkLogBridge.shared,
        ): DebugToolkit {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val logBridge = LogBridge().also { it.start(sdkLog) }
            bridge?.let { logBridge.attachBridge(it.events) }
            val console = ConsoleView(context)
            val perf = PerformanceMonitor()
            val inspector = Inspector(webView, bridge)
            val toolkit = DebugToolkit(console, inspector, perf, logBridge, scope)
            toolkit.start()
            return toolkit
        }
    }
}
