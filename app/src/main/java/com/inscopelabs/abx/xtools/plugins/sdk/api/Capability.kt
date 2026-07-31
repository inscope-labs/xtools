package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.serialization.Serializable

/**
 * Catalog of capabilities — the *positive* declaration of what a plugin
 * provides to the rest of the system. Permissions are what a plugin
 * *needs*; capabilities are what it *exposes*.
 *
 * Capabilities are advisory in the current SDK and become load-bearing
 * when the registry lands in a later phase.
 */
@Serializable
enum class Capability(val tag: String) {

    /** Provides a top-level launcher entry. */
    LAUNCHER_ENTRY("launcher.entry"),

    /** Provides a sidebar panel. */
    SIDEBAR_PANEL("sidebar.panel"),

    /** Exposes one or more [McpConfig.McpTool]s. */
    MCP_TOOLS("mcp.tools"),

    /** Hosts an MCP server. */
    MCP_SERVER_HOST("mcp.server.host"),

    /** Consumes MCP servers (local or remote). */
    MCP_CLIENT_CONSUMER("mcp.client.consumer"),

    /** Provides a settings panel under the main settings screen. */
    SETTINGS_PANEL("settings.panel"),

    /** Provides a terminal-style widget. */
    TERMINAL_WIDGET("terminal.widget"),

    /** Provides an AI tool surface. */
    AI_TOOL("ai.tool");

    companion object {
        private val BY_TAG: Map<String, Capability> =
            entries.associateBy { it.tag }

        fun fromTag(tag: String): Capability? = BY_TAG[tag]

        val ALL_TAGS: List<String> = entries.map { it.tag }
    }
}
