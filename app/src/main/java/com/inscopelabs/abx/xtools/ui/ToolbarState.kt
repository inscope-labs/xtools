package com.inscopelabs.abx.xtools.ui

sealed class ToolbarState {
    object Branded : ToolbarState()
    data class Feature(val title: String) : ToolbarState()
}
