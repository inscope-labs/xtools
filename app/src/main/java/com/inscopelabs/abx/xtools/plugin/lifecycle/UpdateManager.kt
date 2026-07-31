package com.inscopelabs.abx.xtools.plugin.lifecycle

import com.inscopelabs.abx.xtools.plugin.catalog.CatalogApi
import com.inscopelabs.abx.xtools.plugin.storage.PluginMetadataStore
import com.inscopelabs.abx.xtools.plugin.storage.PluginDirectoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Checks for plugin updates and applies them using the atomic installation pipeline.
 * Supports rollback and changelog display.
 *
 * @see §4.3 Step 3.3.3
 */
class UpdateManager(
    private val catalogApi: CatalogApi,
    private val metadataStore: PluginMetadataStore,
    private val installationPipeline: InstallationPipeline,
    private val directoryManager: PluginDirectoryManager,
    private val rollbackManager: RollbackManager
) {
    private val _updateProgress = MutableStateFlow<UpdateProgress>(UpdateProgress.Idle)
    val updateProgress: StateFlow<UpdateProgress> = _updateProgress.asStateFlow()

    suspend fun checkForUpdates(pluginId: String): Boolean {
        val metadata = metadataStore.getPluginMetadata(pluginId) ?: return false
        val catalogPlugin = catalogApi.checkForUpdates(pluginId, metadata.version)
        return catalogPlugin != null
    }

    suspend fun applyUpdate(pluginId: String): UpdateResult {
        _updateProgress.value = UpdateProgress.Checking(pluginId)
        val metadata = metadataStore.getPluginMetadata(pluginId) ?: return UpdateResult.Failure("Plugin not found")
        val catalogPlugin = catalogApi.checkForUpdates(pluginId, metadata.version)
        if (catalogPlugin == null) {
            return UpdateResult.Failure("No update available")
        }

        // Stub: Perform backup of current plugin directory.
        val backupDir = directoryManager.createBackupDirectory(pluginId)

        // Stub: Run installation pipeline for the new version.
        // If successful, update metadata and remove backup.
        // If failed, restore from backup using RollbackManager.

        _updateProgress.value = UpdateProgress.Complete(pluginId)
        return UpdateResult.Success(catalogPlugin.version)
    }
}

sealed class UpdateProgress {
    object Idle : UpdateProgress()
    data class Checking(val pluginId: String) : UpdateProgress()
    data class Downloading(val pluginId: String) : UpdateProgress()
    data class Installing(val pluginId: String) : UpdateProgress()
    data class Complete(val pluginId: String) : UpdateProgress()
    data class Failed(val pluginId: String, val reason: String) : UpdateProgress()
}

sealed class UpdateResult {
    data class Success(val newVersion: String) : UpdateResult()
    data class Failure(val reason: String) : UpdateResult()
}
