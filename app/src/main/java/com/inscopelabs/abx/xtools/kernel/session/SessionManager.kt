package com.inscopelabs.abx.xtools.kernel.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages plugin‑execution sessions (distinct from abx‑server sessions).
 * Each session has its own coroutine scope, enabling clean cancellation.
 *
 * @see §2.1 Step 1.1.2
 */
class SessionManager {
    private val _activeSessions = MutableStateFlow<Map<String, PluginSession>>(emptyMap())
    val activeSessions: StateFlow<Map<String, PluginSession>> = _activeSessions.asStateFlow()

    fun createSession(pluginId: String, scope: CoroutineScope): PluginSession {
        val sessionId = UUID.randomUUID().toString()
        val session = PluginSession(sessionId, pluginId, scope)
        _activeSessions.value = _activeSessions.value + (sessionId to session)
        return session
    }

    fun closeSession(sessionId: String) {
        _activeSessions.value = _activeSessions.value - sessionId
    }

    fun getSession(sessionId: String): PluginSession? = _activeSessions.value[sessionId]
}

data class PluginSession(
    val id: String,
    val pluginId: String,
    val coroutineScope: CoroutineScope
)
