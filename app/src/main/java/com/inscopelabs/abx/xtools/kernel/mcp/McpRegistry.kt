package com.inscopelabs.abx.xtools.kernel.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MCP Registry – ONLY active in Governed Mode.
 * Provides abx‑server with a catalog of plugin‑exposed services.
 * In Standalone Mode, this registry is inert and empty.
 *
 * @see §2.1 Step 1.1.3
 */
class McpRegistry {
    private val _services = MutableStateFlow<Map<String, McpService>>(emptyMap())
    val services: StateFlow<Map<String, McpService>> = _services.asStateFlow()

    fun register(service: McpService) {
        _services.value = _services.value + (service.id to service)
    }

    fun unregister(serviceId: String) {
        _services.value = _services.value - serviceId
    }

    fun findService(serviceType: String): List<McpService> =
        _services.value.values.filter { it.type == serviceType }
}

/**
 * A service that a plugin wishes to expose to a governing abx‑server session.
 */
interface McpService {
    val id: String
    val type: String
    val version: String

    suspend fun invoke(request: McpRequest): McpResponse
}

data class McpRequest(
    val action: String,
    val parameters: Map<String, Any?>
)

data class McpResponse(
    val success: Boolean,
    val payload: Any? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)
