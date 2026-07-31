package com.inscopelabs.abx.xtools.plugins.sdk.bridge

/**
 * Telemetry event the debug layer listens to. Kept separate from
 * [com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent] because the
 * bridge is part of the runtime, not the public plugin surface.
 */
data class BridgeCallEvent(
    val method: String,
    val allowed: Boolean,
    val durationMs: Long,
    val args: List<String> = emptyList(),
    val resultPreview: String,
)
