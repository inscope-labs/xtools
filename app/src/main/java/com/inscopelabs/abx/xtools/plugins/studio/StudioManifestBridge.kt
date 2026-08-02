package com.inscopelabs.abx.xtools.plugins.studio

import com.inscopelabs.abx.xtools.bridge.manifest.PluginDependency
import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest as CanonicalManifest
import com.inscopelabs.abx.xtools.bridge.manifest.PluginUiConfig
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest as StudioManifest

/**
 * Bridges Plugin Studio's authoring manifest schema ([StudioManifest]) to the canonical runtime
 * manifest schema ([CanonicalManifest]) used by the app kernel.
 *
 * Plugin Studio (plugins/studio, plugins/sdk) maintains its own manifest schema for authoring,
 * template generation, and local validation. The runtime app kernel (kernel.registry.PluginRegistry)
 * uses [CanonicalManifest]. This object converts a Studio-authored manifest into the canonical shape
 * so that plugins built via Studio can be registered into the real app runtime upon build completion.
 */
object StudioManifestBridge {

    fun toCanonical(studioManifest: StudioManifest): CanonicalManifest {
        return CanonicalManifest(
            id = studioManifest.id,
            version = studioManifest.version,
            name = studioManifest.name,
            description = studioManifest.description.ifBlank { null },
            author = studioManifest.author.ifBlank { null },
            entryPoint = studioManifest.entry,
            permissions = studioManifest.permissions,
            capabilities = studioManifest.capabilities,
            dependencies = studioManifest.dependencies.map { depId ->
                PluginDependency(pluginId = depId, versionRange = "*")
            },
            uiConfig = if (studioManifest.icon.isNotBlank()) {
                PluginUiConfig(iconUrl = studioManifest.icon)
            } else null
        )
    }
}
