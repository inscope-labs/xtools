package com.inscopelabs.abx.xtools.plugin.lifecycle

import com.inscopelabs.abx.xtools.kernel.registry.PluginRegistry
import com.inscopelabs.abx.xtools.plugin.storage.PluginDirectoryManager
import com.inscopelabs.abx.xtools.plugin.storage.PluginMetadataStore

/**
 * Uninstalls a plugin: revokes permissions, removes files, deregisters from registry.
 * Supports data retention option.
 *
 * @see §4.3 Step 3.3.4
 */
class UninstallManager(
    private val pluginRegistry: PluginRegistry,
    private val metadataStore: PluginMetadataStore,
    private val directoryManager: PluginDirectoryManager
) {

    suspend fun uninstall(pluginId: String, keepData: Boolean = false): Boolean {
        val entry = pluginRegistry.getById(pluginId) ?: return false

        // Stub: deactivate if active.
        // 1. Revoke all permissions.
        // 2. Remove from registry.
        // 3. Delete plugin code directory (or keep data if keepData=true).
        // 4. Remove metadata from store.

        return true
    }
}
