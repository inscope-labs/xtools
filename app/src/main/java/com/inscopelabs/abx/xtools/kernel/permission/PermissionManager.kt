package com.inscopelabs.abx.xtools.kernel.permission

import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter
import com.inscopelabs.abx.xtools.kernel.mode.NotYetWired
import com.inscopelabs.abx.xtools.kernel.mode.OperatingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for delegating permission checks to abx-server when in GOVERNED mode.
 * Marked as NOT_YET_WIRED pending AIDL client integration in Phase 4.
 */
@NotYetWired("Stubbed AIDL permission client for GOVERNED mode")
interface AbxSfmAidlPermissionClient {
    suspend fun authorizeInGovernedMode(pluginId: String, capability: String): Boolean
}

class DefaultAbxSfmAidlPermissionClient : AbxSfmAidlPermissionClient {
    @NotYetWired("Pending abx-sfm AIDL IPC contract implementation")
    override suspend fun authorizeInGovernedMode(pluginId: String, capability: String): Boolean {
        // Fail-safe default in GOVERNED mode until AIDL contract is live
        return false
    }
}

/**
 * Manages permission grants across STANDALONE and GOVERNED operating modes.
 *
 * In STANDALONE mode: consults local capability-based grants.
 * In GOVERNED mode: does NOT consult local grants, defers exclusively to abx-server via AIDL contract.
 *
 * @see §2.1 Step 1.1.2, §5.1.1
 */
class PermissionManager(
    private val modeArbiter: ModeArbiter? = null,
    private val aidlPermissionClient: AbxSfmAidlPermissionClient = DefaultAbxSfmAidlPermissionClient()
) {
    private val _grantedPermissions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val grantedPermissions: StateFlow<Map<String, Set<String>>> = _grantedPermissions.asStateFlow()

    private val _declaredPermissions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val declaredPermissions: StateFlow<Map<String, Set<String>>> = _declaredPermissions.asStateFlow()

    fun registerPluginDeclaredPermissions(pluginId: String, permissions: List<String>) {
        val current = _declaredPermissions.value.toMutableMap()
        current[pluginId] = permissions.toSet()
        _declaredPermissions.value = current
    }

    /**
     * Checks if a plugin is authorized for a given capability in the current mode.
     * - STANDALONE: verifies capability was declared in manifest AND granted locally. Fails safe.
     * - GOVERNED: defers directly to abx-server via AIDL contract; ignores local grants.
     */
    suspend fun isAuthorized(
        pluginId: String,
        capability: String,
        overrideMode: OperatingMode? = null
    ): Boolean {
        val currentMode = overrideMode ?: modeArbiter?.currentMode?.value ?: OperatingMode.STANDALONE
        val authorized = when (currentMode) {
            OperatingMode.STANDALONE -> {
                val declared = _declaredPermissions.value[pluginId]
                if (declared != null && !declared.contains(capability)) {
                    false
                } else {
                    _grantedPermissions.value[pluginId]?.contains(capability) ?: false
                }
            }
            OperatingMode.GOVERNED -> {
                // Defer to abx-server via AIDL contract without consulting local grants
                aidlPermissionClient.authorizeInGovernedMode(pluginId, capability)
            }
        }
        Logger.d("PermissionManager", "isAuthorized: pluginId=$pluginId, capability=$capability, mode=$currentMode -> authorized=$authorized")
        return authorized
    }

    fun grantPermission(pluginId: String, capability: String) {
        Logger.i("PermissionManager", "grantPermission: pluginId=$pluginId, capability=$capability")
        val current = _grantedPermissions.value.toMutableMap()
        val perms = current.getOrDefault(pluginId, emptySet()).toMutableSet()
        perms.add(capability)
        current[pluginId] = perms
        _grantedPermissions.value = current
    }

    fun revokePermission(pluginId: String, capability: String) {
        Logger.i("PermissionManager", "revokePermission: pluginId=$pluginId, capability=$capability")
        val current = _grantedPermissions.value.toMutableMap()
        current[pluginId] = current[pluginId]?.minus(capability) ?: emptySet()
        _grantedPermissions.value = current
    }

    fun clearPermissions(pluginId: String) {
        Logger.i("PermissionManager", "clearPermissions: pluginId=$pluginId")
        val current = _grantedPermissions.value.toMutableMap()
        current.remove(pluginId)
        _grantedPermissions.value = current
    }
}
