package com.inscopelabs.abx.xtools.kernel.dispatcher

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeError
import com.inscopelabs.abx.xtools.kernel.mode.NotYetWired
import com.inscopelabs.abx.xtools.kernel.permission.PermissionManager
import java.util.concurrent.ConcurrentHashMap

interface BridgeActionHandler {
    val actionName: String
    val requiredCapability: String?
    suspend fun handle(pluginId: String, request: BridgeRequest): BridgeResponse
}

interface RateLimiter {
    fun isAllowed(pluginId: String, action: String): Boolean
}

class SimpleRateLimiter(
    private val maxRequestsPerSecond: Int = 50
) : RateLimiter {
    private val requestCounts = ConcurrentHashMap<String, Pair<Long, Int>>()

    override fun isAllowed(pluginId: String, action: String): Boolean {
        val now = System.currentTimeMillis() / 1000
        val key = "$pluginId:$action"
        val current = requestCounts[key]
        if (current == null || current.first != now) {
            requestCounts[key] = Pair(now, 1)
            return true
        }
        if (current.second >= maxRequestsPerSecond) {
            return false
        }
        requestCounts[key] = Pair(now, current.second + 1)
        return true
    }
}

interface JsonSchemaValidator {
    fun validatePayload(action: String, payloadJsonStr: String): SchemaValidationResult
}

data class SchemaValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

class DefaultSchemaValidator : JsonSchemaValidator {
    @NotYetWired("JSON schema validation will be fully wired in Phase 1 Stage 1.3")
    override fun validatePayload(action: String, payloadJsonStr: String): SchemaValidationResult {
        return SchemaValidationResult(isValid = true)
    }
}

/**
 * BridgeDispatcher receives all cross-boundary (Kotlin <-> JS) calls.
 * - Routes calls by action type/target.
 * - Validates payloads against JSON schemas.
 * - Enforces per-plugin rate limits.
 * - Authorizes against PermissionManager (mode-agnostic).
 * - Formats consistent error responses.
 */
class BridgeDispatcher(
    private val permissionManager: PermissionManager,
    private val rateLimiter: RateLimiter = SimpleRateLimiter(),
    private val schemaValidator: JsonSchemaValidator = DefaultSchemaValidator()
) {
    private val handlers = ConcurrentHashMap<String, BridgeActionHandler>()

    fun registerHandler(handler: BridgeActionHandler) {
        handlers[handler.actionName] = handler
    }

    fun unregisterHandler(actionName: String) {
        handlers.remove(actionName)
    }

    suspend fun dispatch(pluginId: String, request: BridgeRequest): BridgeResponse {
        // 1. Rate limiting check
        if (!rateLimiter.isAllowed(pluginId, request.action)) {
            return BridgeResponse.error(
                id = request.id,
                error = "Rate limit exceeded for action: ${request.action}"
            )
        }

        // 2. Handler lookup
        val handler = handlers[request.action]
            ?: return BridgeResponse.error(
                id = request.id,
                error = "Unsupported bridge action: ${request.action}"
            )

        // 3. Permission authorization check (mode-agnostic delegation to PermissionManager)
        val requiredCap = handler.requiredCapability ?: getRequiredCapabilityForAction(request.action)
        if (requiredCap != null) {
            val isAuthorized = permissionManager.isAuthorized(pluginId, requiredCap)
            if (!isAuthorized) {
                return BridgeResponse.error(
                    id = request.id,
                    error = "Permission denied for capability '$requiredCap' on plugin '$pluginId'"
                )
            }
        }

        // 4. Schema validation check
        val schemaResult = schemaValidator.validatePayload(request.action, request.payload.toString())
        if (!schemaResult.isValid) {
            return BridgeResponse.error(
                id = request.id,
                error = "Invalid payload schema: ${schemaResult.errorMessage}"
            )
        }

        // 5. Execution
        return try {
            handler.handle(pluginId, request)
        } catch (e: BridgeError) {
            BridgeResponse.error(request.id, "Error ${e.code}: ${e.message}")
        } catch (e: Exception) {
            BridgeResponse.error(request.id, "Internal error: ${e.message ?: "Unknown error"}")
        }
    }

    private fun getRequiredCapabilityForAction(action: String): String? {
        return when {
            action.startsWith("storage.") -> "storage"
            action.startsWith("db.") -> "storage"
            action.startsWith("ui.") -> "ui"
            action.startsWith("system.") -> "system"
            action.startsWith("http.") -> "http"
            else -> null
        }
    }
}
