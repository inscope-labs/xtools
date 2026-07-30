package com.inscopelabs.abx.xtools.bridge

import com.inscopelabs.abx.xtools.bridge.protocol.StreamMarker
import org.json.JSONObject

data class BridgeRequest(
    val id: String,
    val action: String,
    val payload: JSONObject = JSONObject(),
    val pluginId: String = "",
    val requestType: RequestType = RequestType.EXECUTE,
    val streamMarker: StreamMarker = StreamMarker.NONE
) {
    enum class RequestType {
        EXECUTE, CANCEL, STREAM_ACK
    }

    companion object {
        fun fromJson(jsonStr: String, fallbackPluginId: String = ""): BridgeRequest {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", System.currentTimeMillis().toString())
            val action = json.optString("action", "")
            val payload = json.optJSONObject("payload")
                ?: json.optJSONObject("args")
                ?: JSONObject()
            val pluginId = json.optString("pluginId", fallbackPluginId)
            val typeStr = json.optString("type", "EXECUTE")
            val requestType = try {
                RequestType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                RequestType.EXECUTE
            }
            return BridgeRequest(id, action, payload, pluginId, requestType)
        }
    }
}

