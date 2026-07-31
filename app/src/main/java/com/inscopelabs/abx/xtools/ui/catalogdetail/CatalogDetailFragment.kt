package com.inscopelabs.abx.xtools.ui.catalogdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.diagnostics.Logger
import com.inscopelabs.abx.xtools.plugin.catalog.CatalogApi
import com.inscopelabs.abx.xtools.plugin.lifecycle.InstallationPipeline
import kotlinx.coroutines.launch

/**
 * Displays detailed information about a plugin from the catalog.
 * Includes download and install actions.
 *
 * @see §4.1, §4.3
 */
class CatalogDetailFragment : Fragment() {

    private lateinit var pluginId: String
    private lateinit var pluginName: TextView
    private lateinit var pluginDescription: TextView
    private lateinit var pluginVersion: TextView
    private lateinit var installButton: Button

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
        return inflater.inflate(R.layout.fragment_catalog_detail, container, false).apply {
            pluginName = findViewById(R.id.tv_catalog_plugin_name)
            pluginDescription = findViewById(R.id.tv_catalog_plugin_description)
            pluginVersion = findViewById(R.id.tv_catalog_plugin_version)
            installButton = findViewById(R.id.btn_install)

            // Load details from CatalogApi
            pluginName.text = pluginId
            pluginDescription.text = "Loading details..."
            pluginVersion.text = ""

            lifecycleScope.launch {
                Logger.i("CatalogDetailFragment", "Fetching details for catalog pluginId '$pluginId'")
                val plugin = XToolsApplication.instance.catalogApi.getPluginDetails(pluginId)
                if (plugin != null) {
                    Logger.i("CatalogDetailFragment", "Fetched catalog plugin details for '${plugin.name}'")
                    pluginName.text = plugin.name
                    pluginDescription.text = plugin.description ?: "No description"
                    pluginVersion.text = "v${plugin.version}"
                } else {
                    Logger.w("CatalogDetailFragment", "Plugin not found in catalog for pluginId '$pluginId'")
                    pluginDescription.text = "Plugin not found"
                    installButton.isEnabled = false
                }
            }

            installButton.setOnClickListener {
                Logger.i("CatalogDetailFragment", "Install button clicked for catalog pluginId '$pluginId'")
                lifecycleScope.launch {
                    // install()
                }
            }
        }
    }

    companion object {
        private const val ARG_PLUGIN_ID = "plugin_id"

        @JvmStatic
        fun newInstance(pluginId: String) =
            CatalogDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLUGIN_ID, pluginId)
                }
            }
    }
}
