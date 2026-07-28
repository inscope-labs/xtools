package com.inscopelabs.abx.xtools.plugin

import com.google.gson.annotations.SerializedName

/**
 * Plugin represents an installed plugin with its state and metadata.
 */
data class Plugin(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("author")
    val author: String? = null,

    @SerializedName("entryPoint")
    val entryPoint: String = "index.html",

    @SerializedName("permissions")
    val permissions: List<String> = emptyList(),

    @SerializedName("manifest")
    val manifest: PluginManifest = PluginManifest(
        id = id,
        name = name,
        version = version,
        description = description ?: "",
        author = author ?: "System",
        entryPoint = entryPoint,
        permissions = permissions
    ),

    @SerializedName("state")
    var state: PluginState = PluginState.INSTALLED,

    @SerializedName("installedAt")
    val installedAt: Long = System.currentTimeMillis(),

    @SerializedName("lastUsed")
    var lastUsed: Long? = null,

    @SerializedName("checksum")
    val checksum: String? = null,

    @SerializedName("iconUrl")
    val iconUrl: String? = null,

    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null
) {
    val isEnabled: Boolean get() = state == PluginState.ACTIVE || state == PluginState.INSTALLED
    val isBuiltIn: Boolean get() = id.startsWith("sample") || id == "hello" || id == "system"
    val localPath: String get() = "plugins/$id"

    /**
     * Update last used timestamp.
     */
    fun markUsed() {
        lastUsed = System.currentTimeMillis()
    }

    /**
     * Check if plugin is active.
     */
    fun isActive(): Boolean = state == PluginState.ACTIVE

    /**
     * Get display name.
     */
    fun getDisplayName(): String = name.ifEmpty { id }
}

/**
 * Plugin installation state.
 */
enum class PluginState {
    INSTALLED,
    ACTIVE,
    DISABLED,
    UPDATING,
    ERROR
}
