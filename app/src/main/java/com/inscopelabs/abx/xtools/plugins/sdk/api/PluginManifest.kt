package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The on-disk representation of `plugin-manifest.json`. Schema-versioned
 * via [schema]; the validator refuses any manifest whose schema is higher
 * than what the running SDK understands.
 *
 * Defaults are kept conservative so a minimal manifest — just an `id` and
 * `version` — is still valid.
 */
@Serializable
data class PluginManifest(

    /** Manifest schema version. Must equal or precede [PluginSdk.CURRENT_MANIFEST_SCHEMA]. */
    val schema: Int = 1,

    /** Stable, reverse-DNS plugin id. */
    val id: String,

    /** Semantic version, e.g. `1.0.0`. Validated by the build step. */
    val version: String = "0.1.0",

    /** Human-readable display name. */
    val name: String = id,

    /** Short one-line description. */
    val description: String = "",

    /** Author / publisher name. */
    val author: String = "",

    /** Minimum XTools SDK version this plugin requires (semver). */
    @SerialName("minSdk") val minSdk: String = "0.1.0",

    /** Optional entry-point HTML; defaults to `index.html`. */
    @SerialName("entry") val entry: String = "index.html",

    /** Toolbar icon path relative to the bundle root. */
    @SerialName("icon") val icon: String = "assets/icon.png",

    /** Visual theme. */
    val theme: Theme = Theme.SYSTEM,

    /** Toolbar menu entries. */
    val menu: List<MenuEntry> = emptyList(),

    /** Declared permissions; bridge calls outside this set are denied. */
    val permissions: List<String> = emptyList(),

    /** Declared capabilities (used for the visual manifest editor). */
    val capabilities: List<String> = emptyList(),

    /** Hard dependencies on other plugins, by id. */
    val dependencies: List<String> = emptyList(),

    /** Optional MCP endpoint configuration (Stage 7.15). */
    val mcp: McpConfig? = null,

    /** Content Security Policy. Defaults to a tight inline-none policy. */
    val csp: String = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'",
)

@Serializable
enum class Theme {
    @SerialName("system") SYSTEM,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

@Serializable
data class MenuEntry(
    val id: String,
    val label: String,
    val icon: String? = null,
    val action: String? = null,
)

/**
 * MCP-specific block. When present the plugin can be exposed as an MCP
 * endpoint. The Studio's MCP Project Wizard writes this block in for
 * MCP Client / Server / Hybrid templates.
 */
@Serializable
data class McpConfig(
    val role: Role = Role.CLIENT,
    val endpoint: String? = null,
    val tools: List<McpTool> = emptyList(),
) {
    @Serializable
    enum class Role {
        @SerialName("client") CLIENT,
        @SerialName("server") SERVER,
        @SerialName("hybrid") HYBRID,
    }

    @Serializable
    data class McpTool(
        val name: String,
        val description: String,
        val inputSchema: String? = null,
    )
}
