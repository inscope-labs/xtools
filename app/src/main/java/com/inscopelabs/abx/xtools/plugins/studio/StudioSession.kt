package com.inscopelabs.abx.xtools.plugins.studio

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Process-wide session state for the currently-open project. Fragments
 * read this to know which project is loaded; the
 * [ProjectExplorerFragment] and the build pipeline mutate it.
 *
 * Held as a singleton because only one project is open at a time and
 * keeping the state out of the back stack avoids the "rotated and the
 * whole thing is gone" trap.
 */
object StudioSession {

    data class State(
        val projectRoot: File? = null,
        val manifest: PluginManifest? = null,
        val dirty: Boolean = false,
        val currentFile: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun open(root: File, manifest: PluginManifest) {
        _state.value = State(
            projectRoot = root,
            manifest = manifest,
            dirty = false,
            currentFile = manifest.entry,
        )
    }

    fun close() {
        _state.value = State()
    }

    fun markDirty() {
        if (!_state.value.dirty) {
            _state.value = _state.value.copy(dirty = true)
        }
    }

    fun markClean() {
        if (_state.value.dirty) {
            _state.value = _state.value.copy(dirty = false)
        }
    }

    fun setCurrentFile(path: String) {
        _state.value = _state.value.copy(currentFile = path)
    }
}
