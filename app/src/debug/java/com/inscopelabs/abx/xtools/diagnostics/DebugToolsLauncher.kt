package com.inscopelabs.abx.xtools.diagnostics

import androidx.fragment.app.FragmentActivity

object DebugToolsLauncher {
    fun showLogViewer(activity: FragmentActivity) {
        LogViewerBottomSheet().show(activity.supportFragmentManager, "log_viewer_bottom_sheet")
    }
}
