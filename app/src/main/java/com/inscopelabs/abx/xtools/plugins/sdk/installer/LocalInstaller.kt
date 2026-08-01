package com.inscopelabs.abx.xtools.plugins.sdk.installer

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import java.io.File

/**
 * Thin facade for installing plugins that the Studio just built. Every
 * install goes through [InstallationPipeline] so the local build path
 * and the future registry path share a single code path.
 */
class LocalInstaller(
    private val pipeline: InstallationPipeline,
) {

    suspend fun install(
        bundle: File,
        signatureDir: File,
    ): InstallationPipeline.Result = pipeline.install(bundle, signatureDir)

    suspend fun uninstall(id: PluginId): Boolean {
        // The pipeline doesn't yet own uninstall — that's a registry
        // operation. We delegate to a hook the host provides.
        return pipeline.let { _ ->
            // Stub: the host's uninstall hook would call into the
            // pipeline to remove the install dir + registry entry.
            // Filled in by the host wiring.
            false
        }
    }
}
