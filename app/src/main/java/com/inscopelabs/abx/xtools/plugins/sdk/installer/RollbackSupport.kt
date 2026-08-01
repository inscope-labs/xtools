package com.inscopelabs.abx.xtools.plugins.sdk.installer

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import java.io.File

/**
 * Minimal "previous install" backup. Keeps the last N installs per
 * plugin-id under `<pluginsRoot>/.xtools-snapshots/<id>/<timestamp>/` so
 * [rollback] can restore them.
 *
 * The N is small (3) — this isn't a full VCS, just enough to recover
 * from a bad install. Real version history lives in the project tree.
 */
class RollbackSupport(
    private val pluginsRoot: File,
    private val keepCount: Int = 3,
) {

    private val snapshotsRoot: File = File(pluginsRoot, ".xtools-snapshots")

    fun snapshot(id: PluginId) {
        val current = File(pluginsRoot, id.asPath())
        if (!current.exists()) return
        val dir = File(snapshotsRoot, "${id.asPath()}/${System.currentTimeMillis()}")
        dir.parentFile?.mkdirs()
        current.copyRecursively(dir, overwrite = true)
        pruneOld(id)
    }

    fun rollback(id: PluginId): File? {
        val idDir = File(snapshotsRoot, id.asPath())
        if (!idDir.isDirectory) return null
        val newest = idDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.lastModified() }
            ?: return null
        val target = File(pluginsRoot, id.asPath())
        if (target.exists()) target.deleteRecursively()
        newest.copyRecursively(target, overwrite = true)
        return target
    }

    private fun pruneOld(id: PluginId) {
        val idDir = File(snapshotsRoot, id.asPath())
        if (!idDir.isDirectory) return
        val list = idDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        if (list.size <= keepCount) return
        list.drop(keepCount).forEach { it.deleteRecursively() }
    }
}
