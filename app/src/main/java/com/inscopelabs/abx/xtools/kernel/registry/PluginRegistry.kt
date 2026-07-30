package com.inscopelabs.abx.xtools.kernel.registry

import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canonical list of installed plugins with their manifests and current state.
 * Queryable by ID, capability, and category.
 *
 * @see §2.1 Step 1.1.2
 */
class PluginRegistry {
    private val _plugins = MutableStateFlow<Map<String, PluginEntry>>(emptyMap())
    val plugins: StateFlow<Map<String, PluginEntry>> = _plugins.asStateFlow()

    fun register(manifest: PluginManifest, installationPath: String) {
        val entry = PluginEntry(manifest, installationPath, PluginState.INSTALLED)
        _plugins.value = _plugins.value + (manifest.id to entry)
    }

    fun unregister(pluginId: String) {
        _plugins.value = _plugins.value - pluginId
    }

    fun getById(pluginId: String): PluginEntry? = _plugins.value[pluginId]

    fun getByCapability(capability: String): List<PluginEntry> =
        _plugins.value.values.filter { it.manifest.permissions.contains(capability) }
}

data class PluginEntry(
    val manifest: PluginManifest,
    val installationPath: String,
    val state: PluginState
)

enum class PluginState {
    INSTALLED, ACTIVE, INACTIVE, ERROR
}
