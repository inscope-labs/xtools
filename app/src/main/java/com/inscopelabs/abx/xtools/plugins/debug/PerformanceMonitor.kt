package com.inscopelabs.abx.xtools.plugins.debug

import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeCallEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks aggregate metrics for the Studio's "Performance" panel.
 * Counters are kept in-memory only — the goal is a live readout, not
 * historical analytics.
 */
class PerformanceMonitor {

    data class Metrics(
        val bridgeCalls: Long,
        val bridgeErrors: Long,
        val bridgeAvgMs: Double,
        val bridgeMaxMs: Long,
        val uptimeMs: Long,
        val memoryUsedBytes: Long,
    )

    private val start = System.currentTimeMillis()
    private val calls = AtomicLong(0)
    private val errors = AtomicLong(0)
    private val totalMs = AtomicLong(0)
    private val maxMs = AtomicLong(0)

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<Metrics> = _state.asStateFlow()

    fun record(event: BridgeCallEvent) {
        calls.incrementAndGet()
        if (!event.allowed) errors.incrementAndGet()
        totalMs.addAndGet(event.durationMs)
        // CAS-loop max so we don't lose updates.
        var cur: Long
        do {
            cur = maxMs.get()
            if (event.durationMs <= cur) return
        } while (!maxMs.compareAndSet(cur, event.durationMs))
        push()
    }

    fun reset() {
        calls.set(0); errors.set(0); totalMs.set(0); maxMs.set(0)
        push()
    }

    private fun push() {
        _state.value = snapshot()
    }

    private fun snapshot(): Metrics {
        val n = calls.get().coerceAtLeast(1)
        return Metrics(
            bridgeCalls = calls.get(),
            bridgeErrors = errors.get(),
            bridgeAvgMs = totalMs.get().toDouble() / n,
            bridgeMaxMs = maxMs.get(),
            uptimeMs = System.currentTimeMillis() - start,
            memoryUsedBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() },
        )
    }
}
