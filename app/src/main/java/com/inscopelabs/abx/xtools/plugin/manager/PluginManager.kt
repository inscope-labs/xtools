package com.inscopelabs.abx.xtools.plugin.manager

import android.content.Context
import com.inscopelabs.abx.xtools.plugin.Plugin
import com.inscopelabs.abx.xtools.plugin.PluginManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PluginManager(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    private val _plugins = MutableStateFlow<List<Plugin>>(emptyList())
    val plugins = _plugins.asStateFlow()

    private val _activePlugin = MutableStateFlow<Plugin?>(null)
    val activePlugin = _activePlugin.asStateFlow()

    init {
        loadBuiltInPlugins()
    }

    fun loadBuiltInPlugins() {
        val loadedList = mutableListOf<Plugin>()
        try {
            val assetManager = context.assets
            val pluginDirs = assetManager.list("plugins") ?: emptyArray()

            for (dir in pluginDirs) {
                if (dir.endsWith(".js")) continue // skip xtools-bridge.js
                val manifestPath = "plugins/$dir/plugin.json"
                try {
                    val jsonStr = assetManager.open(manifestPath).bufferedReader().use { it.readText() }
                    val manifest = PluginManifest.fromJson(jsonStr)

                    securityManager.registerPluginPermissions(manifest.id, manifest.permissions)

                    val plugin = Plugin(
                        manifest = manifest,
                        localPath = "plugins/$dir",
                        isInstalled = true,
                        isEnabled = true,
                        isBuiltIn = true
                    )
                    loadedList.add(plugin)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _plugins.value = loadedList
        if (loadedList.isNotEmpty() && _activePlugin.value == null) {
            _activePlugin.value = loadedList.first()
        }
    }

    fun selectPlugin(pluginId: String) {
        val found = _plugins.value.find { it.manifest.id == pluginId }
        if (found != null && found.isEnabled) {
            _activePlugin.value = found
        }
    }

    fun togglePluginEnabled(pluginId: String) {
        _plugins.value = _plugins.value.map { plugin ->
            if (plugin.manifest.id == pluginId) {
                plugin.copy(isEnabled = !plugin.isEnabled)
            } else plugin
        }
        if (_activePlugin.value?.manifest?.id == pluginId) {
            val active = _plugins.value.find { it.manifest.id == pluginId }
            if (active?.isEnabled == false) {
                _activePlugin.value = _plugins.value.firstOrNull { it.isEnabled }
            }
        }
    }

    fun installSampleCustomPlugin(name: String, description: String, permissions: List<String>) {
        val newId = "com.inscopelabs.xtools.plugin.custom_" + System.currentTimeMillis()
        val manifest = PluginManifest(
            id = newId,
            name = name,
            version = "1.0.0",
            author = "User Created",
            description = description,
            entry = "index.html",
            permissions = permissions
        )
        securityManager.registerPluginPermissions(newId, permissions)

        val newPlugin = Plugin(
            manifest = manifest,
            localPath = "plugins/sample", // reuses sample web UI harness with new identity & permissions
            isInstalled = true,
            isEnabled = true,
            isBuiltIn = false
        )
        _plugins.value = _plugins.value + newPlugin
        selectPlugin(newId)
    }

    fun uninstallPlugin(pluginId: String) {
        _plugins.value = _plugins.value.filterNot { it.manifest.id == pluginId }
        if (_activePlugin.value?.manifest?.id == pluginId) {
            _activePlugin.value = _plugins.value.firstOrNull { it.isEnabled }
        }
    }
}
