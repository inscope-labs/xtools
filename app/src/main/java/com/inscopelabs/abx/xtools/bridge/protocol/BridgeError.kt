package com.inscopelabs.abx.xtools.bridge.protocol

open class BridgeError(
    val code: Int,
    override val message: String,
    val contextData: Map<String, Any?>? = null
) : Exception(message)

data class BridgeStructuredError(
    val code: Int,
    val message: String,
    val contextData: Map<String, Any?>? = null
)

object BridgeErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
    const val PERMISSION_DENIED = -32001
    const val RATE_LIMIT_EXCEEDED = -32002
    const val CANCELLED = -32003
    const val TIMEOUT = -32004
}

