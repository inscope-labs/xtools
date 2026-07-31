package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of events a plugin can emit to the host. The debug
 * package's [com.inscopelabs.abx.xtools.plugins.debug.LogBridge] consumes
 * these and renders them into the [ConsoleView].
 *
 * Keep this hierarchy small and stable — every variant is part of the
 * public SDK surface.
 */
@Serializable
sealed interface PluginEvent {

    val pluginId: PluginId
    val timestampMs: Long

    @Serializable
    data class Log(
        override val pluginId: PluginId,
        override val timestampMs: Long,
        val level: Level,
        val message: String,
    ) : PluginEvent {
        @Serializable
        enum class Level { DEBUG, INFO, WARN, ERROR }
    }

    @Serializable
    data class BridgeCall(
        override val pluginId: PluginId,
        override val timestampMs: Long,
        val method: String,
        val allowed: Boolean,
        val durationMs: Long,
        val resultPreview: String? = null,
        val args: List<String> = emptyList(),
    ) : PluginEvent

    @Serializable
    data class Error(
        override val pluginId: PluginId,
        override val timestampMs: Long,
        val message: String,
        val stack: String? = null,
    ) : PluginEvent

    @Serializable
    data class Lifecycle(
        override val pluginId: PluginId,
        override val timestampMs: Long,
        val phase: Phase,
    ) : PluginEvent {
        @Serializable
        enum class Phase { LOADED, RESUMED, PAUSED, DESTROYED }
    }
}
