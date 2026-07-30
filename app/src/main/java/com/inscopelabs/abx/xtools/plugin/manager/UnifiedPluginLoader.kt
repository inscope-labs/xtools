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

    open class Error(open val reason: String) : PluginLoadResult() {
        override fun equals(other: Any?): Boolean = other is Error && reason == other.reason
        override fun hashCode(): Int = reason.hashCode()
        override fun toString(): String = "Error(reason='$reason')"
    }

    data class Unsigned(
        val manifest: PluginManifest,
        val contentHtml: String,
        val baseUrl: String,
        override val reason: String = "Plugin is unsigned and requires explicit user authorization"
    ) : Error(reason)
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

            val entryFileName = if (manifest.entryPoint.isNotBlank()) manifest.entryPoint else "index.html"
            val entryFile = File(pluginDir, entryFileName)
            if (!entryFile.exists()) {
                return@withContext PluginLoadResult.Error("Entry file missing: ${entryFile.absolutePath}")
            }

            val htmlContent = entryFile.readText()
            val baseUrl = "file://${pluginDir.absolutePath}/"

            if (manifest.signature.isNullOrBlank()) {
                // Surface distinct Unsigned result instead of silently treating unsigned as verified Success
                return@withContext PluginLoadResult.Unsigned(manifest, htmlContent, baseUrl)
            }

            // Source real certificate from plugin package alongside manifest
            val certFile = listOf("plugin.crt", "certificate.pem", "cert.pem", "cert.crt")
                .map { File(pluginDir, it) }
                .firstOrNull { it.exists() }
                ?: return@withContext PluginLoadResult.Error(
                    "Signing certificate missing for signed plugin '$pluginId'. Expected cert file (e.g. plugin.crt or certificate.pem) in plugin directory."
                )

            val certificate = try {
                certFile.inputStream().use { stream ->
                    java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(stream)
                }
            } catch (e: Exception) {
                return@withContext PluginLoadResult.Error("Failed to parse signing certificate for plugin '$pluginId': ${e.message}")
            }

            val signatureValid = pluginIdentity.verifySignature(
                manifestJson.toByteArray(),
                manifest.signature.toByteArray(),
                certificate
            )
            if (!signatureValid) {
                return@withContext PluginLoadResult.Error("Signature verification failed for plugin: $pluginId")
            }

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
            if (devResult is PluginLoadResult.Success || devResult is PluginLoadResult.Unsigned) {
                devResult
            } else {
                prodLoader.loadPlugin(pluginId)
            }
        } else {
            val prodResult = prodLoader.loadPlugin(pluginId)
            if (prodResult is PluginLoadResult.Success || prodResult is PluginLoadResult.Unsigned) {
                prodResult
            } else {
                devLoader.loadPlugin(pluginId)
            }
        }
    }
}
