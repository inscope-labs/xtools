package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import com.inscopelabs.abx.xtools.plugins.sdk.api.Permission
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import java.util.concurrent.ConcurrentHashMap

/**
 * The single chokepoint that decides whether a bridge method may run.
 * Backed by the manifest's `permissions` array. Unknown permission strings
 * are resolved against the [Permission] catalog; un-catalogued strings
 * count as "not granted" (fail-closed).
 *
 * Implementations must be safe to call from any thread — the bridge
 * dispatches on [kotlinx.coroutines.Dispatchers.IO].
 */
interface PermissionGate {
    fun isGranted(permission: Permission): Boolean

    companion object {
        /**
         * Strict gate — only permissions explicitly listed in the manifest
         * are granted. This is the default per Stage 7.17's
         * "permission-gated bridge access" principle.
         */
        fun strict(manifest: PluginManifest): PermissionGate = StrictPermissionGate(manifest)
    }
}

private class StrictPermissionGate(manifest: PluginManifest) : PermissionGate {

    private val granted: Set<String> = manifest.permissions.toSet()

    // Cached resolved enums to avoid re-hashing the authority string on
    // every call. Bridge methods are hot.
    private val cache = ConcurrentHashMap<Permission, Boolean>()

    override fun isGranted(permission: Permission): Boolean =
        cache.getOrPut(permission) { granted.contains(permission.authority) }
}
