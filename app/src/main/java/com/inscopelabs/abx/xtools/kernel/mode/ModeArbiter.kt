package com.inscopelabs.abx.xtools.kernel.mode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Annotation marking components or calls that are not yet wired pending Phase 4 AIDL contract integration.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class NotYetWired(val reason: String = "")

/**
 * Interface representing the abx-sfm AIDL contract handshake.
 * Marked as NOT_YET_WIRED pending full AIDL IPC contract binding in Phase 4.
 */
@NotYetWired("Pending abx-sfm AIDL IPC contract binding in Phase 4")
interface AbxSfmAidlContract {
    fun validateSession(token: ModeArbiter.SessionToken): ValidationResult
}

/**
 * The Mode Arbiter is responsible for detecting whether a valid abx-server session
 * is present and switching the kernel between STANDALONE and GOVERNED modes.
 *
 * It is the SINGLE source of truth for the current operating mode.
 * No other component may determine mode independently.
 * Mode transitions are atomic and fail-closed.
 *
 * @see §1.2, §2.1 Step 1.1.2
 */
class ModeArbiter(
    private val sessionValidator: GovernanceSessionValidator,
    private val transitionEnforcer: ModeTransitionEnforcer
) {
    private val _currentMode = MutableStateFlow<OperatingMode>(OperatingMode.STANDALONE)
    val currentMode: StateFlow<OperatingMode> = _currentMode.asStateFlow()

    private val mutex = Mutex()

    /**
     * Called when a potential abx-server session is detected (e.g. via AIDL binding).
     * Validates the session and atomically switches to GOVERNED mode if valid.
     * Fails closed on invalid sessions or transition errors.
     */
    suspend fun validateAndSwitch(sessionToken: SessionToken): Boolean = mutex.withLock {
        try {
            val validationResult = sessionValidator.validate(sessionToken)
            if (validationResult.isValid) {
                transitionEnforcer.enforceTransition(
                    newMode = OperatingMode.GOVERNED,
                    sessionToken = sessionToken
                )
                _currentMode.value = OperatingMode.GOVERNED
                true
            } else {
                // Fail-closed: revert to STANDALONE mode
                transitionEnforcer.enforceTransition(
                    newMode = OperatingMode.STANDALONE,
                    sessionToken = null
                )
                _currentMode.value = OperatingMode.STANDALONE
                false
            }
        } catch (e: Exception) {
            // Fail-closed on any transition exception
            try {
                transitionEnforcer.enforceTransition(
                    newMode = OperatingMode.STANDALONE,
                    sessionToken = null
                )
            } catch (_: Exception) {}
            _currentMode.value = OperatingMode.STANDALONE
            false
        }
    }

    /**
     * Called when an active abx-server session ends or becomes invalid.
     * Atomically reverts to STANDALONE mode and cancels in-flight governed operations.
     */
    suspend fun invalidateAndRevert() = mutex.withLock {
        try {
            transitionEnforcer.enforceTransition(
                newMode = OperatingMode.STANDALONE,
                sessionToken = null
            )
        } finally {
            _currentMode.value = OperatingMode.STANDALONE
        }
    }

    data class SessionToken(val raw: String)
}

interface GovernanceSessionValidator {
    @NotYetWired("Stubbed session validation pending abx-sfm AIDL contract wiring")
    fun validate(token: ModeArbiter.SessionToken): ValidationResult
}

data class ValidationResult(val isValid: Boolean, val reason: String? = null)
