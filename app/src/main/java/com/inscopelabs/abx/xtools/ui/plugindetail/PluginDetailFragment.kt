package com.inscopelabs.abx.xtools.ui.plugindetail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.kernel.registry.PluginEntry
import com.inscopelabs.abx.xtools.kernel.registry.PluginState
import kotlinx.coroutines.launch

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
    private lateinit var tvPermissionsHeader: TextView
    private lateinit var permissionsContainer: LinearLayout
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
        val view = inflater.inflate(R.layout.fragment_plugin_detail, container, false)

        pluginName = view.findViewById(R.id.tv_plugin_name)
        pluginVersion = view.findViewById(R.id.tv_plugin_version)
        pluginDescription = view.findViewById(R.id.tv_plugin_description)
        tvPermissionsHeader = view.findViewById(R.id.tv_permissions_header)
        permissionsContainer = view.findViewById(R.id.permissionsContainer)
        activateButton = view.findViewById(R.id.btn_activate)
        uninstallButton = view.findViewById(R.id.btn_uninstall)

        loadPluginDetails()

        return view
    }

    private fun loadPluginDetails() {
        val app = XToolsApplication.instance
        val entry = app.pluginRegistry.getById(pluginId)

        if (entry == null) {
            pluginName.text = pluginId
            pluginVersion.text = ""
            pluginDescription.text = "Plugin not found"
            tvPermissionsHeader.visibility = View.GONE
            permissionsContainer.visibility = View.GONE
            activateButton.visibility = View.GONE
            uninstallButton.visibility = View.GONE
            return
        }

        bindEntryData(entry)
    }

    private fun bindEntryData(entry: PluginEntry) {
        val app = XToolsApplication.instance

        pluginName.text = entry.manifest.name
        pluginVersion.text = "v${entry.version}"
        pluginDescription.text = entry.manifest.description ?: "No description available"

        val isActive = entry.state == PluginState.ACTIVE
        activateButton.text = if (isActive) "Deactivate" else "Activate"
        activateButton.visibility = View.VISIBLE
        uninstallButton.visibility = View.VISIBLE

        activateButton.setOnClickListener {
            lifecycleScope.launch {
                val currentEntry = app.pluginRegistry.getById(pluginId) ?: return@launch
                val success = if (currentEntry.state == PluginState.ACTIVE) {
                    app.activationManager.deactivate(pluginId)
                } else {
                    app.activationManager.activate(pluginId)
                }
                if (success) {
                    val updatedEntry = app.pluginRegistry.getById(pluginId)
                    if (updatedEntry != null) {
                        bindEntryData(updatedEntry)
                    }
                }
            }
        }

        uninstallButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Uninstall plugin?")
                .setMessage("Are you sure you want to uninstall ${entry.manifest.name}?")
                .setPositiveButton("Uninstall") { _, _ ->
                    lifecycleScope.launch {
                        val success = app.uninstallManager.uninstall(pluginId, keepData = false)
                        if (success) {
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        buildPermissionsSection(entry)
    }

    private fun buildPermissionsSection(entry: PluginEntry) {
        permissionsContainer.removeAllViews()
        val permissions = entry.manifest.permissions

        if (permissions.isEmpty()) {
            tvPermissionsHeader.visibility = View.GONE
            permissionsContainer.visibility = View.GONE
            return
        }

        tvPermissionsHeader.visibility = View.VISIBLE
        permissionsContainer.visibility = View.VISIBLE

        val app = XToolsApplication.instance
        val grantedSet = app.permissionManager.grantedPermissions.value[pluginId] ?: emptySet()

        for (permission in permissions) {
            val switch = SwitchMaterial(requireContext()).apply {
                text = permission
                textSize = 14f
                isChecked = grantedSet.contains(permission)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        app.permissionManager.grantPermission(pluginId, permission)
                    } else {
                        app.permissionManager.revokePermission(pluginId, permission)
                    }
                }
            }
            permissionsContainer.addView(switch)
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
