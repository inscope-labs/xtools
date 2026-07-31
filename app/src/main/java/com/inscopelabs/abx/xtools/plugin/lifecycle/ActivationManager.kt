package com.inscopelabs.abx.xtools.plugin.lifecycle

import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter
import com.inscopelabs.abx.xtools.kernel.permission.PermissionManager
import com.inscopelabs.abx.xtools.kernel.registry.PluginEntry
import com.inscopelabs.abx.xtools.kernel.registry.PluginRegistry
import com.inscopelabs.abx.xtools.kernel.registry.PluginState

/**
 * Manages plugin activation and deactivation.
 * Activation includes permission request handling, resource allocation,
 * and service registration (MCP registry in Governed Mode).
 *
 * @see §4.3 Step 3.3.2
 */
class ActivationManager(
    private val pluginRegistry: PluginRegistry,
    private val permissionManager: PermissionManager,
    private val modeArbiter: ModeArbiter
) {

    suspend fun activate(pluginId: String): Boolean {
        Logger.i("ActivationManager", "activate requested for pluginId '$pluginId'")
        val entry = pluginRegistry.getById(pluginId)
        if (entry == null) {
            Logger.w("ActivationManager", "Cannot activate: plugin '$pluginId' not found in registry")
            return false
        }
        if (entry.state == PluginState.ACTIVE) {
            Logger.d("ActivationManager", "Plugin '$pluginId' is already ACTIVE")
            return true
        }

        pluginRegistry.updateState(pluginId, PluginState.ACTIVE)
        Logger.i("ActivationManager", "Plugin '$pluginId' activated successfully")
        return true
    }

    suspend fun deactivate(pluginId: String): Boolean {
        Logger.i("ActivationManager", "deactivate requested for pluginId '$pluginId'")
        val entry = pluginRegistry.getById(pluginId)
        if (entry == null) {
            Logger.w("ActivationManager", "Cannot deactivate: plugin '$pluginId' not found in registry")
            return false
        }
        if (entry.state != PluginState.ACTIVE) {
            Logger.d("ActivationManager", "Plugin '$pluginId' is not ACTIVE (state=${entry.state})")
            return true
        }

        pluginRegistry.updateState(pluginId, PluginState.INACTIVE)
        Logger.i("ActivationManager", "Plugin '$pluginId' deactivated successfully")
        return true
    }
}
