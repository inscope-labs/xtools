package com.inscopelabs.abx.xtools.kernel.mode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Mode Arbiter is responsible for detecting whether a valid abx‑server session
 * is present and switching the kernel between STANDALONE and GOVERNED modes.
 *
 * It is the single source of truth for the current operating mode.
 * No other component may bypass its current-mode determination.
 *
 * @see §1.2, §2.1 Step 1.1.2
 */
class ModeArbiter(
    private val sessionValidator: GovernanceSessionValidator, // to be implemented in Phase 4
    private val transitionEnforcer: ModeTransitionEnforcer
) {
    private val _currentMode = MutableStateFlow<OperatingMode>(OperatingMode.STANDALONE)
    val currentMode: StateFlow<OperatingMode> = _currentMode.asStateFlow()

    /**
     * Called when a potential abx‑server session is detected (e.g., via AIDL binding).
     * Validates the session and atomically switches to GOVERNED mode if valid.
     */
    suspend fun validateAndSwitch(sessionToken: SessionToken) {
        val validationResult = sessionValidator.validate(sessionToken)
        if (validationResult.isValid) {
            transitionEnforcer.enforceTransition(
                newMode = OperatingMode.GOVERNED,
                sessionToken = sessionToken
            )
            _currentMode.value = OperatingMode.GOVERNED
        } else {
            // Session invalid – stay in STANDALONE, log the failure.
            _currentMode.value = OperatingMode.STANDALONE
        }
    }

    /**
     * Called when an active abx‑server session ends or becomes invalid.
     * Atomically reverts to STANDALONE mode.
     */
    suspend fun invalidateAndRevert() {
        transitionEnforcer.enforceTransition(
            newMode = OperatingMode.STANDALONE,
            sessionToken = null
        )
        _currentMode.value = OperatingMode.STANDALONE
    }

    /**
     * Dummy session token – will be replaced by actual AIDL token in Phase 4.
     */
    data class SessionToken(val raw: String)
}

interface GovernanceSessionValidator {
    fun validate(token: ModeArbiter.SessionToken): ValidationResult
}

data class ValidationResult(val isValid: Boolean, val reason: String? = null)
