package com.inscopelabs.abx.xtools.plugins.sdk.bridge.notify

/**
 * Surfaces a user-facing notification. Implemented in production by
 * [SystemTrayNotifier]; tests can drop in a recording stub.
 */
interface Notifier {
    fun show(title: String, body: String)
}
