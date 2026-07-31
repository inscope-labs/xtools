package com.inscopelabs.abx.xtools

import android.app.Application
import com.inscopelabs.abx.xtools.bridge.api.BridgeApiFacade
import com.inscopelabs.abx.xtools.diagnostics.DiagnosticsInitializer
import com.inscopelabs.abx.xtools.diagnostics.GlobalExceptionHandler
import com.inscopelabs.abx.xtools.kernel.RuntimeKernel
import com.inscopelabs.abx.xtools.kernel.dispatcher.BridgeDispatcher
import com.inscopelabs.abx.xtools.kernel.dispatcher.handlers.DefaultHandlerRegistry
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
import com.inscopelabs.abx.xtools.plugin.catalog.CatalogApi
import com.inscopelabs.abx.xtools.plugin.catalog.CatalogCache
import com.inscopelabs.abx.xtools.plugin.catalog.RemoteCatalogService
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

    lateinit var bridgeApiFacade: BridgeApiFacade
        private set

    val catalogCache: CatalogCache by lazy { CatalogCache(this) }
    val catalogApi: CatalogApi by lazy {
        RemoteCatalogService(baseUrl = "https://catalog.xtools.inscopelabs.com")
    }

    val sessionManager: SessionManager get() = runtimeKernel.sessionManager
    val permissionManager: PermissionManager get() = runtimeKernel.permissionManager
    val bridgeDispatcher: BridgeDispatcher get() = runtimeKernel.bridgeDispatcher
    val modeArbiter: ModeArbiter get() = runtimeKernel.modeArbiter

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
        val eventBus = EventBus(CoroutineScope(Dispatchers.Default + SupervisorJob()))
        val pluginRegistry = PluginRegistry()
        val sessionManager = SessionManager()

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

        val permissionManager = PermissionManager(modeArbiter)
        val bridgeDispatcher = BridgeDispatcher(permissionManager)

        // Register default bridge action handlers
        DefaultHandlerRegistry.registerDefaultHandlers(bridgeDispatcher, this, modeArbiter)

        bridgeApiFacade = BridgeApiFacade(bridgeDispatcher)

        runtimeKernel = RuntimeKernel(
            sessionManager = sessionManager,
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

