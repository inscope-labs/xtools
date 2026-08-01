package com.inscopelabs.abx.xtools.plugins.sdk.registry

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry
import com.inscopelabs.abx.xtools.plugins.sdk.api.RegisteredPlugin
import com.inscopelabs.abx.xtools.plugins.sdk.api.RegistryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Combines an in-memory [PluginRegistry] with a persistent
 * [PluginRepository]. The two are kept in sync on every mutation; the
 * repository is the source of truth across restarts.
 */
class RegistryFacade(
    private val memory: PluginRegistry,
    private val repository: PluginRepository,
) {

    private val _changes = MutableSharedFlow<RegistryEvent>(
        extraBufferCapacity = 64,
    )
    val changes: SharedFlow<RegistryEvent> = _changes.asSharedFlow()

    suspend fun boot() = withContext(Dispatchers.IO) {
        for (p in repository.load()) memory.install(p)
    }

    suspend fun install(plugin: RegisteredPlugin) = withContext(Dispatchers.IO) {
        memory.install(plugin)
        persist()
        _changes.tryEmit(RegistryEvent.Added(plugin.id))
    }

    suspend fun uninstall(id: PluginId): Boolean = withContext(Dispatchers.IO) {
        val removed = memory.uninstall(id)
        if (removed) {
            persist()
            _changes.tryEmit(RegistryEvent.Removed(id))
        }
        removed
    }

    fun all(): List<RegisteredPlugin> = memory.all()

    fun get(id: PluginId): RegisteredPlugin? = memory.get(id)

    private fun persist() {
        repository.save(memory.all())
    }
}
