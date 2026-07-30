package com.inscopelabs.abx.xtools.bridge.api

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import org.json.JSONObject
import java.util.UUID

interface SystemNamespaceApi {
    suspend fun getDeviceInfo(pluginId: String): BridgeResponse
    suspend fun showNotification(pluginId: String, title: String, message: String): BridgeResponse
    suspend fun requestPermission(pluginId: String, capability: String): BridgeResponse
    suspend fun getPreference(pluginId: String, key: String): BridgeResponse
}

class DefaultSystemNamespaceApi(
    private val dispatcher: BridgeDispatcher
) : SystemNamespaceApi {

    override suspend fun getDeviceInfo(pluginId: String): BridgeResponse {
        val request = BridgeRequest(UUID.randomUUID().toString(), "system.getDeviceInfo", JSONObject(), pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun showNotification(pluginId: String, title: String, message: String): BridgeResponse {
        val payload = JSONObject().put("title", title).put("message", message)
        val request = BridgeRequest(UUID.randomUUID().toString(), "system.showNotification", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun requestPermission(pluginId: String, capability: String): BridgeResponse {
        val payload = JSONObject().put("capability", capability)
        val request = BridgeRequest(UUID.randomUUID().toString(), "system.requestPermission", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }

    override suspend fun getPreference(pluginId: String, key: String): BridgeResponse {
        val payload = JSONObject().put("key", key)
        val request = BridgeRequest(UUID.randomUUID().toString(), "system.getPreference", payload, pluginId)
        return dispatcher.dispatch(pluginId, request)
    }
}
