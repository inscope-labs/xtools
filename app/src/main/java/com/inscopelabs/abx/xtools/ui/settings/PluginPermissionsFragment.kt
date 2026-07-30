package com.inscopelabs.abx.xtools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R

/**
 * Displays granted permissions per plugin.
 * Allows users to review and revoke permissions.
 *
 * @see §5.3.1
 */
class PluginPermissionsFragment : Fragment() {

    private lateinit var permissionListView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plugin_permissions, container, false).apply {
            permissionListView = findViewById(R.id.tv_permission_list)

            // Stub: show a placeholder list.
            permissionListView.text = "Plugin: SamplePlugin\n - storage.read\n - storage.write"
        }
    }
}
