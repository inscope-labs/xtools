package com.inscopelabs.abx.xtools

import android.app.Application
import com.inscopelabs.abx.xtools.diagnostics.DiagnosticsInitializer
import com.inscopelabs.abx.xtools.diagnostics.GlobalExceptionHandler
import com.inscopelabs.abx.xtools.kernel.RuntimeKernel
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import com.inscopelabs.abx.xtools.kernel.event.EventBus
import com.inscopelabs.abx.xtools.kernel.mode.GovernedLayerManager
import com.inscopelabs.abx.xtools.kernel.mode.GovernanceSessionValidator
import com.inscopelabs.abx.xtools.kernel.mode.ModeArbiter
import com.inscopelabs.abx.xtools.kernel.mode.ModeTransitionEnforcer
import com.inscopelabs.abx.xtools.kernel.mode.StandaloneLayerManager
import com.inscopelabs.abx.xtools.kernel.mode.ValidationResult
import com.inscopelabs.abx.xtools.kernel.permission.PermissionManager
import com.inscopelabs.abx.xtools.kernel.registry.PluginRegistry
import com.inscopelabs.abx.xtools.kernel.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * xtools Application class for global initialization.
 */
class XToolsApplication : Application() {

    lateinit var runtimeKernel: RuntimeKernel
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        ensureWebViewCacheDirs()

        // Diagnostics must come up before the exception handler,
        // since GlobalExceptionHandler calls Logger + CrashReporterManager.
        DiagnosticsInitializer.initialize(this)
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))

        initRuntimeKernel()
    }

    private fun initRuntimeKernel() {
        val permissionManager = PermissionManager()
        val eventBus = EventBus(CoroutineScope(Dispatchers.Default + SupervisorJob()))
        val pluginRegistry = PluginRegistry()
        val standaloneManager = object : StandaloneLayerManager {
            override fun revokeAllHandles(reason: String) {}
            override fun restoreAllHandles() {}
        }
        val governedManager = object : GovernedLayerManager {
            override fun establishContract(token: ModeArbiter.SessionToken) {}
            override fun tearDownContract(reason: String) {}
        }
        val transitionEnforcer = ModeTransitionEnforcer(standaloneManager, governedManager)
        val sessionValidator = object : GovernanceSessionValidator {
            override fun validate(token: ModeArbiter.SessionToken) = ValidationResult(false)
        }
        val modeArbiter = ModeArbiter(sessionValidator, transitionEnforcer)
        val bridgeDispatcher = BridgeDispatcher(permissionManager)

        runtimeKernel = RuntimeKernel(
            sessionManager = SessionManager(),
            permissionManager = permissionManager,
            eventBus = eventBus,
            pluginRegistry = pluginRegistry,
            modeArbiter = modeArbiter,
            bridgeDispatcher = bridgeDispatcher
        )
    }

    private fun ensureWebViewCacheDirs() {
        try {
            val codeCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(codeCacheDir, "js").mkdirs()
            File(codeCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: XToolsApplication
            private set
    }
}
