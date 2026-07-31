package com.inscopelabs.abx.xtools.plugin.storage

import android.content.Context
import java.io.File

/**
 * Manages the plugin directory structure:
 * - Staging directories for installations/updates.
 * - Final plugin directories (code + assets).
 * - Backup directories for rollback.
 * - User data directories (kept during updates).
 *
 * @see §4.4 Step 4.4.2
 */
class PluginDirectoryManager(private val context: Context) {

    private val pluginsRoot: File by lazy { File(context.filesDir, "plugins") }
    private val stagingRoot: File by lazy { File(context.filesDir, "staging") }
    private val backupsRoot: File by lazy { File(context.filesDir, "backups") }

    init {
        listOf(pluginsRoot, stagingRoot, backupsRoot).forEach { it.mkdirs() }
    }

    fun getPluginDirectory(pluginId: String): File {
        return File(pluginsRoot, pluginId).also { it.mkdirs() }
    }

    fun createStagingDirectory(pluginId: String): File {
        val stagingDir = File(stagingRoot, pluginId)
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()
        return stagingDir
    }

    fun moveToFinalDirectory(stagingDir: File, pluginId: String): File {
        val finalDir = getPluginDirectory(pluginId)
        finalDir.deleteRecursively() // Remove old version.
        stagingDir.renameTo(finalDir)
        return finalDir
    }

    fun createBackupDirectory(pluginId: String): File {
        val backupDir = File(backupsRoot, pluginId)
        backupDir.deleteRecursively()
        backupDir.mkdirs()
        return backupDir
    }

    fun getBackupDirectory(pluginId: String): File {
        return File(backupsRoot, pluginId)
    }

    fun cleanupStagingDirectory(pluginId: String) {
        File(stagingRoot, pluginId).deleteRecursively()
    }
}
