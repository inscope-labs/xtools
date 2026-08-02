package com.inscopelabs.abx.xtools.plugin.lifecycle

import com.inscopelabs.abx.xtools.bridge.manifest.ManifestParser
import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest
import com.inscopelabs.abx.xtools.kernel.registry.PluginTrustTier
import com.inscopelabs.abx.xtools.plugin.catalog.CatalogPlugin
import com.inscopelabs.abx.xtools.plugin.download.BundleExtractor
import com.inscopelabs.abx.xtools.plugin.download.DownloadManager
import com.inscopelabs.abx.xtools.plugin.download.DownloadResult
import com.inscopelabs.abx.xtools.plugin.download.Sha256Verifier
import com.inscopelabs.abx.xtools.plugin.download.SignatureVerifier
import com.inscopelabs.abx.xtools.plugin.storage.PluginDirectoryManager
import com.inscopelabs.abx.xtools.plugin.storage.PluginMetadataStore
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.zip.ZipFile

/**
 * Orchestrates the complete installation pipeline:
 * download → hash verification → signature verification → extraction → manifest validation → registration.
 *
 * @see §4.3 Step 3.3.1
 */
class InstallationPipeline(
    private val downloadManager: DownloadManager,
    private val metadataStore: PluginMetadataStore,
    private val directoryManager: PluginDirectoryManager,
    private val manifestParser: ManifestParser,
    private val dependencyResolver: DependencyResolver
) {
    private val _installProgress = MutableStateFlow<InstallProgress>(InstallProgress.Idle)
    val installProgress: StateFlow<InstallProgress> = _installProgress.asStateFlow()

    suspend fun install(catalogPlugin: CatalogPlugin): InstallResult {
        _installProgress.value = InstallProgress.Starting(catalogPlugin.id)

        return try {
            // Step 1: Create a staging directory.
            val stagingDir = directoryManager.createStagingDirectory(catalogPlugin.id)
            _installProgress.value = InstallProgress.Downloading(catalogPlugin.id)

            // Step 2: Download the plugin bundle.
            val downloadResult = downloadManager.download(catalogPlugin.downloadUrl, File(stagingDir, "bundle.xtp"))
            if (downloadResult is DownloadResult.Failure) {
                return InstallResult.Failure("Download failed: ${downloadResult.reason}")
            }
            val bundleFile = (downloadResult as DownloadResult.Success).file

            // Step 3: Verify SHA-256.
            _installProgress.value = InstallProgress.Verifying(catalogPlugin.id, "SHA-256")
            Sha256Verifier.verify(bundleFile, catalogPlugin.sha256Hash)

            // Step 4: Verify signature (mandatory gate before extraction).
            _installProgress.value = InstallProgress.Verifying(catalogPlugin.id, "Signature")
            val signature = catalogPlugin.signature
            val certificatePem = catalogPlugin.certificatePem
            if (signature.isNullOrBlank() || certificatePem.isNullOrBlank()) {
                directoryManager.cleanupStagingDirectory(catalogPlugin.id)
                return InstallResult.Failure("Plugin bundle is not signed. Installation rejected.")
            }

            val manifestBytes = try {
                ZipFile(bundleFile).use { zip ->
                    val entry = zip.getEntry("plugin-manifest.json") ?: zip.getEntry("plugin.json")
                    if (entry == null) {
                        directoryManager.cleanupStagingDirectory(catalogPlugin.id)
                        return InstallResult.Failure("Missing plugin-manifest.json in bundle")
                    }
                    zip.getInputStream(entry).use { it.readBytes() }
                }
            } catch (e: Exception) {
                directoryManager.cleanupStagingDirectory(catalogPlugin.id)
                return InstallResult.Failure("Failed to read manifest from bundle: ${e.message}")
            }

            val signatureBytes = try {
                Base64.decode(signature, Base64.DEFAULT)
            } catch (e: Exception) {
                directoryManager.cleanupStagingDirectory(catalogPlugin.id)
                return InstallResult.Failure("Invalid signature format: ${e.message}")
            }

            try {
                SignatureVerifier.verify(manifestBytes, signatureBytes, certificatePem)
            } catch (e: SecurityException) {
                directoryManager.cleanupStagingDirectory(catalogPlugin.id)
                return InstallResult.Failure("Signature verification failed: ${e.message}")
            }

            // Step 5: Extract the bundle.
            _installProgress.value = InstallProgress.Extracting(catalogPlugin.id)
            BundleExtractor.extract(bundleFile, stagingDir)

            // Step 6: Parse and validate manifest.
            val manifestFile = File(stagingDir, "plugin-manifest.json")
            if (!manifestFile.exists()) {
                return InstallResult.Failure("Missing plugin-manifest.json in bundle")
            }
            val manifestJson = manifestFile.readText()
            val manifest = manifestParser.parse(manifestJson)

            // Step 7: Validate plugin ID matches catalog.
            if (manifest.id != catalogPlugin.id) {
                return InstallResult.Failure("Plugin ID mismatch: catalog says ${catalogPlugin.id}, manifest says ${manifest.id}")
            }

            // Step 8: Resolve dependencies.
            val resolution = dependencyResolver.resolve(manifest)
            if (!resolution.success) {
                return InstallResult.Failure("Dependency resolution failed: ${resolution.missingDependencies}")
            }

            // Step 9: Move from staging to final directory.
            val finalDir = directoryManager.moveToFinalDirectory(stagingDir, manifest.id)

            // Step 10: Persist metadata.
            metadataStore.savePluginMetadata(manifest, finalDir.absolutePath, catalogPlugin.version)

            // Step 11: Register with PluginRegistry (implicitly via callback).
            _installProgress.value = InstallProgress.Complete(catalogPlugin.id)
            InstallResult.Success(manifest, trustTier = PluginTrustTier.VERIFIED)

        } catch (e: Exception) {
            // Rollback: clean up staging directory.
            directoryManager.cleanupStagingDirectory(catalogPlugin.id)
            _installProgress.value = InstallProgress.Failed(catalogPlugin.id, e.message ?: "Installation failed")
            InstallResult.Failure(e.message ?: "Installation failed")
        }
    }
}

sealed class InstallProgress {
    object Idle : InstallProgress()
    data class Starting(val pluginId: String) : InstallProgress()
    data class Downloading(val pluginId: String) : InstallProgress()
    data class Verifying(val pluginId: String, val step: String) : InstallProgress()
    data class Extracting(val pluginId: String) : InstallProgress()
    data class Complete(val pluginId: String) : InstallProgress()
    data class Failed(val pluginId: String, val reason: String) : InstallProgress()
}

sealed class InstallResult {
    data class Success(val manifest: PluginManifest, val trustTier: PluginTrustTier) : InstallResult()
    data class Failure(val reason: String) : InstallResult()
}
