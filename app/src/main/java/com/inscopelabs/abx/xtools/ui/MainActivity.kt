package com.inscopelabs.abx.xtools.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.bridge.BridgeHandler
import com.inscopelabs.abx.xtools.bridge.JsBridge
import com.inscopelabs.abx.xtools.kernel.RuntimeKernel
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager
import com.inscopelabs.abx.xtools.plugin.manager.SecurityManager
import com.inscopelabs.abx.xtools.ui.feature.FeatureFragment
import com.inscopelabs.abx.xtools.ui.navigation.NavigationRouter
import kotlinx.coroutines.launch

/**
 * Main activity container for the xtools host UI.
 * Manages the dual-container layout (Plugins and Console) toggled via a pill mode switch
 * and coordinates child fragment transactions within each container.
 * Jetpack Compose is NOT used – everything is XML/Fragment-based.
 *
 * @see §3.1.1, §3.1.1 Step 2.1.1
 */
class MainActivity : AppCompatActivity() {

    private lateinit var runtimeKernel: RuntimeKernel
    private lateinit var navigationRouter: NavigationRouter

    private val toolbarViewModel: ToolbarStateViewModel by viewModels()

    private lateinit var securityManager: SecurityManager
    private lateinit var bridgeHandler: BridgeHandler
    private lateinit var jsBridge: JsBridge
    private lateinit var pluginManager: PluginManager

    private lateinit var toolbar: MaterialToolbar
    private lateinit var toolbarBrandedView: LinearLayout
    private lateinit var pluginsContainer: View
    private lateinit var consoleContainer: View
    private lateinit var pillModeSwitch: PillModeSwitch

    private var isPluginsActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // In a real implementation, this would be injected via DI or application context.
        runtimeKernel = (application as XToolsApplication).runtimeKernel

        securityManager = SecurityManager(applicationContext)
        bridgeHandler = BridgeHandler(applicationContext, securityManager)
        jsBridge = JsBridge(bridgeHandler, lifecycleScope)
        pluginManager = PluginManager.getInstance(applicationContext)

        toolbar = findViewById(R.id.toolbar)
        toolbarBrandedView = findViewById(R.id.toolbarBrandedView)
        pluginsContainer = findViewById(R.id.pluginsContainer)
        consoleContainer = findViewById(R.id.consoleContainer)
        pillModeSwitch = findViewById(R.id.pillModeSwitch)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.pluginsContainer, PluginsFragment(), TAG_PLUGINS)
                .add(R.id.consoleContainer, ConsoleFragment(), TAG_CONSOLE)
                .commitNow()
        }

        navigationRouter = NavigationRouter(this)
        navigationRouter.handleDeepLink(intent)

        applyTheme()

        setupContainerBackStackListeners()

        updateSwitchState(isPluginsActive)
        updateToolbarForCurrentContainer()

        pillModeSwitch.setOnToggleListener { active ->
            isPluginsActive = active
            pluginsContainer.isVisible = isPluginsActive
            consoleContainer.isVisible = !isPluginsActive
            updateSwitchState(isPluginsActive)
            updateToolbarForCurrentContainer()
        }

        observeToolbarState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!handleBackPress()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationRouter.handleDeepLink(intent)
    }

    private fun setupContainerBackStackListeners() {
        val pluginsFrag = supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        val consoleFrag = supportFragmentManager.findFragmentByTag(TAG_CONSOLE)

        pluginsFrag?.childFragmentManager?.addOnBackStackChangedListener {
            updateToolbarForCurrentContainer()
        }
        consoleFrag?.childFragmentManager?.addOnBackStackChangedListener {
            updateToolbarForCurrentContainer()
        }
    }

    private fun updateToolbarForCurrentContainer() {
        val activeFragment = if (isPluginsActive) {
            supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        } else {
            supportFragmentManager.findFragmentByTag(TAG_CONSOLE)
        }
        val childFm = activeFragment?.childFragmentManager
        if (childFm != null && childFm.backStackEntryCount > 0) {
            val topFragment = childFm.findFragmentById(R.id.childContainer)
            val title = (topFragment as? FeatureFragment)?.getFeatureTitle()
                ?: topFragment?.arguments?.getString("arg_feature_title")
                ?: if (topFragment is com.inscopelabs.abx.xtools.ui.plugindetail.PluginDetailFragment) "Plugin Details"
                else if (topFragment is com.inscopelabs.abx.xtools.ui.settings.AppearanceFragment) "Settings"
                else "Feature"
            toolbarViewModel.setToolbarState(ToolbarState.Feature(title))
        } else {
            toolbarViewModel.setToolbarState(ToolbarState.Branded)
        }
    }

    private fun updateSwitchState(pluginsActive: Boolean) {
        if (pluginsActive) {
            pillModeSwitch.setState(
                active = true,
                activeLabel = "Plugins",
                activeIcon = R.drawable.ic_plugins,
                inactiveIcon = R.drawable.ic_catalog
            )
        } else {
            pillModeSwitch.setState(
                active = false,
                activeLabel = "Console",
                activeIcon = R.drawable.ic_plugins,
                inactiveIcon = R.drawable.ic_catalog
            )
        }
    }

    private fun observeToolbarState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                toolbarViewModel.toolbarState.collect { state ->
                    when (state) {
                        is ToolbarState.Branded -> {
                            toolbarBrandedView.visibility = View.VISIBLE
                            toolbar.title = null
                            toolbar.navigationIcon = null
                        }
                        is ToolbarState.Feature -> {
                            toolbarBrandedView.visibility = View.GONE
                            toolbar.title = state.title
                            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                            toolbar.setNavigationOnClickListener {
                                handleBackPress()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleBackPress(): Boolean {
        val activeFragment = if (isPluginsActive) {
            supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        } else {
            supportFragmentManager.findFragmentByTag(TAG_CONSOLE)
        }
        val childFm = activeFragment?.childFragmentManager
        if (childFm != null && childFm.backStackEntryCount > 0) {
            val topFragment = childFm.findFragmentById(R.id.childContainer)
            if (topFragment is FeatureFragment && topFragment.webViewCanGoBack()) {
                if (navigationRouter.handleBackPressed(true)) {
                    topFragment.webViewGoBack()
                    return true
                }
            }
        }
        return popActiveBackStack()
    }

    private fun popActiveBackStack(): Boolean {
        val activeFragment = if (isPluginsActive) {
            supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        } else {
            supportFragmentManager.findFragmentByTag(TAG_CONSOLE)
        }
        val childFm = activeFragment?.childFragmentManager
        return if (childFm != null && childFm.backStackEntryCount > 0) {
            childFm.popBackStack()
            true
        } else {
            false
        }
    }

    private fun applyTheme() {
        // Delegates to ThemeManager for dynamic Material You colors.
        ThemeManager.applyTheme(this)
    }

    fun navigateToPluginDetail(pluginId: String) {
        val pluginsFrag = supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        val fragment = com.inscopelabs.abx.xtools.ui.plugindetail.PluginDetailFragment.newInstance(pluginId)
        pluginsFrag?.childFragmentManager?.beginTransaction()
            ?.replace(R.id.childContainer, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    fun navigateToSettings() {
        val pluginsFrag = supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
        val fragment = com.inscopelabs.abx.xtools.ui.settings.AppearanceFragment()
        pluginsFrag?.childFragmentManager?.beginTransaction()
            ?.replace(R.id.childContainer, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    companion object {
        private const val TAG_PLUGINS = "TAG_PLUGINS"
        private const val TAG_CONSOLE = "TAG_CONSOLE"
    }
}
