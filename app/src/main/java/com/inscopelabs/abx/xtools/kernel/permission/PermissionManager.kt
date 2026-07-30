package com.inscopelabs.abx.xtools.kernel.permission

import com.inscopelabs.abx.xtools.kernel.mode.OperatingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages permission grants in Standalone Mode.
 * In Governed Mode, it delegates all authorization decisions to abx‑server
 * and does NOT consult its local grants.
 *
 * @see §2.1 Step 1.1.2, §5.1.1
 */
class PermissionManager {
    private val _grantedPermissions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val grantedPermissions: StateFlow<Map<String, Set<String>>> = _grantedPermissions.asStateFlow()

    /**
     * Checks if a plugin is authorized for a given capability in the current mode.
     * - STANDALONE: consults local grants.
     * - GOVERNED: defers to abx‑server (stub for now, will call AbxSfmAidlClient in Phase 4).
     */
    suspend fun isAuthorized(pluginId: String, capability: String, currentMode: OperatingMode): Boolean {
        return when (currentMode) {
            OperatingMode.STANDALONE -> {
                _grantedPermissions.value[pluginId]?.contains(capability) ?: false
            }
            OperatingMode.GOVERNED -> {
                // TODO: Phase 4 – route to abx‑server via the AIDL contract.
                // For now, conservatively return false until the contract is live.
                false
            }
        }
    }

    fun grantPermission(pluginId: String, capability: String) {
        val current = _grantedPermissions.value.toMutableMap()
        val perms = current.getOrDefault(pluginId, emptySet()).toMutableSet()
        perms.add(capability)
        current[pluginId] = perms
        _grantedPermissions.value = current
    }

    fun revokePermission(pluginId: String, capability: String) {
        val current = _grantedPermissions.value.toMutableMap()
        current[pluginId] = current[pluginId]?.minus(capability) ?: emptySet()
        _grantedPermissions.value = current
    }
}
