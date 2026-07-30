package com.inscopelabs.abx.xtools.bridge.manifest

import com.google.gson.annotations.SerializedName

data class PluginManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val author: String? = null,
    val permissions: List<String> = emptyList(),
    val services: List<ServiceDefinition> = emptyList(),
    @SerializedName("capabilities") val capabilities: List<String> = emptyList(),
    val signature: String? = null
)

data class ServiceDefinition(
    val type: String, // e.g., "mcp"
    val endpoint: String,
    val versionRange: String? = null
)
