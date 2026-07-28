package com.inscopelabs.abx.xtools.plugin

import com.google.gson.annotations.SerializedName

/**
 * PluginManifest defines the structure of a plugin's manifest file.
 */
data class PluginManifest(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("author")
    val author: String = "Unknown",

    @SerializedName("entryPoint")
    val entryPoint: String = "index.html",

    @SerializedName("icon")
    val icon: String? = null,

    @SerializedName("permissions")
    val permissions: List<String> = emptyList(),

    @SerializedName("minHostVersion")
    val minHostVersion: String = "1.0.0",

    @SerializedName("sandbox")
    val sandbox: SandboxConfig = SandboxConfig(),

    @SerializedName("capabilities")
    val capabilities: List<String> = emptyList(),

    @SerializedName("metadata")
    val metadata: Map<String, String>? = null
) {
    val entry: String get() = entryPoint
}

/**
 * Sandbox configuration for plugin execution.
 */
data class SandboxConfig(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("networkAccess")
    val networkAccess: Boolean = true,

    @SerializedName("fileAccess")
    val fileAccess: Boolean = false,

    @SerializedName("maxExecutionTime")
    val maxExecutionTime: Long = 30000,

    @SerializedName("maxMemoryMB")
    val maxMemoryMB: Int = 128,

    @SerializedName("allowedOrigins")
    val allowedOrigins: List<String> = emptyList()
)
