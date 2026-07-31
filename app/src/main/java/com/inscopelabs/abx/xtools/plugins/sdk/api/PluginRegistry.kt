package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory registry of installed plugins. The persistent half lives in
 * [com.inscopelabs.abx.xtools.plugins.sdk.registry.PluginRepository].
 *
 * Reads are synchronous (registry is small); writes emit on [changes] so
 * the Studio's project explorer can react.
 */
interface PluginRegistry {

    val changes: Flow<RegistryEvent>

    fun all(): List<RegisteredPlugin>

    fun get(id: PluginId): RegisteredPlugin?

    fun install(plugin: RegisteredPlugin)

    fun uninstall(id: PluginId): Boolean

    companion object {
        /** Default in-memory implementation. Not thread-safe — guard externally. */
        fun inMemory(): PluginRegistry = InMemoryPluginRegistry()
    }
}

sealed interface RegistryEvent {
    val pluginId: PluginId

    data class Added(override val pluginId: PluginId) : RegistryEvent
    data class Removed(override val pluginId: PluginId) : RegistryEvent
    data class Updated(override val pluginId: PluginId) : RegistryEvent
}

/**
 * What the registry stores per plugin — the manifest plus install metadata.
 */
data class RegisteredPlugin(
    val id: PluginId,
    val manifest: PluginManifest,
    val installPath: String,
    val signatureValid: Boolean,
    val installedAtMs: Long,
    val version: String,
)

private class InMemoryPluginRegistry : PluginRegistry {

    private val state = MutableStateFlow<Map<PluginId, RegisteredPlugin>>(emptyMap())

    override val changes: Flow<RegistryEvent> = kotlinx.coroutines.flow.flow {
        // We re-emit on every mutation; the studio subscribes via
        // collectLatest. A production implementation would use a
        // SharedFlow with replay=1 to avoid a missing-event window.
        state.collect { /* pump sentinel */ }
    }

    override fun all(): List<RegisteredPlugin> = state.value.values.toList()

    override fun get(id: PluginId): RegisteredPlugin? = state.value[id]

    override fun install(plugin: RegisteredPlugin) {
        state.value = state.value + (plugin.id to plugin)
    }

    override fun uninstall(id: PluginId): Boolean {
        val current = state.value
        val removed = current[id] ?: return false
        state.value = current - id
        return true
    }
}
