package com.inscopelabs.abx.xtools.kernel.event

import com.inscopelabs.abx.xtools.kernel.mode.OperatingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * A typed publish-subscribe event bus for inter-component and kernel communication.
 * Provides compile-time type-safe events for mode transitions, permission changes,
 * and plugin session lifecycle events.
 *
 * @see §2.1 Step 1.1.2, §6.3.1
 */
sealed class XEvent {
    abstract val timestampMillis: Long

    data class ModeTransition(
        val previousMode: OperatingMode,
        val newMode: OperatingMode,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : XEvent()

    data class PermissionChanged(
        val pluginId: String,
        val capability: String,
        val granted: Boolean,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : XEvent()

    data class LifecycleStart(
        val pluginId: String,
        val sessionId: String,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : XEvent()

    data class LifecycleEnd(
        val pluginId: String,
        val sessionId: String,
        val reason: String? = null,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : XEvent()
}

class EventBus(val scope: CoroutineScope) {
    private val _events = MutableSharedFlow<XEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<XEvent> = _events.asSharedFlow()

    suspend fun publish(event: XEvent) {
        _events.emit(event)
    }

    fun tryPublish(event: XEvent): Boolean {
        return _events.tryEmit(event)
    }

    /**
     * Subscribe to specific typed events in a compile-time safe manner.
     */
    inline fun <reified T : XEvent> subscribe(
        coroutineScope: CoroutineScope = scope,
        crossinline onEvent: suspend (T) -> Unit
    ) {
        coroutineScope.launch {
            events.filter { it is T }.collect { event ->
                onEvent(event as T)
            }
        }
    }
}
