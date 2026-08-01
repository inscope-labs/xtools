package com.inscopelabs.abx.xtools.plugins.sdk

/**
 * Root marker for the Plugin SDK. Every public type intended to be consumed
 * by plugin authors lives under [com.inscopelabs.abx.xtools.plugins.sdk.api].
 *
 * This file intentionally contains no runtime code — it exists to make the
 * package discoverable in IDEs and to provide a single import for plugin
 * authors who want to be explicit about SDK coupling.
 */
object PluginSdk {
    /** Bump this when the bridge surface or manifest schema changes incompatibly. */
    const val VERSION: String = "0.1.0"

    /** Minimum plugin-manifest schema this SDK understands. */
    const val MIN_MANIFEST_SCHEMA: Int = 1

    /** Current plugin-manifest schema this SDK writes. */
    const val CURRENT_MANIFEST_SCHEMA: Int = 1
}
