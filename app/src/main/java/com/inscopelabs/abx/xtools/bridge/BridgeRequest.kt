package com.inscopelabs.abx.xtools.bridge

import org.json.JSONObject

data class BridgeRequest(
    val id: String,
    val action: String,
    val payload: JSONObject = JSONObject()
) {
    companion object {
        fun fromJson(jsonStr: String): BridgeRequest {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", System.currentTimeMillis().toString())
            val action = json.optString("action", "")
            val payload = json.optJSONObject("payload") ?: JSONObject()
            return BridgeRequest(id, action, payload)
        }
    }
}
