package com.inscopelabs.abx.xtools.bridge.api

import com.inscopelabs.abx.xtools.bridge.BridgeRequest
import com.inscopelabs.abx.xtools.bridge.BridgeResponse
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher

/**
 * Explicit, narrow Kotlin-side facade interface exposing namespaced Bridge APIs to plugins.
 * Every call routes directly through BridgeDispatcher for permission checking, schema validation, and rate-limiting.
 */
class BridgeApiFacade(
    val dispatcher: BridgeDispatcher,
    val storage: StorageNamespaceApi = DefaultStorageNamespaceApi(dispatcher),
    val context: ContextNamespaceApi = DefaultContextNamespaceApi(dispatcher),
    val system: SystemNamespaceApi = DefaultSystemNamespaceApi(dispatcher)
) {
    /**
     * Direct execution route for custom or lower-level bridge requests.
     */
    suspend fun execute(pluginId: String, request: BridgeRequest): BridgeResponse {
        return dispatcher.dispatch(pluginId, request)
    }
}
