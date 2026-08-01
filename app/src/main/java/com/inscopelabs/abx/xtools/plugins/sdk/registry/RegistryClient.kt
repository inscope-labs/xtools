package com.inscopelabs.abx.xtools.plugins.sdk.registry

/**
 * Future remote-registry client. The shape mirrors what a JSON-over-HTTP
 * API would look like, but no endpoints are wired up yet — this is
 * deliberately a compile-clean placeholder so the Studio can show a
 * "Browse Registry" button without crashing.
 */
interface RegistryClient {

    suspend fun search(query: String): List<RegistryEntry>

    suspend fun fetch(id: String): RegistryEntry?

    suspend fun publish(entry: RegistryEntry): PublishResult

    data class RegistryEntry(
        val id: String,
        val name: String,
        val summary: String,
        val version: String,
        val downloadUrl: String,
        val sha256: String,
    )

    sealed interface PublishResult {
        data class Accepted(val registryId: String) : PublishResult
        data class Rejected(val reason: String) : PublishResult
    }
}

/** Default no-op client — every call returns an empty result. */
object NoopRegistryClient : RegistryClient {
    override suspend fun search(query: String): List<RegistryClient.RegistryEntry> = emptyList()
    override suspend fun fetch(id: String): RegistryClient.RegistryEntry? = null
    override suspend fun publish(entry: RegistryClient.RegistryEntry): RegistryClient.PublishResult =
        RegistryClient.PublishResult.Rejected("remote registry not configured")
}
