package com.inscopelabs.abx.xtools.kernel.registry

import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest
import com.inscopelabs.abx.xtools.diagnostics.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canonical list of installed plugins with their manifests and current state.
 * Queryable by ID, capability, and category. Mode-agnostic.
 *
 * @see §2.1 Step 1.1.2
 */
class PluginRegistry {
    private val _plugins = MutableStateFlow<Map<String, PluginEntry>>(emptyMap())
    val plugins: StateFlow<Map<String, PluginEntry>> = _plugins.asStateFlow()

    fun register(
        manifest: PluginManifest,
        installationPath: String,
        category: String = "general",
        trustTier: PluginTrustTier
    ) {
        Logger.i("PluginRegistry", "register: pluginId='${manifest.id}', v${manifest.version}, category=$category, trustTier=$trustTier")
        val entry = PluginEntry(
            id = manifest.id,
            manifest = manifest,
            installationPath = installationPath,
            version = manifest.version,
            permissions = manifest.permissions,
            category = category,
            trustTier = trustTier,
            state = PluginState.INSTALLED
        )
        _plugins.value = _plugins.value + (manifest.id to entry)
    }

    fun updateState(pluginId: String, newState: PluginState) {
        val current = _plugins.value[pluginId] ?: return
        Logger.i("PluginRegistry", "updateState: pluginId='$pluginId', ${current.state} -> $newState")
        _plugins.value = _plugins.value + (pluginId to current.copy(state = newState))
    }

    fun unregister(pluginId: String) {
        Logger.i("PluginRegistry", "unregister: pluginId='$pluginId'")
        _plugins.value = _plugins.value - pluginId
    }

    fun getById(pluginId: String): PluginEntry? = _plugins.value[pluginId]

    fun getByCapability(capability: String): List<PluginEntry> =
        _plugins.value.values.filter { it.permissions.contains(capability) || it.manifest.capabilities.contains(capability) }

    fun getByCategory(category: String): List<PluginEntry> =
        _plugins.value.values.filter { it.category.equals(category, ignoreCase = true) }

    fun getAllPlugins(): List<PluginEntry> = _plugins.value.values.toList()
}

data class PluginEntry(
    val id: String,
    val manifest: PluginManifest,
    val installationPath: String,
    val version: String,
    val permissions: List<String>,
    val category: String = "general",
    val trustTier: PluginTrustTier,
    val state: PluginState = PluginState.INSTALLED
)

enum class PluginTrustTier {
    VERIFIED,
    PIPELINE_SIGNED
}

enum class PluginState {
    INSTALLED, ACTIVE, INACTIVE, ERROR
}
