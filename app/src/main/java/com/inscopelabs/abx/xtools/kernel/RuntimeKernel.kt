package com.inscopelabs.abx.xtools.kernel

import com.inscopelabs.abx.xtools.kernel.event.EventBus
import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter
import com.inscopelabs.abx.xtools.kernel.permission.PermissionManager
import com.inscopelabs.abx.xtools.kernel.registry.PluginRegistry
import com.inscopelabs.abx.xtools.kernel.session.SessionManager

/**
 * The Runtime Kernel is the foundational component that all other systems depend upon.
 * It manages the overall application lifecycle, provides services to the WebView
 * plugin environment, and is entirely self‑sufficient in Standalone Mode.
 *
 * @see §2.1 Step 1.1.2
 */
class RuntimeKernel(
    val sessionManager: SessionManager,
    val permissionManager: PermissionManager,
    val eventBus: EventBus,
    val pluginRegistry: PluginRegistry,
    val modeArbiter: ModeArbiter
) {
    // Additional lifecycle management, e.g., initialization and shutdown, can be added here.
    suspend fun initialize() {
        // Start session cleanup coroutines, pre‑warm registries, etc.
    }
}
