package com.inscopelabs.abx.xtools.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.xtools.plugin.Plugin
import com.inscopelabs.abx.xtools.plugin.manager.PluginManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginStoreScreen(
    pluginManager: PluginManager,
    onLaunchPlugin: (Plugin) -> Unit
) {
    val plugins by pluginManager.plugins.collectAsState()
    val activePlugin by pluginManager.activePlugin.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredPlugins = remember(plugins, searchQuery) {
        if (searchQuery.isBlank()) plugins
        else plugins.filter {
            it.manifest.name.contains(searchQuery, ignoreCase = true) ||
            it.manifest.description.contains(searchQuery, ignoreCase = true) ||
            it.manifest.id.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Plugin Manager & Store",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${plugins.size} installed web app modules",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("btn_add_custom_plugin")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Plugin")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search plugins or capabilities...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("plugin_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredPlugins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching plugins found.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredPlugins, key = { it.manifest.id }) { plugin ->
                    val isActive = activePlugin?.manifest?.id == plugin.manifest.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plugin_card_${plugin.manifest.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isActive) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (plugin.isBuiltIn) Icons.Default.Extension else Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = plugin.manifest.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "by ${plugin.manifest.author} • v${plugin.manifest.version}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = plugin.isEnabled,
                                    onCheckedChange = { pluginManager.togglePluginEnabled(plugin.manifest.id) },
                                    modifier = Modifier.testTag("switch_enable_${plugin.manifest.id}")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = plugin.manifest.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Permissions tags
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                plugin.manifest.permissions.forEach { perm ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = perm,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!plugin.isBuiltIn) {
                                    TextButton(
                                        onClick = { pluginManager.uninstallPlugin(plugin.manifest.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Button(
                                    onClick = {
                                        pluginManager.selectPlugin(plugin.manifest.id)
                                        onLaunchPlugin(plugin)
                                    },
                                    enabled = plugin.isEnabled,
                                    modifier = Modifier.testTag("btn_launch_${plugin.manifest.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isActive) "Active Runner" else "Run Plugin")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePluginDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, perms ->
                pluginManager.installSampleCustomPlugin(name, desc, perms)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreatePluginDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var permStorage by remember { mutableStateOf(true) }
    var permUi by remember { mutableStateOf(true) }
    var permSystem by remember { mutableStateOf(true) }
    var permHttp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Plugin Sandbox") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plugin Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Requested Bridge Permissions:", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permStorage, onCheckedChange = { permStorage = it })
                    Text("Storage (Encrypted Key-Value)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permUi, onCheckedChange = { permUi = it })
                    Text("UI (Toasts, Dialogs, Haptic)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permSystem, onCheckedChange = { permSystem = it })
                    Text("System (Hardware Info, SHA-256)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permHttp, onCheckedChange = { permHttp = it })
                    Text("HTTP (Sandboxed Proxy Fetch)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val perms = mutableListOf<String>()
                    if (permStorage) perms.add("storage")
                    if (permUi) perms.add("ui")
                    if (permSystem) perms.add("system")
                    if (permHttp) perms.add("http")
                    onCreate(name.ifBlank { "Custom Web Plugin" }, description.ifBlank { "Custom registered plugin harness" }, perms)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create & Launch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
