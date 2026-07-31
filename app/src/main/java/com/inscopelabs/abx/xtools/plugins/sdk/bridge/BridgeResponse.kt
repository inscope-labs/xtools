package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import org.json.JSONArray
import org.json.JSONObject

/**
 * Typed response shape returned to JS. The JS side decodes via:
 *
 *     if (response.ok) use response.data else handle(response.code, response.message)
 *
 * Use [toJsLiteral] to embed the response in an `evaluateJavascript` call.
 */
data class BridgeResponse(
    val ok: Boolean,
    val data: Any?,
    val code: String? = null,
    val message: String? = null,
) {
    fun preview(): String = when {
        !ok -> "denied: ${code ?: "ERROR"}"
        data == null -> "null"
        data is String && data.length > 80 -> "<${data.length} chars>"
        else -> data.toString().take(80)
    }

    fun toJsLiteral(): String = JSONObject(toJsonMap()).toString()

    private fun toJsonMap(): Map<String, Any?> = buildMap {
        put("ok", ok)
        if (ok) put("data", data ?: JSONObject.NULL)
        else {
            put("code", code ?: "ERROR")
            put("message", message ?: "")
        }
    }

    companion object {
        fun success(data: Any?): BridgeResponse = BridgeResponse(ok = true, data = data)

        fun failure(code: String, message: String): BridgeResponse =
            BridgeResponse(ok = false, data = null, code = code, message = message)
    }
}

/** Tiny helpers for converting common shapes without pulling in Moshi/Gson. */
internal object Json {
    fun toJson(value: Any?): String = when (value) {
        null -> "null"
        is String -> JSONObject.quote(value)
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> JSONObject(value.mapKeys { it.key.toString() }).toString()
        is Iterable<*> -> JSONArray(value.toList()).toString()
        else -> JSONObject.quote(value.toString())
    }
}
