package com.inscopelabs.abx.xtools.ui

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
import com.inscopelabs.abx.xtools.bridge.BridgeHandler
import com.inscopelabs.abx.xtools.bridge.JsBridge
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager
import com.inscopelabs.abx.xtools.plugin.manager.SecurityManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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
                .commit()
        }

        updateSwitchState(isPluginsActive)

        pillModeSwitch.setOnToggleListener { active ->
            isPluginsActive = active
            pluginsContainer.isVisible = isPluginsActive
            consoleContainer.isVisible = !isPluginsActive
            updateSwitchState(isPluginsActive)
        }

        observeToolbarState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!popActiveBackStack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
                                popActiveBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun popActiveBackStack(): Boolean {
        val activeFragment = if (pluginsContainer.isVisible) {
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

    companion object {
        private const val TAG_PLUGINS = "TAG_PLUGINS"
        private const val TAG_CONSOLE = "TAG_CONSOLE"
    }
}
