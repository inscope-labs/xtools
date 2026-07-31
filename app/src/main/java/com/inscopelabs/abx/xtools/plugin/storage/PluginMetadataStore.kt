package com.inscopelabs.abx.xtools.plugin.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest

/**
 * Stores plugin metadata (manifests, permission grants, user preferences)
 * using Android EncryptedSharedPreferences.
 *
 * @see §4.4 Step 4.4.1
 */
class PluginMetadataStore(private val context: Context) {
    private val gson = Gson()

    private val encryptedPrefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "plugin_metadata",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePluginMetadata(manifest: PluginManifest, installPath: String, installedVersion: String) {
        val json = gson.toJson(manifest)
        encryptedPrefs.edit()
            .putString("${manifest.id}_manifest", json)
            .putString("${manifest.id}_path", installPath)
            .putString("${manifest.id}_version", installedVersion)
            .putLong("${manifest.id}_installed_at", System.currentTimeMillis())
            .apply()
    }

    fun getPluginMetadata(pluginId: String): PluginMetadata? {
        val manifestJson = encryptedPrefs.getString("${pluginId}_manifest", null) ?: return null
        val manifest = gson.fromJson(manifestJson, PluginManifest::class.java)
        val installPath = encryptedPrefs.getString("${pluginId}_path", null) ?: return null
        val version = encryptedPrefs.getString("${pluginId}_version", null) ?: return null
        return PluginMetadata(manifest, installPath, version)
    }

    fun removePluginMetadata(pluginId: String) {
        encryptedPrefs.edit()
            .remove("${pluginId}_manifest")
            .remove("${pluginId}_path")
            .remove("${pluginId}_version")
            .remove("${pluginId}_installed_at")
            .apply()
    }
}

data class PluginMetadata(
    val manifest: PluginManifest,
    val installPath: String,
    val version: String
)
