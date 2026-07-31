package com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport

import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeResponse

interface HttpClient {
    fun execute(method: String, url: String, body: String?): BridgeResponse
}
