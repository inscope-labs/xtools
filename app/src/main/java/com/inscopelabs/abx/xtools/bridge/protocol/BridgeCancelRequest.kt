package com.inscopelabs.abx.xtools.bridge.protocol

/**
 * Cancellation request for aborting an in-progress operation.
 */
data class BridgeCancelRequest(
    val targetRequestId: String,
    val pluginId: String = "",
    val reason: String? = null
)
