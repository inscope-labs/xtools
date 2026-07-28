package com.inscopelabs.abx.xtools.bridge

import com.google.gson.annotations.SerializedName

/**
 * BridgeResponse represents the response structure for bridge communication.
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

        fun error(id: String, error: String): BridgeResponse {
            return BridgeResponse(
                id = id,
                success = false,
                error = error
            )
        }
    }
}
