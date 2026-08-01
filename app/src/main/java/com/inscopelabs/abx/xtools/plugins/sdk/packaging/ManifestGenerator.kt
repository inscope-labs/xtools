package com.inscopelabs.abx.xtools.plugins.sdk.packaging

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
import java.io.File

/**
 * Writes the manifest into a project tree. Used by the Studio's
 * ManifestEditorFragment whenever a field changes.
 */
class ManifestGenerator {

    fun write(manifest: PluginManifest, into: File): File {
        val target = File(into, "plugin-manifest.json")
        target.parentFile?.mkdirs()
        target.writeText(ManifestCodec.encode(manifest), Charsets.UTF_8)
        return target
    }
}
