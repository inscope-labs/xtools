package com.inscopelabs.abx.xtools.plugins.sdk.packaging

import java.io.File

/**
 * Manages a plugin's `build/` directory: cleans stale artifacts, exposes
 * the conventional slots the build pipeline writes to.
 */
class BuildDirectoryManager(
    val root: File,
) {
    val buildDir: File get() = File(root, "build")
    val artifactsDir: File get() = File(buildDir, "artifacts")
    val stagingDir: File get() = File(buildDir, "staging")
    val backupsDir: File get() = File(root, ".xtools/backups")

    fun ensure() {
        artifactsDir.mkdirs()
        stagingDir.mkdirs()
        backupsDir.mkdirs()
    }

    fun clean() {
        if (buildDir.exists()) buildDir.deleteRecursively()
        ensure()
    }

    fun outputPath(pluginId: String, version: String): File =
        File(artifactsDir, "$pluginId-$version.xtool")
}
