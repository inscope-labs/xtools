package com.inscopelabs.abx.xtools.bridge.protocol

/**
 * Streaming response markers for long-running operations.
 */
enum class StreamMarker {
    NONE,
    START,
    CHUNK,
    PROGRESS,
    END
}

data class StreamProgressInfo(
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val percentage: Float = 0f,
    val statusMessage: String? = null
)
