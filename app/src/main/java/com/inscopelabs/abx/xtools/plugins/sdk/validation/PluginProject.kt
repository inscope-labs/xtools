package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import java.io.File

/**
 * The blob of stuff a validator typically needs. The Studio's build
 * pipeline produces one of these per project and passes it down the
 * validator chain.
 */
data class PluginProject(
    val root: File,
    val manifest: PluginManifest,
    val manifestFile: File,
    val entryFile: File,
    val assets: List<File>,
    val totalSizeBytes: Long,
    val declaredDependencies: List<String>,
) {
    fun hasAsset(relativePath: String): Boolean =
        assets.any { it.relativeTo(root).invariantSeparatorsPath == relativePath }

    fun read(path: String): String? {
        val f = File(root, path)
        if (!f.isFile) return null
        return f.readText(Charsets.UTF_8)
    }
}
