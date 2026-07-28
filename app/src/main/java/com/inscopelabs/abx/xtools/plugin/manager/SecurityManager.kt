package com.inscopelabs.abx.xtools.plugin.manager

import android.content.Context
import android.content.SharedPreferences
import com.inscopelabs.abx.xtools.plugin.Plugin
import java.security.MessageDigest

class SecurityManager(private val context: Context) {

    private val pluginPermissionsMap = mutableMapOf<String, Set<String>>()

    fun registerPluginPermissions(pluginId: String, permissions: List<String>) {
        pluginPermissionsMap[pluginId] = permissions.toSet()
    }

    fun hasPermission(pluginId: String, permission: String): Boolean {
        if (pluginId == "system") return true
        val granted = pluginPermissionsMap[pluginId]
        return granted?.contains(permission) ?: false
    }

    private fun getPluginPrefs(pluginId: String): SharedPreferences {
        val safeName = "xtools_storage_" + pluginId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        return context.getSharedPreferences(safeName, Context.MODE_PRIVATE)
    }

    fun getEncryptedStorage(pluginId: String, key: String): String? {
        val prefs = getPluginPrefs(pluginId)
        return prefs.getString(key, null)
    }

    fun setEncryptedStorage(pluginId: String, key: String, value: String) {
        val prefs = getPluginPrefs(pluginId)
        prefs.edit().putString(key, value).apply()
    }

    fun removeEncryptedStorage(pluginId: String, key: String) {
        val prefs = getPluginPrefs(pluginId)
        prefs.edit().remove(key).apply()
    }

    fun clearEncryptedStorage(pluginId: String) {
        val prefs = getPluginPrefs(pluginId)
        prefs.edit().clear().apply()
    }

    fun verifyChecksum(content: String, expectedChecksum: String): Boolean {
        if (expectedChecksum.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
        return hash.equals(expectedChecksum, ignoreCase = true)
    }
}
