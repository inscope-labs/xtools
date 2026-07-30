package com.inscopelabs.abx.xtools.bridge.protocol

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import org.json.JSONArray

/**
 * Batch request container for executing multiple independent bridge operations together.
 */
data class BridgeBatchRequest(
    val requests: List<BridgeRequest>
) {
    companion object {
        fun fromJsonArray(jsonArray: JSONArray, defaultPluginId: String = ""): BridgeBatchRequest {
            val list = mutableListOf<BridgeRequest>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    list.add(BridgeRequest.fromJson(obj.toString(), defaultPluginId))
                }
            }
            return BridgeBatchRequest(list)
        }
    }
}
