package com.inscopelabs.abx.xtools.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolbarStateViewModel : ViewModel() {
    private val _toolbarState = MutableStateFlow<ToolbarState>(ToolbarState.Branded)
    val toolbarState: StateFlow<ToolbarState> = _toolbarState.asStateFlow()

    fun setToolbarState(state: ToolbarState) {
        _toolbarState.value = state
    }
}
