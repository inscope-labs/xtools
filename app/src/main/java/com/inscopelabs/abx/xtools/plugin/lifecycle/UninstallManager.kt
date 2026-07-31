package com.inscopelabs.abx.xtools.plugin.lifecycle

import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.kernel.permission.PermissionManager
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
    private val directoryManager: PluginDirectoryManager,
    private val permissionManager: PermissionManager
) {

    suspend fun uninstall(pluginId: String, keepData: Boolean = false): Boolean {
        Logger.i("UninstallManager", "uninstall requested for plugin '$pluginId' (keepData=$keepData)")
        val entry = pluginRegistry.getById(pluginId)
        if (entry == null) {
            Logger.w("UninstallManager", "Cannot uninstall: plugin '$pluginId' not found in registry")
            return false
        }
        permissionManager.clearPermissions(pluginId)
        if (!keepData) {
            directoryManager.getPluginDirectory(pluginId).deleteRecursively()
        }
        metadataStore.removePluginMetadata(pluginId)
        pluginRegistry.unregister(pluginId)
        Logger.i("UninstallManager", "Plugin '$pluginId' uninstalled successfully")
        return true
    }
}
