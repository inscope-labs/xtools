package com.inscopelabs.abx.xtools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inscopelabs.abx.xtools.bridge.BridgeHandler
import com.inscopelabs.abx.xtools.bridge.JsBridge
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager
import com.inscopelabs.abx.xtools.plugin.manager.SecurityManager

enum class NavigationTab(val title: String) {
    RUNNER("Runner"),
    STORE("Plugins"),
    LOGS("Logs"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XToolsApp(
    pluginManager: PluginManager,
    securityManager: SecurityManager,
    jsBridge: JsBridge,
    bridgeHandler: BridgeHandler
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.RUNNER) }
    val activePlugin by pluginManager.activePlugin.collectAsState()

    // Keep bridgeHandler activePluginId synced with active plugin ID
    LaunchedEffect(activePlugin) {
        bridgeHandler.activePluginId = activePlugin?.manifest?.id ?: "system"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "xtools",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Kotlin-JS Bridge Platform",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bridge Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.RUNNER,
                    onClick = { selectedTab = NavigationTab.RUNNER },
                    icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Runner") },
                    label = { Text(NavigationTab.RUNNER.title) },
                    modifier = Modifier.testTag("tab_runner")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.STORE,
                    onClick = { selectedTab = NavigationTab.STORE },
                    icon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = "Store") },
                    label = { Text(NavigationTab.STORE.title) },
                    modifier = Modifier.testTag("tab_store")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.LOGS,
                    onClick = { selectedTab = NavigationTab.LOGS },
                    icon = { Icon(imageVector = Icons.Default.Terminal, contentDescription = "Logs") },
                    label = { Text(NavigationTab.LOGS.title) },
                    modifier = Modifier.testTag("tab_logs")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SETTINGS,
                    onClick = { selectedTab = NavigationTab.SETTINGS },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(NavigationTab.SETTINGS.title) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.RUNNER -> {
                    PluginRunnerScreen(
                        activePlugin = activePlugin,
                        jsBridge = jsBridge,
                        bridgeHandler = bridgeHandler,
                        onNavigateToStore = { selectedTab = NavigationTab.STORE }
                    )
                }
                NavigationTab.STORE -> {
                    PluginStoreScreen(
                        pluginManager = pluginManager,
                        onLaunchPlugin = { selectedTab = NavigationTab.RUNNER }
                    )
                }
                NavigationTab.LOGS -> {
                    ConsoleLogsScreen(bridgeHandler = bridgeHandler)
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(securityManager = securityManager)
                }
            }
        }
    }
}
