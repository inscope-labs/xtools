package com.inscopelabs.abx.xtools.bridge.api

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import org.json.JSONObject
import java.util.UUID

interface StorageNamespaceApi {
    suspend fun read(pluginId: String, path: String): BridgeResponse
    suspend fun write(pluginId: String, path: String, content: String): BridgeResponse
    suspend fun list(pluginId: String, path: String): BridgeResponse
    suspend fun createDirectory(pluginId: String, path: String): BridgeResponse
    suspend fun deleteFile(pluginId: String, path: String): BridgeResponse
    suspend fun deleteDirectory(pluginId: String, path: String): BridgeResponse
}

class DefaultStorageNamespaceApi(
    private val dispatcher: BridgeDispatcher
) : StorageNamespaceApi {

    override suspend fun read(pluginId: String, path: String): BridgeResponse {
        val payload = JSONObject().put("path", path)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.read", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun write(pluginId: String, path: String, content: String): BridgeResponse {
        val payload = JSONObject().put("path", path).put("content", content)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.write", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun list(pluginId: String, path: String): BridgeResponse {
        val payload = JSONObject().put("path", path)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.list", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun createDirectory(pluginId: String, path: String): BridgeResponse {
        val payload = JSONObject().put("path", path)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.createDirectory", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun deleteFile(pluginId: String, path: String): BridgeResponse {
        val payload = JSONObject().put("path", path)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.deleteFile", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun deleteDirectory(pluginId: String, path: String): BridgeResponse {
        val payload = JSONObject().put("path", path)
        val request = BridgeRequest(UUID.randomUUID().toString(), "storage.deleteDirectory", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }
}
