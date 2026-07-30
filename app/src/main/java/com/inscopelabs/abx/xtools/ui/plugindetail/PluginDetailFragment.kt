package com.inscopelabs.abx.xtools.ui.plugindetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.kernel.registry.PluginRegistry

/**
 * Displays detailed information about a specific plugin.
 * Includes: name, description, version, permissions, and actions
 * (activate, deactivate, uninstall, configure settings).
 *
 * @see §3.1.1 Step 2.1.4, §4.3
 */
class PluginDetailFragment : Fragment() {

    private lateinit var pluginId: String
    private lateinit var pluginName: TextView
    private lateinit var pluginVersion: TextView
    private lateinit var pluginDescription: TextView
    private lateinit var activateButton: Button
    private lateinit var uninstallButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pluginId = it.getString(ARG_PLUGIN_ID) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plugin_detail, container, false).apply {
            pluginName = findViewById(R.id.tv_plugin_name)
            pluginVersion = findViewById(R.id.tv_plugin_version)
            pluginDescription = findViewById(R.id.tv_plugin_description)
            activateButton = findViewById(R.id.btn_activate)
            uninstallButton = findViewById(R.id.btn_uninstall)

            // Stub: load actual data from PluginRegistry when Phase 3 is complete.
            // For now, display placeholder content.
            pluginName.text = pluginId
            pluginVersion.text = "v1.0.0 (stub)"
            pluginDescription.text = "Plugin details will load here in Phase 3."

            activateButton.setOnClickListener {
                // TODO: Phase 3 – call ActivationManager.activate(pluginId)
            }

            uninstallButton.setOnClickListener {
                // TODO: Phase 3 – call UninstallManager.uninstall(pluginId)
            }
        }
    }

    companion object {
        private const val ARG_PLUGIN_ID = "plugin_id"

        @JvmStatic
        fun newInstance(pluginId: String) =
            PluginDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLUGIN_ID, pluginId)
                }
            }
    }
}
