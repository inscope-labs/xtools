package com.inscopelabs.abx.xtools.plugin.manager

import android.content.Context
import com.inscopelabs.abx.xtools.bridge.manifest.ManifestParser
import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest
import com.inscopelabs.abx.xtools.security.PluginIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class PluginLoadResult {
    data class Success(
        val manifest: PluginManifest,
        val contentHtml: String,
        val baseUrl: String
    ) : PluginLoadResult()

    data class Error(val reason: String) : PluginLoadResult()
}

interface PluginLoaderStrategy {
    suspend fun loadPlugin(pluginId: String): PluginLoadResult
}

class DevelopmentPluginLoader(private val context: Context) : PluginLoaderStrategy {
    private val manifestParser = ManifestParser()

    override suspend fun loadPlugin(pluginId: String): PluginLoadResult = withContext(Dispatchers.IO) {
        try {
            val assetDir = "plugins/$pluginId"
            val manifestName = listOf("plugin-manifest.json", "plugin.json").firstOrNull { filename ->
                try {
                    context.assets.open("$assetDir/$filename").close()
                    true
                } catch (_: Exception) {
                    false
                }
            } ?: return@withContext PluginLoadResult.Error("Manifest not found in assets for plugin: $pluginId")

            val manifestJson = context.assets.open("$assetDir/$manifestName").bufferedReader().use { it.readText() }
            val manifest = manifestParser.parse(manifestJson)

            val entryFile = if (manifest.entryPoint.isNotBlank()) manifest.entryPoint else "index.html"
            val htmlContent = context.assets.open("$assetDir/$entryFile").bufferedReader().use { it.readText() }
            val baseUrl = "file:///android_asset/$assetDir/"

            PluginLoadResult.Success(manifest, htmlContent, baseUrl)
        } catch (e: Exception) {
            PluginLoadResult.Error("Failed to load development plugin '$pluginId': ${e.message}")
        }
    }
}

class ProductionPluginLoader(private val context: Context) : PluginLoaderStrategy {
    private val manifestParser = ManifestParser()
    private val pluginIdentity = PluginIdentity()

    override suspend fun loadPlugin(pluginId: String): PluginLoadResult = withContext(Dispatchers.IO) {
        try {
            val pluginDir = File(context.filesDir, "plugins/$pluginId")
            if (!pluginDir.exists() || !pluginDir.isDirectory) {
                return@withContext PluginLoadResult.Error("Production plugin directory does not exist: $pluginId")
            }

            val manifestFile = File(pluginDir, "plugin-manifest.json").takeIf { it.exists() }
                ?: File(pluginDir, "plugin.json").takeIf { it.exists() }
                ?: return@withContext PluginLoadResult.Error("Manifest file missing in $pluginDir")

            val manifestJson = manifestFile.readText()
            val manifest = manifestParser.parse(manifestJson)

            // Validate signature if present
            if (!manifest.signature.isNullOrBlank()) {
                val mockCert = java.security.cert.CertificateFactory.getInstance("X.509")
                    .generateCertificate("-----BEGIN CERTIFICATE-----\nMIIC+DCCA...\n-----END CERTIFICATE-----".byteInputStream())
                val signatureValid = pluginIdentity.verifySignature(
                    manifestJson.toByteArray(),
                    manifest.signature.toByteArray(),
                    mockCert
                )
                if (!signatureValid) {
                    return@withContext PluginLoadResult.Error("Signature verification failed for plugin: $pluginId")
                }
            }

            val entryFileName = if (manifest.entryPoint.isNotBlank()) manifest.entryPoint else "index.html"
            val entryFile = File(pluginDir, entryFileName)
            if (!entryFile.exists()) {
                return@withContext PluginLoadResult.Error("Entry file missing: ${entryFile.absolutePath}")
            }

            val htmlContent = entryFile.readText()
            val baseUrl = "file://${pluginDir.absolutePath}/"

            PluginLoadResult.Success(manifest, htmlContent, baseUrl)
        } catch (e: Exception) {
            PluginLoadResult.Error("Failed to load production plugin '$pluginId': ${e.message}")
        }
    }
}

class UnifiedPluginLoader(
    private val context: Context,
    private val isDevelopmentMode: Boolean = true
) : PluginLoaderStrategy {

    private val devLoader = DevelopmentPluginLoader(context)
    private val prodLoader = ProductionPluginLoader(context)

    override suspend fun loadPlugin(pluginId: String): PluginLoadResult {
        return if (isDevelopmentMode) {
            val devResult = devLoader.loadPlugin(pluginId)
            if (devResult is PluginLoadResult.Success) {
                devResult
            } else {
                prodLoader.loadPlugin(pluginId)
            }
        } else {
            val prodResult = prodLoader.loadPlugin(pluginId)
            if (prodResult is PluginLoadResult.Success) {
                prodResult
            } else {
                devLoader.loadPlugin(pluginId)
            }
        }
    }
}
