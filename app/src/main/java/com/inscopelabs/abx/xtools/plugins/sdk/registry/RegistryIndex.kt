package com.inscopelabs.abx.xtools.plugins.sdk.registry

import com.inscopelabs.abx.xtools.plugins.sdk.api.McpConfig
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest

/**
 * Search index over the local registry. Not a database — just a
 * `Map`-backed structure that supports the queries the Studio's
 * ProjectExplorerFragment needs:
 *
 *  - by id (exact)
 *  - by tag (capability or declared feature)
 *  - by MCP role
 *  - by permission
 */
class RegistryIndex {

    private data class Entry(val manifest: PluginManifest)

    private val byId = HashMap<PluginId, Entry>()

    fun rebuild(manifests: Collection<PluginManifest>) {
        byId.clear()
        for (m in manifests) {
            if (PluginId.isValid(m.id)) {
                byId[PluginId.of(m.id)] = Entry(m)
            }
        }
    }

    fun get(id: PluginId): PluginManifest? = byId[id]?.manifest

    fun findByPermission(authority: String): List<PluginManifest> =
        byId.values.map { it.manifest }.filter { it.permissions.contains(authority) }

    fun findByCapability(tag: String): List<PluginManifest> =
        byId.values.map { it.manifest }.filter { it.capabilities.contains(tag) }

    fun findMcpServers(): List<PluginManifest> =
        byId.values.map { it.manifest }.filter { m ->
            m.mcp?.role == McpConfig.Role.SERVER
        }
}
