package com.inscopelabs.abx.xtools.kernel.mode

import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter.SessionToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enforces atomic, fail‑closed transitions between STANDALONE and GOVERNED modes.
 * Responsible for:
 * - Revoking live handles to standalone data layers when entering GOVERNED.
 * - Tearing down abx‑sfm AIDL handles when exiting GOVERNED.
 * - Cancelling in‑flight operations that straddle the transition.
 *
 * @see §5.1.5
 */
class ModeTransitionEnforcer(
    private val standaloneLayerManager: StandaloneLayerManager, // injected, to be implemented in Phase 4
    private val governedLayerManager: GovernedLayerManager    // injected, to be implemented in Phase 4
) {
    private val transitionMutex = Mutex()

    suspend fun enforceTransition(newMode: OperatingMode, sessionToken: SessionToken?) {
        transitionMutex.withLock {
            when (newMode) {
                OperatingMode.GOVERNED -> {
                    // 1. Revoke all live handles to standalone data layers.
                    standaloneLayerManager.revokeAllHandles(reason = "Entering Governed Mode")

                    // 2. Establish the AIDL contract with the provided session token.
                    governedLayerManager.establishContract(sessionToken!!)
                }
                OperatingMode.STANDALONE -> {
                    // 1. Tear down any active AIDL handles.
                    governedLayerManager.tearDownContract(reason = "Exiting Governed Mode")

                    // 2. Re‑enable standalone data layers (preserving previous enable‑switch state).
                    standaloneLayerManager.restoreAllHandles()
                }
            }
        }
    }
}

// Dummy interfaces – will be fleshed out in Phase 4
interface StandaloneLayerManager {
    fun revokeAllHandles(reason: String)
    fun restoreAllHandles()
}

interface GovernedLayerManager {
    fun establishContract(token: ModeArbiter.SessionToken)
    fun tearDownContract(reason: String)
}
