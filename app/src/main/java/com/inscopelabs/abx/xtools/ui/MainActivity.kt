package com.inscopelabs.abx.xtools.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.inscopelabs.abx.xtools.ui.theme.XToolsTheme
import com.inscopelabs.abx.xtools.bridge.BridgeHandler
import com.inscopelabs.abx.xtools.bridge.JsBridge
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager
import com.inscopelabs.abx.xtools.plugin.manager.SecurityManager
import com.inscopelabs.abx.xtools.ui.XToolsApp

class MainActivity : ComponentActivity() {

    private lateinit var securityManager: SecurityManager
    private lateinit var bridgeHandler: BridgeHandler
    private lateinit var jsBridge: JsBridge
    private lateinit var pluginManager: PluginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        securityManager = SecurityManager(applicationContext)
        bridgeHandler = BridgeHandler(applicationContext, securityManager)
        jsBridge = JsBridge(bridgeHandler, lifecycleScope)
        pluginManager = PluginManager(applicationContext, securityManager)

        setContent {
            XToolsTheme {
                XToolsApp(
                    pluginManager = pluginManager,
                    securityManager = securityManager,
                    jsBridge = jsBridge,
                    bridgeHandler = bridgeHandler
                )
            }
        }
    }
}
