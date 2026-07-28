package com.inscopelabs.abx.xtools.bridge

import org.json.JSONObject

data class BridgeRequest(
    val id: String,
    val action: String,
    val payload: JSONObject
) {
    companion object {
        fun fromJson(jsonStr: String): BridgeRequest {
            val obj = JSONObject(jsonStr)
            return BridgeRequest(
                id = obj.optString("id", ""),
                action = obj.optString("action", ""),
                payload = obj.optJSONObject("payload") ?: JSONObject()
            )
        }
    }
}

data class BridgeResponse(
    val id: String,
    val result: Any? = null,
    val error: String? = null
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        if (error != null) {
            obj.put("error", error)
        } else {
            when (result) {
                is JSONObject -> obj.put("result", result)
                is Map<*, *> -> obj.put("result", JSONObject(result as Map<String, Any?>))
                else -> obj.put("result", result)
            }
        }
        return obj.toString()
    }
}

data class ConsoleLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val level: String, // "LOG", "WARN", "ERROR", "INFO"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pluginId: String = "system"
)
