package com.inscopelabs.abx.xtools

import android.app.Application
import com.inscopelabs.abx.xtools.bridge.api.BridgeApiFacade
import com.inscopelabs.abx.xtools.diagnostics.CrashReporterManager
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
import com.inscopelabs.abx.xtools.bridge.manifest.ManifestParser
import com.inscopelabs.abx.xtools.bridge.manifest.PluginManifest
import com.inscopelabs.abx.xtools.kernel.registry.PluginState
import com.inscopelabs.abx.xtools.plugin.lifecycle.ActivationManager
import com.inscopelabs.abx.xtools.plugin.lifecycle.UninstallManager
import com.inscopelabs.abx.xtools.plugin.storage.PluginDirectoryManager
import com.inscopelabs.abx.xtools.plugin.storage.PluginMetadataStore
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
    val pluginRegistry: PluginRegistry get() = runtimeKernel.pluginRegistry
    val bridgeDispatcher: BridgeDispatcher get() = runtimeKernel.bridgeDispatcher
    val modeArbiter: ModeArbiter get() = runtimeKernel.modeArbiter

    val pluginMetadataStore: PluginMetadataStore by lazy { PluginMetadataStore(this) }
    val pluginDirectoryManager: PluginDirectoryManager by lazy { PluginDirectoryManager(this) }
    val activationManager: ActivationManager by lazy {
        ActivationManager(pluginRegistry, permissionManager, modeArbiter)
    }
    val uninstallManager: UninstallManager by lazy {
        UninstallManager(pluginRegistry, pluginMetadataStore, pluginDirectoryManager, permissionManager)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        ensureWebViewCacheDirs()

        // Global exception handler and crash reporter initialized before diagnostics.
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))
        CrashReporterManager.initialize(this)
        DiagnosticsInitializer.initialize(this)

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

        registerBundledSamplePlugins(pluginRegistry, permissionManager)
    }

    private fun registerBundledSamplePlugins(
        pluginRegistry: PluginRegistry,
        permissionManager: PermissionManager
    ) {
        val bundledPlugins = listOf("database", "sample", "system-info")
        val manifestParser = ManifestParser()

        for (dirName in bundledPlugins) {
            try {
                val json = assets.open("plugins/$dirName/plugin.json").bufferedReader().use { it.readText() }
                val manifest = manifestParser.parse(json)
                pluginRegistry.register(manifest, installationPath = dirName, category = "sample")
                pluginRegistry.updateState(manifest.id, PluginState.ACTIVE)
                permissionManager.registerPluginDeclaredPermissions(manifest.id, manifest.permissions)
                for (permission in manifest.permissions) {
                    permissionManager.grantPermission(manifest.id, permission)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

