package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.serialization.Serializable

/**
 * Catalog of permissions a plugin can declare. Each value is a stable
 * string — once a permission is shipped, it cannot be renamed without a
 * SDK major version bump.
 *
 * Permission strings are intentionally lower-snake-case so they read well
 * in the manifest JSON.
 */
@Serializable
enum class Permission(val authority: String) {

    /** Read files from the plugin's own storage scope. */
    STORAGE_READ("storage.read"),

    /** Write files within the plugin's own storage scope. */
    STORAGE_WRITE("storage.write"),

    /** Read user-selected files via the system file picker. */
    FILESYSTEM_PICK("filesystem.pick"),

    /** Access device network state (connectivity, type). */
    NETWORK_STATUS("network.status"),

    /**
     * Make outbound HTTP requests. Denied by default — see
     * Stage 7.17: "External network access is denied unless explicitly
     * authorized."
     */
    NETWORK_HTTP("network.http"),

    /** Use the OS clipboard. */
    CLIPBOARD("clipboard"),

    /** Show native toasts and notifications. */
    NOTIFICATIONS("notifications"),

    /** Capture screenshots / screen recording. */
    SCREEN_CAPTURE("screen.capture"),

    /** Read device location. */
    LOCATION("location"),

    /** Read device camera. */
    CAMERA("camera"),

    /** Read device microphone. */
    MICROPHONE("microphone"),

    /** Register an MCP server endpoint (Stage 7.15). */
    MCP_SERVER("mcp.server"),

    /** Consume MCP endpoints from other plugins. */
    MCP_CLIENT("mcp.client");

    companion object {
        private val BY_AUTHORITY: Map<String, Permission> =
            entries.associateBy { it.authority }

        /** Resolve a permission by its manifest string, or `null` if unknown. */
        fun fromAuthority(authority: String): Permission? =
            BY_AUTHORITY[authority]

        /** All permission strings, useful for the visual manifest editor. */
        val ALL_AUTHORITIES: List<String> = entries.map { it.authority }
    }
}
