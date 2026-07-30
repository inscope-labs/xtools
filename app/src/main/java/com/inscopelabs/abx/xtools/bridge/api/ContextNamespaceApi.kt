package com.inscopelabs.abx.xtools.bridge.api

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import org.json.JSONObject
import java.util.UUID

interface ContextNamespaceApi {
    suspend fun addEntry(pluginId: String, key: String, value: String): BridgeResponse
    suspend fun getEntries(pluginId: String, query: String? = null): BridgeResponse
    suspend fun exportContext(pluginId: String): BridgeResponse
    suspend fun clearContext(pluginId: String): BridgeResponse
    suspend fun estimateSize(pluginId: String): BridgeResponse
}

class DefaultContextNamespaceApi(
    private val dispatcher: BridgeDispatcher
) : ContextNamespaceApi {

    override suspend fun addEntry(pluginId: String, key: String, value: String): BridgeResponse {
        val payload = JSONObject().put("key", key).put("value", value)
        val request = BridgeRequest(UUID.randomUUID().toString(), "context.addEntry", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun getEntries(pluginId: String, query: String?): BridgeResponse {
        val payload = JSONObject().apply {
            if (query != null) put("query", query)
        }
        val request = BridgeRequest(UUID.randomUUID().toString(), "context.getEntries", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun exportContext(pluginId: String): BridgeResponse {
        val request = BridgeRequest(UUID.randomUUID().toString(), "context.exportContext", JSONObject(), pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun clearContext(pluginId: String): BridgeResponse {
        val request = BridgeRequest(UUID.randomUUID().toString(), "context.clearContext", JSONObject(), pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun estimateSize(pluginId: String): BridgeResponse {
        val request = BridgeRequest(UUID.randomUUID().toString(), "context.estimateSize", JSONObject(), pluginId)
        return dispatcher.dispatch(pluginId, request)
    }
}
