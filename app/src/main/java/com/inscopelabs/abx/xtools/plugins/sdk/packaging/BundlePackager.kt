package com.inscopelabs.abx.xtools.plugins.sdk.packaging

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
import com.inscopelabs.abx.xtools.plugins.sdk.validation.PluginProject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds a registry-compatible ZIP from a [PluginProject]. The output
 * layout is intentionally identical to what the future plugin registry
 * will accept — Stage 7.16 says no conversion should ever be required.
 *
 * Entry order is deterministic so byte-identical rebuilds produce the
 * same hash (useful for caching and for the signature in [signing]).
 */
class BundlePackager {

    fun packageProject(project: PluginProject, out: File): File {
        out.parentFile?.mkdirs()
        if (out.exists()) out.delete()
        val sortedAssets = project.assets.sortedBy { it.relativeTo(project.root).invariantSeparatorsPath }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zip ->
            // Manifest first.
            zip.putNextEntry(ZipEntry("plugin-manifest.json"))
            zip.write(ManifestCodec.encode(project.manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            // Then everything else, sorted.
            for (asset in sortedAssets) {
                val rel = asset.relativeTo(project.root).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry(rel))
                if (asset.isFile) {
                    asset.inputStream().use { it.copyTo(zip) }
                }
                zip.closeEntry()
            }
        }
        return out
    }
}
