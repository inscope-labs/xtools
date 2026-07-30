package com.inscopelabs.abx.xtools.bridge

import com.google.gson.annotations.SerializedName
import com.inscopelabs.abx.xtools.bridge.protocol.BridgeStructuredError
import com.inscopelabs.abx.xtools.bridge.protocol.StreamMarker
import com.inscopelabs.abx.xtools.bridge.protocol.StreamProgressInfo

/**
 * BridgeResponse represents the canonical response structure for bridge communication.
 * Used to send responses back from Kotlin to JavaScript.
 */
data class BridgeResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("data")
    val data: Any? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("structuredError")
    val structuredError: BridgeStructuredError? = null,

    @SerializedName("streamMarker")
    val streamMarker: StreamMarker = StreamMarker.NONE,

    @SerializedName("progress")
    val progress: StreamProgressInfo? = null,

    @SerializedName("batchResponses")
    val batchResponses: List<BridgeResponse>? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    val result: Any? get() = data

    constructor(id: String, result: Any?, error: String? = null) : this(
        id = id,
        success = (error == null),
        data = result,
        error = error
    )

    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }

    companion object {
        fun success(id: String, data: Any? = null): BridgeResponse {
            return BridgeResponse(
                id = id,
                success = true,
                data = data
            )
        }

        fun error(
            id: String,
            error: String,
            code: Int = -32603,
            contextData: Map<String, Any?>? = null
        ): BridgeResponse {
            return BridgeResponse(
                id = id,
                success = false,
                error = error,
                structuredError = BridgeStructuredError(code, error, contextData)
            )
        }

        fun streamChunk(
            id: String,
            chunkData: Any?,
            marker: StreamMarker,
            progressInfo: StreamProgressInfo? = null
        ): BridgeResponse {
            return BridgeResponse(
                id = id,
                success = true,
                data = chunkData,
                streamMarker = marker,
                progress = progressInfo
            )
        }

        fun batch(id: String, responses: List<BridgeResponse>): BridgeResponse {
            return BridgeResponse(
                id = id,
                success = responses.all { it.success },
                batchResponses = responses
            )
        }
    }
}

