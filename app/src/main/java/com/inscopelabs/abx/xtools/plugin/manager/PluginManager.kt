package com.inscopelabs.abx.xtools.plugin.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inscopelabs.abx.xtools.plugin.Plugin
import com.inscopelabs.abx.xtools.plugin.PluginManifest
import com.inscopelabs.abx.xtools.plugin.PluginState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PluginManager handles plugin lifecycle, storage, and preferences.
 */
class PluginManager private constructor(private val context: Context) {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val prefs: SharedPreferences by lazy {
        getEncryptedPrefs()
    }

    private val pluginsDir: File by lazy {
        File(context.filesDir, "plugins").apply { mkdirs() }
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, "plugin_cache").apply { mkdirs() }
    }

    private val _plugins = MutableStateFlow<List<Plugin>>(emptyList())
    val plugins: StateFlow<List<Plugin>> = _plugins.asStateFlow()

    private val _activePlugin = MutableStateFlow<Plugin?>(null)
    val activePlugin: StateFlow<Plugin?> = _activePlugin.asStateFlow()

    init {
        scope.launch {
            loadInitialPlugins()
        }
    }

    companion object {
        private const val PREFS_NAME = "xtools_plugin_prefs"
        private const val KEY_PLUGINS = "installed_plugins"
        private const val KEY_ACTIVE_PLUGIN = "active_plugin"
        private const val KEY_PREFERENCES = "plugin_preferences"

        @Volatile
        private var instance: PluginManager? = null

        fun getInstance(context: Context): PluginManager {
            return instance ?: synchronized(this) {
                instance ?: PluginManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun getEncryptedPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private suspend fun loadInitialPlugins() = withContext(Dispatchers.IO) {
        val saved = getAllPlugins().toMutableList()

        val defaultPlugins = listOf(
            PluginManifest(
                id = "sample",
                name = "Sample Plugin",
                version = "1.0.0",
                description = "Demonstration harness for xtools JavaScript Bridge capabilities.",
                author = "XTools Team",
                entryPoint = "index.html",
                permissions = listOf("storage", "ui", "system", "http")
            ),
            PluginManifest(
                id = "database",
                name = "Database Explorer",
                version = "1.0.0",
                description = "Interactive SQLite database plugin for managing local app data.",
                author = "XTools Team",
                entryPoint = "index.html",
                permissions = listOf("storage", "ui", "system")
            )
        )

        defaultPlugins.forEach { manifest ->
            if (saved.none { it.id == manifest.id }) {
                val p = Plugin(
                    id = manifest.id,
                    name = manifest.name,
                    version = manifest.version,
                    description = manifest.description,
                    author = manifest.author,
                    entryPoint = manifest.entryPoint,
                    manifest = manifest,
                    state = PluginState.INSTALLED,
                    permissions = manifest.permissions
                )
                saved.add(p)
            }
        }

        _plugins.value = saved
        savePluginsToPrefs(saved)

        val activeId = getActivePluginId() ?: "sample"
        val active = saved.find { it.id == activeId } ?: saved.firstOrNull()
        if (active != null) {
            selectPlugin(active.id)
        }
    }

    suspend fun getAllPlugins(): List<Plugin> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_PLUGINS, null) ?: return@withContext emptyList()
        try {
            val type = object : TypeToken<List<Plugin>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlugin(pluginId: String): Plugin? = withContext(Dispatchers.IO) {
        _plugins.value.find { it.id == pluginId } ?: getAllPlugins().find { it.id == pluginId }
    }

    fun getActivePluginId(): String? {
        return prefs.getString(KEY_ACTIVE_PLUGIN, null)
    }

    fun selectPlugin(pluginId: String) {
        scope.launch {
            prefs.edit().putString(KEY_ACTIVE_PLUGIN, pluginId).apply()
            val list = _plugins.value.map { p ->
                if (p.id == pluginId) {
                    p.copy(state = PluginState.ACTIVE).also { it.markUsed() }
                } else if (p.state == PluginState.ACTIVE) {
                    p.copy(state = PluginState.INSTALLED)
                } else {
                    p
                }
            }
            _plugins.value = list
            _activePlugin.value = list.find { it.id == pluginId }
            savePluginsToPrefs(list)
        }
    }

    fun togglePluginEnabled(pluginId: String) {
        scope.launch {
            val list = _plugins.value.map { p ->
                if (p.id == pluginId) {
                    val newState = if (p.isEnabled) PluginState.DISABLED else PluginState.INSTALLED
                    p.copy(state = newState)
                } else p
            }
            _plugins.value = list
            if (_activePlugin.value?.id == pluginId && list.find { it.id == pluginId }?.isEnabled == false) {
                _activePlugin.value = null
            }
            savePluginsToPrefs(list)
        }
    }

    fun uninstallPlugin(pluginId: String) {
        scope.launch {
            uninstall(pluginId)
        }
    }

    fun installSampleCustomPlugin(name: String, description: String, permissions: List<String>) {
        scope.launch {
            val id = "custom_" + System.currentTimeMillis() % 10000
            val manifest = PluginManifest(
                id = id,
                name = name,
                version = "1.0.0",
                description = description,
                author = "User",
                entryPoint = "index.html",
                permissions = permissions
            )
            val plugin = Plugin(
                id = id,
                name = name,
                version = "1.0.0",
                description = description,
                author = "User",
                entryPoint = "index.html",
                manifest = manifest,
                state = PluginState.INSTALLED,
                permissions = permissions
            )
            val updated = _plugins.value + plugin
            _plugins.value = updated
            savePluginsToPrefs(updated)
            selectPlugin(id)
        }
    }

    private fun savePluginsToPrefs(list: List<Plugin>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_PLUGINS, json).apply()
    }

    suspend fun uninstall(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        val pluginDir = File(pluginsDir, pluginId)
        val deleted = pluginDir.deleteRecursively()

        val updated = _plugins.value.filterNot { it.id == pluginId }
        _plugins.value = updated
        savePluginsToPrefs(updated)

        if (_activePlugin.value?.id == pluginId) {
            _activePlugin.value = updated.firstOrNull()
        }

        deleted
    }

    fun getPluginPath(pluginId: String): String {
        return File(pluginsDir, pluginId).absolutePath
    }

    suspend fun isInstalled(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        _plugins.value.any { it.id == pluginId } || File(pluginsDir, pluginId).exists()
    }

    fun getPreference(key: String): Any? {
        val prefsJson = prefs.getString(KEY_PREFERENCES, null) ?: return null
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val prefsMap: Map<String, Any> = gson.fromJson(prefsJson, type)
            prefsMap[key]
        } catch (e: Exception) {
            null
        }
    }

    fun setPreference(key: String, value: Any) {
        val prefsJson = prefs.getString(KEY_PREFERENCES, null) ?: "{}"
        val type = object : TypeToken<MutableMap<String, Any>>() {}.type
        val prefsMap: MutableMap<String, Any> = try {
            gson.fromJson(prefsJson, type)
        } catch (e: Exception) {
            mutableMapOf()
        }
        prefsMap[key] = value
        prefs.edit().putString(KEY_PREFERENCES, gson.toJson(prefsMap)).apply()
    }

    fun clearPreferences() {
        prefs.edit().remove(KEY_PREFERENCES).apply()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    fun getPluginCacheDir(pluginId: String): File {
        return File(cacheDir, pluginId).apply { mkdirs() }
    }
}
