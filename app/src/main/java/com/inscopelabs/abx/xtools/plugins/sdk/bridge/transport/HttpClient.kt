package com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport

import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeResponse

/**
 * Outbound HTTP capability for plugins. The implementation must enforce
 * the same permission-gating the bridge already does — but it is also
 * responsible for not letting a plugin abuse the connection pool, so a
 * timeout-bounded client is required.
 */
interface HttpClient {
    suspend fun execute(method: String, url: String, body: String?): BridgeResponse
}
