package com.inscopelabs.abx.xtools.kernel.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Interface for listening to plugin session lifecycle events.
 */
interface SessionLifecycleListener {
    fun onSessionStart(session: PluginSession)
    fun onSessionEnd(session: PluginSession)
}

/**
 * Manages plugin-execution sessions (distinct from abx-server sessions).
 * Each session has its own isolated coroutine scope, enabling clean cancellation.
 * Provides typed lifecycle callbacks (onStart/onEnd).
 *
 * @see §2.1 Step 1.1.2
 */
class SessionManager {
    private val _activeSessions = MutableStateFlow<Map<String, PluginSession>>(emptyMap())
    val activeSessions: StateFlow<Map<String, PluginSession>> = _activeSessions.asStateFlow()

    private val listeners = CopyOnWriteArrayList<SessionLifecycleListener>()

    fun addLifecycleListener(listener: SessionLifecycleListener) {
        listeners.add(listener)
    }

    fun removeLifecycleListener(listener: SessionLifecycleListener) {
        listeners.remove(listener)
    }

    fun createSession(pluginId: String, parentScope: CoroutineScope? = null): PluginSession {
        val sessionId = UUID.randomUUID().toString()
        val sessionScope = CoroutineScope(
            (parentScope?.coroutineContext ?: Dispatchers.Default) + SupervisorJob()
        )
        val session = PluginSession(
            id = sessionId,
            pluginId = pluginId,
            coroutineScope = sessionScope,
            startTimeMillis = System.currentTimeMillis()
        )
        _activeSessions.value = _activeSessions.value + (sessionId to session)
        listeners.forEach { it.onSessionStart(session) }
        return session
    }

    fun closeSession(sessionId: String) {
        val session = _activeSessions.value[sessionId]
        if (session != null) {
            session.coroutineScope.cancel("Plugin session closed")
            _activeSessions.value = _activeSessions.value - sessionId
            listeners.forEach { it.onSessionEnd(session) }
        }
    }

    fun closeAllSessions(reason: String = "Closing all plugin sessions") {
        val current = _activeSessions.value
        current.values.forEach { session ->
            session.coroutineScope.cancel(reason)
            listeners.forEach { it.onSessionEnd(session) }
        }
        _activeSessions.value = emptyMap()
    }

    fun getSession(sessionId: String): PluginSession? = _activeSessions.value[sessionId]

    fun getSessionsForPlugin(pluginId: String): List<PluginSession> =
        _activeSessions.value.values.filter { it.pluginId == pluginId }
}

data class PluginSession(
    val id: String,
    val pluginId: String,
    val coroutineScope: CoroutineScope,
    val startTimeMillis: Long = System.currentTimeMillis()
)
