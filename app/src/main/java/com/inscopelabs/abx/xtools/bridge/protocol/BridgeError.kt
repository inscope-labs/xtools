package com.inscopelabs.abx.xtools.bridge.protocol

class BridgeError(val code: Int, override val message: String) : Exception(message)

object BridgeErrorCodes {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}
