package com.inscopelabs.abx.xtools.plugin.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.inscopelabs.abx.xtools.plugin.PluginManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * PluginLoader handles loading plugin manifests and validating plugin structure.
 */
class PluginLoader(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val MANIFEST_FILENAME = "plugin-manifest.json"

        // Required manifest fields
        private val REQUIRED_FIELDS = listOf("id", "name", "version")
    }

    /**
     * Load manifest from assets.
     */
    suspend fun loadManifestFromAssets(assetPath: String): PluginManifest? = withContext(Dispatchers.IO) {
        try {
            val manifestPath = if (assetPath.endsWith("/")) {
                "$assetPath$MANIFEST_FILENAME"
            } else {
                "$assetPath/$MANIFEST_FILENAME"
            }

            val json = context.assets.open(manifestPath).use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }

            parseManifest(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load manifest from JSON string.
     */
    fun parseManifest(json: String): PluginManifest? {
        return try {
            val manifest = gson.fromJson(json, PluginManifest::class.java)
            if (validateManifest(manifest)) {
                manifest
            } else {
                null
            }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Validate manifest structure.
     */
    private fun validateManifest(manifest: PluginManifest): Boolean {
        // Check required fields
        for (field in REQUIRED_FIELDS) {
            when (field) {
                "id" -> if (manifest.id.isBlank()) return false
                "name" -> if (manifest.name.isBlank()) return false
                "version" -> if (manifest.version.isBlank()) return false
            }
        }

        // Validate version format (simple check)
        if (!manifest.version.matches(Regex("^\\d+\\.\\d+\\.\\d+.*$"))) {
            return false
        }

        // Validate entry point
        if (manifest.entryPoint.isBlank() || !manifest.entryPoint.endsWith(".html")) {
            return false
        }

        return true
    }

    /**
     * Check if plugin has required files.
     */
    suspend fun validatePluginStructure(assetPath: String): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val manifest = loadManifestFromAssets(assetPath)
            if (manifest == null) {
                return@withContext ValidationResult.Invalid("Missing or invalid manifest")
            }

            // Check if entry point exists
            val entryPointPath = if (assetPath.endsWith("/")) {
                "$assetPath${manifest.entryPoint}"
            } else {
                "$assetPath/${manifest.entryPoint}"
            }

            try {
                context.assets.open(entryPointPath).close()
            } catch (e: Exception) {
                return@withContext ValidationResult.Invalid("Entry point not found: ${manifest.entryPoint}")
            }

            ValidationResult.Valid(manifest)
        } catch (e: Exception) {
            ValidationResult.Invalid("Validation error: ${e.message}")
        }
    }

    /**
     * Get available plugins from assets.
     */
    suspend fun getAvailablePluginsFromAssets(): List<PluginManifest> = withContext(Dispatchers.IO) {
        val plugins = mutableListOf<PluginManifest>()

        try {
            val directories = context.assets.list("plugins") ?: return@withContext plugins

            for (dir in directories) {
                val manifest = loadManifestFromAssets("plugins/$dir")
                if (manifest != null && manifest.id == dir) {
                    plugins.add(manifest)
                }
            }
        } catch (e: Exception) {
            // No plugins directory
        }

        plugins
    }

    /**
     * Validation result.
     */
    sealed class ValidationResult {
        data class Valid(val manifest: PluginManifest) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
