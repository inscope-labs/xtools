package com.inscopelabs.abx.xtools.bridge.manifest

import com.google.gson.annotations.SerializedName

/**
 * Complete Plugin Manifest Schema for xtools plugins.
 * Supports reverse-domain id, semver version, permissions, service definitions (for MCP in Governed mode),
 * dependencies, resource quotas, UI configurations, and signature verification.
 */
data class PluginManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val author: String? = null,
    val entryPoint: String = "index.html",
    val permissions: List<String> = emptyList(),
    val services: List<ServiceDefinition> = emptyList(),
    @SerializedName("capabilities") val capabilities: List<String> = emptyList(),
    val dependencies: List<PluginDependency> = emptyList(),
    val resourceQuotas: ResourceQuotas? = null,
    val uiConfig: PluginUiConfig? = null,
    val signature: String? = null
)

data class ServiceDefinition(
    val type: String, // e.g., "mcp"
    val endpoint: String,
    val versionRange: String? = null
)

data class PluginDependency(
    val pluginId: String,
    val versionRange: String,
    val optional: Boolean = false
)

data class ResourceQuotas(
    val maxExecutionTimeMs: Long? = null,
    val maxMemoryMb: Int? = null,
    val maxStorageMb: Int? = null
)

data class PluginUiConfig(
    val settingsPageUrl: String? = null,
    val iconUrl: String? = null,
    val hasCustomSettings: Boolean = false
)

