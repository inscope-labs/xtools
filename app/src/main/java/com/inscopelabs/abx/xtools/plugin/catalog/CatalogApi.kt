package com.inscopelabs.abx.xtools.plugin.catalog

/**
 * Data models for the plugin catalog API.
 * Mirrors the remote catalog schema defined in §4.1.1.
 */
data class CatalogPlugin(
    val id: String,
    val name: String,
    val description: String? = null,
    val author: String? = null,
    val version: String,
    val minHostVersion: String? = null,
    val permissions: List<String> = emptyList(),
    val downloadUrl: String,
    val sha256Hash: String,
    val signature: String? = null,
    val certificatePem: String? = null,
    val fileSizeBytes: Long,
    val releaseDate: String? = null,
    val screenshots: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val changelog: String? = null,
    val isPremium: Boolean = false
)

data class CatalogSearchResult(
    val plugins: List<CatalogPlugin>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * Defines the catalog API operations.
 * Implemented by RemoteCatalogService.
 *
 * @see §4.1.1
 */
interface CatalogApi {
    suspend fun search(query: String?, category: String?, page: Int, pageSize: Int): CatalogSearchResult
    suspend fun getPluginDetails(pluginId: String): CatalogPlugin?
    suspend fun getFeaturedPlugins(): List<CatalogPlugin>
    suspend fun getCategories(): List<String>
    suspend fun checkForUpdates(pluginId: String, currentVersion: String): CatalogPlugin? // returns newer version if available
}
